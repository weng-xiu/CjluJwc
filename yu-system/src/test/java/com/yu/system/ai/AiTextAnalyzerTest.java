package com.yu.system.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.system.ai.AiTextAnalyzer.AiTextIndex;
import com.yu.system.domain.SysAiKnowledge;

/**
 * 轻量检索分析器测试（Phase34 AI 应用试点）。
 *
 * <p>该口径是「可解释检索」的实现底座：切词规则、字段加权、关键词整词加成、标题包含加成、
 * 抽取式片段都直接决定门户问答给出的答案与「为什么推荐这条」的说明文案。切词与加成规则一旦被
 * 后人「顺手优化」，问答的相关性会静默劣化而不会有任何报错，因此逐条钉住。</p>
 *
 * @author G5
 */
class AiTextAnalyzerTest
{
    /**
     * 构造知识条目。keywords / content 允许为 null，用于覆盖字段缺失的分支。
     */
    private static SysAiKnowledge doc(Long id, String title, String keywords, String content)
    {
        SysAiKnowledge k = new SysAiKnowledge();
        k.setKnowledgeId(id);
        k.setTitle(title);
        k.setKeywords(keywords);
        k.setContent(content);
        return k;
    }

    /**
     * 两条覆盖「补选」与「退课」的常用知识，多个用例复用。
     */
    private static List<SysAiKnowledge> sampleDocs()
    {
        return List.of(
                doc(1L, "补选流程", "补选,选课", "补选需在开学第二周前提交申请。逾期不予受理。"),
                doc(2L, "退课规则", "退课", "退课需任课教师审批。"));
    }

    @Test
    @DisplayName("tokenize：连续中文按「单字 + 相邻二字组」切分")
    void tokenize_cjkProducesUnigramAndBigram()
    {
        assertEquals(List.of("选", "课", "选课"), AiTextAnalyzer.tokenize("选课"));
        assertEquals(List.of("补", "选", "课", "补选", "选课"), AiTextAnalyzer.tokenize("补选课"),
                "三字词要产出 3 个单字 + 2 个相邻二字组");
    }

    @Test
    @DisplayName("tokenize：英文数字按整词切分并与中文段互不粘连")
    void tokenize_latinRunsAsWholeWord()
    {
        assertEquals(List.of("abc"), AiTextAnalyzer.tokenize("ABC"), "拉丁文要统一小写");
        assertEquals(List.of("退", "课", "退课", "abc"), AiTextAnalyzer.tokenize("退课abc"),
                "中英分界要各自成段，不能拼成退课abc 一个词");
        assertEquals(List.of("abc", "考", "试", "考试"), AiTextAnalyzer.tokenize("ABC考试"));
        assertEquals(List.of("a", "b"), AiTextAnalyzer.tokenize("a b"), "空格是切分符而不是词的一部分");
    }

    @Test
    @DisplayName("tokenize：空串、纯空白、纯标点都产出空列表而不是 null")
    void tokenize_blankInputsYieldEmptyList()
    {
        assertTrue(AiTextAnalyzer.tokenize(null).isEmpty(), "null 不能返回 null，否则下游遍历 NPE");
        // 本项目 StringUtils.isEmpty 带 trim，纯空白同样判为空
        assertTrue(AiTextAnalyzer.tokenize("   ").isEmpty(), "纯空白判为空");
        assertTrue(AiTextAnalyzer.tokenize("，").isEmpty(), "标点既不是中文也不是拉丁，不应产生词元");
    }

    @Test
    @DisplayName("matchKeywordPhrase：命中最长的关键词，单字关键词不参与")
    void matchKeywordPhrase_prefersLongestPhrase()
    {
        assertEquals("补选", AiTextAnalyzer.matchKeywordPhrase("补选怎么办", "补选,退课"));
        assertEquals("补选流程", AiTextAnalyzer.matchKeywordPhrase("补选流程怎么走", "补选,补选流程"),
                "两个关键词都命中时要取更长的那个，否则说明文案会显得含糊");
        assertNull(AiTextAnalyzer.matchKeywordPhrase("退课", "课"),
                "单字关键词噪声太大，规则要求长度 >= 2");
    }

    @Test
    @DisplayName("matchKeywordPhrase：逗号顿号分号斜杠与全角形态都要能分隔")
    void matchKeywordPhrase_supportsMixedSeparators()
    {
        assertEquals("补选", AiTextAnalyzer.matchKeywordPhrase("申请补选", "退课，补选"), "全角逗号");
        assertEquals("补选", AiTextAnalyzer.matchKeywordPhrase("申请补选", "退课、补选"), "顿号");
        assertEquals("补选", AiTextAnalyzer.matchKeywordPhrase("申请补选", "退课；补选"), "全角分号");
        assertEquals("补选", AiTextAnalyzer.matchKeywordPhrase("申请补选", "退课/补选"), "斜杠");
        assertEquals("补选", AiTextAnalyzer.matchKeywordPhrase("申请补选", " 退课 , 补选 "),
                "关键词前后的空格要被 trim 掉");
    }

    @Test
    @DisplayName("matchKeywordPhrase：问题或关键词为空时返回 null")
    void matchKeywordPhrase_blankInputs()
    {
        assertNull(AiTextAnalyzer.matchKeywordPhrase(null, "补选"));
        assertNull(AiTextAnalyzer.matchKeywordPhrase("申请补选", null));
        assertNull(AiTextAnalyzer.matchKeywordPhrase("申请补选", "   "));
    }

    @Test
    @DisplayName("buildIndex：空知识库返回空索引而非 null")
    void buildIndex_emptyDocs()
    {
        assertTrue(AiTextAnalyzer.buildIndex(null).isEmpty(), "null 知识库要返回空索引");
        AiTextIndex empty = AiTextAnalyzer.buildIndex(List.of());
        assertTrue(empty.isEmpty());
        assertEquals(0, empty.size());
        assertTrue(empty.getKnowledges().isEmpty());
    }

    @Test
    @DisplayName("buildIndex：索引条目数与入参一致，且知识列表不可变")
    void buildIndex_tracksDocsAndExposesReadOnlyView()
    {
        AiTextIndex index = AiTextAnalyzer.buildIndex(sampleDocs());

        assertFalse(index.isEmpty());
        assertEquals(2, index.size());
        assertEquals("补选流程", index.getKnowledges().get(0).getTitle(), "索引要按入参顺序保留条目");

        assertThrows(UnsupportedOperationException.class, () -> index.getKnowledges().add(new SysAiKnowledge()),
                "只读视图被写穿会导致索引与实际知识库不一致");
    }

    @Test
    @DisplayName("search：索引为空、问题为空时返回空结果而不是抛异常")
    void search_guardsBlankInputs()
    {
        AiTextIndex index = AiTextAnalyzer.buildIndex(sampleDocs());

        assertTrue(AiTextAnalyzer.search(null, "补选", 5).isEmpty(), "索引为 null 不能抛异常");
        assertTrue(AiTextAnalyzer.search(AiTextAnalyzer.buildIndex(List.of()), "补选", 5).isEmpty(),
                "空索引没有任何可检索条目");
        assertTrue(AiTextAnalyzer.search(index, null, 5).isEmpty());
        assertTrue(AiTextAnalyzer.search(index, "   ", 5).isEmpty(), "空白问题判为空");
    }

    @Test
    @DisplayName("search：相关条目排在前面，并按关键词整词命中给出加成说明")
    void search_ranksRelevantDocFirst()
    {
        AiTextIndex index = AiTextAnalyzer.buildIndex(sampleDocs());

        List<AiKnowledgeHit> hits = AiTextAnalyzer.search(index, "补选怎么办", 5);

        assertEquals(2, hits.size(), "未过滤阈值，所有条目都要回传由调用方筛选");
        assertEquals("补选流程", hits.get(0).getTitle(), "口语化提问命中业务词时应把对应条目排到最前");
        assertTrue(hits.get(0).getMatchedOn().contains("命中关键词「补选」"),
                "命中说明要能解释为什么推荐这条");
        assertTrue(hits.get(0).getScore() > hits.get(1).getScore(), "得分必须严格降序");
    }

    @Test
    @DisplayName("search：问题含完整条目标题时叠加标题加成并拼接两种说明")
    void search_questionContainsTitleAddsBonus()
    {
        AiTextIndex index = AiTextAnalyzer.buildIndex(sampleDocs());

        AiKnowledgeHit top = AiTextAnalyzer.search(index, "补选流程", 5).get(0);

        assertEquals("命中关键词「补选」，且与条目名称高度一致", top.getMatchedOn(),
                "关键词加成与标题加成要同时生效，说明文案用顿号式拼接");
    }

    @Test
    @DisplayName("search：长标题被短问题包含时同样触发标题加成")
    void search_questionContainedInLongTitleAddsBonus()
    {
        AiTextIndex index = AiTextAnalyzer.buildIndex(List.of(
                doc(1L, "学生选课管理办法与流程", null, null)));

        AiKnowledgeHit top = AiTextAnalyzer.search(index, "选课管理办法", 5).get(0);

        assertTrue(top.getMatchedOn().contains("与条目名称高度一致"),
                "标题长度大于 6 且包含问题时，也要认定为高度一致");
    }

    @Test
    @DisplayName("search：无加成时回落到词义重合度说明")
    void search_fallsBackToOverlapExplanation()
    {
        AiTextIndex index = AiTextAnalyzer.buildIndex(List.of(
                doc(1L, "成绩查询", null, "期末成绩在教务系统查询。")));

        AiKnowledgeHit top = AiTextAnalyzer.search(index, "期末考试分数怎么看", 5).get(0);

        assertTrue(top.getMatchedOn().startsWith("按词义重合度匹配（重合词 "),
                "既未命中关键词也未命中标题时，必须给出重合词计数，保持可解释");
    }

    @Test
    @DisplayName("search：得分上限收敛到 1.0")
    void search_scoreCappedAtOne()
    {
        AiTextIndex index = AiTextAnalyzer.buildIndex(List.of(doc(1L, "补选流程", null, null)));

        AiKnowledgeHit top = AiTextAnalyzer.search(index, "补选流程", 5).get(0);

        // 余弦 1.0 + 标题加成 0.35 = 1.35，必须被 Math.min 收敛
        assertEquals(1.0, top.getScore(), 1e-9, "得分对外承诺取值 0~1");
    }

    @Test
    @DisplayName("search：完全无共同词的条目得 0 分但仍回传")
    void search_noOverlapScoresZero()
    {
        AiTextIndex index = AiTextAnalyzer.buildIndex(List.of(
                doc(1L, "补选流程", null, null)));

        List<AiKnowledgeHit> hits = AiTextAnalyzer.search(index, "python", 5);

        assertEquals(1, hits.size(), "阈值过滤由调用方负责，检索侧不做丢弃");
        assertEquals(0.0, hits.get(0).getScore(), 1e-9);
    }

    @Test
    @DisplayName("search：空条目模长为 0 时得分按 0 处理，不产生除零")
    void search_emptyDocDoesNotDivideByZero()
    {
        AiTextIndex index = AiTextAnalyzer.buildIndex(List.of(doc(1L, null, null, null)));

        List<AiKnowledgeHit> hits = AiTextAnalyzer.search(index, "补选", 5);

        assertEquals(1, hits.size());
        assertEquals(0.0, hits.get(0).getScore(), 1e-9, "模长为 0 必须短路成 0，否则得到 NaN");
        assertFalse(Double.isNaN(hits.get(0).getScore()), "NaN 会让下游排序结果不可预测");
    }

    @Test
    @DisplayName("search：topK 截断与不截断两种分支")
    void search_appliesTopK()
    {
        AiTextIndex index = AiTextAnalyzer.buildIndex(sampleDocs());

        assertEquals(2, AiTextAnalyzer.search(index, "补选", 2).size(), "条目数等于 topK 时全部返回");
        assertEquals(1, AiTextAnalyzer.search(index, "补选", 1).size());
        assertEquals(0, AiTextAnalyzer.search(index, "补选", 0).size(), "topK 为 0 返回空列表");
    }

    @Test
    @DisplayName("bestSentence：内容为空时返回空串")
    void bestSentence_blankContent()
    {
        assertEquals("", AiTextAnalyzer.bestSentence(null, "补选", 20));
        assertEquals("", AiTextAnalyzer.bestSentence("   ", "补选", 20));
    }

    @Test
    @DisplayName("bestSentence：挑选与问题词重叠最多的句子")
    void bestSentence_picksHighestOverlap()
    {
        String content = "退课需任课教师审批；补选需在开学第二周前提交申请。";

        String sentence = AiTextAnalyzer.bestSentence(content, "补选申请截止时间", 100);

        assertEquals("补选需在开学第二周前提交申请", sentence,
                "抽取式作答必须选命中词最多的一句，而不是固定取首句");
    }

    @Test
    @DisplayName("bestSentence：超长时截断并加省略号，maxLength 非正数时保留全文")
    void bestSentence_truncatesByMaxLength()
    {
        String content = "补选需在开学第二周前提交申请。";

        assertEquals("补选需在开……", AiTextAnalyzer.bestSentence(content, "补选申请", 5),
                "截断长度包含省略号前的正文，共 maxLength 个字符");
        assertEquals("补选需在开学第二周前提交申请", AiTextAnalyzer.bestSentence(content, "补选申请", 0),
                "maxLength <= 0 表示不限制");
        assertEquals("补选需在开学第二周前提交申请", AiTextAnalyzer.bestSentence(content, "补选申请", -1));
    }

    @Test
    @DisplayName("bestSentence：全是空句时回落到整段文本")
    void bestSentence_fallsBackToWholeContent()
    {
        assertEquals("。。。", AiTextAnalyzer.bestSentence("。。。", "补选", 10),
                "没有可抽取的句子时不能返回 null，否则调用方拼接话术会得到 null 字样");
    }
}
