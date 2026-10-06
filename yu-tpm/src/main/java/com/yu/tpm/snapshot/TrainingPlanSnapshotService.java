package com.yu.tpm.snapshot;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import org.springframework.stereotype.Service;
import com.yu.tpm.domain.TpmTrainingPlan;

/**
 * 培养方案版本快照服务（N2 方案版本快照）。
 *
 * <p>对方案内容计算稳定指纹：仅纳入影响毕业审核的实质字段（专业、层次、年份、总学分、
 * 版本号、发布状态），忽略 remark/updateTime 等非实质字段，保证同一实质内容得到相同指纹，
 * 任一实质字段变化则指纹改变，从而支撑"按旧版追溯"。为纯计算逻辑，无外部依赖、可单测。
 *
 * @author N2
 */
@Service
public class TrainingPlanSnapshotService
{
    /** 生成方案快照（含内容指纹）。 */
    public PlanSnapshot snapshot(TpmTrainingPlan plan)
    {
        if (plan == null)
        {
            return null;
        }
        return new PlanSnapshot(plan.getPlanId(), plan.getVersion(), fingerprint(plan), new Date());
    }

    /**
     * 判断方案实质内容相较既有指纹是否发生变化。
     *
     * @param plan       当前方案
     * @param priorHash  历史快照指纹
     * @return true 表示内容已变更，需要生成新版本快照
     */
    public boolean isChanged(TpmTrainingPlan plan, String priorHash)
    {
        if (plan == null)
        {
            return false;
        }
        return !fingerprint(plan).equals(priorHash);
    }

    /** 计算方案内容指纹（SHA-256，十六进制小写）。 */
    public String fingerprint(TpmTrainingPlan plan)
    {
        String canonical = String.join("|",
                nvl(plan.getMajorId()),
                nvl(plan.getDeptId()),
                nvl(plan.getEducationLevel()),
                nvl(plan.getPlanYear()),
                nvl(plan.getTotalCredits()),
                nvl(plan.getVersion()),
                nvl(plan.getPublishStatus()));
        return sha256Hex(canonical);
    }

    private static String nvl(Object v)
    {
        return v == null ? "" : String.valueOf(v);
    }

    private static String sha256Hex(String input)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(bytes.length * 2);
            for (byte b : bytes)
            {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        }
        catch (NoSuchAlgorithmException e)
        {
            // SHA-256 是 JDK 强制算法，理论不可达
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }
}
