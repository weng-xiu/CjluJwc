package com.yu.aem.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.aem.mapper.AemEvaluationStatMapper;
import com.yu.aem.service.IAemEvaluationStatService;

/**
 * 评教统计Service业务层处理（A6：多维统计与教师反馈报告）
 *
 * @author ruoyi
 * @date 2026-09-21
 */
@Service
public class AemEvaluationStatServiceImpl implements IAemEvaluationStatService
{
    private static final Logger log = LoggerFactory.getLogger(AemEvaluationStatServiceImpl.class);

    @Autowired
    private AemEvaluationStatMapper aemEvaluationStatMapper;

    /** 正向评价关键词（轻量词频分析，无外部分词依赖） */
    private static final List<String> POSITIVE_WORDS = Arrays.asList(
            "认真", "负责", "耐心", "生动", "清晰", "条理", "充实", "收获", "受益",
            "很好", "不错", "喜欢", "推荐", "详细", "易懂", "严谨", "亲和", "精彩", "用心");

    /** 待改进关键词 */
    private static final List<String> IMPROVE_WORDS = Arrays.asList(
            "太快", "枯燥", "照本宣科", "互动少", "作业多", "听不懂", "敷衍", "迟到",
            "占用", "划重点", "点名", "考试多", "板书", "节奏", "案例少", "照读", "无趣");

    @Override
    public Map<String, Object> overview(Long questionnaireId, Long semesterId)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> ov = aemEvaluationStatMapper.statOverview(questionnaireId, semesterId);
        Map<String, Object> dist = aemEvaluationStatMapper.statDistribution(questionnaireId, semesterId);
        result.put("overview", ov == null ? new LinkedHashMap<>() : ov);
        result.put("distribution", dist == null ? new LinkedHashMap<>() : dist);
        return result;
    }

    @Override
    public List<Map<String, Object>> byCourse(Long questionnaireId, Long semesterId)
    {
        List<Map<String, Object>> list = aemEvaluationStatMapper.statByCourse(questionnaireId, semesterId);
        return list == null ? new ArrayList<>() : list;
    }

    @Override
    public List<Map<String, Object>> byTeacher(Long questionnaireId, Long semesterId)
    {
        List<Map<String, Object>> list = aemEvaluationStatMapper.statByTeacher(questionnaireId, semesterId);
        if (list == null)
        {
            return new ArrayList<>();
        }
        // 附加排名（已按 avgScore 降序）
        for (int i = 0; i < list.size(); i++)
        {
            list.get(i).put("rank", i + 1);
        }
        return list;
    }

    @Override
    public List<Map<String, Object>> byClass(Long questionnaireId, Long semesterId, Long courseId)
    {
        List<Map<String, Object>> list = aemEvaluationStatMapper.statByClass(questionnaireId, semesterId, courseId);
        return list == null ? new ArrayList<>() : list;
    }

    @Override
    public List<Map<String, Object>> trend(Long questionnaireId)
    {
        List<Map<String, Object>> list = aemEvaluationStatMapper.statTrend(questionnaireId);
        return list == null ? new ArrayList<>() : list;
    }

    @Override
    public Map<String, Object> teacherReport(Long teacherId)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> summary = aemEvaluationStatMapper.teacherSummary(teacherId);
        result.put("summary", summary == null ? new LinkedHashMap<>() : summary);
        List<Map<String, Object>> courses = aemEvaluationStatMapper.teacherCourseBreakdown(teacherId);
        result.put("courses", courses == null ? new ArrayList<>() : courses);
        Map<String, Object> dist = aemEvaluationStatMapper.teacherDistribution(teacherId);
        result.put("distribution", dist == null ? new LinkedHashMap<>() : dist);
        // 评语词频分析
        List<String> comments = aemEvaluationStatMapper.selectComments(teacherId, null, null);
        result.put("commentAnalysis", analyze(comments));
        log.info("教师[{}]评教分析报告生成完成：{}门课程，{}条评语", teacherId,
                courses == null ? 0 : courses.size(), comments == null ? 0 : comments.size());
        return result;
    }

    @Override
    public List<Map<String, Object>> teacherCourseBreakdown(Long teacherId)
    {
        List<Map<String, Object>> list = aemEvaluationStatMapper.teacherCourseBreakdown(teacherId);
        return list == null ? new ArrayList<>() : list;
    }

    @Override
    public Map<String, Object> commentAnalysis(Long teacherId, Long courseId, Long questionnaireId)
    {
        List<String> comments = aemEvaluationStatMapper.selectComments(teacherId, courseId, questionnaireId);
        return analyze(comments);
    }

    /**
     * 轻量中文关键词词频分析：命中预置正向/待改进词表计数，不做分词（无外部依赖）。
     */
    private Map<String, Object> analyze(List<String> comments)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        int total = comments == null ? 0 : comments.size();
        result.put("commentCount", total);
        List<Map<String, Object>> positive = new ArrayList<>();
        List<Map<String, Object>> improve = new ArrayList<>();
        if (total > 0)
        {
            for (String w : POSITIVE_WORDS)
            {
                int c = countWord(comments, w);
                if (c > 0) positive.add(wordItem(w, c));
            }
            for (String w : IMPROVE_WORDS)
            {
                int c = countWord(comments, w);
                if (c > 0) improve.add(wordItem(w, c));
            }
        }
        positive.sort((a, b) -> ((Integer) b.get("count")) - ((Integer) a.get("count")));
        improve.sort((a, b) -> ((Integer) b.get("count")) - ((Integer) a.get("count")));
        result.put("positive", positive);
        result.put("improve", improve);
        return result;
    }

    private Map<String, Object> wordItem(String word, int count)
    {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("word", word);
        m.put("count", count);
        return m;
    }

    private int countWord(List<String> comments, String word)
    {
        int count = 0;
        for (String c : comments)
        {
            if (c == null) continue;
            int idx = c.indexOf(word);
            while (idx >= 0)
            {
                count++;
                idx = c.indexOf(word, idx + word.length());
            }
        }
        return count;
    }
}
