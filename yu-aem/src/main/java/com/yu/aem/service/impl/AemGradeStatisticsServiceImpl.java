package com.yu.aem.service.impl;

import java.util.List;
import java.util.Map;
import com.yu.common.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
    private static final Logger log = LoggerFactory.getLogger(AemGradeStatisticsServiceImpl.class);

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
     * 按课程聚合成绩统计（使用SQL聚合，避免全量加载成绩到内存）
     */
    @Override
    @Transactional
    public AemGradeStatistics aggregateByCourse(Long courseId, Long semesterId)
    {
        Map<String, Object> agg = aemGradeStatisticsMapper.aggregateGradeByCourse(courseId, semesterId);
        if (agg == null || toInt(agg.get("totalStudents")) == 0)
        {
            log.warn("课程[{}]学期[{}]无成绩记录", courseId, semesterId);
            return null;
        }
        AemGradeStatistics stat = upsertStat(courseId, semesterId, agg);
        log.info("课程[{}]学期[{}]成绩统计聚合完成：{}人，平均分{}，通过率{}%",
                courseId, semesterId, stat.getTotalStudents(), stat.getAvgScore(), stat.getPassRate());
        return stat;
    }

    /**
     * 按学期批量聚合所有课程的成绩统计
     */
    @Override
    @Transactional
    public int aggregateBySemester(Long semesterId)
    {
        List<Map<String, Object>> list = aemGradeStatisticsMapper.aggregateGradeBySemester(semesterId);
        if (list == null || list.isEmpty())
        {
            return 0;
        }
        String batchNo = "STAT" + System.currentTimeMillis();
        int count = 0;
        for (Map<String, Object> agg : list)
        {
            Long courseId = toLong(agg.get("courseId"));
            if (courseId == null)
            {
                continue;
            }
            AemGradeStatistics stat = upsertStat(courseId, semesterId, agg);
            stat.setBatchNo(batchNo);
            aemGradeStatisticsMapper.updateAemGradeStatistics(stat);
            count++;
        }
        log.info("学期[{}]批量成绩统计聚合完成，共{}门课程，批次{}", semesterId, count, batchNo);
        return count;
    }

    /**
     * 分数段分布（供前端图表）
     */
    @Override
    public Map<String, Object> scoreDistribution(Long courseId, Long semesterId)
    {
        Map<String, Object> dist = aemGradeStatisticsMapper.scoreDistribution(courseId, semesterId);
        return dist == null ? Map.of() : dist;
    }

    @Override
    public Map<String, Object> semesterOverview(Long semesterId)
    {
        Map<String, Object> overview = aemGradeStatisticsMapper.semesterOverview(semesterId);
        return overview == null ? Map.of() : overview;
    }

    @Override
    public List<Map<String, Object>> courseRanking(Long courseId, Long semesterId)
    {
        return aemGradeStatisticsMapper.courseRanking(courseId, semesterId);
    }

    /**
     * 新增或更新统计快照
     */
    private AemGradeStatistics upsertStat(Long courseId, Long semesterId, Map<String, Object> agg)
    {
        AemGradeStatistics existing = aemGradeStatisticsMapper.selectByCourseAndSemester(courseId, semesterId);
        AemGradeStatistics stat = existing != null ? existing : new AemGradeStatistics();
        if (existing == null)
        {
            stat.setCourseId(courseId);
            stat.setSemesterId(semesterId);
            stat.setCreateTime(DateUtils.getNowDate());
        }
        stat.setTotalStudents(toInt(agg.get("totalStudents")));
        stat.setMaxScore(toDouble(agg.get("maxScore")));
        stat.setMinScore(toDouble(agg.get("minScore")));
        stat.setAvgScore(toDouble(agg.get("avgScore")));
        stat.setPassCount(toInt(agg.get("passCount")));
        stat.setFailCount(toInt(agg.get("failCount")));
        stat.setPassRate(toDouble(agg.get("passRate")));
        stat.setExcellentCount(toInt(agg.get("excellentCount")));
        stat.setExcellentRate(toDouble(agg.get("excellentRate")));
        stat.setStatus("0");
        stat.setStatTime(DateUtils.getNowDate());
        stat.setUpdateTime(DateUtils.getNowDate());
        if (existing != null)
        {
            aemGradeStatisticsMapper.updateAemGradeStatistics(stat);
        }
        else
        {
            aemGradeStatisticsMapper.insertAemGradeStatistics(stat);
        }
        return stat;
    }

    private Integer toInt(Object o) { return o == null ? 0 : ((Number) o).intValue(); }
    private Double toDouble(Object o) { return o == null ? 0.0 : ((Number) o).doubleValue(); }
    private Long toLong(Object o) { return o == null ? null : ((Number) o).longValue(); }
}
