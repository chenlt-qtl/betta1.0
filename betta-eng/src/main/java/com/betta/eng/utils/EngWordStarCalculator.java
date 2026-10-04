package com.betta.eng.utils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;

/** 单词关卡、复习和遗忘星级的纯规则计算器。 */
public final class EngWordStarCalculator
{
    private EngWordStarCalculator() {}

    /** 按百分制成绩换算关卡星级。 */
    public static int levelStars(int score)
    {
        if (score >= 100) return 3;
        if (score >= 90) return 2;
        return score >= 80 ? 1 : 0;
    }

    /** 按单词本轮正确题数换算复习星级。 */
    public static int reviewStars(int correctCount, int totalCount)
    {
        if (totalCount <= 0 || correctCount <= 0) return 0;
        int wrongCount = totalCount - correctCount;
        if (wrongCount <= 0) return 3;
        return wrongCount == 1 ? 2 : 1;
    }

    /** 根据最新星级和最近测试日实时计算当前星级。 */
    public static int currentStars(Integer latestStars, Date latestTestTime, Date now)
    {
        int stars = latestStars == null ? 0 : Math.max(0, Math.min(3, latestStars));
        if (stars == 0 || latestTestTime == null) return stars;
        LocalDate tested = latestTestTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate current = (now == null ? new Date() : now).toInstant()
                .atZone(ZoneId.systemDefault()).toLocalDate();
        long days = Math.max(0, ChronoUnit.DAYS.between(tested, current));
        if (stars == 3) return days >= 52 ? 0 : days >= 45 ? 1 : days >= 31 ? 2 : 3;
        if (stars == 2) return days >= 22 ? 0 : days >= 15 ? 1 : 2;
        return days >= 8 ? 0 : 1;
    }

    /** 返回从已奖励星级提升到目标星级应补发的累计里程碑金币。 */
    public static long milestoneCoin(int rewardedStars, int targetStars)
    {
        return cumulativeCoin(targetStars) - cumulativeCoin(Math.min(rewardedStars, targetStars));
    }

    private static long cumulativeCoin(int stars)
    {
        if (stars >= 3) return 6;
        if (stars == 2) return 3;
        return stars == 1 ? 1 : 0;
    }
}
