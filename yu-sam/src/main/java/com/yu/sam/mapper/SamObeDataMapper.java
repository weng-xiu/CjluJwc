package com.yu.sam.mapper;

import java.util.List;
import com.yu.sam.domain.dto.ObeCourseScore;

/**
 * OBE达成度成绩数据源Mapper（跨模块只读，JOIN aem_grade_record）。
 *
 * @author N1
 */
public interface SamObeDataMapper
{
    /**
     * 某学生各课程的最佳归一化得分（max(total_score)/100，仅取有效成绩）。
     */
    public List<ObeCourseScore> selectStudentCourseScores(Long studentId);

    /**
     * 群体各课程平均归一化得分（avg(total_score)/100，仅取有效成绩）。
     * semesterId 为空时统计全部学期。
     */
    public List<ObeCourseScore> selectCourseAverageScores(Long semesterId);

    /**
     * 课程ID → 课程名称（联表展示用）。
     */
    public List<java.util.Map<String, Object>> selectCourseNames();
}
