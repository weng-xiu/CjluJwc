package com.yu.oa.mapper;

import java.util.List;
import com.yu.oa.domain.OaProcessDefinition;

/**
 * 流程定义快照Mapper接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface OaProcessDefinitionMapper 
{
    public OaProcessDefinition selectOaProcessDefinitionByDefinitionId(Long definitionId);
    public List<OaProcessDefinition> selectOaProcessDefinitionList(OaProcessDefinition oaProcessDefinition);
    public int insertOaProcessDefinition(OaProcessDefinition oaProcessDefinition);
    public int updateOaProcessDefinition(OaProcessDefinition oaProcessDefinition);
    public int deleteOaProcessDefinitionByDefinitionId(Long definitionId);
    public int deleteOaProcessDefinitionByDefinitionIds(Long[] definitionIds);
}
