package com.yu.system.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import com.yu.system.ai.AiAnswer;
import com.yu.system.ai.AiKnowledgeHit;
import com.yu.system.domain.SysAiEvalCase;
import com.yu.system.domain.SysAiEvalResult;
import com.yu.system.domain.SysAiEvalRun;
import com.yu.system.mapper.SysAiEvalCaseMapper;
import com.yu.system.mapper.SysAiEvalRunMapper;
import com.yu.system.service.ISysAiChatService;
import com.yu.system.service.ISysAiEvalService;
import com.yu.system.service.ISysConfigService;

/**
 * 智能体发布前评测 服务实现（K4 AI 治理 · 评测集存档 + 发布门禁）
 *
 * 逐条以启用用例驱动问答助手（复用生产链路 {@link ISysAiChatService#ask}，含内容治理），
 * 按「期望命中知识ID / 期望关键词 / 检索置信度」三档口径判定通过，汇总通过率与阈值比较得出门禁结果，
 * 批次与明细落库留痕。评测问答以 scene=eval 记入问答留痕，便于与真实提问区分。
 *
 * @author yu
 * @date 2026-10-12
 */
@Service
public class SysAiEvalServiceImpl implements ISysAiEvalService
{
    private static final Logger log = LoggerFactory.getLogger(SysAiEvalServiceImpl.class);

    /** 发布前评测通过率阈值（百分比 0~100），低于即拦截 */
    private static final String CFG_THRESHOLD = "ai.eval.pass.threshold";

    /** 用例无显式期望时的通过口径：检索置信度下限 */
    private static final String CFG_MIN_CONFIDENCE = "ai.eval.min.confidence";

    /** 评测问答场景标识 */
    private static final String EVAL_SCENE = "eval";

    @Autowired
    private SysAiEvalCaseMapper evalCaseMapper;

    @Autowired
    private SysAiEvalRunMapper evalRunMapper;

    @Autowired
    private ISysAiChatService chatService;

    @Autowired
    private ISysConfigService configService;

    @Override
    public List<SysAiEvalCase> selectEvalCaseList(SysAiEvalCase evalCase)
    {
        return evalCaseMapper.selectEvalCaseList(evalCase);
    }

    @Override
    public SysAiEvalCase selectEvalCaseById(Long caseId)
    {
        return evalCaseMapper.selectEvalCaseById(caseId);
    }

    @Override
    public int insertEvalCase(SysAiEvalCase evalCase)
    {
        evalCase.setCreateTime(DateUtils.getNowDate());
        return evalCaseMapper.insertEvalCase(evalCase);
    }

    @Override
    public int updateEvalCase(SysAiEvalCase evalCase)
    {
        evalCase.setUpdateTime(DateUtils.getNowDate());
        return evalCaseMapper.updateEvalCase(evalCase);
    }

    @Override
    public int deleteEvalCaseByIds(Long[] caseIds)
    {
        return evalCaseMapper.deleteEvalCaseByIds(caseIds);
    }

    @Override
    public SysAiEvalRun evaluate(String triggerType, String operator)
    {
        List<SysAiEvalCase> cases = evalCaseMapper.selectEnabledList();
        double threshold = doubleConfig(CFG_THRESHOLD, 80d);
        double minConfidence = doubleConfig(CFG_MIN_CONFIDENCE, 0.12d);

        List<SysAiEvalResult> results = new ArrayList<>();
        int passedCount = 0;
        double confSum = 0d;
        String modelVersion = null;

        for (SysAiEvalCase c : cases)
        {
            SysAiEvalResult r = new SysAiEvalResult();
            r.setCaseId(c.getCaseId());
            r.setQuestion(StringUtils.substring(c.getQuestion(), 0, 500));
            r.setCreateTime(DateUtils.getNowDate());
            try
            {
                AiAnswer ans = chatService.ask(c.getQuestion(), 0, EVAL_SCENE, null, operator, EVAL_SCENE);
                if (modelVersion == null && StringUtils.isNotEmpty(ans.getModelVersion()))
                {
                    modelVersion = ans.getModelVersion();
                }
                double conf = ans.getConfidence() == null ? 0d : ans.getConfidence();
                confSum += conf;
                r.setAnswer(StringUtils.substring(ans.getAnswer(), 0, 4000));
                r.setAnswerSource(ans.getAnswerSource());
                r.setConfidence(conf);
                r.setModerationResult(StringUtils.isEmpty(ans.getModerationResult()) ? "0" : ans.getModerationResult());

                boolean moderatedBlock = AiAnswer.SOURCE_MODERATED.equals(ans.getAnswerSource())
                        || "1".equals(r.getModerationResult());
                boolean matched = judgeMatched(c, ans, conf, minConfidence);
                boolean passed = matched && !moderatedBlock;
                r.setMatched(matched ? "0" : "1");
                r.setPassed(passed ? "0" : "1");
                if (passed)
                {
                    passedCount++;
                }
            }
            catch (Exception e)
            {
                log.warn("评测用例[{}]驱动问答失败：{}", c.getCaseId(), e.getMessage());
                r.setAnswerSource(AiAnswer.SOURCE_ERROR);
                r.setConfidence(0d);
                r.setModerationResult("0");
                r.setMatched("1");
                r.setPassed("1");
                r.setAnswer("评测调用异常：" + StringUtils.substring(e.getMessage(), 0, 500));
            }
            results.add(r);
        }

        int total = cases.size();
        double passRate = total > 0 ? round(passedCount * 100d / total) : 0d;
        double avgConfidence = total > 0 ? round(confSum / total, 4) : 0d;
        // 无启用用例视为无法验证，发布门禁判为拦截；通过率达标则放行
        String gateResult = (total > 0 && passRate >= threshold) ? SysAiEvalRun.RESULT_PASS : SysAiEvalRun.RESULT_BLOCK;

        SysAiEvalRun run = new SysAiEvalRun();
        run.setTriggerType(StringUtils.isEmpty(triggerType) ? SysAiEvalRun.TRIGGER_MANUAL : triggerType);
        run.setModelVersion(modelVersion);
        run.setTotalCases(total);
        run.setPassedCases(passedCount);
        run.setPassRate(passRate);
        run.setAvgConfidence(avgConfidence);
        run.setPassThreshold(threshold);
        run.setResult(gateResult);
        run.setOperator(operator);
        run.setRemark(total == 0 ? "无启用评测用例，发布门禁默认拦截" : "通过率 " + passRate + "% / 阈值 " + threshold + "%");
        run.setCreateBy(operator);
        run.setCreateTime(DateUtils.getNowDate());
        evalRunMapper.insertEvalRun(run);

        for (SysAiEvalResult r : results)
        {
            r.setRunId(run.getRunId());
            evalRunMapper.insertEvalResult(r);
        }
        return run;
    }

    /**
     * 命中判定：优先期望知识ID（与检索命中集合求交），其次期望关键词（回答须全部包含），
     * 两者皆空时以「非未命中/非失败且置信度达下限」作为通过口径。
     */
    private boolean judgeMatched(SysAiEvalCase c, AiAnswer ans, double conf, double minConfidence)
    {
        String expIds = c.getExpectKnowledgeIds();
        String expKw = c.getExpectKeywords();
        if (StringUtils.isNotBlank(expIds))
        {
            Set<String> expected = splitToSet(expIds);
            List<AiKnowledgeHit> refs = ans.getReferences();
            if (StringUtils.isNotEmpty(refs))
            {
                for (AiKnowledgeHit hit : refs)
                {
                    if (hit.getKnowledgeId() != null && expected.contains(String.valueOf(hit.getKnowledgeId())))
                    {
                        return true;
                    }
                }
            }
            return false;
        }
        if (StringUtils.isNotBlank(expKw))
        {
            String answerText = ans.getAnswer() == null ? "" : ans.getAnswer();
            for (String kw : splitToSet(expKw))
            {
                if (!answerText.contains(kw))
                {
                    return false;
                }
            }
            return true;
        }
        boolean answered = !(AiAnswer.SOURCE_NONE.equals(ans.getAnswerSource())
                || AiAnswer.SOURCE_ERROR.equals(ans.getAnswerSource())
                || AiAnswer.SOURCE_MODERATED.equals(ans.getAnswerSource()));
        return answered && conf >= minConfidence;
    }

    private Set<String> splitToSet(String joined)
    {
        Set<String> set = new HashSet<>();
        for (String part : joined.split("[,，;；\\s]+"))
        {
            String v = part.trim();
            if (!v.isEmpty())
            {
                set.add(v);
            }
        }
        return set;
    }

    @Override
    public List<SysAiEvalRun> selectEvalRunList(SysAiEvalRun run)
    {
        return evalRunMapper.selectEvalRunList(run);
    }

    @Override
    public SysAiEvalRun selectEvalRunById(Long runId)
    {
        return evalRunMapper.selectEvalRunById(runId);
    }

    @Override
    public List<SysAiEvalResult> selectResultsByRunId(Long runId)
    {
        return evalRunMapper.selectResultsByRunId(runId);
    }

    private double doubleConfig(String key, double defaultValue)
    {
        String v = configService.selectConfigByKey(key);
        try
        {
            return StringUtils.isEmpty(v) ? defaultValue : Double.parseDouble(v.trim());
        }
        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }

    private double round(double value)
    {
        return round(value, 2);
    }

    private double round(double value, int scale)
    {
        return BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
