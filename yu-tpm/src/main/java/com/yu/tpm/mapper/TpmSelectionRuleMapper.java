package com.yu.tpm.mapper;

import java.util.List;
import com.yu.tpm.domain.TpmSelectionRule;

/**
 * 选课规则Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface TpmSelectionRuleMapper 
{
    public TpmSelectionRule selectTpmSelectionRuleByRuleId(Long ruleId);
    public List<TpmSelectionRule> selectTpmSelectionRuleList(TpmSelectionRule tpmSelectionRule);
    public int insertTpmSelectionRule(TpmSelectionRule tpmSelectionRule);
    public int updateTpmSelectionRule(TpmSelectionRule tpmSelectionRule);
    public int deleteTpmSelectionRuleByRuleId(Long ruleId);
    public int deleteTpmSelectionRuleByRuleIds(Long[] ruleIds);
}
