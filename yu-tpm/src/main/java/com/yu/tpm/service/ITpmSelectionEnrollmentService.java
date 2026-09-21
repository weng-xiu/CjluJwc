package com.yu.tpm.service;

import java.util.List;
import java.util.Map;
import com.yu.common.core.domain.AjaxResult;
import com.yu.tpm.domain.TpmSelectionEnrollment;
import com.yu.tpm.domain.dto.ConflictWarning;
import com.yu.tpm.domain.dto.CourseSuggestion;

/**
 * 选课名单Service接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface ITpmSelectionEnrollmentService 
{
    public TpmSelectionEnrollment selectTpmSelectionEnrollmentByEnrollId(Long enrollId);
    public List<TpmSelectionEnrollment> selectTpmSelectionEnrollmentList(TpmSelectionEnrollment tpmSelectionEnrollment);
    public int insertTpmSelectionEnrollment(TpmSelectionEnrollment tpmSelectionEnrollment);
    public int updateTpmSelectionEnrollment(TpmSelectionEnrollment tpmSelectionEnrollment);
    public int deleteTpmSelectionEnrollmentByEnrollIds(Long[] enrollIds);
    public int deleteTpmSelectionEnrollmentByEnrollId(Long enrollId);

    /**
     * 检测选课冲突
     *
     * @param studentId        学生ID
     * @param courseOfferingId 开课ID
     * @param roundId          轮次ID
     * @return 冲突警告列表
     */
    public List<ConflictWarning> checkSelectionConflicts(Long studentId, Long courseOfferingId, Long roundId);

    /**
     * 获取替代课程建议
     *
     * @param studentId        学生ID
     * @param courseOfferingId 开课ID
     * @param roundId          轮次ID
     * @return 替代课程建议列表
     */
    public List<CourseSuggestion> getAlternativeCourses(Long studentId, Long courseOfferingId, Long roundId);

    /**
     * 带验证的选课（含冲突检测+Redis并发控制）
     *
     * @param studentId        学生ID
     * @param courseOfferingId 开课ID
     * @param roundId          轮次ID
     * @return 操作结果
     */
    public AjaxResult enrollWithValidation(Long studentId, Long courseOfferingId, Long roundId);

    /**
     * 执行抽签（超容量课程公平抽签）
     *
     * @param roundId 轮次ID
     * @return 抽签结果统计
     */
    public Map<String, Object> runLottery(Long roundId);

    /**
     * T6：执行抽签（可指定随机种子以支持结果复现审计）。
     *
     * @param roundId 轮次ID
     * @param seed    随机种子；为空时自动生成并记录到轮次
     * @return 抽签结果统计（含实际使用的种子）
     */
    public Map<String, Object> runLottery(Long roundId, Long seed);

    /**
     * T6：候补递补——当中签者退课释放容量时，按候补排名顺序将落选学生递补为中签。
     *
     * @param offeringId 开课ID
     * @return 递补结果统计
     */
    public Map<String, Object> promoteWaitlist(Long offeringId);

    /**
     * 学生退课：将结果状态置为退课并回补Redis容量
     *
     * @param enrollId 选课记录ID
     * @return 操作结果
     */
    public AjaxResult dropCourse(Long enrollId);
}
