package com.yu.aem.strategy;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import com.yu.aem.domain.AemGpaScoreMapping;

/**
 * 自定义映射GPA计算策略
 * 
 * @author ruoyi
 */
public class CustomMappingGpaStrategy implements IGpaCalculationStrategy
{
    private final String algorithmCode;
    private final List<AemGpaScoreMapping> mappings;

    public CustomMappingGpaStrategy(String algorithmCode, List<AemGpaScoreMapping> mappings)
    {
        this.algorithmCode = algorithmCode;
        this.mappings = mappings;
        if (this.mappings != null)
        {
            this.mappings.sort(Comparator.comparing(AemGpaScoreMapping::getSortOrder, Comparator.nullsLast(Comparator.naturalOrder())));
        }
    }

    @Override
    public double calculate(double score)
    {
        AemGpaScoreMapping mapping = findMapping(score);
        if (mapping != null && mapping.getGradePoint() != null)
        {
            return mapping.getGradePoint().doubleValue();
        }
        return 0.00;
    }

    @Override
    public String getGradeLevel(double score)
    {
        AemGpaScoreMapping mapping = findMapping(score);
        if (mapping != null)
        {
            return mapping.getGradeLevel();
        }
        return "";
    }

    @Override
    public String getAlgorithmCode()
    {
        return algorithmCode;
    }

    private AemGpaScoreMapping findMapping(double score)
    {
        if (mappings == null)
        {
            return null;
        }
        BigDecimal scoreDecimal = new BigDecimal(Double.toString(score));
        for (AemGpaScoreMapping mapping : mappings)
        {
            if (mapping.getMinScore() != null && mapping.getMaxScore() != null)
            {
                if (scoreDecimal.compareTo(mapping.getMinScore()) >= 0
                        && scoreDecimal.compareTo(mapping.getMaxScore()) <= 0)
                {
                    return mapping;
                }
            }
        }
        return null;
    }
}
