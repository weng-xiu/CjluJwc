package com.yu.aem.service.impl;

import java.util.List;
import com.yu.common.annotation.DataScope;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.aem.mapper.AemExamPlanMapper;
import com.yu.aem.mapper.AemExamSeatMapper;
import com.yu.aem.mapper.AemExamInvigilationMapper;
import com.yu.aem.domain.AemExamPlan;
import com.yu.aem.domain.AemExamSeat;
import com.yu.aem.domain.AemExamInvigilation;
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

    @Autowired
    private AemExamSeatMapper aemExamSeatMapper;

    @Autowired
    private AemExamInvigilationMapper aemExamInvigilationMapper;

    @Override
    public AemExamPlan selectAemExamPlanByExamId(Long examId)
    {
        return aemExamPlanMapper.selectAemExamPlanByExamId(examId);
    }

    @Override
    @DataScope(deptAlias = "d")
    public List<AemExamPlan> selectAemExamPlanList(AemExamPlan aemExamPlan)
    {
        return aemExamPlanMapper.selectAemExamPlanList(aemExamPlan);
    }

    @Override
    @Transactional
    public int insertAemExamPlan(AemExamPlan aemExamPlan)
    {
        aemExamPlan.setCreateTime(DateUtils.getNowDate());
        return aemExamPlanMapper.insertAemExamPlan(aemExamPlan);
    }

    @Override
    @Transactional
    public int updateAemExamPlan(AemExamPlan aemExamPlan)
    {
        aemExamPlan.setUpdateTime(DateUtils.getNowDate());
        return aemExamPlanMapper.updateAemExamPlan(aemExamPlan);
    }

    @Override
    @Transactional
    public int deleteAemExamPlanByExamId(Long examId)
    {
        AemExamSeat seatQuery = new AemExamSeat();
        seatQuery.setExamId(examId);
        List<AemExamSeat> seats = aemExamSeatMapper.selectAemExamSeatList(seatQuery);
        if (seats != null && !seats.isEmpty())
        {
            throw new ServiceException("该考试计划下存在座位安排，不允许删除");
        }
        AemExamInvigilation invQuery = new AemExamInvigilation();
        invQuery.setExamId(examId);
        List<AemExamInvigilation> invigilations = aemExamInvigilationMapper.selectAemExamInvigilationList(invQuery);
        if (invigilations != null && !invigilations.isEmpty())
        {
            throw new ServiceException("该考试计划下存在监考安排，不允许删除");
        }
        return aemExamPlanMapper.deleteAemExamPlanByExamId(examId);
    }

    @Override
    @Transactional
    public int deleteAemExamPlanByExamIds(Long[] examIds)
    {
        for (Long examId : examIds)
        {
            AemExamSeat seatQuery = new AemExamSeat();
            seatQuery.setExamId(examId);
            List<AemExamSeat> seats = aemExamSeatMapper.selectAemExamSeatList(seatQuery);
            if (seats != null && !seats.isEmpty())
            {
                throw new ServiceException("该考试计划下存在座位安排，不允许删除");
            }
            AemExamInvigilation invQuery = new AemExamInvigilation();
            invQuery.setExamId(examId);
            List<AemExamInvigilation> invigilations = aemExamInvigilationMapper.selectAemExamInvigilationList(invQuery);
            if (invigilations != null && !invigilations.isEmpty())
            {
                throw new ServiceException("该考试计划下存在监考安排，不允许删除");
            }
        }
        return aemExamPlanMapper.deleteAemExamPlanByExamIds(examIds);
    }
}
