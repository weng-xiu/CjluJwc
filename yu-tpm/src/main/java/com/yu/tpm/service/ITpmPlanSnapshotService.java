package com.yu.tpm.service;

import java.util.List;
import com.yu.tpm.domain.TpmPlanSnapshot;
import com.yu.tpm.domain.TpmTrainingPlan;

/**
 * 人才培养方案版本快照Service接口（F2）。
 *
 * <p>方案"发布"即生成一条不可变内容快照，记录实质内容指纹 + 学分结构 + 课程清单的
 * 发布时刻定格 JSON，使毕业审核可选定"按某一历史版本比对"，解决政策变更后无法追溯的问题。
 *
 * @author F2
 * @date 2026-10-17
 */
public interface ITpmPlanSnapshotService
{
    /**
     * 在方案发布时生成并落库一条版本快照。
     *
     * @param plan 刚发布（含版本号、发布状态）的方案实体
     * @return 落库后的快照（含自增 snapshotId）
     */
    public TpmPlanSnapshot createSnapshotOnPublish(TpmTrainingPlan plan);

    /** 查询某方案的全部历史快照（最新版本在前）。 */
    public List<TpmPlanSnapshot> selectSnapshotsByPlanId(Long planId);

    /** 按快照ID查询单条快照（含定格 JSON）。 */
    public TpmPlanSnapshot selectSnapshotBySnapshotId(Long snapshotId);
}
