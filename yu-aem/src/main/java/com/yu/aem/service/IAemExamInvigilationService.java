package com.yu.aem.service;

import java.util.List;
import com.yu.aem.domain.AemExamInvigilation;

/**
 * 监考教师分配Service接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface IAemExamInvigilationService 
{
    public AemExamInvigilation selectAemExamInvigilationByInvigilationId(Long invigilationId);
    public List<AemExamInvigilation> selectAemExamInvigilationList(AemExamInvigilation aemExamInvigilation);
    public int insertAemExamInvigilation(AemExamInvigilation aemExamInvigilation);
    public int updateAemExamInvigilation(AemExamInvigilation aemExamInvigilation);
    public int deleteAemExamInvigilationByInvigilationIds(Long[] invigilationIds);
    public int deleteAemExamInvigilationByInvigilationId(Long invigilationId);

    /**
     * 自动派发监考教师
     * 智能分配监考，回避同一教师多场时间冲突
     *
     * @param examId 考试ID
     * @return 派发结果统计
     */
    public java.util.Map<String, Object> autoDispatchInvigilators(Long examId);

    /**
     * 批量导入监考安排
     *
     * @param list      监考记录列表
     * @param operator  操作人
     * @return 导入条数
     */
    public int importInvigilation(List<AemExamInvigilation> list, String operator);
}
