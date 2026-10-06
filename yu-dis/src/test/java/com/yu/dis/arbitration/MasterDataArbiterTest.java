package com.yu.dis.arbitration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yu.dis.arbitration.MasterDataArbiter.MasterCandidate;

/**
 * N2 主数据裁决：优先级 + 更新时间裁决规则单元验证。
 *
 * @author N2
 */
@ExtendWith(MockitoExtension.class)
class MasterDataArbiterTest
{
    @Spy
    private MasterDataPriorityPolicy priorityPolicy = new MasterDataPriorityPolicy();

    @InjectMocks
    private MasterDataArbiter arbiter;

    @Test
    @DisplayName("高优先级源胜出")
    void higherPriorityWins()
    {
        MasterCandidate grad = new MasterCandidate("2024001", "GRAD", new Date(1_000L), "grad-value");
        MasterCandidate card = new MasterCandidate("2024001", "CARD", new Date(2_000L), "card-value");

        MasterCandidate winner = arbiter.pickWinner(List.of(card, grad));

        assertEquals("GRAD", winner.sourceCode());
    }

    @Test
    @DisplayName("同优先级取更新时间最新")
    void samePriorityTakesNewest()
    {
        MasterCandidate older = new MasterCandidate("T001", "HR", new Date(1_000L), "old");
        MasterCandidate newer = new MasterCandidate("T001", "HR", new Date(9_000L), "new");

        MasterCandidate winner = arbiter.pickWinner(List.of(older, newer));

        assertEquals("new", winner.value());
    }

    @Test
    @DisplayName("未登记源回落默认优先级仍参与裁决")
    void unknownSourceFallback()
    {
        MasterCandidate unknown = new MasterCandidate("K", "UNKNOWN_SYS", new Date(5_000L), "u");
        MasterCandidate known = new MasterCandidate("K", "JW", new Date(1_000L), "k");

        MasterCandidate winner = arbiter.pickWinner(List.of(unknown, known));

        assertEquals("JW", winner.sourceCode());
    }

    @Test
    @DisplayName("批量裁决按业务主键分组各出一个权威")
    void arbitrateGroupsByKey()
    {
        List<MasterCandidate> all = List.of(
                new MasterCandidate("S1", "CARD", new Date(1L), "s1-card"),
                new MasterCandidate("S1", "JW", new Date(1L), "s1-jw"),
                new MasterCandidate("S2", "FINANCE", new Date(1L), "s2-fin"));

        Map<String, MasterCandidate> result = arbiter.arbitrate(all);

        assertEquals(2, result.size());
        assertEquals("JW", result.get("S1").sourceCode());
        assertEquals("FINANCE", result.get("S2").sourceCode());
    }

    @Test
    @DisplayName("空候选返回 null")
    void emptyReturnsNull()
    {
        assertNull(arbiter.pickWinner(List.of()));
        assertNull(arbiter.pickWinner(null));
    }
}
