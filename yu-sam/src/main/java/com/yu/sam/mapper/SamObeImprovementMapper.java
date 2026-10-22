package com.yu.sam.mapper;

import java.util.List;
import com.yu.sam.domain.SamObeImprovement;

/**
 * OBE持续改进台账 Mapper 接口（F3 整改闭环）
 *
 * @author N1
 */
public interface SamObeImprovementMapper
{
    List<SamObeImprovement> selectSamObeImprovementList(SamObeImprovement query);

    SamObeImprovement selectSamObeImprovementByImpId(Long impId);

    int insertSamObeImprovement(SamObeImprovement record);

    int updateSamObeImprovement(SamObeImprovement record);

    int deleteSamObeImprovementByImpIds(Long[] impIds);
}
