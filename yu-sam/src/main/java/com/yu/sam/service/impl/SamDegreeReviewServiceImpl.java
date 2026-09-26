package com.yu.sam.service.impl;

import java.util.Date;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.sam.mapper.SamDegreeReviewMapper;
import com.yu.sam.mapper.SamThesisMapper;
import com.yu.sam.mapper.SamWarningDataMapper;
import com.yu.sam.domain.SamDegreeReview;
import com.yu.sam.domain.SamDegreeConfig;
import com.yu.sam.domain.SamThesis;
import com.yu.sam.service.ISamDegreeReviewService;
import com.yu.sam.service.ISamDegreeConfigService;

@Service
public class SamDegreeReviewServiceImpl implements ISamDegreeReviewService
{
    private static final Logger log = LoggerFactory.getLogger(SamDegreeReviewServiceImpl.class);

    @Autowired
    private SamDegreeReviewMapper samDegreeReviewMapper;

    @Autowired
    private SamWarningDataMapper samWarningDataMapper;

    /** 毕业论文（设计）数据源：学位审核论文分项取 sam_thesis.is_qualified。 */
    @Autowired
    private SamThesisMapper samThesisMapper;

    /** S3：学位授予条件后台可配置服务（GPA、学位课程、外语、论文、学术成果）。 */
    @Autowired
    private ISamDegreeConfigService samDegreeConfigService;

    @Override
    public SamDegreeReview selectSamDegreeReviewByReviewId(Long reviewId) { return samDegreeReviewMapper.selectSamDegreeReviewByReviewId(reviewId); }
    @Override
    public List<SamDegreeReview> selectSamDegreeReviewList(SamDegreeReview samDegreeReview) { return samDegreeReviewMapper.selectSamDegreeReviewList(samDegreeReview); }
    @Override
    @Transactional
    public int insertSamDegreeReview(SamDegreeReview samDegreeReview) { samDegreeReview.setCreateTime(DateUtils.getNowDate()); return samDegreeReviewMapper.insertSamDegreeReview(samDegreeReview); }
    @Override
    @Transactional
    public int updateSamDegreeReview(SamDegreeReview samDegreeReview) { samDegreeReview.setUpdateTime(DateUtils.getNowDate()); return samDegreeReviewMapper.updateSamDegreeReview(samDegreeReview); }
    @Override
    @Transactional
    public int deleteSamDegreeReviewByReviewId(Long reviewId) { return samDegreeReviewMapper.deleteSamDegreeReviewByReviewId(reviewId); }
    @Override
    @Transactional
    public int deleteSamDegreeReviewByReviewIds(Long[] reviewIds) { return samDegreeReviewMapper.deleteSamDegreeReviewByReviewIds(reviewIds); }

    /**
     * 自动审核学位资格（S3：条件全部来自后台可配置的学位授予条件配置）
     * 校验条件：GPA达标、学位课程（必修）合格要求、外语课程合格要求、论文、学术成果。
     * 数据来源：跨模块查询 aem_grade_record + tpm_course_library；论文取 sam_thesis 归档结论。
     */
    @Override
    @Transactional
    public SamDegreeReview autoReview(Long studentId)
    {
        return doReview(studentId, true);
    }

    /**
     * 试算学位审核结果（不落库），供学生端预审与论文模块查看分项差距使用。
     */
    @Override
    public SamDegreeReview simulateReview(Long studentId)
    {
        return doReview(studentId, false);
    }

    private SamDegreeReview doReview(Long studentId, boolean persist)
    {
        SamDegreeConfig cfg = samDegreeConfigService.resolveEffective();
        double gpaThreshold = cfg.getGpaThreshold();

        // 查询学生GPA（全部学期累计加权）
        Double gpa = samWarningDataMapper.selectStudentGpa(studentId, null);
        // 学位课程（必修课）不及格门数
        Integer degreeFail = samWarningDataMapper.countDegreeCourseFail(studentId, null);
        // 外语课程（以课程名含"英语"代理判定）不及格门数
        Integer foreignFail = samWarningDataMapper.countFailByCourseNameKeyword(studentId, null, "英语");

        SamDegreeReview review = new SamDegreeReview();
        review.setStudentId(studentId);
        review.setGpa(gpa != null ? gpa : 0.0);

        // GPA校验
        boolean gpaQualified = gpa != null && gpa >= gpaThreshold;
        review.setIsGpaQualified(gpaQualified ? "1" : "0");

        // 学位课程校验（配置要求时必修须全部通过，否则视为合格）
        boolean degreeCourseQualified = !"1".equals(cfg.getRequireDegreeCourse())
                || degreeFail == null || degreeFail == 0;
        review.setIsDegreeCourseQualified(degreeCourseQualified ? "1" : "0");

        // 外语校验（配置要求时须无外语课程不及格）
        boolean foreignQualified = !"1".equals(cfg.getRequireForeignLanguage())
                || foreignFail == null || foreignFail == 0;

        // 论文校验：取毕业论文（设计）归档结论（Phase32 论文全过程管理）。
        //   requireThesis=0：不强制论文（保持历史口径）；
        //   requireThesis=1：必须有已归档且判定合格的论文记录，否则不通过并给出具体原因。
        SamThesis thesis = samThesisMapper.selectLatestByStudentId(studentId);
        boolean thesisRequired = "1".equals(cfg.getRequireThesis());
        boolean thesisQualified;
        String thesisReason = null;
        if (!thesisRequired)
        {
            thesisQualified = true;
        }
        else if (thesis == null)
        {
            thesisQualified = false;
            thesisReason = "该生无毕业论文（设计）档案";
        }
        else if (thesis.getArchiveTime() == null)
        {
            thesisQualified = false;
            thesisReason = "论文《" + thesis.getTopicName() + "》尚未完成成绩归档（当前环节未结束）";
        }
        else if (!"1".equals(thesis.getIsQualified()))
        {
            thesisQualified = false;
            thesisReason = "论文《" + thesis.getTopicName() + "》总评 " + thesis.getTotalScore() + " 分，结论不合格";
        }
        else
        {
            thesisQualified = true;
        }
        review.setIsThesisQualified(thesisQualified ? "1" : "0");

        // 学术成果校验：无数据源，requireAchievement=1 时判不合格并提示需人工确认
        boolean achievementQualified = !"1".equals(cfg.getRequireAchievement());

        // 综合审核结果
        boolean allQualified = gpaQualified && degreeCourseQualified && foreignQualified && thesisQualified && achievementQualified;
        review.setReviewStatus(allQualified ? "1" : "2");
        review.setReviewDate(new Date());
        review.setReviewer("系统自动审核");

        StringBuilder opinion = new StringBuilder();
        if (allQualified) {
            opinion.append("自动审核通过：GPA=").append(gpa).append("，满足配置["+cfg.getConfigName()+"]全部学位授予条件");
        } else {
            opinion.append("自动审核不通过：");
            if (!gpaQualified) opinion.append("GPA=").append(gpa != null ? gpa : "无").append("，未达到").append(gpaThreshold).append("要求；");
            if (!degreeCourseQualified) opinion.append("有").append(degreeFail).append("门学位课程（必修）未通过；");
            if (!foreignQualified) opinion.append("有").append(foreignFail).append("门外语课程未通过；");
            if (!thesisQualified) opinion.append(StringUtils.isNotEmpty(thesisReason) ? thesisReason + "；" : "论文未达要求；");
            if (!achievementQualified) opinion.append("要求学术成果但暂无数据源，需人工确认；");
        }
        review.setReviewOpinion(opinion.toString());
        review.setStatus("0");
        review.setCreateTime(DateUtils.getNowDate());

        if (persist)
        {
            samDegreeReviewMapper.insertSamDegreeReview(review);
        }
        log.info("学生[{}]学位资格{}完成：{}", studentId, persist ? "自动审核" : "预审试算", allQualified ? "通过" : "不通过");
        return review;
    }

    /**
     * 批量自动审核学位资格（S1）。
     */
    @Override
    @Transactional
    public Map<String, Object> batchAutoReview(List<Long> studentIds)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> details = new ArrayList<>();
        int total = 0, passed = 0, rejected = 0;
        if (studentIds != null) {
            for (Long sid : studentIds) {
                if (sid == null) continue;
                total++;
                try {
                    SamDegreeReview r = autoReview(sid);
                    if ("1".equals(r.getReviewStatus())) passed++; else rejected++;
                    Map<String, Object> d = new LinkedHashMap<>();
                    d.put("studentId", sid);
                    d.put("reviewStatus", r.getReviewStatus());
                    d.put("reviewOpinion", r.getReviewOpinion());
                    details.add(d);
                } catch (Exception e) {
                    rejected++;
                    log.error("学生[{}]学位审核异常", sid, e);
                    Map<String, Object> d = new LinkedHashMap<>();
                    d.put("studentId", sid);
                    d.put("reviewStatus", "error");
                    d.put("reviewOpinion", e.getMessage());
                    details.add(d);
                }
            }
        }
        result.put("total", total);
        result.put("passed", passed);
        result.put("rejected", rejected);
        result.put("details", details);
        log.info("学位批量审核完成：共{}，通过{}，不通过{}", total, passed, rejected);
        return result;
    }
}
