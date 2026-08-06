package com.yu.aem.service.impl;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.yu.common.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.aem.mapper.AemEvaluationResultMapper;
import com.yu.aem.domain.AemEvaluationResult;
import com.yu.aem.service.IAemEvaluationResultService;

/**
 * 评教结果Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@Service
public class AemEvaluationResultServiceImpl implements IAemEvaluationResultService
{
    private static final Logger log = LoggerFactory.getLogger(AemEvaluationResultServiceImpl.class);

    @Autowired
    private AemEvaluationResultMapper aemEvaluationResultMapper;

    @Override
    public AemEvaluationResult selectAemEvaluationResultByResultId(Long resultId)
    {
        return aemEvaluationResultMapper.selectAemEvaluationResultByResultId(resultId);
    }

    @Override
    public List<AemEvaluationResult> selectAemEvaluationResultList(AemEvaluationResult aemEvaluationResult)
    {
        return aemEvaluationResultMapper.selectAemEvaluationResultList(aemEvaluationResult);
    }

    @Override
    @Transactional
    public int insertAemEvaluationResult(AemEvaluationResult aemEvaluationResult)
    {
        aemEvaluationResult.setCreateTime(DateUtils.getNowDate());
        return aemEvaluationResultMapper.insertAemEvaluationResult(aemEvaluationResult);
    }

    @Override
    @Transactional
    public int updateAemEvaluationResult(AemEvaluationResult aemEvaluationResult)
    {
        aemEvaluationResult.setUpdateTime(DateUtils.getNowDate());
        return aemEvaluationResultMapper.updateAemEvaluationResult(aemEvaluationResult);
    }

    @Override
    @Transactional
    public int deleteAemEvaluationResultByResultId(Long resultId)
    {
        return aemEvaluationResultMapper.deleteAemEvaluationResultByResultId(resultId);
    }

    @Override
    @Transactional
    public int deleteAemEvaluationResultByResultIds(Long[] resultIds)
    {
        return aemEvaluationResultMapper.deleteAemEvaluationResultByResultIds(resultIds);
    }

    /**
     * 按教师和课程聚合评教结果
     */
    @Override
    public Map<String, Object> aggregateByTeacher(Long teacherId, Long courseId)
    {
        Map<String, Object> result = new HashMap<>();
        // 查询该教师该课程的所有评教结果
        AemEvaluationResult query = new AemEvaluationResult();
        query.setTeacherId(teacherId);
        query.setCourseId(courseId);
        query.setStatus("0");
        List<AemEvaluationResult> results = aemEvaluationResultMapper.selectAemEvaluationResultList(query);
        if (results == null || results.isEmpty())
        {
            result.put("message", "无评教数据");
            result.put("totalCount", 0);
            return result;
        }
        // 聚合计算
        int totalCount = results.size();
        double sumScore = 0;
        double maxScore = Double.MIN_VALUE;
        double minScore = Double.MAX_VALUE;
        int validCount = 0;
        for (AemEvaluationResult r : results)
        {
            if (r.getTotalScore() != null)
            {
                double score = r.getTotalScore();
                sumScore += score;
                if (score > maxScore) maxScore = score;
                if (score < minScore) minScore = score;
                validCount++;
            }
        }
        double avgScore = validCount > 0 ? sumScore / validCount : 0;
        avgScore = Math.round(avgScore * 100) / 100.0;
        result.put("teacherId", teacherId);
        result.put("courseId", courseId);
        result.put("totalCount", totalCount);
        result.put("validCount", validCount);
        result.put("avgScore", avgScore);
        if (validCount > 0)
        {
            result.put("maxScore", maxScore);
            result.put("minScore", minScore);
        }
        result.put("message", String.format("评教聚合完成：共%d条评教，平均分%s", totalCount, avgScore));
        log.info("教师[{}]课程[{}]评教聚合完成：{}条评教，平均分{}", teacherId, courseId, totalCount, avgScore);
        return result;
    }
}
