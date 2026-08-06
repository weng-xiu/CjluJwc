package com.yu.aem.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.aem.mapper.AemGradeStatisticsMapper;
import com.yu.aem.domain.AemGradeRecord;
import com.yu.aem.mapper.AemGradeRecordMapper;
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
    private static final Logger log = LoggerFactory.getLogger(AemGradeStatisticsServiceImpl.class);

    @Autowired
    private AemGradeStatisticsMapper aemGradeStatisticsMapper;

    @Autowired
    private AemGradeRecordMapper aemGradeRecordMapper;

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
    @Transactional
    public int insertAemGradeStatistics(AemGradeStatistics aemGradeStatistics)
    {
        aemGradeStatistics.setCreateTime(DateUtils.getNowDate());
        return aemGradeStatisticsMapper.insertAemGradeStatistics(aemGradeStatistics);
    }

    @Override
    @Transactional
    public int deleteAemGradeStatisticsByStatId(Long statId)
    {
        return aemGradeStatisticsMapper.deleteAemGradeStatisticsByStatId(statId);
    }

    @Override
    @Transactional
    public int deleteAemGradeStatisticsByStatIds(Long[] statIds)
    {
        return aemGradeStatisticsMapper.deleteAemGradeStatisticsByStatIds(statIds);
    }

    /**
     * 按课程聚合成绩统计
     */
    @Override
    @Transactional
    public AemGradeStatistics aggregateByCourse(Long courseId, Long semesterId)
    {
        // 查询该课程该学期所有成绩记录
        AemGradeRecord query = new AemGradeRecord();
        query.setCourseId(courseId);
        query.setSemesterId(semesterId);
        List<AemGradeRecord> records = aemGradeRecordMapper.selectAemGradeRecordList(query);
        if (records == null || records.isEmpty())
        {
            log.warn("课程[{}]学期[{}]无成绩记录", courseId, semesterId);
            return null;
        }
        // 过滤出有总成绩的记录
        List<AemGradeRecord> validRecords = new java.util.ArrayList<>();
        for (AemGradeRecord r : records)
        {
            if (r.getTotalScore() != null)
            {
                validRecords.add(r);
            }
        }
        if (validRecords.isEmpty())
        {
            return null;
        }
        // 计算统计数据
        int totalStudents = validRecords.size();
        double maxScore = Double.MIN_VALUE;
        double minScore = Double.MAX_VALUE;
        double sumScore = 0;
        int passCount = 0;
        int excellentCount = 0;
        for (AemGradeRecord r : validRecords)
        {
            double score = r.getTotalScore();
            if (score > maxScore) maxScore = score;
            if (score < minScore) minScore = score;
            sumScore += score;
            if (score >= 60) passCount++;
            if (score >= 90) excellentCount++;
        }
        double avgScore = sumScore / totalStudents;
        int failCount = totalStudents - passCount;
        double passRate = totalStudents > 0 ? (double) passCount / totalStudents * 100 : 0;
        double excellentRate = totalStudents > 0 ? (double) excellentCount / totalStudents * 100 : 0;
        // 四舍五入保留两位小数
        avgScore = Math.round(avgScore * 100) / 100.0;
        passRate = Math.round(passRate * 100) / 100.0;
        excellentRate = Math.round(excellentRate * 100) / 100.0;
        // 查询是否已有统计记录
        AemGradeStatistics existing = aemGradeStatisticsMapper.selectByCourseAndSemester(courseId, semesterId);
        AemGradeStatistics stat;
        if (existing != null)
        {
            stat = existing;
        }
        else
        {
            stat = new AemGradeStatistics();
            stat.setCourseId(courseId);
            stat.setSemesterId(semesterId);
            stat.setCreateTime(DateUtils.getNowDate());
        }
        stat.setTotalStudents(totalStudents);
        stat.setMaxScore(maxScore);
        stat.setMinScore(minScore);
        stat.setAvgScore(avgScore);
        stat.setPassCount(passCount);
        stat.setFailCount(failCount);
        stat.setPassRate(passRate);
        stat.setExcellentCount(excellentCount);
        stat.setExcellentRate(excellentRate);
        stat.setStatus("0");
        stat.setUpdateTime(DateUtils.getNowDate());
        if (existing != null)
        {
            aemGradeStatisticsMapper.updateAemGradeStatistics(stat);
        }
        else
        {
            aemGradeStatisticsMapper.insertAemGradeStatistics(stat);
        }
        log.info("课程[{}]学期[{}]成绩统计聚合完成：{}人，平均分{}，通过率{}%", courseId, semesterId, totalStudents, avgScore, passRate);
        return stat;
    }
}
