package com.yu.tpm.task;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.yu.system.domain.SysNotice;
import com.yu.system.service.ISysConfigService;
import com.yu.system.service.ISysNoticeService;
import com.yu.tpm.domain.TpmSelectionRound;
import com.yu.tpm.domain.dto.OfferingCapacityStat;
import com.yu.tpm.mapper.TpmCourseOfferingMapper;
import com.yu.tpm.mapper.TpmSelectionRoundMapper;

/**
 * P2 选课容量水位告警任务的单元验证。
 *
 * <p>覆盖：无进行中轮次静默、阈值判定（未满但达阈值/已满计入、低于阈值不计）、
 * 未设容量上限不参与、同轮次冷却去重、通知标题长度截断与内容要素。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SelectionCapacityTaskTest
{
    @Mock
    private TpmSelectionRoundMapper tpmSelectionRoundMapper;

    @Mock
    private TpmCourseOfferingMapper tpmCourseOfferingMapper;

    @Mock
    private ISysConfigService configService;

    @Mock
    private ISysNoticeService noticeService;

    @InjectMocks
    private SelectionCapacityTask task;

    private TpmSelectionRound activeRound(long roundId, String name)
    {
        TpmSelectionRound r = new TpmSelectionRound();
        r.setRoundId(roundId);
        r.setRoundName(name);
        r.setRoundStatus("1");
        r.setStatus("0");
        return r;
    }

    private OfferingCapacityStat stat(String course, String teacher, Integer max, Integer enrolled, Integer wait)
    {
        OfferingCapacityStat s = new OfferingCapacityStat();
        s.setOfferingId(100L);
        s.setCourseName(course);
        s.setTeacherName(teacher);
        s.setMaxStudents(max);
        s.setEnrolled(enrolled);
        s.setWaitlisted(wait);
        return s;
    }

    @Test
    @DisplayName("无进行中轮次：不查询统计、不发告警")
    void noActiveRoundSilent()
    {
        when(tpmSelectionRoundMapper.selectTpmSelectionRoundList(any(TpmSelectionRound.class)))
                .thenReturn(Collections.emptyList());
        assertEquals(0, task.check());
        verify(noticeService, never()).insertNotice(any(SysNotice.class));
    }

    @Test
    @DisplayName("全部开课低于阈值：不触发告警")
    void belowThresholdNoAlert()
    {
        when(tpmSelectionRoundMapper.selectTpmSelectionRoundList(any(TpmSelectionRound.class)))
                .thenReturn(List.of(activeRound(1L, "第一轮选课")));
        when(tpmCourseOfferingMapper.selectRoundCapacityStats(1L))
                .thenReturn(List.of(stat("高等数学", "张老师", 100, 50, 0)));
        assertEquals(0, task.check());
        verify(noticeService, never()).insertNotice(any(SysNotice.class));
    }

    @Test
    @DisplayName("满员与达阈值开课同时存在：触发一条告警且明细含两者")
    void fullAndNearFullAlert()
    {
        when(tpmSelectionRoundMapper.selectTpmSelectionRoundList(any(TpmSelectionRound.class)))
                .thenReturn(List.of(activeRound(1L, "第一轮选课")));
        when(tpmCourseOfferingMapper.selectRoundCapacityStats(1L))
                .thenReturn(List.of(
                        stat("大学英语", "李老师", 60, 60, 5),   // 已满
                        stat("体育", "王老师", 50, 46, 0),       // 92% ≥ 默认阈值 90
                        stat("选修导论", "赵老师", 200, 100, 0), // 50% 不计
                        stat("离散数学", null, null, 30, 0)));   // 未设上限不参与
        ArgumentCaptor<SysNotice> captor = ArgumentCaptor.forClass(SysNotice.class);
        assertEquals(1, task.check());
        verify(noticeService, times(1)).insertNotice(captor.capture());
        SysNotice notice = captor.getValue();
        assertTrue(notice.getNoticeTitle().length() <= 50, "标题须截断至 50 字符内");
        assertTrue(notice.getNoticeContent().contains("大学英语"), "明细应包含满员课程");
        assertTrue(notice.getNoticeContent().contains("体育"), "明细应包含达阈值课程");
        assertTrue(notice.getNoticeContent().indexOf("选修导论") < 0, "低于阈值课程不应入明细");
        assertTrue(notice.getNoticeContent().contains("候补总人数：5"), "候补人数应汇总");
        assertEquals("monitor", notice.getCreateBy());
    }

    @Test
    @DisplayName("同轮次冷却期内二次检查：不再重复告警")
    void cooldownSuppressesSecondAlert()
    {
        when(tpmSelectionRoundMapper.selectTpmSelectionRoundList(any(TpmSelectionRound.class)))
                .thenReturn(List.of(activeRound(7L, "补选轮次")));
        when(tpmCourseOfferingMapper.selectRoundCapacityStats(7L))
                .thenReturn(List.of(stat("线性代数", "陈老师", 40, 40, 2)));
        when(configService.selectConfigByKey(SelectionCapacityTask.CFG_COOLDOWN_MINUTES)).thenReturn("30");
        assertEquals(1, task.check());
        assertEquals(0, task.check());
        verify(noticeService, times(1)).insertNotice(any(SysNotice.class));
    }

    @Test
    @DisplayName("阈值可由参数覆盖：80% 阈值下 82% 开课计入告警")
    void thresholdConfigurable()
    {
        when(tpmSelectionRoundMapper.selectTpmSelectionRoundList(any(TpmSelectionRound.class)))
                .thenReturn(List.of(activeRound(2L, "第二轮选课")));
        when(tpmCourseOfferingMapper.selectRoundCapacityStats(2L))
                .thenReturn(List.of(stat("概率论", "孙老师", 50, 41, 0))); // 82%
        when(configService.selectConfigByKey(SelectionCapacityTask.CFG_CAPACITY_PERCENT)).thenReturn("80");
        assertEquals(1, task.check());
        verify(noticeService).insertNotice(any(SysNotice.class));
    }

    @Test
    @DisplayName("告警参数读取异常时回退默认值且不中断")
    void configFailureFallsBack()
    {
        when(tpmSelectionRoundMapper.selectTpmSelectionRoundList(any(TpmSelectionRound.class)))
                .thenReturn(List.of(activeRound(3L, "第三轮选课")));
        when(tpmCourseOfferingMapper.selectRoundCapacityStats(3L))
                .thenReturn(List.of(stat("数据结构", "周老师", 80, 78, 1)));
        when(configService.selectConfigByKey(anyString())).thenThrow(new RuntimeException("db down"));
        assertEquals(1, task.check()); // 回退默认 90%：78/80=97.5% 仍触发
        verify(noticeService).insertNotice(any(SysNotice.class));
    }
}
