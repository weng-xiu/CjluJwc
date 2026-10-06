package com.yu.dis.quality;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yu.dis.domain.DisSyncTask;

/**
 * N2 质量看板：同步任务成功率/失败源聚合的单元验证。
 *
 * @author N2
 */
@ExtendWith(MockitoExtension.class)
class DisQualityServiceTest
{
    @InjectMocks
    private DisQualityService service;

    private DisSyncTask task(long id, String name, int exec, int fail)
    {
        DisSyncTask t = new DisSyncTask();
        t.setTaskId(id);
        t.setTaskName(name);
        t.setExecuteCount(exec);
        t.setFailCount(fail);
        return t;
    }

    @Test
    @DisplayName("汇总成功率与最高失败源")
    void aggregatesSuccessRateAndWorst()
    {
        List<DisSyncTask> tasks = List.of(
                task(1L, "学籍同步", 100, 5),   // 5% fail
                task(2L, "成绩同步", 100, 30)); // 30% fail -> worst

        DisQualityVo vo = service.compute(tasks, 1_700_000_000_000L);

        assertEquals(2, vo.getTaskCount());
        assertEquals(200, vo.getTotalExecute());
        assertEquals(35, vo.getTotalFail());
        assertEquals(82.5, vo.getSuccessRate(), 0.001);
        assertEquals("成绩同步", vo.getWorstTaskName());
        assertEquals(30.0, vo.getWorstFailRate(), 0.001);
    }

    @Test
    @DisplayName("空任务列表安全返回")
    void emptySafe()
    {
        DisQualityVo vo = service.compute(List.of(), 1_700_000_000_000L);
        assertEquals(0, vo.getTaskCount());
        assertEquals(0, vo.getTotalExecute());
    }

    @Test
    @DisplayName("执行数为零不误判成功率")
    void zeroExecute()
    {
        DisQualityVo vo = service.compute(List.of(task(9L, "空跑", 0, 0)), 0L);
        assertEquals(100.0, vo.getSuccessRate(), 0.001);
        assertEquals(0.0, vo.getTasks().get(0).getFailRate(), 0.001);
    }
}
