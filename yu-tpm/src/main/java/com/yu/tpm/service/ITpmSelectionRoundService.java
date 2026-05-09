package com.yu.tpm.service;

import java.util.List;
import com.yu.tpm.domain.TpmSelectionRound;

/**
 * 选课轮次Service接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface ITpmSelectionRoundService 
{
    public TpmSelectionRound selectTpmSelectionRoundByRoundId(Long roundId);
    public List<TpmSelectionRound> selectTpmSelectionRoundList(TpmSelectionRound tpmSelectionRound);
    public int insertTpmSelectionRound(TpmSelectionRound tpmSelectionRound);
    public int updateTpmSelectionRound(TpmSelectionRound tpmSelectionRound);
    public int deleteTpmSelectionRoundByRoundIds(Long[] roundIds);
    public int deleteTpmSelectionRoundByRoundId(Long roundId);
}
