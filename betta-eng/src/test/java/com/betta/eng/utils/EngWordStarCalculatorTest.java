package com.betta.eng.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;

/** 星级、遗忘边界和里程碑金币的纯规则回归入口。 */
public class EngWordStarCalculatorTest
{
    public static void main(String[] args)
    {
        assertEquals(0, EngWordStarCalculator.levelStars(79), "79 分应为零星");
        assertEquals(1, EngWordStarCalculator.levelStars(80), "80 分应为一星");
        assertEquals(2, EngWordStarCalculator.levelStars(90), "90 分应为二星");
        assertEquals(3, EngWordStarCalculator.levelStars(100), "100 分应为三星");
        assertEquals(3, EngWordStarCalculator.reviewStars(4, 4), "复习全对应为三星");
        assertEquals(2, EngWordStarCalculator.reviewStars(3, 4), "复习错一题应为二星");
        assertEquals(1, EngWordStarCalculator.reviewStars(1, 4), "至少对一题应为一星");
        assertEquals(0, EngWordStarCalculator.reviewStars(0, 4), "复习全错应为零星");

        Date now = date(2026, 10, 4);
        assertEquals(3, current(3, now, 30), "三星第30天不衰减");
        assertEquals(2, current(3, now, 31), "三星第31天降为二星");
        assertEquals(1, current(3, now, 45), "三星第45天降为一星");
        assertEquals(0, current(3, now, 52), "三星第52天降为零星");
        assertEquals(1, current(2, now, 15), "二星第15天降为一星");
        assertEquals(0, current(2, now, 22), "二星第22天降为零星");
        assertEquals(0, current(1, now, 8), "一星第8天降为零星");

        assertEquals(1L, EngWordStarCalculator.milestoneCoin(0, 1), "首次一星奖励1金币");
        assertEquals(2L, EngWordStarCalculator.milestoneCoin(1, 2), "一星升二星补2金币");
        assertEquals(3L, EngWordStarCalculator.milestoneCoin(2, 3), "二星升三星补3金币");
        assertEquals(0L, EngWordStarCalculator.milestoneCoin(3, 3), "重复三星不发里程碑金币");
    }

    private static int current(int stars, Date now, int days)
    {
        Date tested = Date.from(now.toInstant().minusSeconds(days * 86400L));
        return EngWordStarCalculator.currentStars(stars, tested, now);
    }

    private static Date date(int year, int month, int day)
    {
        return Date.from(LocalDate.of(year, month, day).atStartOfDay(ZoneId.systemDefault()).toInstant());
    }

    private static void assertEquals(Object expected, Object actual, String message)
    {
        if (!expected.equals(actual)) throw new AssertionError(message + "，期望=" + expected + "，实际=" + actual);
    }
}
