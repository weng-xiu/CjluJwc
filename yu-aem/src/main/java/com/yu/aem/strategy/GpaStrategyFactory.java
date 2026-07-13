package com.yu.aem.strategy;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import com.yu.aem.domain.AemGpaScoreMapping;

/**
 * GPA计算策略工厂
 * 
 * @author ruoyi
 */
@Component
public class GpaStrategyFactory
{
    private final Map<String, IGpaCalculationStrategy> strategies = new HashMap<>();

    @PostConstruct
    public void init()
    {
        register(new ChinaStandardGpaStrategy());
        register(new FourScaleGpaStrategy());
    }

    /**
     * 注册策略
     */
    public void register(IGpaCalculationStrategy strategy)
    {
        if (strategy != null)
        {
            strategies.put(strategy.getAlgorithmCode(), strategy);
        }
    }

    /**
     * 获取策略
     */
    public IGpaCalculationStrategy getStrategy(String algorithmCode)
    {
        return strategies.get(algorithmCode);
    }

    /**
     * 注册自定义映射策略
     */
    public void registerCustomStrategy(String algorithmCode, List<AemGpaScoreMapping> mappings)
    {
        strategies.put(algorithmCode, new CustomMappingGpaStrategy(algorithmCode, mappings));
    }

    /**
     * 判断是否存在指定策略
     */
    public boolean containsStrategy(String algorithmCode)
    {
        return strategies.containsKey(algorithmCode);
    }
}
