package com.yu.aem.strategy;

/**
 * GPA计算策略接口
 * 
 * @author ruoyi
 */
public interface IGpaCalculationStrategy
{
    /** 计算单门课程绩点 */
    double calculate(double score);

    /** 获取等级描述 */
    String getGradeLevel(double score);

    /** 获取算法代码 */
    String getAlgorithmCode();
}
