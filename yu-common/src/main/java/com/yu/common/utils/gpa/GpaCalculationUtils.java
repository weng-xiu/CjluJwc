package com.yu.common.utils.gpa;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * GPA计算辅助工具类
 *
 * @author ruoyi
 */
public class GpaCalculationUtils
{
    /** GPA默认保留小数位数 */
    private static final int GPA_SCALE = 2;

    /** 这个类不能实例化 */
    private GpaCalculationUtils()
    {
    }

    /**
     * 中国标准绩点计算: (score/10) - 5, score<60则为0
     *
     * @param score 百分制分数（0-100）
     * @return 绩点值
     */
    public static double calculateChinaStandardGpa(double score)
    {
        if (score < 60)
        {
            return 0.00;
        }
        BigDecimal scoreDecimal = new BigDecimal(Double.toString(score));
        BigDecimal ten = new BigDecimal("10");
        BigDecimal five = new BigDecimal("5");
        BigDecimal gpa = scoreDecimal.divide(ten, 2, RoundingMode.HALF_UP).subtract(five);
        return gpa.setScale(GPA_SCALE, RoundingMode.HALF_UP).doubleValue();
    }

    /**
     * 加权GPA计算
     * 计算公式：GPA = Σ(绩点 × 学分) / Σ(学分)
     *
     * @param scoreAndCredits 成绩与学分列表，每个元素为double[]{score, credit}
     * @return 加权平均绩点，保留两位小数
     */
    public static double calculateWeightedGpa(List<double[]> scoreAndCredits)
    {
        if (scoreAndCredits == null || scoreAndCredits.isEmpty())
        {
            return 0.00;
        }
        BigDecimal totalWeightedGpa = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        for (double[] scoreAndCredit : scoreAndCredits)
        {
            if (scoreAndCredit == null || scoreAndCredit.length < 2)
            {
                continue;
            }
            double score = scoreAndCredit[0];
            double credit = scoreAndCredit[1];
            if (credit <= 0)
            {
                continue;
            }
            double gpa = calculateChinaStandardGpa(score);
            BigDecimal gpaDecimal = new BigDecimal(Double.toString(gpa));
            BigDecimal creditDecimal = new BigDecimal(Double.toString(credit));
            totalWeightedGpa = totalWeightedGpa.add(gpaDecimal.multiply(creditDecimal));
            totalCredits = totalCredits.add(creditDecimal);
        }
        if (totalCredits.compareTo(BigDecimal.ZERO) == 0)
        {
            return 0.00;
        }
        BigDecimal weightedGpa = totalWeightedGpa.divide(totalCredits, GPA_SCALE, RoundingMode.HALF_UP);
        return weightedGpa.doubleValue();
    }
}
