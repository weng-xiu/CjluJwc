package com.yu.aem.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 评教统计Mapper接口（A6：多维统计与教师反馈报告）
 *
 * 说明：全部为只读聚合查询，JOIN 真实课程库/教师/班级/学期表，消除展示层硬编码。
 * 全局关闭了驼峰转换，故列名统一使用 as 别名输出 camelCase 键。
 *
 * @author ruoyi
 * @date 2026-09-21
 */
public interface AemEvaluationStatMapper
{
    /** 总览：评教人次、覆盖教师/课程数、平均分（按满分归一化百分比）、满意度（≥85%占比） */
    public Map<String, Object> statOverview(@Param("questionnaireId") Long questionnaireId,
                                            @Param("semesterId") Long semesterId);

    /** 分数段（百分比归一化）分布：优/良/中/及格/待改进 */
    public Map<String, Object> statDistribution(@Param("questionnaireId") Long questionnaireId,
                                                @Param("semesterId") Long semesterId);

    /** 按课程聚合（真实课程名），参与人次、平均分、最高/最低、满意度 */
    public List<Map<String, Object>> statByCourse(@Param("questionnaireId") Long questionnaireId,
                                                  @Param("semesterId") Long semesterId);

    /** 按教师聚合（真实教师名），参与人次、所授课程数、平均分 */
    public List<Map<String, Object>> statByTeacher(@Param("questionnaireId") Long questionnaireId,
                                                   @Param("semesterId") Long semesterId);

    /** 按班级聚合（经学生→班级映射），参与人次、平均分 */
    public List<Map<String, Object>> statByClass(@Param("questionnaireId") Long questionnaireId,
                                                 @Param("semesterId") Long semesterId,
                                                 @Param("courseId") Long courseId);

    /** 趋势：按评教月份（yyyy-MM）聚合人次与平均分 */
    public List<Map<String, Object>> statTrend(@Param("questionnaireId") Long questionnaireId);

    /** 某教师按课程拆分的明细（真实课程名+学期名），用于教师个人报告 */
    public List<Map<String, Object>> teacherCourseBreakdown(@Param("teacherId") Long teacherId);

    /** 某教师的分数段（百分比归一化）分布 */
    public Map<String, Object> teacherDistribution(@Param("teacherId") Long teacherId);

    /** 某教师的总体汇总（人次、平均分、覆盖课程数） */
    public Map<String, Object> teacherSummary(@Param("teacherId") Long teacherId);

    /** 查询评语列表（非空），供文本分析；teacherId 为空则查全部 */
    public List<String> selectComments(@Param("teacherId") Long teacherId,
                                       @Param("courseId") Long courseId,
                                       @Param("questionnaireId") Long questionnaireId);
}
