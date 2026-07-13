package com.yu.aem.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 中国标准GPA计算策略
 * 
 * @author ruoyi
 */
public class ChinaStandardGpaStrategy implements IGpaCalculationStrategy
{
    private static final int GPA_SCALE = 2;

    @Override
    public double calculate(double score)
    {
        if (score < 60)
        {
            return 0.00;
        }
        BigDecimal scoreDecimal = new BigDecimal(Double.toString(score));
        BigDecimal gpa = scoreDecimal.divide(new BigDecimal("10"), GPA_SCALE, RoundingMode.HALF_UP)
                .subtract(new BigDecimal("5"));
        return gpa.setScale(GPA_SCALE, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    public String getGradeLevel(double score)
    {
        if (score >= 90)
        {
            return "优秀";
        }
        else if (score >= 80)
        {
            return "良好";
        }
        else if (score >= 70)
        {
            return "中等";
        }
        else if (score >= 60)
        {
            return "及格";
        }
        return "不及格";
    }

    @Override
    public String getAlgorithmCode()
    {
        return "CN_STANDARD";
    }
}
