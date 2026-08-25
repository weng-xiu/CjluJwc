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
    public AemExamPlan selectAemExamPlanDetail(Long examId)
    {
        AemExamPlan plan = aemExamPlanMapper.selectAemExamPlanByExamId(examId);
        if (plan == null)
        {
            return null;
        }
        AemExamSeat seatQuery = new AemExamSeat();
        seatQuery.setExamId(examId);
        plan.setSeats(aemExamSeatMapper.selectAemExamSeatList(seatQuery));

        AemExamInvigilation invQuery = new AemExamInvigilation();
        invQuery.setExamId(examId);
        plan.setInvigilations(aemExamInvigilationMapper.selectAemExamInvigilationList(invQuery));
        return plan;
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
        // 级联校验：发布状态的考试安排必须至少完成座位或监考编排
        if ("2".equals(aemExamPlan.getPlanStatus()))
        {
            AemExamSeat seatQuery = new AemExamSeat();
            seatQuery.setExamId(aemExamPlan.getExamId());
            List<AemExamSeat> seats = aemExamSeatMapper.selectAemExamSeatList(seatQuery);
            AemExamInvigilation invQuery = new AemExamInvigilation();
            invQuery.setExamId(aemExamPlan.getExamId());
            List<AemExamInvigilation> invs = aemExamInvigilationMapper.selectAemExamInvigilationList(invQuery);
            if ((seats == null || seats.isEmpty()) && (invs == null || invs.isEmpty()))
            {
                throw new ServiceException("发布前请先完成座位编排或监考安排");
            }
        }
        aemExamPlan.setUpdateTime(DateUtils.getNowDate());
        return aemExamPlanMapper.updateAemExamPlan(aemExamPlan);
    }

    @Override
    @Transactional
    public int deleteAemExamPlanByExamId(Long examId)
    {
        // 级联删除子表，保证主子表数据一致性
        aemExamSeatMapper.deleteByExamId(examId);
        aemExamInvigilationMapper.deleteByExamId(examId);
        return aemExamPlanMapper.deleteAemExamPlanByExamId(examId);
    }

    @Override
    @Transactional
    public int deleteAemExamPlanByExamIds(Long[] examIds)
    {
        for (Long examId : examIds)
        {
            aemExamSeatMapper.deleteByExamId(examId);
            aemExamInvigilationMapper.deleteByExamId(examId);
        }
        return aemExamPlanMapper.deleteAemExamPlanByExamIds(examIds);
    }
}
