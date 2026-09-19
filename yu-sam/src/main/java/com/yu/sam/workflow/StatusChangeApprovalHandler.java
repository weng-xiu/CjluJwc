package com.yu.sam.workflow;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import com.yu.sam.domain.SamStatusChange;
import com.yu.sam.domain.SamStudent;

/**
 * 学籍异动审批通过后的回写联动处理器（纯逻辑组件，无Spring/网络依赖，可离线单元测试）。
 *
 * 职责：
 * 1. 根据异动类型和审批结果，计算新的学籍状态
 * 2. 构建联动操作列表（选课冻结、成绩标记、缴费标记等）
 *
 * 异动类型：0休学 1复学 2转学 3退学 4保留学籍
 * 学籍状态：0在读 1休学 2退学 3毕业 4转出 5保留学籍
 */
@Component
public class StatusChangeApprovalHandler
{
    /** 异动类型 -> 目标学籍状态映射 */
    private static final Map<String, String> CHANGE_TO_STATUS = new HashMap<>();
    static
    {
        CHANGE_TO_STATUS.put("0", "1"); // 休学 -> 学籍状态1(休学)
        CHANGE_TO_STATUS.put("1", "0"); // 复学 -> 学籍状态0(在读)
        CHANGE_TO_STATUS.put("2", "4"); // 转学 -> 学籍状态4(转出)
        CHANGE_TO_STATUS.put("3", "2"); // 退学 -> 学籍状态2(退学)
        CHANGE_TO_STATUS.put("4", "5"); // 保留学籍 -> 学籍状态5(保留学籍)
    }

    /**
     * 计算审批通过后的目标学籍状态。
     *
     * @param changeType 异动类型
     * @param newStatusField SamStatusChange中显式指定的新状态（优先）
     * @return 目标学籍状态；null表示无法映射
     */
    public String resolveTargetStatus(String changeType, String newStatusField)
    {
        // 优先使用异动记录中显式填写的 newStatus
        if (newStatusField != null && !newStatusField.trim().isEmpty())
        {
            return newStatusField;
        }
        return CHANGE_TO_STATUS.get(changeType);
    }

    /**
     * 构建联动操作列表。
     * 审批通过后需要根据学籍变化执行不同的联动动作。
     *
     * @param student         学生信息
     * @param targetStatus    目标学籍状态
     * @return 联动操作描述列表（供Service层逐一执行）
     */
    public List<String> buildLinkageActions(SamStudent student, String targetStatus)
    {
        List<String> actions = new ArrayList<>();
        if (student == null || targetStatus == null)
        {
            return actions;
        }
        // 学籍状态 1休学/2退学/4转出/5保留学籍 -> 冻结选课、标记成绩
        boolean isFrozen = "1".equals(targetStatus) || "2".equals(targetStatus)
                || "4".equals(targetStatus) || "5".equals(targetStatus);
        if (isFrozen)
        {
            actions.add("FREEZE_ENROLLMENT:" + student.getStudentId());
            actions.add("FLAG_GRADE:" + student.getStudentId());
            actions.add("STOP_PAYMENT:" + student.getStudentId());
        }
        // 复学(状态变为0在读) -> 解冻
        if ("0".equals(targetStatus))
        {
            actions.add("UNFREEZE_ENROLLMENT:" + student.getStudentId());
            actions.add("RESUME_PAYMENT:" + student.getStudentId());
        }
        return actions;
    }

    /**
     * 校验异动类型和目标状态的合法性。
     *
     * @param change  异动记录
     * @return 错误信息；null表示合法
     */
    public String validate(SamStatusChange change)
    {
        if (change == null)
        {
            return "异动记录不能为空";
        }
        if (change.getStudentId() == null)
        {
            return "学生ID不能为空";
        }
        String changeType = change.getChangeType();
        if (changeType == null || !CHANGE_TO_STATUS.containsKey(changeType))
        {
            // 如果有显式 newStatus 也可以
            if (change.getNewStatus() == null || change.getNewStatus().trim().isEmpty())
            {
                return "无效的异动类型：" + changeType;
            }
        }
        return null; // ok
    }
}
