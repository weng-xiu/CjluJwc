package com.yu.tpm.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.tpm.mapper.TpmSelectionRoundMapper;
import com.yu.tpm.mapper.TpmSelectionRuleMapper;
import com.yu.tpm.mapper.TpmSelectionEnrollmentMapper;
import com.yu.tpm.domain.TpmSelectionRound;
import com.yu.tpm.domain.TpmSelectionRule;
import com.yu.tpm.domain.TpmSelectionEnrollment;
import com.yu.tpm.service.ITpmSelectionRoundService;

/**
 * 选课轮次Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmSelectionRoundServiceImpl implements ITpmSelectionRoundService 
{
    @Autowired
    private TpmSelectionRoundMapper tpmSelectionRoundMapper;

    @Autowired
    private TpmSelectionRuleMapper tpmSelectionRuleMapper;

    @Autowired
    private TpmSelectionEnrollmentMapper tpmSelectionEnrollmentMapper;

    @Override
    public TpmSelectionRound selectTpmSelectionRoundByRoundId(Long roundId)
    {
        return tpmSelectionRoundMapper.selectTpmSelectionRoundByRoundId(roundId);
    }

    @Override
    public List<TpmSelectionRound> selectTpmSelectionRoundList(TpmSelectionRound tpmSelectionRound)
    {
        return tpmSelectionRoundMapper.selectTpmSelectionRoundList(tpmSelectionRound);
    }

    @Transactional
    @Override
    public int insertTpmSelectionRound(TpmSelectionRound tpmSelectionRound)
    {
        tpmSelectionRound.setCreateTime(DateUtils.getNowDate());
        return tpmSelectionRoundMapper.insertTpmSelectionRound(tpmSelectionRound);
    }

    @Transactional
    @Override
    public int updateTpmSelectionRound(TpmSelectionRound tpmSelectionRound)
    {
        tpmSelectionRound.setUpdateTime(DateUtils.getNowDate());
        return tpmSelectionRoundMapper.updateTpmSelectionRound(tpmSelectionRound);
    }

    @Transactional
    @Override
    public int deleteTpmSelectionRoundByRoundId(Long roundId)
    {
        TpmSelectionRule ruleQuery = new TpmSelectionRule();
        ruleQuery.setRoundId(roundId);
        List<TpmSelectionRule> rules = tpmSelectionRuleMapper.selectTpmSelectionRuleList(ruleQuery);
        if (rules != null && !rules.isEmpty())
        {
            throw new ServiceException("该选课轮次下存在选课规则，不允许删除");
        }
        TpmSelectionEnrollment enrollQuery = new TpmSelectionEnrollment();
        enrollQuery.setRoundId(roundId);
        List<TpmSelectionEnrollment> enrollments = tpmSelectionEnrollmentMapper.selectTpmSelectionEnrollmentList(enrollQuery);
        if (enrollments != null && !enrollments.isEmpty())
        {
            throw new ServiceException("该选课轮次下存在选课记录，不允许删除");
        }
        return tpmSelectionRoundMapper.deleteTpmSelectionRoundByRoundId(roundId);
    }

    @Transactional
    @Override
    public int deleteTpmSelectionRoundByRoundIds(Long[] roundIds)
    {
        for (Long roundId : roundIds)
        {
            TpmSelectionRule ruleQuery = new TpmSelectionRule();
            ruleQuery.setRoundId(roundId);
            List<TpmSelectionRule> rules = tpmSelectionRuleMapper.selectTpmSelectionRuleList(ruleQuery);
            if (rules != null && !rules.isEmpty())
            {
                throw new ServiceException("该选课轮次下存在选课规则，不允许删除");
            }
            TpmSelectionEnrollment enrollQuery = new TpmSelectionEnrollment();
            enrollQuery.setRoundId(roundId);
            List<TpmSelectionEnrollment> enrollments = tpmSelectionEnrollmentMapper.selectTpmSelectionEnrollmentList(enrollQuery);
            if (enrollments != null && !enrollments.isEmpty())
            {
                throw new ServiceException("该选课轮次下存在选课记录，不允许删除");
            }
        }
        return tpmSelectionRoundMapper.deleteTpmSelectionRoundByRoundIds(roundIds);
    }
}
