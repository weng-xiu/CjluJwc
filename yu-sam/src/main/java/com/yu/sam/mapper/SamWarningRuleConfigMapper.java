package com.yu.sam.mapper;

import java.util.List;
import com.yu.sam.domain.SamWarningRuleConfig;

/**
 * 预警规则配置Mapper接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface SamWarningRuleConfigMapper 
{
    /**
     * 查询预警规则配置
     * 
     * @param ruleId 规则ID
     * @return 预警规则配置
     */
    public SamWarningRuleConfig selectSamWarningRuleConfigById(Long ruleId);

    /**
     * 查询预警规则配置列表
     * 
     * @param samWarningRuleConfig 预警规则配置
     * @return 预警规则配置集合
     */
    public List<SamWarningRuleConfig> selectSamWarningRuleConfigList(SamWarningRuleConfig samWarningRuleConfig);

    /**
     * 查询所有启用的规则
     * 
     * @return 启用的规则集合
     */
    public List<SamWarningRuleConfig> selectEnabledRules();

    /**
     * 按规则代码查询
     * 
     * @param ruleCode 规则代码
     * @return 预警规则配置
     */
    public SamWarningRuleConfig selectByRuleCode(String ruleCode);

    /**
     * 新增预警规则配置
     * 
     * @param samWarningRuleConfig 预警规则配置
     * @return 结果
     */
    public int insertSamWarningRuleConfig(SamWarningRuleConfig samWarningRuleConfig);

    /**
     * 修改预警规则配置
     * 
     * @param samWarningRuleConfig 预警规则配置
     * @return 结果
     */
    public int updateSamWarningRuleConfig(SamWarningRuleConfig samWarningRuleConfig);

    /**
     * 删除预警规则配置
     * 
     * @param ruleId 规则ID
     * @return 结果
     */
    public int deleteSamWarningRuleConfigById(Long ruleId);

    /**
     * 批量删除预警规则配置
     * 
     * @param ruleIds 需要删除的数据ID
     * @return 结果
     */
    public int deleteSamWarningRuleConfigByIds(Long[] ruleIds);
}
