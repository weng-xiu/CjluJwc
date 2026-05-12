package com.yu.aem.mapper;

import java.util.List;
import com.yu.aem.domain.AemGradeStatistics;

/**
 * 成绩统计分析Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
public interface AemGradeStatisticsMapper 
{
    public AemGradeStatistics selectAemGradeStatisticsByStatId(Long statId);
    public List<AemGradeStatistics> selectAemGradeStatisticsList(AemGradeStatistics aemGradeStatistics);
    public int insertAemGradeStatistics(AemGradeStatistics aemGradeStatistics);
    public int updateAemGradeStatistics(AemGradeStatistics aemGradeStatistics);
    public int deleteAemGradeStatisticsByStatId(Long statId);
    public int deleteAemGradeStatisticsByStatIds(Long[] statIds);
}
