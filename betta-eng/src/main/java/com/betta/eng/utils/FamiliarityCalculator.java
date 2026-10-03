package com.betta.eng.utils;

import java.util.Date;

/**
 * 熟悉度计算器：数据库保存基础值，对外展示按记忆稳定期衰减后的当前值。
 */
public final class FamiliarityCalculator {
    private static final double CURVE_FACTOR = 19.0D / 81.0D;
    private static final long MILLIS_PER_DAY = 24L * 60L * 60L * 1000L;
    private static final int[] STABILITY_DAYS = {0, 1, 2, 4, 7, 14, 30, 60, 120, 240, 365};

    private FamiliarityCalculator() {
    }

    /** 将基础熟悉度限制在零至十。 */
    public static int clamp(Integer familiarity) {
        return Math.max(0, Math.min(10, familiarity == null ? 0 : familiarity));
    }

    /** 返回指定基础熟悉度对应的记忆稳定天数；零级没有稳定期。 */
    public static int stabilityDays(Integer familiarity) {
        return STABILITY_DAYS[clamp(familiarity)];
    }

    /** 计算距最后复习的天数；未来时间按零天处理，缺失时间返回正无穷。 */
    public static double elapsedDays(Date lastReviewTime, Date now) {
        if (lastReviewTime == null) {
            return Double.POSITIVE_INFINITY;
        }
        long current = now == null ? System.currentTimeMillis() : now.getTime();
        return Math.max(0D, (current - lastReviewTime.getTime()) / (double) MILLIS_PER_DAY);
    }

    /** 按缓降记忆曲线计算当前有效熟悉度；缺少复习时间的历史成绩按已遗忘处理。 */
    public static int effective(Integer familiarity, Date lastReviewTime, Date now) {
        int base = clamp(familiarity);
        if (base == 0) {
            return 0;
        }
        double elapsed = elapsedDays(lastReviewTime, now);
        if (Double.isInfinite(elapsed)) {
            return 0;
        }
        double retention = Math.pow(1D + CURVE_FACTOR * elapsed / stabilityDays(base), -0.5D);
        return clamp((int) Math.round(base * retention));
    }

    /** 判断已有成绩是否达到复习时间；低于三级由学习期规则处理。 */
    public static boolean isReviewDue(Integer familiarity, Date lastReviewTime, Date now) {
        int base = clamp(familiarity);
        return base >= 3 && elapsedDays(lastReviewTime, now) >= stabilityDays(base);
    }

    /** 计算超期比例，用于到期复习词的优先级排序。 */
    public static double overdueRatio(Integer familiarity, Date lastReviewTime, Date now) {
        int stability = stabilityDays(familiarity);
        return stability == 0 ? 0D : elapsedDays(lastReviewTime, now) / stability;
    }
}
