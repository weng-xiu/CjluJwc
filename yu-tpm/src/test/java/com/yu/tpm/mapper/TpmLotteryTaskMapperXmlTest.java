package com.yu.tpm.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.ibatis.builder.xml.XMLMapperBuilder;
import org.apache.ibatis.io.Resources;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.session.Configuration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.tpm.domain.TpmLotteryTask;

/**
 * T-5：抽签任务表 SQL 的结构测试（不连数据库）。
 *
 * <p>状态机是异步任务正确性的根：一旦某条语句丢掉了「旧状态谓词」，就退化成 read-then-write——
 * 两个执行者会同时认为自己抢到任务。这里用 MyBatis 自身解析器取 BoundSql，把每条迁移语句的
 * WHERE 谓词锁死；同时锁住「生成列 active_round 不得出现在任何 DML 里」（否则插入直接报错）。</p>
 */
class TpmLotteryTaskMapperXmlTest
{
    private static final String NS = "com.yu.tpm.mapper.TpmLotteryTaskMapper.";

    private static Configuration configuration;

    @BeforeAll
    static void parseMapperXml() throws Exception
    {
        configuration = new Configuration();
        configuration.getTypeAliasRegistry().registerAlias("TpmLotteryTask", TpmLotteryTask.class);
        String resource = "mapper/tpm/TpmLotteryTaskMapper.xml";
        try (InputStream in = Resources.getResourceAsStream(resource))
        {
            new XMLMapperBuilder(in, configuration, resource, configuration.getSqlFragments()).parse();
        }
    }

    private BoundSql boundSqlOf(String statementId, Object parameter)
    {
        MappedStatement ms = configuration.getMappedStatement(NS + statementId);
        return ms.getBoundSql(parameter);
    }

    private Map<String, Object> params(Object... keyValues)
    {
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < keyValues.length; i += 2)
        {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    private String normalize(String sql)
    {
        return sql.replaceAll("\\s+", " ").trim();
    }

    @Test
    @DisplayName("markRunning：只允许 '0' 待执行 → '1' 执行中（旧状态谓词不可丢）")
    void markRunning_requiresPendingState()
    {
        String sql = normalize(boundSqlOf("markRunning", params("taskId", 1L, "startTime", new Date())).getSql());

        assertTrue(sql.contains("set task_status = '1'"), sql);
        assertTrue(sql.contains("where task_id = ? and task_status = '0' and del_flag = '0'"), sql);
    }

    @Test
    @DisplayName("updateProgress：只在执行中推进进度，并带回 total 快照")
    void updateProgress_onlyWhileRunning()
    {
        String sql = normalize(boundSqlOf("updateProgress",
                params("taskId", 1L, "processedCount", 500, "totalCount", 1200, "admittedCount", 400, "rejectedCount", 100))
                .getSql());

        assertTrue(sql.contains("total_count = ?"), sql);
        assertTrue(sql.contains("processed_count = ?"), sql);
        assertTrue(sql.contains("where task_id = ? and task_status = '1' and del_flag = '0'"), sql);
    }

    @Test
    @DisplayName("markSucceeded：只从执行中切换，成功后清空上次残留的失败原因")
    void markSucceeded_requiresRunningState()
    {
        TpmLotteryTask task = new TpmLotteryTask();
        task.setTaskId(1L);
        task.setLotterySeed(4242L);
        task.setLotteryCourses(3);
        task.setTotalCount(1200);
        task.setProcessedCount(1200);
        task.setAdmittedCount(900);
        task.setRejectedCount(300);
        task.setEndTime(new Date());
        String sql = normalize(boundSqlOf("markSucceeded", task).getSql());

        assertTrue(sql.contains("set task_status = '2'"), sql);
        assertTrue(sql.contains("error_msg = null"), "重试成功后必须清掉上一次的失败原因：" + sql);
        assertTrue(sql.contains("where task_id = ? and task_status = '1' and del_flag = '0'"), sql);
    }

    @Test
    @DisplayName("markFailed：待执行与执行中都可判失败（线程池拒绝发生在 '0' 状态）")
    void markFailed_allowsPendingAndRunning()
    {
        String sql = normalize(boundSqlOf("markFailed",
                params("taskId", 1L, "errorMsg", "队列已满", "endTime", new Date())).getSql());

        assertTrue(sql.contains("set task_status = '3'"), sql);
        assertTrue(sql.contains("where task_id = ? and task_status in ('0','1') and del_flag = '0'"), sql);
    }

    @Test
    @DisplayName("insert：不得写生成列 active_round（写了直接报错）")
    void insert_mustNotWriteGeneratedColumn()
    {
        TpmLotteryTask task = new TpmLotteryTask();
        task.setRoundId(1L);
        task.setTaskStatus(TpmLotteryTask.STATUS_PENDING);
        task.setCreateTime(new Date());
        String sql = normalize(boundSqlOf("insertTpmLotteryTask", task).getSql());

        assertFalse(sql.contains("active_round"), "active_round 是生成列，禁止出现在 insert 列清单里：" + sql);
        assertTrue(sql.contains("del_flag"), sql);
    }

    @Test
    @DisplayName("update 语句一律不得触碰生成列 active_round")
    void updates_mustNotWriteGeneratedColumn()
    {
        String running = normalize(boundSqlOf("markRunning", params("taskId", 1L, "startTime", new Date())).getSql());
        String progress = normalize(boundSqlOf("updateProgress",
                params("taskId", 1L, "processedCount", 0, "totalCount", 0, "admittedCount", 0, "rejectedCount", 0)).getSql());
        String failed = normalize(boundSqlOf("markFailed",
                params("taskId", 1L, "errorMsg", "x", "endTime", new Date())).getSql());

        assertFalse(running.contains("active_round"), running);
        assertFalse(progress.contains("active_round"), progress);
        assertFalse(failed.contains("active_round"), failed);
    }

    @Test
    @DisplayName("查询：按任务号与轮次最近一条，均过滤逻辑删除")
    void selects_filterDeleted()
    {
        String byId = normalize(boundSqlOf("selectTpmLotteryTaskByTaskId", 1L).getSql());
        String latest = normalize(boundSqlOf("selectLatestTaskByRoundId", 1L).getSql());

        assertTrue(byId.contains("where t.task_id = ? and t.del_flag = '0'"), byId);
        assertTrue(latest.contains("order by t.task_id desc"), latest);
        assertEquals(1, byId.split("del_flag = '0'", -1).length - 1, "按主键查询只应有一处 del_flag 过滤");
    }
}
