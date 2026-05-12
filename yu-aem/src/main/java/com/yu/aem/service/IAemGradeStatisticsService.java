package com.yu.aem.service;

import java.util.List;
import com.yu.aem.domain.AemGradeStatistics;

/**
 * 成绩统计分析Service接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface IAemGradeStatisticsService 
{
    public AemGradeStatistics selectAemGradeStatisticsByStatId(Long statId);
    public List<AemGradeStatistics> selectAemGradeStatisticsList(AemGradeStatistics aemGradeStatistics);
    public int insertAemGradeStatistics(AemGradeStatistics aemGradeStatistics);
    public int deleteAemGradeStatisticsByStatIds(Long[] statIds);
    public int deleteAemGradeStatisticsByStatId(Long statId);
}
