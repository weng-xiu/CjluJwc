package com.yu.aem.strategy;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 4.0制GPA计算策略
 * 
 * @author ruoyi
 */
public class FourScaleGpaStrategy implements IGpaCalculationStrategy
{
    private static final int GPA_SCALE = 2;

    @Override
    public double calculate(double score)
    {
        if (score >= 90)
        {
            return 4.00;
        }
        else if (score >= 85)
        {
            return 3.70;
        }
        else if (score >= 82)
        {
            return 3.30;
        }
        else if (score >= 78)
        {
            return 3.00;
        }
        else if (score >= 75)
        {
            return 2.70;
        }
        else if (score >= 72)
        {
            return 2.30;
        }
        else if (score >= 68)
        {
            return 2.00;
        }
        else if (score >= 64)
        {
            return 1.50;
        }
        else if (score >= 60)
        {
            return 1.00;
        }
        return 0.00;
    }

    @Override
    public String getGradeLevel(double score)
    {
        if (score >= 90)
        {
            return "A";
        }
        else if (score >= 85)
        {
            return "A-";
        }
        else if (score >= 82)
        {
            return "B+";
        }
        else if (score >= 78)
        {
            return "B";
        }
        else if (score >= 75)
        {
            return "B-";
        }
        else if (score >= 72)
        {
            return "C+";
        }
        else if (score >= 68)
        {
            return "C";
        }
        else if (score >= 64)
        {
            return "C-";
        }
        else if (score >= 60)
        {
            return "D";
        }
        return "F";
    }

    @Override
    public String getAlgorithmCode()
    {
        return "GPA_4_0";
    }
}
