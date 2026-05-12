package com.yu.aem.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.aem.mapper.AemExamPlanMapper;
import com.yu.aem.domain.AemExamPlan;
import com.yu.aem.service.IAemExamPlanService;

/**
 * 考试安排Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@Service
public class AemExamPlanServiceImpl implements IAemExamPlanService 
{
    @Autowired
    private AemExamPlanMapper aemExamPlanMapper;

    @Override
    public AemExamPlan selectAemExamPlanByExamId(Long examId)
    {
        return aemExamPlanMapper.selectAemExamPlanByExamId(examId);
    }

    @Override
    public List<AemExamPlan> selectAemExamPlanList(AemExamPlan aemExamPlan)
    {
        return aemExamPlanMapper.selectAemExamPlanList(aemExamPlan);
    }

    @Override
    public int insertAemExamPlan(AemExamPlan aemExamPlan)
    {
        aemExamPlan.setCreateTime(DateUtils.getNowDate());
        return aemExamPlanMapper.insertAemExamPlan(aemExamPlan);
    }

    @Override
    public int updateAemExamPlan(AemExamPlan aemExamPlan)
    {
        aemExamPlan.setUpdateTime(DateUtils.getNowDate());
        return aemExamPlanMapper.updateAemExamPlan(aemExamPlan);
    }

    @Override
    public int deleteAemExamPlanByExamId(Long examId)
    {
        return aemExamPlanMapper.deleteAemExamPlanByExamId(examId);
    }

    @Override
    public int deleteAemExamPlanByExamIds(Long[] examIds)
    {
        return aemExamPlanMapper.deleteAemExamPlanByExamIds(examIds);
    }
}
