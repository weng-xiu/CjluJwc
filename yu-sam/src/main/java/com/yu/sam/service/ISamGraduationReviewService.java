package com.yu.sam.service;

import java.util.List;
import java.util.Map;
import com.yu.sam.domain.SamGraduationReview;

public interface ISamGraduationReviewService 
{
    public SamGraduationReview selectSamGraduationReviewByReviewId(Long reviewId);
    public List<SamGraduationReview> selectSamGraduationReviewList(SamGraduationReview samGraduationReview);
    public int insertSamGraduationReview(SamGraduationReview samGraduationReview);
    public int updateSamGraduationReview(SamGraduationReview samGraduationReview);
    public int deleteSamGraduationReviewByReviewIds(Long[] reviewIds);
    public int deleteSamGraduationReviewByReviewId(Long reviewId);

    /**
     * 自动审核毕业资格
     * 自动校验学分、课程、英语、体育等条件
     *
     * @param studentId 学生ID
     * @return 审核结果
     */
    public SamGraduationReview autoReview(Long studentId);

    /**
     * F2 按版自动审核：应修学分口径取自方案版本快照（发布时刻定格），
     * 已修学分/不及格/英语/体育取实时成绩，支撑“政策变更后按旧版方案追溯核验”。
     *
     * @param studentId  学生ID
     * @param snapshotId 方案版本快照ID
     * @return 审核结果（已落库）
     */
    public SamGraduationReview autoReviewBySnapshot(Long studentId, Long snapshotId);

    /**
     * F2 按版复核联动：返回某学生所属已发布方案的历史版本快照清单（供前端下拉选择）。
     *
     * @param studentId 学生ID
     * @return 快照清单（无匹配方案或无快照时为空）
     */
    public List<Map<String, Object>> planSnapshotsForStudent(Long studentId);

    /**
     * 批量自动审核毕业资格
     *
     * @param studentIds 学生ID列表
     * @return 汇总结果
     */
    public Map<String, Object> batchAutoReview(List<Long> studentIds);

    /**
     * 学生自助毕业预审（S4，只读，不落库）
     * 分项返回：总学分达成、培养方案学分结构分项达成、课程/英语/体育合格情况、
     * 差距清单与预计结论，供学生门户自助查询。
     *
     * @param studentId 学生ID
     * @return 预审结果（只读视图）
     */
    public Map<String, Object> preReview(Long studentId);
}
