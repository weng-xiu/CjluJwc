package com.yu.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import com.yu.aem.domain.AemGradeReview;
import com.yu.aem.service.IAemGradeReviewService;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.common.exception.ServiceException;

/**
 * G4 批次2集成测试 · 成绩变更 Flowable 多级审批链路（V5.0 §7.2 G4 / O1 状态机）。
 *
 * <p>走真实装配：AEM 复核服务 + Flowable 7.1 引擎（内存流程实例落 ACT_* 表）+ MyBatis 落库。
 * 覆盖状态机 0待审/初审中 → 4待教务处终审 → 1通过/2驳回/3已撤销 的全部出口路径：
 * 初审+终审通过（终审回写成绩）、终审驳回、申请人撤销、以及重复提交/缺意见等守门口径。
 *
 * <p>调用口径说明：submitForApproval/approveReviewByFlow 依赖 SecurityUtils 登录上下文，
 * 本类以编程方式注入 admin（userId=1）的 LoginUser 到 SecurityContext，等价于控制器层
 * 已认证的调用方；HTTP 权限入口（@PreAuthorize）已由首批登录链路用例覆盖。
 *
 * <p>fixture：复用 testdata 97xx 段（成绩 9731 草稿 87 分），复核行使用 9782-9784。
 * 运行期增量（复核行、oa_process_instance、Flowable 运行时实例、通知待办/消息）由用例
 * 自清理复位；已完成流程实例的历史记录（ACT_HI_*）作为运行留痕保留，不做删除。
 */
class GradeReviewFlowIntegrationTest extends AbstractIntegrationTest
{
    private static final long GRADE_ID = 9731L;
    private static final long STUDENT_ID = 101L;
    /** 全链通过用例 */
    private static final long R_APPROVE = 9782L;
    /** 终审驳回用例 */
    private static final long R_REJECT = 9783L;
    /** 申请人撤销用例 */
    private static final long R_CANCEL = 9784L;
    /** 本类涉及的复核行范围（含首批 9781，保证互不残留） */
    private static final String ID_RANGE = "9781, 9782, 9783, 9784";
    private static final String PROCESS_KEY = "aem-grade-review-flow";

    @Autowired
    private IAemGradeReviewService gradeReviewService;

    @Autowired
    private RuntimeService runtimeService;

    @Autowired
    private RepositoryService repositoryService;

    @BeforeEach
    void seedAndLogin()
    {
        resetRunState();
        // 流程定义兜底：正常由 flowable starter 启动时自动部署 classpath:/processes/**
        if (repositoryService.createProcessDefinitionQuery()
                .processDefinitionKey(PROCESS_KEY).count() == 0)
        {
            repositoryService.createDeployment().name("g4-it-" + PROCESS_KEY)
                    .addClasspathResource("processes/aem-grade-review.bpmn20.xml").deploy();
        }
        // 同一成绩仅允许一笔在审申请，故复核行由各用例自行插入，seed 只复位环境
        loginAsAdmin();
    }

    @AfterEach
    void resetAndLogout()
    {
        resetRunState();
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("全链通过：提交进入初审 → 初审通过转终审 → 终审通过回写成绩并办结流程")
    void fullChainApproveWritesBackGrade()
    {
        insertPendingReview(R_APPROVE);
        // 提交进入流程
        assertEquals(1, gradeReviewService.submitForApproval(R_APPROVE));
        AemGradeReview review = gradeReviewService.selectAemGradeReviewByReviewId(R_APPROVE);
        String procInstId = review.getProcInstId();
        assertNotNull(procInstId, "提交后应挂接流程实例");
        assertEquals("0", review.getApproveStatus(), "提交后应处于初审中");

        // 流程实例运行中，当前活动任务为课程负责人初审
        ProcessInstance running = runtimeService.createProcessInstanceQuery()
                .processInstanceId(procInstId).singleResult();
        assertNotNull(running, "流程实例应运行中");
        Map<String, Object> detail = gradeReviewService.traceReview(R_APPROVE);
        List<Map<String, Object>> tasks = tasksOf(detail);
        assertEquals(1, tasks.size(), "提交后应只有初审任务");
        assertNull(tasks.get(0).get("endTime"), "初审任务应未完成");
        assertEquals("课程负责人初审", tasks.get(0).get("taskName"));

        // 初审通过 → 状态 4 待教务处终审，流转出终审任务
        assertEquals(1, gradeReviewService.approveReviewByFlow(R_APPROVE, true, "初审同意"));
        review = gradeReviewService.selectAemGradeReviewByReviewId(R_APPROVE);
        assertEquals("4", review.getApproveStatus());
        assertEquals("admin", review.getDeptApproveBy());
        detail = gradeReviewService.traceReview(R_APPROVE);
        tasks = tasksOf(detail);
        assertEquals(2, tasks.size(), "初审通过后应产生终审任务");
        assertEquals("教务处终审", tasks.get(1).get("taskName"));
        assertNull(tasks.get(1).get("endTime"));

        // 终审通过 → 状态 1，成绩回写 92 并置已审核，流程实例结束
        assertEquals(1, gradeReviewService.approveReviewByFlow(R_APPROVE, true, "终审同意"));
        review = gradeReviewService.selectAemGradeReviewByReviewId(R_APPROVE);
        assertEquals("1", review.getApproveStatus());
        assertEquals("admin", review.getAaApproveBy());
        assertGrade(92.00, "1");
        assertNull(runtimeService.createProcessInstanceQuery()
                .processInstanceId(procInstId).singleResult(), "办结后流程实例应结束");
        assertEquals("1", processStatusOf(procInstId), "oa_process_instance 应同步置通过");
    }

    @Test
    @DisplayName("终审驳回：状态置2，成绩保持原分未审核，流程实例结束")
    void finalRejectKeepsGradeAndEndsProcess()
    {
        insertPendingReview(R_REJECT);
        gradeReviewService.submitForApproval(R_REJECT);
        AemGradeReview review = gradeReviewService.selectAemGradeReviewByReviewId(R_REJECT);
        String procInstId = review.getProcInstId();

        gradeReviewService.approveReviewByFlow(R_REJECT, true, "初审同意");
        assertEquals(1, gradeReviewService.approveReviewByFlow(R_REJECT, false, "证据不足"));

        review = gradeReviewService.selectAemGradeReviewByReviewId(R_REJECT);
        assertEquals("2", review.getApproveStatus(), "终审驳回应置驳回");
        assertEquals("admin", review.getAaApproveBy());
        assertGrade(87.00, "0");
        assertNull(runtimeService.createProcessInstanceQuery()
                .processInstanceId(procInstId).singleResult());
        assertEquals("2", processStatusOf(procInstId));
    }

    @Test
    @DisplayName("申请人撤销：状态置3，流程实例删除，oa 关联置撤销")
    void cancelByApplicantTerminatesProcess()
    {
        insertPendingReview(R_CANCEL);
        gradeReviewService.submitForApproval(R_CANCEL);
        AemGradeReview review = gradeReviewService.selectAemGradeReviewByReviewId(R_CANCEL);
        String procInstId = review.getProcInstId();
        assertNotNull(procInstId);

        assertEquals(1, gradeReviewService.cancelByApplicant(R_CANCEL, "admin"));
        review = gradeReviewService.selectAemGradeReviewByReviewId(R_CANCEL);
        assertEquals("3", review.getApproveStatus(), "撤销应置已撤销");
        assertNull(runtimeService.createProcessInstanceQuery()
                .processInstanceId(procInstId).singleResult(), "撤销后运行时实例应删除");
        assertEquals("3", processStatusOf(procInstId));
        assertGrade(87.00, "0");
    }

    @Test
    @DisplayName("守门口径：重复提交被拒；驳回缺意见被拒且不流转；已挂流程单禁走旧单级审批")
    void guardRulesOfFlowMode()
    {
        insertPendingReview(R_APPROVE);
        gradeReviewService.submitForApproval(R_APPROVE);

        // 已挂流程不允许重复提交
        assertThrows(ServiceException.class,
                () -> gradeReviewService.submitForApproval(R_APPROVE));

        // 驳回未填意见：校验先于任务完成，状态不流转
        assertThrows(ServiceException.class,
                () -> gradeReviewService.approveReviewByFlow(R_APPROVE, false, null));
        AemGradeReview review = gradeReviewService.selectAemGradeReviewByReviewId(R_APPROVE);
        assertEquals("0", review.getApproveStatus(), "缺意见驳回失败后应仍在初审");
        List<Map<String, Object>> tasks = tasksOf(gradeReviewService.traceReview(R_APPROVE));
        assertEquals(1, tasks.size(), "失败驳回不应产生新任务");

        // 已接入流程的申请禁用旧单级审批入口
        ServiceException e = assertThrows(ServiceException.class,
                () -> gradeReviewService.approveReview(R_APPROVE, true, "admin", "越级"));
        assertTrue(e.getMessage().contains("流程"), "应提示改用流程入口");
    }

    // ========== 辅助 ==========

    private void resetRunState()
    {
        // 失败残留的运行中流程实例（businessKey=gradeReview:97xx）
        for (ProcessInstance pi : runtimeService.createProcessInstanceQuery().list())
        {
            if (pi.getBusinessKey() != null && pi.getBusinessKey().startsWith("gradeReview:97"))
            {
                runtimeService.deleteProcessInstance(pi.getId(), "G4集成测试复位");
            }
        }
        jdbc().update("delete from aem_grade_review where review_id in (" + ID_RANGE + ")");
        jdbc().update("delete from oa_process_instance where business_type = 'gradeReview' and business_id in (" + ID_RANGE + ")");
        jdbc().update("delete from sys_todo where business_type = 'gradeReview' and business_id in (" + ID_RANGE + ")");
        jdbc().update("delete from sys_message where business_type = 'gradeReview' and business_id in (" + ID_RANGE + ")");
        jdbc().update("update aem_grade_record set total_score = 87.00, is_reviewed = '0' where grade_id = ?", GRADE_ID);
    }

    private void insertPendingReview(long reviewId)
    {
        jdbc().update("insert into aem_grade_review (review_id, grade_id, student_id, course_id,"
                + " original_score, new_score, review_reason, review_type, approve_status, create_by, create_time)"
                + " values (?, ?, ?, 9301, 87.00, 92.00, 'G4批次2流程链路', '0', '0', 'admin', now())",
                reviewId, GRADE_ID, STUDENT_ID);
    }

    /** 模拟控制器层已认证的 admin（userId=1）调用方 */
    private void loginAsAdmin()
    {
        SysUser user = new SysUser();
        user.setUserId(1L);
        user.setUserName("admin");
        LoginUser loginUser = new LoginUser(1L, 103L, user, new HashSet<>());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities()));
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> tasksOf(Map<String, Object> detail)
    {
        assertNotNull(detail, "已挂流程的申请应可追溯流程详情");
        return (List<Map<String, Object>>) detail.get("tasks");
    }

    private String processStatusOf(String procInstId)
    {
        return jdbc().queryForObject(
                "select process_status from oa_process_instance where proc_inst_id = ?",
                String.class, procInstId);
    }

    private void assertGrade(double expectedScore, String expectedReviewed)
    {
        Map<String, Object> grade = jdbc().queryForMap(
                "select total_score, is_reviewed from aem_grade_record where grade_id = ?", GRADE_ID);
        assertEquals(0, new BigDecimal(String.valueOf(expectedScore))
                .compareTo((BigDecimal) grade.get("total_score")), "成绩应为 " + expectedScore);
        assertEquals(expectedReviewed, grade.get("is_reviewed"), "is_reviewed 应为 " + expectedReviewed);
    }
}
