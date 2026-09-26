package com.yu.system.service.impl;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yu.common.utils.DateUtils;
import com.yu.common.utils.SecurityUtils;
import com.yu.common.utils.StringUtils;
import com.yu.common.core.domain.entity.SysRole;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.system.ai.AiAnswer;
import com.yu.system.ai.AiKnowledgeHit;
import com.yu.system.ai.AiTextAnalyzer;
import com.yu.system.domain.SysAiChatRecord;
import com.yu.system.domain.SysAiKnowledge;
import com.yu.system.mapper.SysAiChatRecordMapper;
import com.yu.system.service.IAiLlmClient;
import com.yu.system.service.ISysAiChatService;
import com.yu.system.service.ISysAiKnowledgeService;
import com.yu.system.service.ISysConfigService;

/**
 * AI 智能问答 服务实现（Phase34 AI应用试点）
 *
 * 作答编排遵循三条原则：
 * 1) 只依据知识库作答——检索未命中时明确告知并给出可用问法，绝不生成无依据结论；
 * 2) 大模型可插拔——未配置模型时降级为本地抽取式回答，配置后自动转为 RAG 生成；
 *    模型调用异常同样降级为抽取式回答，并把失败原因写入留痕，保证门户可用性与可审计性；
 * 3) 每次问答都留痕——回答来源、引用条目、置信度、耗时全部入库，支撑「AI 是否真的可用」量化评估。
 *
 * @author yu
 * @date 2026-09-26
 */
@Service
public class SysAiChatServiceImpl implements ISysAiChatService
{
    private static final Logger log = LoggerFactory.getLogger(SysAiChatServiceImpl.class);

    /** 检索返回条数 */
    private static final String CFG_TOP_K = "ai.qa.topK";

    /** 系统提示：约束模型只能依据检索到的知识作答 */
    private static final String SYSTEM_PROMPT = "你是长江大学教务管理系统的教务政策咨询助手，服务于本校师生。"
            + "回答必须严格依据【已知依据】，不得编造依据中未出现的规则、时间、比例或数字；"
            + "依据不足以回答时，直接说明「知识库暂无对应口径，请咨询教务处」，并指出缺口所在。"
            + "用简体中文回答，先给结论再分点说明，篇幅控制在 300 字以内，末尾以「依据：」列出所引用条目的来源。";

    @Autowired
    private ISysAiKnowledgeService knowledgeService;

    @Autowired
    private IAiLlmClient aiLlmClient;

    @Autowired
    private SysAiChatRecordMapper sysAiChatRecordMapper;

    @Autowired
    private ISysConfigService configService;

    @Override
    public AiAnswer ask(String question, int topK, String scene, Long userId, String userName, String userRole)
    {
        long start = System.currentTimeMillis();
        AiAnswer answer = new AiAnswer();
        answer.setQuestion(question);
        answer.setEngineNote(aiLlmClient.describeEngine());

        String q = StringUtils.isBlank(question) ? "" : question.trim();
        int limit = topK > 0 ? topK : intConfig(CFG_TOP_K, 4);
        List<AiKnowledgeHit> hits = knowledgeService.retrieve(q, limit);
        answer.setReferences(hits);

        String errorMsg = null;
        if (hits.isEmpty())
        {
            answer.setAnswerSource(AiAnswer.SOURCE_NONE);
            answer.setConfidence(0d);
            answer.setAnswer(buildMissMessage(q));
        }
        else
        {
            answer.setConfidence(hits.get(0).getScore());
            if (aiLlmClient.isEnabled())
            {
                try
                {
                    answer.setAnswer(aiLlmClient.complete(SYSTEM_PROMPT, buildUserPrompt(q, hits)));
                    answer.setAnswerSource(AiAnswer.SOURCE_LLM);
                }
                catch (Exception e)
                {
                    // 模型不可用时降级为本地抽取，回答仍然可用，但要如实标注来源
                    errorMsg = StringUtils.substring(safeMessage(e), 0, 480);
                    log.warn("AI大模型调用失败，降级为本地抽取式回答：{}", errorMsg);
                    answer.setAnswer(buildExtractAnswer(q, hits));
                    answer.setAnswerSource(AiAnswer.SOURCE_ERROR);
                    answer.setErrorMsg(errorMsg);
                }
            }
            else
            {
                answer.setAnswer(buildExtractAnswer(q, hits));
                answer.setAnswerSource(AiAnswer.SOURCE_EXTRACT);
            }
            knowledgeService.markHit(idsOf(hits));
        }
        answer.setCostTime((int) (System.currentTimeMillis() - start));

        saveRecord(q, answer, scene, userId, userName, userRole);
        return answer;
    }

    @Override
    public AiAnswer ask(String question, int topK, String scene)
    {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        return ask(question, topK, scene, loginUser.getUserId(), loginUser.getUsername(), roleKeysOf(loginUser));
    }

    /**
     * 汇总角色标识，供统计页按角色区分提问人
     */
    private String roleKeysOf(LoginUser loginUser)
    {
        if (loginUser == null || loginUser.getUser() == null || loginUser.getUser().getRoles() == null)
        {
            return "";
        }
        List<String> keys = new ArrayList<>();
        for (SysRole role : loginUser.getUser().getRoles())
        {
            if (StringUtils.isNotEmpty(role.getRoleKey()))
            {
                keys.add(role.getRoleKey());
            }
        }
        return StringUtils.substring(String.join(",", keys), 0, 60);
    }

    @Override
    public List<SysAiChatRecord> selectSysAiChatRecordList(SysAiChatRecord sysAiChatRecord)
    {
        return sysAiChatRecordMapper.selectSysAiChatRecordList(sysAiChatRecord);
    }

    @Override
    public SysAiChatRecord selectSysAiChatRecordByRecordId(Long recordId)
    {
        return sysAiChatRecordMapper.selectSysAiChatRecordByRecordId(recordId);
    }

    @Override
    public Map<String, Object> selectChatStat(SysAiChatRecord sysAiChatRecord, int trendDays, int unmatchedLimit)
    {
        Map<String, Object> stat = new LinkedHashMap<>();
        Map<String, Object> overview = sysAiChatRecordMapper.selectOverviewStat(sysAiChatRecord);
        stat.put("overview", overview == null ? new LinkedHashMap<>() : overview);
        stat.put("sourceStat", sysAiChatRecordMapper.selectSourceStat(sysAiChatRecord));
        stat.put("sceneStat", sysAiChatRecordMapper.selectSceneStat(sysAiChatRecord));
        stat.put("trend", sysAiChatRecordMapper.selectDailyTrend(trendDays > 0 ? trendDays : 14));
        stat.put("unmatched", sysAiChatRecordMapper.selectUnmatchedQuestions(unmatchedLimit > 0 ? unmatchedLimit : 10));
        stat.put("hotKnowledge", knowledgeService.selectHotKnowledge(unmatchedLimit > 0 ? unmatchedLimit : 10));
        return stat;
    }

    @Override
    public List<String> suggestQuestions(int limit)
    {
        List<String> questions = new ArrayList<>();
        int n = limit > 0 ? limit : 6;
        List<SysAiKnowledge> docs = knowledgeService.listEnabled();
        // 热度优先，热度相同时按条目号稳定排序，保证首页推荐问法可复现
        docs.sort(Comparator.comparing((SysAiKnowledge k) -> k.getHitCount() == null ? 0 : k.getHitCount()).reversed()
                .thenComparing(k -> k.getKnowledgeId() == null ? 0L : k.getKnowledgeId()));
        for (SysAiKnowledge k : docs)
        {
            if (questions.size() >= n)
            {
                break;
            }
            if (StringUtils.isNotEmpty(k.getTitle()))
            {
                questions.add(k.getTitle());
            }
        }
        return questions;
    }

    /**
     * 未命中提示：如实告知缺口，并给出知识库现有覆盖方向，避免用户误以为系统故障
     */
    private String buildMissMessage(String question)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("教务政策知识库中暂未收录与「").append(StringUtils.isEmpty(question) ? "该问题" : question)
                .append("」直接对应的口径，为避免误导，不作推测性回答。");
        List<SysAiKnowledge> docs = knowledgeService.listEnabled();
        if (docs.isEmpty())
        {
            sb.append("\n当前知识库还没有启用条目，请联系教务处管理员维护。");
            return sb.toString();
        }
        Set<String> categories = new LinkedHashSet<>();
        for (SysAiKnowledge k : docs)
        {
            categories.add(k.getCategory());
        }
        sb.append("\n知识库共 ").append(docs.size()).append(" 条、覆盖 ").append(categories.size())
                .append(" 类业务，可尝试换一种问法，例如：");
        int i = 0;
        for (SysAiKnowledge k : docs)
        {
            if (i >= 5)
            {
                break;
            }
            sb.append("\n· ").append(k.getTitle());
            i++;
        }
        sb.append("\n如需权威结论，请直接咨询教务处或所在学院教学办。");
        return sb.toString();
    }

    /**
     * 抽取式回答：拼接最相关的若干原句，并标注出处
     */
    private String buildExtractAnswer(String question, List<AiKnowledgeHit> hits)
    {
        StringBuilder sb = new StringBuilder();
        AiKnowledgeHit first = hits.get(0);
        sb.append("根据知识库条目《").append(first.getTitle()).append("》：\n");
        sb.append(extract(first.getKnowledge().getContent(), question, 3, 300));
        sb.append("\n相关度 ").append(first.getScore()).append("，").append(first.getMatchedOn());
        String source = first.getKnowledge().getSource();
        if (StringUtils.isNotEmpty(source))
        {
            sb.append("\n依据：").append(source);
        }
        // 次高命中得分接近首位时一并给出，便于用户横向核对
        for (int i = 1; i < hits.size(); i++)
        {
            AiKnowledgeHit hit = hits.get(i);
            if (first.getScore() == null || hit.getScore() == null || hit.getScore() < first.getScore() * 0.6d)
            {
                continue;
            }
            sb.append("\n\n另可参考《").append(hit.getTitle()).append("》：\n")
                    .append(extract(hit.getKnowledge().getContent(), question, 1, 160))
                    .append("\n相关度 ").append(hit.getScore());
        }
        sb.append("\n\n（本回答由本地知识库检索抽取生成，未接入大模型，具体以教务处正式文件为准）");
        return sb.toString();
    }

    /**
     * 组装 RAG 用户提示，单条正文按 800 字截断以控制 token 规模
     */
    private String buildUserPrompt(String question, List<AiKnowledgeHit> hits)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("【已知依据】\n");
        int idx = 1;
        for (AiKnowledgeHit hit : hits)
        {
            SysAiKnowledge k = hit.getKnowledge();
            sb.append(idx++).append(". ").append(k.getTitle());
            if (StringUtils.isNotEmpty(k.getSource()))
            {
                sb.append("（来源：").append(k.getSource()).append("）");
            }
            sb.append("\n").append(StringUtils.substring(StringUtils.defaultString(k.getContent()), 0, 800)).append("\n\n");
        }
        sb.append("【问题】").append(question);
        return sb.toString();
    }

    /**
     * 从正文中挑选与问题词面重合度最高的若干句，按原文顺序输出
     */
    private String extract(String content, String question, int sentenceCount, int maxLength)
    {
        if (StringUtils.isBlank(content))
        {
            return "";
        }
        Set<String> qTokens = new LinkedHashSet<>(AiTextAnalyzer.tokenize(question));
        String[] parts = content.split("[。；\n;]");
        List<int[]> scored = new ArrayList<>();
        for (int i = 0; i < parts.length; i++)
        {
            int overlap = 0;
            for (String t : AiTextAnalyzer.tokenize(parts[i]))
            {
                if (qTokens.contains(t))
                {
                    overlap++;
                }
            }
            scored.add(new int[] { i, overlap });
        }
        scored.sort((a, b) -> b[1] != a[1] ? b[1] - a[1] : a[0] - b[0]);
        List<Integer> picked = new ArrayList<>();
        for (int i = 0; i < scored.size() && i < sentenceCount; i++)
        {
            if (StringUtils.isNotBlank(parts[scored.get(i)[0]]))
            {
                picked.add(scored.get(i)[0]);
            }
        }
        if (picked.isEmpty() && parts.length > 0)
        {
            picked.add(0);
        }
        picked.sort(Comparator.naturalOrder());
        StringBuilder sb = new StringBuilder();
        for (Integer pi : picked)
        {
            String sent = parts[pi].trim();
            if (sb.length() > 0)
            {
                sb.append("；");
            }
            sb.append(sent);
        }
        String text = sb.toString();
        if (maxLength > 0 && text.length() > maxLength)
        {
            text = text.substring(0, maxLength) + "……";
        }
        return text;
    }

    private List<Long> idsOf(List<AiKnowledgeHit> hits)
    {
        List<Long> ids = new ArrayList<>();
        for (AiKnowledgeHit hit : hits)
        {
            if (hit.getKnowledgeId() != null)
            {
                ids.add(hit.getKnowledgeId());
            }
        }
        return ids;
    }

    /**
     * 留痕落库，失败只告警不影响作答
     */
    private void saveRecord(String question, AiAnswer answer, String scene, Long userId, String userName, String userRole)
    {
        try
        {
            SysAiChatRecord record = new SysAiChatRecord();
            record.setUserId(userId);
            record.setUserName(userName);
            record.setUserRole(userRole);
            record.setScene(StringUtils.isEmpty(scene) ? "portal" : scene);
            record.setQuestion(StringUtils.substring(question, 0, 990));
            record.setAnswer(answer.getAnswer());
            record.setAnswerSource(answer.getAnswerSource());
            List<AiKnowledgeHit> refs = answer.getReferences();
            if (StringUtils.isNotEmpty(refs))
            {
                List<String> ids = new ArrayList<>();
                List<String> titles = new ArrayList<>();
                for (AiKnowledgeHit hit : refs)
                {
                    if (hit.getKnowledgeId() != null)
                    {
                        ids.add(String.valueOf(hit.getKnowledgeId()));
                    }
                    if (StringUtils.isNotEmpty(hit.getTitle()))
                    {
                        titles.add(hit.getTitle());
                    }
                }
                record.setKnowledgeIds(StringUtils.substring(String.join(",", ids), 0, 250));
                record.setKnowledgeTitles(StringUtils.substring(String.join(",", titles), 0, 990));
            }
            record.setConfidence(answer.getConfidence() == null ? 0d : answer.getConfidence());
            record.setCostTime(answer.getCostTime());
            // 大模型降级属于系统缺陷，状态记为失败以便管理端筛出排查
            record.setStatus(AiAnswer.SOURCE_ERROR.equals(answer.getAnswerSource()) ? "1" : "0");
            record.setErrorMsg(answer.getErrorMsg());
            record.setCreateTime(DateUtils.getNowDate());
            sysAiChatRecordMapper.insertSysAiChatRecord(record);
        }
        catch (Exception e)
        {
            log.warn("AI问答留痕写入失败：{}", e.getMessage());
        }
    }

    private String safeMessage(Exception e)
    {
        return e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
    }

    private int intConfig(String key, int defaultValue)
    {
        String v = configService.selectConfigByKey(key);
        try
        {
            return StringUtils.isEmpty(v) ? defaultValue : Integer.parseInt(v.trim());
        }
        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }
}
