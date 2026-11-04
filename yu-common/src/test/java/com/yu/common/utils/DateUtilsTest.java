package com.yu.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * G5：{@link DateUtils} 的契约测试（42 行、零覆盖）。
 *
 * <p>重点是两条错误语义的分叉：{@code parseDate} 解析失败<b>静默返回 null</b>，
 * 而 {@code dateTime(format, ts)} 解析失败<b>抛 RuntimeException</b>。
 * 调用方按哪一种处理决定了是「拿到空值继续走」还是「整条链路 500」，
 * 这层差异必须在测试里显式钉住，否则改动时很容易把一边改成另一边。</p>
 */
class DateUtilsTest
{
    private static Date at(String text) throws Exception
    {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(text);
    }

    @Test
    @DisplayName("当前时间系列：格式正确且与系统时钟一致")
    void now()
    {
        assertNotNull(DateUtils.getNowDate());
        assertTrue(Math.abs(System.currentTimeMillis() - DateUtils.getNowDate().getTime()) < 5000);

        assertTrue(DateUtils.getDate().matches("\\d{4}-\\d{2}-\\d{2}"), "实际=" + DateUtils.getDate());
        assertTrue(DateUtils.getTime().matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}"), "实际=" + DateUtils.getTime());
        assertTrue(DateUtils.dateTimeNow().matches("\\d{14}"), "默认格式是紧凑型 yyyyMMddHHmmss");
        assertTrue(DateUtils.datePath().matches("\\d{4}/\\d{2}/\\d{2}"), "实际=" + DateUtils.datePath());
        assertTrue(DateUtils.dateTime().matches("\\d{8}"), "实际=" + DateUtils.dateTime());
    }

    @Test
    @DisplayName("parseDateToStr / dateTime(Date)：按指定格式输出")
    void toStr() throws Exception
    {
        Date d = new Date(at("2020-01-02 03:04:05").getTime());
        assertEquals("2020-01-02", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD, d));
        assertEquals("2020", DateUtils.parseDateToStr(DateUtils.YYYY, d));
        assertEquals("2020-01-02", DateUtils.dateTime(d), "dateTime(Date) 固定用 yyyy-MM-dd");
        assertEquals("2020-01-02 03:04:05", DateUtils.parseDateToStr(DateUtils.YYYY_MM_DD_HH_MM_SS, d));
    }

    @Test
    @DisplayName("dateTime(format, ts)：格式不匹配时抛 RuntimeException（与 parseDate 的静默 null 不同）")
    void parseStrict() throws Exception
    {
        assertEquals(at("2020-01-02 00:00:00").getTime(),
                DateUtils.dateTime("yyyy-MM-dd", "2020-01-02").getTime());
        assertThrows(RuntimeException.class, () -> DateUtils.dateTime("yyyy-MM-dd", "not-a-date"),
                "严格解析必须抛异常，不能返回 null");
    }

    @Test
    @DisplayName("parseDate：支持 12 种常见格式，解析失败静默返回 null")
    void parseDate()
    {
        assertNull(DateUtils.parseDate(null), "null 直接返回 null");
        assertNull(DateUtils.parseDate("not-a-date"), "解析失败必须静默返回 null，不能抛异常");

        assertNotNull(DateUtils.parseDate("2020-01-02"));
        assertNotNull(DateUtils.parseDate("2020-01-02 03:04:05"));
        assertNotNull(DateUtils.parseDate("2020-01-02 03:04"));
        assertNotNull(DateUtils.parseDate("2020-01"));
        assertNotNull(DateUtils.parseDate("2020/01/02"), "斜杠格式也要认");
        assertNotNull(DateUtils.parseDate("2020.01.02"), "点号格式也要认");

        assertEquals(DateUtils.parseDate("2020-01-02").getTime(),
                DateUtils.parseDate("2020/01/02").getTime(), "同一天的不同写法必须解析成同一时刻");
    }

    @Test
    @DisplayName("getServerStartDate：不能晚于当前时间")
    void serverStart()
    {
        Date start = DateUtils.getServerStartDate();
        assertNotNull(start);
        assertTrue(start.getTime() <= System.currentTimeMillis() + 1000, "服务器启动时间不可能在未来");
    }

    @Test
    @DisplayName("differentDaysByMillisecond：取绝对值后按整天截断")
    void differentDays()
    {
        Date d1 = new Date(0);
        Date d2 = new Date(2L * 24 * 3600 * 1000 + 3600 * 1000);
        assertEquals(2, DateUtils.differentDaysByMillisecond(d1, d2), "不满一天要向下截断");
        assertEquals(2, DateUtils.differentDaysByMillisecond(d2, d1), "顺序颠倒仍取绝对值");
        assertEquals(0, DateUtils.differentDaysByMillisecond(d1, d1));
    }

    @Test
    @DisplayName("timeDistance：输出「X天Y小时Z分钟」")
    void timeDistance() throws Exception
    {
        Date start = at("2020-01-01 00:00:00");
        Date end = at("2020-01-02 02:03:00");
        assertEquals("1天2小时3分钟", DateUtils.timeDistance(end, start));
        assertEquals("0天0小时0分钟", DateUtils.timeDistance(start, start));
    }

    @Test
    @DisplayName("toDate：LocalDateTime / LocalDate 按系统时区换算")
    void toDate()
    {
        LocalDateTime ldt = LocalDateTime.of(2020, 1, 2, 3, 4, 5);
        assertEquals(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(),
                DateUtils.toDate(ldt).getTime());

        LocalDate ld = LocalDate.of(2020, 1, 2);
        Date d = DateUtils.toDate(ld);
        assertEquals(ld.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli(), d.getTime(),
                "LocalDate 必须落到当天 00:00:00");
    }
}
