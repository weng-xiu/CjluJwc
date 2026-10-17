package com.yu.tpm.mapper;

import java.util.List;
import com.yu.tpm.domain.TpmPlanSnapshot;

/**
 * 人才培养方案版本快照Mapper接口（F2）
 *
 * @author F2
 * @date 2026-10-17
 */
public interface TpmPlanSnapshotMapper
{
    /** 按快照ID查询 */
    public TpmPlanSnapshot selectTpmPlanSnapshotBySnapshotId(Long snapshotId);

    /** 查询某方案的全部历史快照（按快照ID倒序，最新在前） */
    public List<TpmPlanSnapshot> selectSnapshotsByPlanId(Long planId);

    /** 条件查询快照列表 */
    public List<TpmPlanSnapshot> selectTpmPlanSnapshotList(TpmPlanSnapshot tpmPlanSnapshot);

    /** 新增快照 */
    public int insertTpmPlanSnapshot(TpmPlanSnapshot tpmPlanSnapshot);
}
