package com.yu.tpm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.tpm.mapper.TpmSelectionRoundMapper;
import com.yu.tpm.domain.TpmSelectionRound;
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

    @Override
    public int insertTpmSelectionRound(TpmSelectionRound tpmSelectionRound)
    {
        tpmSelectionRound.setCreateTime(DateUtils.getNowDate());
        return tpmSelectionRoundMapper.insertTpmSelectionRound(tpmSelectionRound);
    }

    @Override
    public int updateTpmSelectionRound(TpmSelectionRound tpmSelectionRound)
    {
        tpmSelectionRound.setUpdateTime(DateUtils.getNowDate());
        return tpmSelectionRoundMapper.updateTpmSelectionRound(tpmSelectionRound);
    }

    @Override
    public int deleteTpmSelectionRoundByRoundId(Long roundId)
    {
        return tpmSelectionRoundMapper.deleteTpmSelectionRoundByRoundId(roundId);
    }

    @Override
    public int deleteTpmSelectionRoundByRoundIds(Long[] roundIds)
    {
        return tpmSelectionRoundMapper.deleteTpmSelectionRoundByRoundIds(roundIds);
    }
}
