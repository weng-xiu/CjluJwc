package com.yu.tpm.plan;

import java.util.ArrayList;
import java.util.List;
import com.yu.tpm.domain.TpmCourseOffering;

/**
 * 开课计划批量生成结果（T4 生成器输出）。
 *
 * @author ruoyi
 */
public class OfferingGenerationResult
{
    /** 待插入的开课计划（均为待确认状态） */
    private final List<TpmCourseOffering> offerings = new ArrayList<>();

    /** 因已存在开课而被跳过的课程数 */
    private int skippedExisting;

    /** 成功预分配到教师的开课数 */
    private int teacherAssigned;

    public List<TpmCourseOffering> getOfferings() { return offerings; }

    public void add(TpmCourseOffering offering) { offerings.add(offering); }

    public int getSkippedExisting() { return skippedExisting; }
    public void incrementSkippedExisting() { this.skippedExisting++; }

    public int getTeacherAssigned() { return teacherAssigned; }
    public void incrementTeacherAssigned() { this.teacherAssigned++; }

    public int getGenerated() { return offerings.size(); }
}
