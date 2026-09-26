package com.yu.oa.workflow.service;

import java.util.List;
import java.util.Map;

import com.yu.oa.workflow.domain.OaCountersignItem;

/**
 * 通用工作流协同服务（加签 / 会签 / 委托）
 *
 * 设计要点：协同动作不改变引擎的流转职责，最终提交仍由各业务模块经
 * {@link IOaWorkflowService#completeTask} 完成；本服务在 completeTask 处设置门禁，
 * 因此对已接入 Flowable 的公文、学籍异动、教室借用、成绩变更四类流程统一生效，
 * 不会出现"引擎已推进但业务状态未回写"的断裂。
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public interface IOaCountersignService
{
    /** 动作类型：前加签 */
    String MODE_PRE_SIGN = "0";

    /** 动作类型：后加签 */
    String MODE_POST_SIGN = "1";

    /** 动作类型：会签 */
    String MODE_COUNTER_SIGN = "2";

    /** 动作类型：委托代办 */
    String MODE_DELEGATE = "3";

    /** 表决规则：全部同意 */
    String RULE_ALL = "ALL";

    /** 表决规则：一人同意即决 */
    String RULE_ANY = "ANY";

    /**
     * 加签（征询意见）
     *
     * @param taskId    任务ID
     * @param operator  操作人（当前办理人）
     * @param mode      0 前加签（意见未齐不可提交）/ 1 后加签（不阻塞本人提交，意见事后归档）
     * @param handlers  被加签人登录名集合
     * @param reason    加签说明
     * @return 批次ID
     */
    public Long addSign(String taskId, String operator, String mode, List<String> handlers, String reason);

    /**
     * 发起会签：多人并行表决，意见齐备前原办理人不可提交
     *
     * @param rule ALL 全部同意 / ANY 一人同意即决
     */
    public Long counterSign(String taskId, String operator, List<String> handlers, String rule, String reason);

    /**
     * 委托代办：任务转交他人办理，原办理人保留为 owner，办结后自动知会
     */
    public Long delegate(String taskId, String operator, String handler, String reason);

    /**
     * 收回委托：原办理人将任务收回自己办理
     */
    public void reclaimDelegate(String taskId, String operator, String reason);

    /**
     * 被加签/会签人提交意见
     */
    public void submitOpinion(Long itemId, String operator, boolean agree, String opinion);

    /**
     * 我的协同待办（待我表态的加签/会签/委托）
     */
    public List<OaCountersignItem> listMyPending(String handler);

    /**
     * 按流程实例查询协同留痕
     */
    public List<OaCountersignItem> listByProcessInstance(String processInstanceId);

    /**
     * 可选协同办理人列表
     */
    public List<Map<String, Object>> listHandlerOptions();

    /**
     * completeTask 门禁：存在未回复的前加签/会签意见时拒绝提交
     */
    public void assertGatePassed(String taskId);

    /**
     * 任务办结后的协同收尾（委托知会原办理人、未表态意见作废并通知）
     */
    public void afterTaskCompleted(String taskId, String operator);

    /**
     * 流程实例被终止时取消进行中的协同批次
     */
    public void cancelByProcessInstance(String processInstanceId);
}
