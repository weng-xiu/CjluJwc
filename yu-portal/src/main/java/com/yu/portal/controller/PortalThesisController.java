package com.yu.portal.controller;

import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.SecurityUtils;
import com.yu.sam.domain.SamStudent;
import com.yu.sam.domain.SamThesis;
import com.yu.sam.domain.SamThesisTopic;
import com.yu.sam.service.ISamDegreeReviewService;
import com.yu.sam.service.ISamStudentService;
import com.yu.sam.service.ISamThesisService;
import com.yu.sam.service.ISamThesisTopicService;

/**
 * 毕业论文（设计）门户Controller
 *
 * <p>学生侧：可选题列表、自主选题、环节材料提交、本人过程与学位预审结果查询；
 * 教师侧：名下论文列表、环节审核、查重登记与成绩归档。所有写操作均校验归属，
 * 学生只能操作本人档案，审核仅放开给指导教师本人或持有 sam:thesis:audit 的教务角色。</p>
 *
 * @author ruoyi
 * @date 2026-09-26
 */
@RestController
@RequestMapping("/portal/thesis")
public class PortalThesisController extends BaseController
{
    @Autowired
    private ISamThesisService samThesisService;

    @Autowired
    private ISamThesisTopicService samThesisTopicService;

    @Autowired
    private ISamStudentService samStudentService;

    @Autowired
    private ISamDegreeReviewService samDegreeReviewService;

    /** 可选题题目列表（仅返回可选题状态） */
    @PreAuthorize("@ss.hasPermi('portal:thesis:list')")
    @GetMapping("/topics")
    public AjaxResult topics(SamThesisTopic query)
    {
        query.setStatus("1");
        List<SamThesisTopic> list = samThesisTopicService.selectSamThesisTopicList(query);
        return success(list);
    }

    /** 学生选题 */
    @PreAuthorize("@ss.hasPermi('portal:thesis:submit')")
    @PostMapping("/choose")
    public AjaxResult choose(@RequestBody ThesisAction action)
    {
        SamStudent mine = requireMyStudent();
        SamThesis thesis = samThesisService.chooseTopic(mine.getStudentId(), action.getTopicId(), getUsername());
        return success(thesis);
    }

    /** 本人论文档案（含环节留痕） */
    @PreAuthorize("@ss.hasPermi('portal:thesis:list')")
    @GetMapping("/my")
    public AjaxResult my()
    {
        SamStudent mine = requireMyStudent();
        SamThesis query = new SamThesis();
        query.setStudentId(mine.getStudentId());
        List<SamThesis> list = samThesisService.selectSamThesisList(query);
        if (list.isEmpty())
        {
            return success(null);
        }
        return success(samThesisService.selectDetailWithProcess(list.get(0).getThesisId()));
    }

    /** 学生提交环节材料（开题、中期、答辩） */
    @PreAuthorize("@ss.hasPermi('portal:thesis:submit')")
    @PostMapping("/submit")
    public AjaxResult submit(@RequestBody ThesisAction action)
    {
        SamThesis thesis = requireOwnedThesis(action.getThesisId());
        int rows = samThesisService.submitStage(thesis.getThesisId(), action.getStage(), action.getTitle(),
                action.getContent(), action.getAttachment(), getUsername());
        return toAjax(rows);
    }

    /** 指导教师名下论文列表（教务管理员可见全部，按分页返回） */
    @PreAuthorize("@ss.hasPermi('portal:thesis:audit')")
    @GetMapping("/advisorList")
    public TableDataInfo advisorList(SamThesis query)
    {
        if (!isThesisManager())
        {
            query.setAdvisor(getUsername());
        }
        startPage();
        List<SamThesis> list = samThesisService.selectSamThesisList(query);
        return getDataTable(list);
    }

    /** 论文档案详情（含环节留痕）：仅限本人、指导教师或教务查看 */
    @PreAuthorize("@ss.hasPermi('portal:thesis:list')")
    @GetMapping("/detail/{thesisId}")
    public AjaxResult detail(@PathVariable("thesisId") Long thesisId)
    {
        SamThesis thesis = samThesisService.selectSamThesisByThesisId(thesisId);
        if (thesis == null)
        {
            return error("论文档案不存在");
        }
        if (!getUsername().equals(thesis.getAdvisor()) && !isThesisManager() && !isMyThesis(thesis))
        {
            return error("无权查看该论文档案");
        }
        return success(samThesisService.selectDetailWithProcess(thesisId));
    }

    /** 环节审核（开题、中期检查、答辩） */
    @PreAuthorize("@ss.hasPermi('portal:thesis:audit')")
    @PostMapping("/audit")
    public AjaxResult audit(@RequestBody ThesisAction action)
    {
        SamThesis thesis = requireAuditableThesis(action.getThesisId());
        int rows = samThesisService.auditStage(thesis.getThesisId(), action.getStage(),
                Boolean.TRUE.equals(action.getPass()), action.getScore(), action.getOpinion(), getUsername());
        return toAjax(rows);
    }

    /** 查重结果登记 */
    @PreAuthorize("@ss.hasPermi('portal:thesis:audit')")
    @PostMapping("/check")
    public AjaxResult check(@RequestBody ThesisAction action)
    {
        SamThesis thesis = requireAuditableThesis(action.getThesisId());
        int rows = samThesisService.recordCheck(thesis.getThesisId(), action.getScore(),
                action.getAttachment(), action.getOpinion(), getUsername());
        return toAjax(rows);
    }

    /** 成绩归档 */
    @PreAuthorize("@ss.hasPermi('portal:thesis:audit')")
    @PostMapping("/archive")
    public AjaxResult archive(@RequestBody ThesisAction action)
    {
        SamThesis thesis = requireAuditableThesis(action.getThesisId());
        int rows = samThesisService.archiveGrade(thesis.getThesisId(), action.getTotalScore(),
                action.getDefenseScore(), action.getOpinion(), getUsername());
        return toAjax(rows);
    }

    /** 学生端：结合论文结论的学位资格预审（只读试算，不落库） */
    @PreAuthorize("@ss.hasPermi('portal:thesis:list')")
    @GetMapping("/degreePreview")
    public AjaxResult degreePreview()
    {
        SamStudent mine = requireMyStudent();
        return success(samDegreeReviewService.simulateReview(mine.getStudentId()));
    }

    /** 解析本人学籍：先按 user_id 关联，再兼容 student_id==user_id 口径 */
    private SamStudent requireMyStudent()
    {
        SamStudent mine = samStudentService.selectSamStudentByUserId(getUserId());
        if (mine == null)
        {
            mine = samStudentService.selectSamStudentByStudentId(getUserId());
        }
        if (mine == null)
        {
            throw new ServiceException("当前登录账号未关联学籍信息，请联系教务管理员");
        }
        return mine;
    }

    /** 学生归属校验：只能操作本人论文档案 */
    private SamThesis requireOwnedThesis(Long thesisId)
    {
        SamThesis thesis = thesisId == null ? null : samThesisService.selectSamThesisByThesisId(thesisId);
        if (thesis == null)
        {
            throw new ServiceException("论文档案不存在");
        }
        SamStudent mine = requireMyStudent();
        if (!mine.getStudentId().equals(thesis.getStudentId()))
        {
            throw new ServiceException("无权操作他人的论文档案");
        }
        return thesis;
    }

    /** 审核权限校验：仅指导教师本人或教务角色可审核 */
    private SamThesis requireAuditableThesis(Long thesisId)
    {
        SamThesis thesis = thesisId == null ? null : samThesisService.selectSamThesisByThesisId(thesisId);
        if (thesis == null)
        {
            throw new ServiceException("论文档案不存在");
        }
        if (!getUsername().equals(thesis.getAdvisor()) && !isThesisManager())
        {
            throw new ServiceException("仅论文指导教师本人或教务管理员可执行该操作");
        }
        return thesis;
    }

    /** 是否为学生本人档案（学生查看详情时使用） */
    private boolean isMyThesis(SamThesis thesis)
    {
        try
        {
            return requireMyStudent().getStudentId().equals(thesis.getStudentId());
        }
        catch (ServiceException e)
        {
            return false;
        }
    }

    /** 是否具备论文管理端审核权限（教务、学院管理员等按菜单授权自动生效） */
    private boolean isThesisManager()
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        SysUser user = loginUser == null ? null : loginUser.getUser();
        if (user == null)
        {
            return false;
        }
        if (SecurityUtils.isAdmin(user.getUserId()))
        {
            return true;
        }
        return loginUser.getPermissions() != null
                && (loginUser.getPermissions().contains("*:*:*") || loginUser.getPermissions().contains("sam:thesis:audit"));
    }

    /**
     * 门户端论文动作入参
     */
    public static class ThesisAction
    {
        private Long thesisId;
        private Long topicId;
        private String stage;
        private String title;
        private String content;
        private String attachment;
        private Boolean pass;
        private Double score;
        private Double defenseScore;
        private Double totalScore;
        private String opinion;

        public Long getThesisId() { return thesisId; }
        public void setThesisId(Long thesisId) { this.thesisId = thesisId; }
        public Long getTopicId() { return topicId; }
        public void setTopicId(Long topicId) { this.topicId = topicId; }
        public String getStage() { return stage; }
        public void setStage(String stage) { this.stage = stage; }
        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public String getAttachment() { return attachment; }
        public void setAttachment(String attachment) { this.attachment = attachment; }
        public Boolean getPass() { return pass; }
        public void setPass(Boolean pass) { this.pass = pass; }
        public Double getScore() { return score; }
        public void setScore(Double score) { this.score = score; }
        public Double getDefenseScore() { return defenseScore; }
        public void setDefenseScore(Double defenseScore) { this.defenseScore = defenseScore; }
        public Double getTotalScore() { return totalScore; }
        public void setTotalScore(Double totalScore) { this.totalScore = totalScore; }
        public String getOpinion() { return opinion; }
        public void setOpinion(String opinion) { this.opinion = opinion; }
    }
}
