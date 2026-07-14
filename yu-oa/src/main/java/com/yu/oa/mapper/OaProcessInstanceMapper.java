package com.yu.oa.mapper;

import java.util.List;
import com.yu.oa.domain.OaProcessInstance;

/**
 * 业务流程实例关联Mapper接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface OaProcessInstanceMapper 
{
    public OaProcessInstance selectOaProcessInstanceByInstanceId(Long instanceId);
    public OaProcessInstance selectOaProcessInstanceByBusiness(String businessType, Long businessId);
    public List<OaProcessInstance> selectOaProcessInstanceList(OaProcessInstance oaProcessInstance);
    public int insertOaProcessInstance(OaProcessInstance oaProcessInstance);
    public int updateOaProcessInstance(OaProcessInstance oaProcessInstance);
    public int deleteOaProcessInstanceByInstanceId(Long instanceId);
    public int deleteOaProcessInstanceByInstanceIds(Long[] instanceIds);
}
