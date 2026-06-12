package com.yu.tpm.service.impl;

import java.util.List;
import com.yu.common.annotation.DataScope;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.tpm.mapper.TpmTrainingPlanMapper;
import com.yu.tpm.mapper.TpmCreditStructureMapper;
import com.yu.tpm.domain.TpmTrainingPlan;
import com.yu.tpm.domain.TpmCreditStructure;
import com.yu.tpm.service.ITpmTrainingPlanService;

/**
 * 人才培养方案Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmTrainingPlanServiceImpl implements ITpmTrainingPlanService 
{
    @Autowired
    private TpmTrainingPlanMapper tpmTrainingPlanMapper;

    @Autowired
    private TpmCreditStructureMapper tpmCreditStructureMapper;

    @Override
    public TpmTrainingPlan selectTpmTrainingPlanByPlanId(Long planId)
    {
        return tpmTrainingPlanMapper.selectTpmTrainingPlanByPlanId(planId);
    }

    @Override
    @DataScope(deptAlias = "d")
    public List<TpmTrainingPlan> selectTpmTrainingPlanList(TpmTrainingPlan tpmTrainingPlan)
    {
        return tpmTrainingPlanMapper.selectTpmTrainingPlanList(tpmTrainingPlan);
    }

    @Transactional
    @Override
    public int insertTpmTrainingPlan(TpmTrainingPlan tpmTrainingPlan)
    {
        tpmTrainingPlan.setCreateTime(DateUtils.getNowDate());
        return tpmTrainingPlanMapper.insertTpmTrainingPlan(tpmTrainingPlan);
    }

    @Transactional
    @Override
    public int updateTpmTrainingPlan(TpmTrainingPlan tpmTrainingPlan)
    {
        tpmTrainingPlan.setUpdateTime(DateUtils.getNowDate());
        return tpmTrainingPlanMapper.updateTpmTrainingPlan(tpmTrainingPlan);
    }

    @Transactional
    @Override
    public int deleteTpmTrainingPlanByPlanId(Long planId)
    {
        TpmCreditStructure query = new TpmCreditStructure();
        query.setPlanId(planId);
        List<TpmCreditStructure> structures = tpmCreditStructureMapper.selectTpmCreditStructureList(query);
        if (structures != null && !structures.isEmpty())
        {
            throw new ServiceException("该培养方案下存在学分结构，不允许删除");
        }
        return tpmTrainingPlanMapper.deleteTpmTrainingPlanByPlanId(planId);
    }

    @Transactional
    @Override
    public int deleteTpmTrainingPlanByPlanIds(Long[] planIds)
    {
        for (Long planId : planIds)
        {
            TpmCreditStructure query = new TpmCreditStructure();
            query.setPlanId(planId);
            List<TpmCreditStructure> structures = tpmCreditStructureMapper.selectTpmCreditStructureList(query);
            if (structures != null && !structures.isEmpty())
            {
                throw new ServiceException("该培养方案下存在学分结构，不允许删除");
            }
        }
        return tpmTrainingPlanMapper.deleteTpmTrainingPlanByPlanIds(planIds);
    }
}
