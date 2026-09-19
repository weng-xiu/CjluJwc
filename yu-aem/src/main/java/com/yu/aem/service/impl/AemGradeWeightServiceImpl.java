package com.yu.aem.service.impl;

import java.util.List;
import java.util.Map;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.aem.domain.AemGradeWeight;
import com.yu.aem.mapper.AemGradeWeightMapper;
import com.yu.aem.service.IAemGradeWeightService;

/**
 * 成绩权重配置Service业务层处理（A4）
 *
 * @author ruoyi
 * @date 2026-09-19
 */
@Service
public class AemGradeWeightServiceImpl implements IAemGradeWeightService
{
    /** 系统默认权重：平时30% + 考试70%（与历史硬编码口径一致） */
    private static final double[] DEFAULT_RATIOS = {0.3, 0.7};

    @Autowired
    private AemGradeWeightMapper aemGradeWeightMapper;

    @Override
    public AemGradeWeight selectAemGradeWeightByWeightId(Long weightId)
    {
        return aemGradeWeightMapper.selectAemGradeWeightByWeightId(weightId);
    }

    @Override
    public List<AemGradeWeight> selectAemGradeWeightList(AemGradeWeight aemGradeWeight)
    {
        return aemGradeWeightMapper.selectAemGradeWeightList(aemGradeWeight);
    }

    @Transactional
    @Override
    public int insertAemGradeWeight(AemGradeWeight aemGradeWeight)
    {
        checkWeight(aemGradeWeight);
        aemGradeWeight.setCreateTime(DateUtils.getNowDate());
        return aemGradeWeightMapper.insertAemGradeWeight(aemGradeWeight);
    }

    @Transactional
    @Override
    public int updateAemGradeWeight(AemGradeWeight aemGradeWeight)
    {
        checkWeight(aemGradeWeight);
        aemGradeWeight.setUpdateTime(DateUtils.getNowDate());
        return aemGradeWeightMapper.updateAemGradeWeight(aemGradeWeight);
    }

    @Transactional
    @Override
    public int deleteAemGradeWeightByWeightIds(Long[] weightIds)
    {
        return aemGradeWeightMapper.deleteAemGradeWeightByWeightIds(weightIds);
    }

    /**
     * 保存校验：占比之和须为100；同一课程/同一类别仅允许一条配置（NULL 组合无法建库级唯一索引，代码层保证）。
     */
    private void checkWeight(AemGradeWeight weight)
    {
        double regular = weight.getRegularRatio() == null ? -1 : weight.getRegularRatio();
        double exam = weight.getExamRatio() == null ? -1 : weight.getExamRatio();
        if (regular < 0 || exam < 0 || regular > 100 || exam > 100 || Math.abs(regular + exam - 100) > 0.001)
        {
            throw new ServiceException("平时占比与考试占比之和必须等于100");
        }
        AemGradeWeight query = new AemGradeWeight();
        query.setCourseId(weight.getCourseId());
        query.setCourseCategory(weight.getCourseCategory());
        List<AemGradeWeight> exists = aemGradeWeightMapper.selectAemGradeWeightList(query);
        if (exists != null)
        {
            for (AemGradeWeight e : exists)
            {
                boolean sameCourse = weight.getCourseId() != null && weight.getCourseId().equals(e.getCourseId());
                boolean sameCategory = StringUtils.isNotBlank(weight.getCourseCategory())
                        && weight.getCourseCategory().equals(e.getCourseCategory()) && e.getCourseId() == null;
                boolean bothGlobal = weight.getCourseId() == null && StringUtils.isBlank(weight.getCourseCategory())
                        && e.getCourseId() == null && StringUtils.isBlank(e.getCourseCategory());
                if ((sameCourse || sameCategory || bothGlobal)
                        && !e.getWeightId().equals(weight.getWeightId()))
                {
                    throw new ServiceException("相同课程/类别的权重配置已存在（配置ID：" + e.getWeightId() + "），请直接修改而非新增");
                }
            }
        }
    }

    @Override
    public double[] resolveRatios(Long courseId)
    {
        if (courseId != null)
        {
            Map<String, Object> row = aemGradeWeightMapper.selectEffectiveWeightByCourseId(courseId);
            if (row != null && row.get("regularRatio") != null && row.get("examRatio") != null)
            {
                double regular = ((Number) row.get("regularRatio")).doubleValue();
                double exam = ((Number) row.get("examRatio")).doubleValue();
                double sum = regular + exam;
                if (sum > 0)
                {
                    // 归一化为小数占比，容忍 30/70 与 0.3/0.7 两种录入口径
                    return new double[]{regular / sum, exam / sum};
                }
            }
        }
        return DEFAULT_RATIOS;
    }
}
