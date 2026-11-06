package com.yu.aem.service;

import java.util.List;
import com.yu.aem.domain.AemGradeImportResult;
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
     * 自动校验：必填字段、分数范围0-100；总成绩为空时按权重计算；自动算绩点与等级。
     * 重复(学生+课程+学期+类型)口径：未提交(0)/已驳回(3) 覆盖为新值；
     * 已提交待审(1)/已锁定(2) 不覆盖，直接跳过并回显 Excel 行号。
     *
     * @param list           Excel解析后的成绩列表
     * @param operator       操作人
     * @param algorithmCode  GPA算法代码（空则用默认）
     * @return 导入结果（处理条数/跳过条数/跳过行号/数据库影响行数）
     */
    public AemGradeImportResult importGrade(List<AemGradeRecord> list, String operator, String algorithmCode);

    /** A5：教师提交成绩（批量） */
    public int submitGrade(Long[] gradeIds, String operator);

    /** A5：教研室审核成绩（批量）。approved=true 锁定，false 驳回可改 */
    public int auditGrade(Long[] gradeIds, boolean approved, String operator);

    /** A5：管理解锁已锁定成绩（异常纠正） */
    public int unlockGrade(Long[] gradeIds, String operator);

    /** A5：查询成绩录入开放期状态（供前端提示） */
    public java.util.Map<String, Object> getEntryWindowStatus();
}
