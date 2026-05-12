package com.yu.aem.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.aem.mapper.AemGradeStatisticsMapper;
import com.yu.aem.domain.AemGradeStatistics;
import com.yu.aem.service.IAemGradeStatisticsService;

/**
 * 成绩统计分析Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@Service
public class AemGradeStatisticsServiceImpl implements IAemGradeStatisticsService 
{
    @Autowired
    private AemGradeStatisticsMapper aemGradeStatisticsMapper;

    @Override
    public AemGradeStatistics selectAemGradeStatisticsByStatId(Long statId)
    {
        return aemGradeStatisticsMapper.selectAemGradeStatisticsByStatId(statId);
    }

    @Override
    public List<AemGradeStatistics> selectAemGradeStatisticsList(AemGradeStatistics aemGradeStatistics)
    {
        return aemGradeStatisticsMapper.selectAemGradeStatisticsList(aemGradeStatistics);
    }

    @Override
    public int insertAemGradeStatistics(AemGradeStatistics aemGradeStatistics)
    {
        aemGradeStatistics.setCreateTime(DateUtils.getNowDate());
        return aemGradeStatisticsMapper.insertAemGradeStatistics(aemGradeStatistics);
    }

    @Override
    public int deleteAemGradeStatisticsByStatId(Long statId)
    {
        return aemGradeStatisticsMapper.deleteAemGradeStatisticsByStatId(statId);
    }

    @Override
    public int deleteAemGradeStatisticsByStatIds(Long[] statIds)
    {
        return aemGradeStatisticsMapper.deleteAemGradeStatisticsByStatIds(statIds);
    }
}
