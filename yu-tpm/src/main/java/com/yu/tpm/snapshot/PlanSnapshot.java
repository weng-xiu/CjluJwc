package com.yu.tpm.snapshot;

import java.util.Date;

/**
 * 人才培养方案版本快照（N2 方案版本快照）。
 *
 * <p>对方案关键字段做规范化并计算内容指纹（SHA-256），形成不可变的"版本快照"标识。
 * 毕业审核等追溯场景只需记录 {@code contentHash}，即可精确锁定审核当时生效的方案内容，
 * 解决此前 {@code tpm_training_plan.version} 仅为字符串标记、政策变更后无法按旧版追溯的问题。
 *
 * @param planId      方案ID
 * @param version     版本号（业务标记）
 * @param contentHash 内容指纹（十六进制小写）
 * @param snapshotAt  快照生成时间
 * @author N2
 */
public record PlanSnapshot(Long planId, String version, String contentHash, Date snapshotAt)
{
}
