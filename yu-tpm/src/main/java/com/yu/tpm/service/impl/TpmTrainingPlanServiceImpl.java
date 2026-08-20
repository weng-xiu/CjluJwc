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
import com.yu.tpm.mapper.TpmCourseLibraryMapper;
import com.yu.tpm.domain.TpmTrainingPlan;
import com.yu.tpm.domain.TpmCreditStructure;
import com.yu.tpm.domain.TpmCourseLibrary;
import com.yu.tpm.service.ITpmTrainingPlanService;
import com.yu.tpm.service.ITpmCourseLibraryService;
import com.yu.tpm.service.ITpmCreditStructureService;

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

    @Autowired
    private TpmCourseLibraryMapper tpmCourseLibraryMapper;

    @Autowired
    private ITpmCourseLibraryService tpmCourseLibraryService;

    @Autowired
    private ITpmCreditStructureService tpmCreditStructureService;

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
        checkCanDelete(planId);
        return tpmTrainingPlanMapper.deleteTpmTrainingPlanByPlanId(planId);
    }

    @Transactional
    @Override
    public int deleteTpmTrainingPlanByPlanIds(Long[] planIds)
    {
        for (Long planId : planIds)
        {
            checkCanDelete(planId);
        }
        return tpmTrainingPlanMapper.deleteTpmTrainingPlanByPlanIds(planIds);
    }

    /**
     * 删除前级联校验：不允许存在学分结构或课程
     */
    private void checkCanDelete(Long planId)
    {
        TpmCreditStructure structQuery = new TpmCreditStructure();
        structQuery.setPlanId(planId);
        List<TpmCreditStructure> structures = tpmCreditStructureMapper.selectTpmCreditStructureList(structQuery);
        if (structures != null && !structures.isEmpty())
        {
            throw new ServiceException("该培养方案下存在学分结构，不允许删除");
        }
        TpmCourseLibrary courseQuery = new TpmCourseLibrary();
        courseQuery.setPlanId(planId);
        List<TpmCourseLibrary> courses = tpmCourseLibraryMapper.selectTpmCourseLibraryList(courseQuery);
        if (courses != null && !courses.isEmpty())
        {
            throw new ServiceException("该培养方案下存在课程，不允许删除");
        }
    }

    @Transactional
    @Override
    public int publishTrainingPlan(Long planId)
    {
        TpmTrainingPlan plan = new TpmTrainingPlan();
        plan.setPlanId(planId);
        plan.setPublishStatus("1");
        plan.setPublishDate(DateUtils.getNowDate());
        plan.setUpdateTime(DateUtils.getNowDate());
        return tpmTrainingPlanMapper.updateTpmTrainingPlan(plan);
    }

    @Transactional
    @Override
    public int deprecateTrainingPlan(Long planId)
    {
        TpmTrainingPlan plan = new TpmTrainingPlan();
        plan.setPlanId(planId);
        plan.setPublishStatus("2");
        plan.setUpdateTime(DateUtils.getNowDate());
        return tpmTrainingPlanMapper.updateTpmTrainingPlan(plan);
    }

    @Transactional
    @Override
    public int savePlanWithChildren(TpmTrainingPlan plan, List<TpmCourseLibrary> courseList, List<TpmCreditStructure> creditList)
    {
        int rows;
        if (plan.getPlanId() == null)
        {
            rows = insertTpmTrainingPlan(plan);
        }
        else
        {
            rows = updateTpmTrainingPlan(plan);
        }
        if (courseList != null)
        {
            for (TpmCourseLibrary course : courseList)
            {
                course.setPlanId(plan.getPlanId());
                if (course.getStatus() == null)
                {
                    course.setStatus("0");
                }
                if (course.getCourseId() == null)
                {
                    tpmCourseLibraryService.insertTpmCourseLibrary(course);
                }
                else
                {
                    tpmCourseLibraryService.updateTpmCourseLibrary(course);
                }
            }
        }
        if (creditList != null)
        {
            for (TpmCreditStructure credit : creditList)
            {
                credit.setPlanId(plan.getPlanId());
                if (credit.getStatus() == null)
                {
                    credit.setStatus("0");
                }
                if (credit.getStructId() == null)
                {
                    tpmCreditStructureService.insertTpmCreditStructure(credit);
                }
                else
                {
                    tpmCreditStructureService.updateTpmCreditStructure(credit);
                }
            }
        }
        return rows;
    }
}
