package com.yu.tpm.service;

import java.util.List;
import com.yu.tpm.domain.TpmSelectionRule;

/**
 * 选课规则Service接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface ITpmSelectionRuleService 
{
    public TpmSelectionRule selectTpmSelectionRuleByRuleId(Long ruleId);
    public List<TpmSelectionRule> selectTpmSelectionRuleList(TpmSelectionRule tpmSelectionRule);
    public int insertTpmSelectionRule(TpmSelectionRule tpmSelectionRule);
    public int updateTpmSelectionRule(TpmSelectionRule tpmSelectionRule);
    public int deleteTpmSelectionRuleByRuleIds(Long[] ruleIds);
    public int deleteTpmSelectionRuleByRuleId(Long ruleId);

    /**
     * 规则引擎：校验学生选课时是否违反该轮次下启用的规则
     *
     * @param roundId          轮次ID
     * @param studentId        学生ID
     * @param courseOfferingId 开课ID
     * @return 违规提示列表，为空表示全部通过
     */
    public List<String> validate(Long roundId, Long studentId, Long courseOfferingId);
}
