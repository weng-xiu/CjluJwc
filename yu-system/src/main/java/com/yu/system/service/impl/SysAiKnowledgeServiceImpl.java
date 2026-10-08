package com.yu.system.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import com.yu.system.ai.AiCorpusDesensitizer;
import com.yu.system.ai.AiKnowledgeHit;
import com.yu.system.ai.AiTextAnalyzer;
import com.yu.system.domain.SysAiKnowledge;
import com.yu.system.mapper.SysAiKnowledgeMapper;
import com.yu.system.service.ISysAiKnowledgeService;
import com.yu.system.service.ISysConfigService;

/**
 * 教务政策AI知识库 服务实现
 *
 * 检索口径见 AiTextAnalyzer 说明；此处只负责「取全量启用条目 → 建索引 → 打分 → 按阈值裁剪」，
 * 阈值与返回条数均由 sys_config 控制，教务处可在参数配置中调节问答灵敏度而无需改代码。
 *
 * @author yu
 * @date 2026-09-26
 */
@Service
public class SysAiKnowledgeServiceImpl implements ISysAiKnowledgeService
{
    private static final Logger log = LoggerFactory.getLogger(SysAiKnowledgeServiceImpl.class);

    /** 检索最低置信度 */
    private static final String CFG_MIN_SCORE = "ai.qa.minScore";

    /** 检索返回条数 */
    private static final String CFG_TOP_K = "ai.qa.topK";

    @Autowired
    private SysAiKnowledgeMapper sysAiKnowledgeMapper;

    @Autowired
    private ISysConfigService configService;

    /** K4 垂直语料脱敏口径：知识库是 RAG 唯一事实来源，入库前对正文内嵌个人信息去标识化 */
    @Autowired
    private AiCorpusDesensitizer corpusDesensitizer;

    @Override
    public SysAiKnowledge selectSysAiKnowledgeByKnowledgeId(Long knowledgeId)
    {
        return sysAiKnowledgeMapper.selectSysAiKnowledgeByKnowledgeId(knowledgeId);
    }

    @Override
    public List<SysAiKnowledge> selectSysAiKnowledgeList(SysAiKnowledge sysAiKnowledge)
    {
        return sysAiKnowledgeMapper.selectSysAiKnowledgeList(sysAiKnowledge);
    }

    @Override
    public List<SysAiKnowledge> listEnabled()
    {
        return sysAiKnowledgeMapper.selectEnabledList();
    }

    @Override
    public List<AiKnowledgeHit> retrieve(String question, int topK)
    {
        List<AiKnowledgeHit> result = new ArrayList<>();
        if (StringUtils.isBlank(question))
        {
            return result;
        }
        int limit = topK > 0 ? topK : intConfig(CFG_TOP_K, 4);
        List<SysAiKnowledge> docs = listEnabled();
        if (docs.isEmpty())
        {
            log.warn("AI知识库无启用条目，问答将返回未命中提示");
            return result;
        }
        AiTextAnalyzer.AiTextIndex index = AiTextAnalyzer.buildIndex(docs);
        double threshold = minScore();
        for (AiKnowledgeHit hit : AiTextAnalyzer.search(index, question, limit))
        {
            if (hit.getScore() != null && hit.getScore() >= threshold)
            {
                result.add(hit);
            }
        }
        return result;
    }

    @Override
    public double minScore()
    {
        String v = configService.selectConfigByKey(CFG_MIN_SCORE);
        try
        {
            return StringUtils.isEmpty(v) ? 0.12d : Double.parseDouble(v.trim());
        }
        catch (NumberFormatException e)
        {
            return 0.12d;
        }
    }

    @Override
    public int insertSysAiKnowledge(SysAiKnowledge sysAiKnowledge)
    {
        applyCorpusDesensitize(sysAiKnowledge);
        if (sysAiKnowledge.getCreateTime() == null)
        {
            sysAiKnowledge.setCreateTime(DateUtils.getNowDate());
        }
        return sysAiKnowledgeMapper.insertSysAiKnowledge(sysAiKnowledge);
    }

    @Override
    public int updateSysAiKnowledge(SysAiKnowledge sysAiKnowledge)
    {
        applyCorpusDesensitize(sysAiKnowledge);
        sysAiKnowledge.setUpdateTime(DateUtils.getNowDate());
        return sysAiKnowledgeMapper.updateSysAiKnowledge(sysAiKnowledge);
    }

    /**
     * K4 垂直语料脱敏口径：正文命中个人信息时，按模式自动掩码（MASK）或拒绝入库（BLOCK）。
     * 关闭开关时不做任何处理，交由机构自行把握语料合规。
     */
    private void applyCorpusDesensitize(SysAiKnowledge knowledge)
    {
        if (!corpusDesensitizer.isEnabled() || knowledge == null)
        {
            return;
        }
        Map<String, Integer> hits = corpusDesensitizer.scan(knowledge.getContent());
        if (hits.isEmpty())
        {
            return;
        }
        if (AiCorpusDesensitizer.MODE_BLOCK.equals(corpusDesensitizer.mode()))
        {
            throw new ServiceException("知识条目正文含个人信息" + hits.keySet() + "，请先去标识化后再入库（语料脱敏口径：BLOCK）");
        }
        String masked = corpusDesensitizer.mask(knowledge.getContent());
        knowledge.setContent(masked);
        log.warn("AI知识库条目[{}]入库时自动脱敏命中个人信息{}，已按 MASK 口径掩码", knowledge.getTitle(), hits.keySet());
    }

    @Override
    public int deleteSysAiKnowledgeByKnowledgeIds(Long[] knowledgeIds)
    {
        return sysAiKnowledgeMapper.deleteSysAiKnowledgeByKnowledgeIds(knowledgeIds);
    }

    @Override
    public void markHit(List<Long> knowledgeIds)
    {
        if (knowledgeIds == null || knowledgeIds.isEmpty())
        {
            return;
        }
        try
        {
            sysAiKnowledgeMapper.increaseHitCount(knowledgeIds.toArray(new Long[0]));
        }
        catch (Exception e)
        {
            // 热度统计失败不得影响作答结果
            log.warn("AI知识库命中热度累加失败：{}", e.getMessage());
        }
    }

    @Override
    public List<Map<String, Object>> selectCategoryStat()
    {
        return sysAiKnowledgeMapper.selectCategoryStat();
    }

    @Override
    public List<Map<String, Object>> selectHotKnowledge(int limit)
    {
        return sysAiKnowledgeMapper.selectHotKnowledge(limit > 0 ? limit : 10);
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
