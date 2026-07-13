package com.yu.tpm.service;

import java.util.List;
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
}
