package com.yu.tpm.snapshot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.tpm.domain.TpmTrainingPlan;

/**
 * N2 方案版本快照：内容指纹稳定性与变更检测的单元验证。
 *
 * @author N2
 */
class TrainingPlanSnapshotServiceTest
{
    private final TrainingPlanSnapshotService service = new TrainingPlanSnapshotService();

    private TpmTrainingPlan plan(Long majorId, String year, Double credits, String version)
    {
        TpmTrainingPlan p = new TpmTrainingPlan();
        p.setPlanId(1L);
        p.setMajorId(majorId);
        p.setPlanYear(year);
        p.setTotalCredits(credits);
        p.setVersion(version);
        p.setPublishStatus("1");
        return p;
    }

    @Test
    @DisplayName("相同实质内容得到相同指纹")
    void stableHashForSameContent()
    {
        String h1 = service.fingerprint(plan(10L, "2024", 160.0, "v1"));
        String h2 = service.fingerprint(plan(10L, "2024", 160.0, "v1"));
        assertEquals(h1, h2);
    }

    @Test
    @DisplayName("任一实质字段变化则指纹变化")
    void changedFieldChangesHash()
    {
        String base = service.fingerprint(plan(10L, "2024", 160.0, "v1"));
        String changed = service.fingerprint(plan(10L, "2024", 165.0, "v1")); // 学分变了
        assertNotEquals(base, changed);
    }

    @Test
    @DisplayName("非实质字段(remark)不影响指纹")
    void nonEssentialFieldIgnored()
    {
        TpmTrainingPlan a = plan(10L, "2024", 160.0, "v1");
        TpmTrainingPlan b = plan(10L, "2024", 160.0, "v1");
        b.setRemark("随便写的备注");
        assertEquals(service.fingerprint(a), service.fingerprint(b));
    }

    @Test
    @DisplayName("isChanged 依据既有指纹判定")
    void detectChangeAgainstPriorHash()
    {
        TpmTrainingPlan v1 = plan(10L, "2024", 160.0, "v1");
        String hash = service.fingerprint(v1);
        assertFalse(service.isChanged(plan(10L, "2024", 160.0, "v1"), hash));
        assertTrue(service.isChanged(plan(10L, "2024", 170.0, "v1"), hash));
    }

    @Test
    @DisplayName("快照对象携带方案ID/版本/指纹")
    void snapshotCarriesMeta()
    {
        PlanSnapshot snap = service.snapshot(plan(10L, "2024", 160.0, "v3"));
        assertEquals(1L, snap.planId().longValue());
        assertEquals("v3", snap.version());
        assertEquals(service.fingerprint(plan(10L, "2024", 160.0, "v3")), snap.contentHash());
    }
}
