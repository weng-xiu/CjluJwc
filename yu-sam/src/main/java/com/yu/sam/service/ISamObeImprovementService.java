package com.yu.sam.service;

import java.util.List;
import com.yu.sam.domain.SamObeImprovement;

/**
 * OBE持续改进台账 Service 接口（F3 整改闭环）
 *
 * @author N1
 */
public interface ISamObeImprovementService
{
    List<SamObeImprovement> selectImprovementList(SamObeImprovement query);

    SamObeImprovement selectImprovementByImpId(Long impId);

    int insertImprovement(SamObeImprovement record);

    int updateImprovement(SamObeImprovement record);

    int deleteImprovementByImpIds(Long[] impIds);
}
