package com.yu.sam.service;

import java.util.Map;

/**
 * N6 学生数字档案全息画像服务（跨模块只读聚合）。
 *
 * @author N6
 */
public interface ISamStudentProfileService
{
    /**
     * 汇总某学生的全息数字档案：学籍、学业汇总、培养方案与学分结构、OBE达成度、
     * 学业预警、毕业/学位审核、毕业论文、近期成绩，以及各板块数据完备度。
     *
     * @param studentId 学生ID
     * @return 全息画像（只读视图）
     */
    Map<String, Object> buildProfile(Long studentId);
}
