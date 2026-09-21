package com.yu.aem.service;

import java.util.List;
import com.yu.aem.domain.AemGradeRecord;

/**
 * 成绩记录Service接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface IAemGradeRecordService 
{
    public AemGradeRecord selectAemGradeRecordByGradeId(Long gradeId);

    /** 查询成绩记录明细（含复核记录子表） */
    public AemGradeRecord selectAemGradeRecordDetail(Long gradeId);

    public List<AemGradeRecord> selectAemGradeRecordList(AemGradeRecord aemGradeRecord);

    /** 门户端：学生个人成绩列表（不套用部门数据范围，附带课程名/学分/学期名） */
    public List<AemGradeRecord> selectAemGradeRecordListForPortal(AemGradeRecord aemGradeRecord);
    public int insertAemGradeRecord(AemGradeRecord aemGradeRecord);
    public int updateAemGradeRecord(AemGradeRecord aemGradeRecord);
    public int deleteAemGradeRecordByGradeIds(Long[] gradeIds);
    public int deleteAemGradeRecordByGradeId(Long gradeId);

    /** 计算学生某学期GPA */
    public Double calculateStudentGpa(Long studentId, Long semesterId, String algorithmCode);

    /** 批量重算某学期所有学生GPA */
    public void batchRecalculateGpa(Long semesterId, String algorithmCode);

    /**
     * 批量导入成绩（Excel）。
     * 自动校验：必填字段、分数范围0-100；按平时30%+考试70%计算总成绩（若总成绩为空）；
     * 按默认GPA算法计算绩点与等级；重复(学生+课程+学期+类型)执行更新。
     *
     * @param list           Excel解析后的成绩列表
     * @param operator       操作人
     * @param algorithmCode  GPA算法代码（空则用默认）
     * @return 成功导入条数
     */
    public int importGrade(List<AemGradeRecord> list, String operator, String algorithmCode);

    /** A5：教师提交成绩（批量） */
    public int submitGrade(Long[] gradeIds, String operator);

    /** A5：教研室审核成绩（批量）。approved=true 锁定，false 驳回可改 */
    public int auditGrade(Long[] gradeIds, boolean approved, String operator);

    /** A5：管理解锁已锁定成绩（异常纠正） */
    public int unlockGrade(Long[] gradeIds, String operator);

    /** A5：查询成绩录入开放期状态（供前端提示） */
    public java.util.Map<String, Object> getEntryWindowStatus();
}
