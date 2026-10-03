package com.betta.eng.utils;

import java.util.Date;

/** 熟悉度曲线的独立回归入口。 */
public class FamiliarityCalculatorTest {
    private static final long DAY = 24L * 60L * 60L * 1000L;

    /** 执行熟悉度上下限、稳定期和异常时间测试。 */
    public static void main(String[] args) {
        Date now = new Date(2_000L * DAY);
        assertEquals(0, FamiliarityCalculator.clamp(-1), "负值应截断为零");
        assertEquals(10, FamiliarityCalculator.clamp(11), "超过十应截断为十");
        int[] expected = {0, 1, 2, 4, 7, 14, 30, 60, 120, 240, 365};
        for (int level = 1; level <= 10; level++) {
            assertEquals(expected[level], FamiliarityCalculator.stabilityDays(level), "稳定期映射错误");
        }
        assertEquals(10, FamiliarityCalculator.effective(10, now, now), "刚复习不应衰减");
        assertEquals(9, FamiliarityCalculator.effective(10,
                new Date(now.getTime() - 365L * DAY), now), "稳定期时应约保留九成");
        assertEquals(0, FamiliarityCalculator.effective(8, null, now), "缺少时间应视为已遗忘");
        assertTrue(FamiliarityCalculator.isReviewDue(8, null, now), "缺少时间的历史成绩应立即到期");
        assertEquals(8, FamiliarityCalculator.effective(8,
                new Date(now.getTime() + DAY), now), "未来时间应按零天处理");
        assertTrue(!FamiliarityCalculator.isReviewDue(2,
                new Date(now.getTime() - 10L * DAY), now), "学习期词不应进入到期复习分类");
    }

    private static void assertEquals(int expected, int actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + "，expected=" + expected + "，actual=" + actual);
        }
    }

    private static void assertTrue(boolean value, String message) {
        if (!value) {
            throw new AssertionError(message);
        }
    }
}
