package com.yu.aem.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.aem.domain.AemGradeReview;

/**
 * 成绩复核审批Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface AemGradeReviewMapper 
{
    public AemGradeReview selectAemGradeReviewByReviewId(Long reviewId);
    public List<AemGradeReview> selectAemGradeReviewList(AemGradeReview aemGradeReview);
    public int insertAemGradeReview(AemGradeReview aemGradeReview);
    public int updateAemGradeReview(AemGradeReview aemGradeReview);
    public int deleteAemGradeReviewByReviewId(Long reviewId);
    public int deleteAemGradeReviewByReviewIds(Long[] reviewIds);

    /** 根据成绩ID删除复核记录 */
    public int deleteByGradeId(Long gradeId);

    /** 批量插入复核记录 */
    public int batchInsert(List<AemGradeReview> list);

    /** O1：取课程最近一次开课的教师登录名（课程负责人初审人候选，无则返回 null） */
    public String selectCourseTeacherLogin(@Param("courseId") Long courseId, @Param("semesterId") Long semesterId);

    /** O1：按学生ID取关联的系统用户ID（用于成绩变更结果通知学生） */
    public Long selectStudentUserId(Long studentId);
}
