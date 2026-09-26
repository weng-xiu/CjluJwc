package com.yu.system.ai;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.yu.common.utils.StringUtils;
import com.yu.system.domain.SysAiKnowledge;

/**
 * 面向中文教务文本的轻量检索分析器（Phase34 AI应用试点）
 *
 * 不引入分词与向量库依赖，采用可解释的口径：
 * 1) 切词：连续中文按「单字 + 相邻二字组」切分，英文数字按词切分（二字组近似短语，可区分「选课/退课」等近义表达）；
 * 2) 打分：字段加权词频（标题 3.0、关键词 2.5、正文 1.0，频次取 1+log 抑制长文本）× IDF，再做余弦归一化；
 * 3) 加成：关键词整词命中问题文本 +0.20，问题与标题互为包含 +0.35，上限 1.0；
 * 4) 抽取：从正文中挑选与问题词重叠度最高的句子作为作答片段。
 * 该口径在小规模知识库（数百条）上与向量检索效果接近，且每一条得分都可向用户解释。
 *
 * @author yu
 * @date 2026-09-26
 */
public class AiTextAnalyzer
{
    /** 字段权重 */
    private static final double W_TITLE = 3.0;
    private static final double W_KEYWORD = 2.5;
    private static final double W_CONTENT = 1.0;

    /** 命中加成 */
    private static final double BONUS_KEYWORD_PHRASE = 0.20;
    private static final double BONUS_TITLE_MATCH = 0.35;

    private AiTextAnalyzer()
    {
    }

    /**
     * 中文单字 + 二字组、英文数字整词的切词
     */
    public static List<String> tokenize(String text)
    {
        List<String> tokens = new ArrayList<>();
        if (StringUtils.isEmpty(text))
        {
            return tokens;
        }
        String lower = text.toLowerCase();
        StringBuilder latin = new StringBuilder();
        List<Character> cjk = new ArrayList<>();
        for (int i = 0; i < lower.length(); i++)
        {
            char ch = lower.charAt(i);
            if (ch >= 0x4E00 && ch <= 0x9FFF)
            {
                flushLatin(tokens, latin);
                cjk.add(ch);
            }
            else if ((ch >= 'a' && ch <= 'z') || (ch >= '0' && ch <= '9'))
            {
                flushCjk(tokens, cjk);
                latin.append(ch);
            }
            else
            {
                flushLatin(tokens, latin);
                flushCjk(tokens, cjk);
            }
        }
        flushLatin(tokens, latin);
        flushCjk(tokens, cjk);
        return tokens;
    }

    private static void flushLatin(List<String> tokens, StringBuilder latin)
    {
        if (latin.length() > 0)
        {
            tokens.add(latin.toString());
            latin.setLength(0);
        }
    }

    /** 中文段产出单字与相邻二字组 */
    private static void flushCjk(List<String> tokens, List<Character> cjk)
    {
        if (cjk.isEmpty())
        {
            return;
        }
        for (Character c : cjk)
        {
            tokens.add(String.valueOf(c));
        }
        for (int i = 0; i + 1 < cjk.size(); i++)
        {
            tokens.add("" + cjk.get(i) + cjk.get(i + 1));
        }
        cjk.clear();
    }

    /**
     * 构建检索索引（每次问答基于当前启用知识库重建，条目量级小，无需缓存）
     */
    public static AiTextIndex buildIndex(List<SysAiKnowledge> docs)
    {
        AiTextIndex index = new AiTextIndex();
        if (docs == null || docs.isEmpty())
        {
            return index;
        }
        index.total = docs.size();
        // 先算词频（含字段权重）
        for (SysAiKnowledge k : docs)
        {
            Map<String, Double> vec = new HashMap<>();
            addField(vec, k.getTitle(), W_TITLE);
            addField(vec, k.getKeywords(), W_KEYWORD);
            addField(vec, k.getContent(), W_CONTENT);
            index.vectors.add(vec);
            index.knowledges.add(k);
            for (String term : vec.keySet())
            {
                index.df.merge(term, 1, Integer::sum);
            }
        }
        // 再乘 IDF 并计算模长
        for (Map<String, Double> vec : index.vectors)
        {
            double sum2 = 0;
            for (Map.Entry<String, Double> en : vec.entrySet())
            {
                double w = en.getValue() * idf(index.df.get(en.getKey()), index.total);
                en.setValue(w);
                sum2 += w * w;
            }
            index.norms.add(Math.sqrt(sum2));
        }
        return index;
    }

    private static void addField(Map<String, Double> vec, String text, double fieldWeight)
    {
        if (StringUtils.isEmpty(text))
        {
            return;
        }
        Map<String, Integer> tf = new HashMap<>();
        for (String token : tokenize(text))
        {
            tf.merge(token, 1, Integer::sum);
        }
        for (Map.Entry<String, Integer> en : tf.entrySet())
        {
            // 1+log 抑制高频词，字段权重体现「命中标题比命中正文更相关」
            double contribution = fieldWeight * (1.0 + Math.log(en.getValue()));
            vec.merge(en.getKey(), contribution, Double::sum);
        }
    }

    private static double idf(int df, int total)
    {
        return Math.log(1.0 + (double) total / (df <= 0 ? 0.5 : df));
    }

    /**
     * 检索：返回按得分降序的命中项（未过滤阈值，由调用方按 minScore 决定）
     */
    public static List<AiKnowledgeHit> search(AiTextIndex index, String question, int topK)
    {
        List<AiKnowledgeHit> hits = new ArrayList<>();
        if (index == null || index.isEmpty() || StringUtils.isBlank(question))
        {
            return hits;
        }
        Map<String, Double> qVec = new HashMap<>();
        addField(qVec, question, 1.0);
        double qSum2 = 0;
        for (Map.Entry<String, Double> en : qVec.entrySet())
        {
            double w = en.getValue() * idf(index.df.getOrDefault(en.getKey(), 0), index.total);
            en.setValue(w);
            qSum2 += w * w;
        }
        double qNorm = Math.sqrt(qSum2);
        if (qNorm == 0)
        {
            return hits;
        }
        Set<String> questionBigrams = new LinkedHashSet<>(tokenize(question));
        for (int i = 0; i < index.vectors.size(); i++)
        {
            SysAiKnowledge k = index.knowledges.get(i);
            double dot = 0;
            for (Map.Entry<String, Double> en : qVec.entrySet())
            {
                Double dw = index.vectors.get(i).get(en.getKey());
                if (dw != null)
                {
                    dot += en.getValue() * dw;
                }
            }
            double norm = index.norms.get(i);
            double score = norm == 0 ? 0 : dot / (qNorm * norm);
            String matchedOn = null;
            // 关键词整词命中加成：口语化提问往往不含书面词形，但会含「补选」「抽签」这类业务词
            String phrase = matchKeywordPhrase(question, k.getKeywords());
            if (phrase != null)
            {
                score += BONUS_KEYWORD_PHRASE;
                matchedOn = "命中关键词「" + phrase + "」";
            }
            // 问题与标题互为包含：接近「原问」的情形
            String title = k.getTitle();
            if (title != null && (question.contains(title) || (title.length() > 6 && title.contains(question))))
            {
                score += BONUS_TITLE_MATCH;
                matchedOn = StringUtils.isEmpty(matchedOn) ? "与条目名称高度一致" : matchedOn + "，且与条目名称高度一致";
            }
            if (matchedOn == null)
            {
                matchedOn = "按词义重合度匹配（重合词 " + countOverlap(questionBigrams, tokenize(title + " " + nvl(k.getKeywords()))) + " 个）";
            }
            hits.add(new AiKnowledgeHit(k, round(Math.min(score, 1.0)), matchedOn));
        }
        hits.sort(Comparator.comparing(AiKnowledgeHit::getScore, Comparator.reverseOrder()));
        return hits.size() > topK ? new ArrayList<>(hits.subList(0, topK)) : hits;
    }

    private static String nvl(String v)
    {
        return v == null ? "" : v;
    }

    private static int countOverlap(Set<String> a, List<String> b)
    {
        int n = 0;
        for (String t : b)
        {
            if (a.contains(t))
            {
                n++;
            }
        }
        return n;
    }

    /**
     * 找出被问题整词包含的关键词（关键词以逗号/顿号/分号分隔）
     */
    public static String matchKeywordPhrase(String question, String keywords)
    {
        if (StringUtils.isBlank(question) || StringUtils.isBlank(keywords))
        {
            return null;
        }
        String best = null;
        for (String kw : keywords.split("[,\uFF0C\u3001;\uFF1B/]"))
        {
            String phrase = kw.trim();
            if (phrase.length() >= 2 && question.contains(phrase))
            {
                if (best == null || phrase.length() > best.length())
                {
                    best = phrase;
                }
            }
        }
        return best;
    }

    /**
     * 抽取式片段：从正文中挑选与问题词重叠度最高的句子
     */
    public static String bestSentence(String content, String question, int maxLength)
    {
        if (StringUtils.isBlank(content))
        {
            return "";
        }
        Set<String> qTokens = new LinkedHashSet<>(tokenize(question));
        String[] sentences = content.split("[\u3002\uFF1B\n;]");
        String best = null;
        int bestScore = -1;
        for (String s : sentences)
        {
            String sent = s.trim();
            if (sent.isEmpty())
            {
                continue;
            }
            int score = countOverlap(qTokens, tokenize(sent));
            if (score > bestScore)
            {
                bestScore = score;
                best = sent;
            }
        }
        if (best == null)
        {
            best = content.trim();
        }
        if (maxLength > 0 && best.length() > maxLength)
        {
            best = best.substring(0, maxLength) + "……";
        }
        return best;
    }

    private static double round(double v)
    {
        return Math.round(v * 10000d) / 10000d;
    }

    /**
     * 检索索引：文档向量、词频文档数与模长
     */
    public static class AiTextIndex
    {
        private final List<SysAiKnowledge> knowledges = new ArrayList<>();
        private final List<Map<String, Double>> vectors = new ArrayList<>();
        private final List<Double> norms = new ArrayList<>();
        private final Map<String, Integer> df = new HashMap<>();
        private int total = 0;

        public boolean isEmpty()
        {
            return knowledges.isEmpty();
        }

        public int size()
        {
            return knowledges.size();
        }

        public List<SysAiKnowledge> getKnowledges()
        {
            return Collections.unmodifiableList(knowledges);
        }
    }
}
