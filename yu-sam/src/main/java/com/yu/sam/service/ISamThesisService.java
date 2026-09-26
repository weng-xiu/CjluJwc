package com.yu.sam.service;

import java.util.List;
import java.util.Map;
import com.yu.sam.domain.SamThesis;
import com.yu.sam.domain.SamThesisProcess;

/**
 * 毕业论文（设计）全过程Service接口
 *
 * <p>环节状态机：1选题 → 2开题 → 3中期检查 → 4查重 → 5答辩 → 6成绩归档。
 * 每个环节由 stage_status 表达待提交/待审核/已通过/已退回，所有动作写入环节留痕表，
 * 归档后 is_qualified 供学位审核论文分项使用。</p>
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public interface ISamThesisService
{
    /** 环节：选题 */
    public static final String STAGE_TOPIC = "1";
    /** 环节：开题 */
    public static final String STAGE_PROPOSAL = "2";
    /** 环节：中期检查 */
    public static final String STAGE_MIDTERM = "3";
    /** 环节：查重 */
    public static final String STAGE_CHECK = "4";
    /** 环节：答辩 */
    public static final String STAGE_DEFENSE = "5";
    /** 环节：成绩归档 */
    public static final String STAGE_ARCHIVE = "6";

    /** 环节状态：待提交 */
    public static final String ST_PENDING = "0";
    /** 环节状态：待审核 */
    public static final String ST_AUDITING = "1";
    /** 环节状态：已通过 */
    public static final String ST_PASSED = "2";
    /** 环节状态：已退回 */
    public static final String ST_REJECTED = "3";

    public SamThesis selectSamThesisByThesisId(Long thesisId);

    public List<SamThesis> selectSamThesisList(SamThesis samThesis);

    public int insertSamThesis(SamThesis samThesis);

    public int updateSamThesis(SamThesis samThesis);

    public int deleteSamThesisByThesisIds(Long[] thesisIds);

    /** 查询论文档案（含环节留痕明细） */
    public SamThesis selectDetailWithProcess(Long thesisId);

    /** 查询某生某届论文档案（不存在返回 null） */
    public SamThesis selectByStudentAndYear(Long studentId, String planYear);

    /** 查询某生最近一届论文档案（学位审核使用） */
    public SamThesis selectLatestByStudentId(Long studentId);

    /**
     * 学生选题：校验题目可用与名额，建立论文档案并进入开题环节。
     *
     * @param studentId 学生ID
     * @param topicId   选题库题目ID
     * @param operator  操作人登录名（学生本人）
     * @return 新建的论文档案
     */
    public SamThesis chooseTopic(Long studentId, Long topicId, String operator);

    /**
     * 学生提交环节材料（开题报告、中期报告、答辩材料），提交后进入待审核。
     */
    public int submitStage(Long thesisId, String stage, String title, String content, String attachment, String operator);

    /**
     * 环节审核：通过则推进到下一环节，退回则置为已退回等待学生重新提交。
     *
     * @param stage 被审核的环节，必须与论文当前环节一致
     */
    public int auditStage(Long thesisId, String stage, boolean pass, Double score, String opinion, String operator);

    /**
     * 登记查重结果：重复率不高于阈值即判定达标并推进到答辩环节，否则退回修改。
     */
    public int recordCheck(Long thesisId, Double checkRate, String attachment, String opinion, String operator);

    /**
     * 成绩归档：按总评成绩折算等级与合格结论，作为学位审核论文分项依据。
     */
    public int archiveGrade(Long thesisId, Double totalScore, Double defenseScore, String opinion, String operator);

    /**
     * 抽检状态维护：送检、回填抽检结果；抽检不合格将撤回合格结论。
     */
    public int markSample(Long thesisId, String sampleStatus, String opinion, String operator);

    /** 环节留痕列表 */
    public List<SamThesisProcess> selectProcessByThesisId(Long thesisId);

    /** 过程统计：汇总指标 + 各环节进度 */
    public Map<String, Object> statSummary(SamThesis samThesis);
}
