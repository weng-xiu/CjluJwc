package com.yu.aem.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yu.aem.mapper.AemGradePredictionMapper;
import com.yu.aem.service.IAemGradePredictionService;

/**
 * 成绩与学业预测Service实现（F2-3 智能算法深化）。
 *
 * <p>算法均为可解释的确定性计算，不含黑盒模型：</p>
 * <ul>
 *   <li>趋势预测：对逐学期平均成绩做最小二乘线性拟合，斜率即趋势，外推得下一学期预测值；</li>
 *   <li>风险分级：综合"最近学期表现 + 趋势斜率 + 累计不及格门数"三维判据，阈值可配置；</li>
 *   <li>课程难度：难度指数 = 0.5×(100−平均分) + 0.3×(100−通过率) + 0.2×(不及格率×100)，归一至 0-100。</li>
 * </ul>
 *
 * @author ruoyi
 */
@Service
public class AemGradePredictionServiceImpl implements IAemGradePredictionService
{
    @Autowired
    private AemGradePredictionMapper predictionMapper;

    /** 风险判据：学期平均成绩低于该值计为"低表现" */
    @org.springframework.beans.factory.annotation.Value("${aem.prediction.lowScoreLine:60}")
    private double lowScoreLine;

    /** 风险判据：趋势斜率小于该值计为"明显下滑"（每学期分数变化） */
    @org.springframework.beans.factory.annotation.Value("${aem.prediction.declineSlope:-3.0}")
    private double declineSlope;

    @Override
    public Map<String, Object> predictStudentRisk(Long studentId)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> base = predictionMapper.selectStudentBase(studentId);
        result.put("base", base != null ? base : new LinkedHashMap<>());

        List<Map<String, Object>> series = predictionMapper.selectStudentSemesterSeries(studentId);
        if (series == null) { series = new ArrayList<>(); }
        result.put("series", series);

        int n = series.size();
        double[] scores = new double[n];
        double[] gpas = new double[n];
        int recentFail = 0;
        for (int i = 0; i < n; i++)
        {
            scores[i] = toDouble(series.get(i).get("avgScore"));
            gpas[i] = toDouble(series.get(i).get("avgGpa"));
        }
        if (n >= 1) { recentFail = (int) toDouble(series.get(n - 1).get("failCount")); }

        // 最小二乘拟合 y = a + b·x，x = 学期序号（0 基）
        double slope = 0.0;
        double intercept = n > 0 ? scores[n - 1] : 0.0;
        if (n >= 2)
        {
            double sx = 0, sy = 0, sxy = 0, sxx = 0;
            for (int i = 0; i < n; i++)
            {
                sx += i; sy += scores[i]; sxy += i * scores[i]; sxx += (double) i * i;
            }
            double denom = n * sxx - sx * sx;
            slope = denom == 0 ? 0 : (n * sxy - sx * sy) / denom;
            intercept = (sy - slope * sx) / n;
        }

        // 外推下一学期（按拟合线，裁剪到 0-100）
        double predictedNextScore = clamp(slope != 0 ? (intercept + slope * n) : (n > 0 ? scores[n - 1] : 0), 0, 100);
        double predictedNextGpa = clamp(predictedNextScore / 10.0 - 5.0, 0, 5.0);
        result.put("trendSlope", round(slope, 2));
        result.put("predictedNextScore", round(predictedNextScore, 1));
        result.put("predictedNextGpa", round(predictedNextGpa, 2));
        result.put("semesterCount", n);

        double lastScore = n > 0 ? scores[n - 1] : 0;
        double overallAvg = 0;
        for (double s : scores) { overallAvg += s; }
        overallAvg = n > 0 ? overallAvg / n : 0;

        // 三维判据分级：0 平稳向好 / 1 关注 / 2 预警 / 3 高危
        String riskLevel = "0";
        String riskLabel = "平稳";
        if (n == 0)
        {
            riskLevel = "1"; riskLabel = "无成绩数据";
        }
        else if (lastScore < lowScoreLine || recentFail >= 3)
        {
            riskLevel = "3"; riskLabel = "高危";
        }
        else if (predictedNextScore < lowScoreLine || recentFail >= 1 || slope < declineSlope)
        {
            riskLevel = "2"; riskLabel = "预警";
        }
        else if (slope < 0 || overallAvg < 72)
        {
            riskLevel = "1"; riskLabel = "关注";
        }
        else if (slope > -declineSlope && recentFail == 0)
        {
            riskLevel = "0"; riskLabel = "平稳向好";
        }
        result.put("riskLevel", riskLevel);
        result.put("riskLabel", riskLabel);

        List<Map<String, Object>> factors = new ArrayList<>();
        factors.add(factor("趋势斜率", round(slope, 2), slope < declineSlope ? "成绩持续下滑" : (slope > -declineSlope ? "成绩稳中有升" : "成绩基本平稳")));
        factors.add(factor("最近学期均分", round(lastScore, 1), lastScore < lowScoreLine ? "低于及格线" : "达到及格线"));
        factors.add(factor("预测下学期均分", round(predictedNextScore, 1), predictedNextScore < lowScoreLine ? "预测可能不及格" : "预测可及格"));
        factors.add(factor("最近学期不及格门数", recentFail, recentFail >= 1 ? "存在不及格课程" : "无不及格"));
        result.put("factors", factors);

        result.put("suggestions", buildSuggestions(riskLevel, slope, recentFail, lastScore));
        return result;
    }

    @Override
    public List<Map<String, Object>> courseDifficultyProfile(Long semesterId)
    {
        List<Map<String, Object>> list = predictionMapper.selectCourseDifficulty(semesterId);
        List<Map<String, Object>> out = new ArrayList<>();
        if (list == null) { return out; }
        for (Map<String, Object> row : list)
        {
            double avg = toDouble(row.get("avgScore"));
            double passRate = toDouble(row.get("passRate"));
            double failRate = clamp(100 - passRate, 0, 100);
            // 难度指数：分数越低、通过率越低、不及格率越高 → 越难
            double index = clamp(0.5 * (100 - avg) + 0.3 * (100 - passRate) + 0.2 * failRate, 0, 100);
            String level = index >= 55 ? "高" : (index >= 30 ? "中" : "低");
            Map<String, Object> r = new LinkedHashMap<>(row);
            r.put("failRate", round(failRate, 2));
            r.put("difficultyIndex", round(index, 1));
            r.put("difficultyLevel", level);
            out.add(r);
        }
        // 按难度指数降序（最难的在前）
        out.sort((a, b) -> Double.compare(toDouble(b.get("difficultyIndex")), toDouble(a.get("difficultyIndex"))));
        return out;
    }

    @Override
    public Map<String, Object> riskBoard(Long semesterId, Long classId)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> base = predictionMapper.selectStudentRiskBase(semesterId, classId);
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Integer> dist = new LinkedHashMap<>();
        dist.put("高危", 0); dist.put("预警", 0); dist.put("关注", 0); dist.put("平稳", 0);
        if (base != null)
        {
            for (Map<String, Object> row : base)
            {
                double avg = toDouble(row.get("avgScore"));
                double gpa = toDouble(row.get("avgGpa"));
                int fail = (int) toDouble(row.get("failCount"));
                String label;
                String level;
                if (avg < lowScoreLine || fail >= 3) { level = "3"; label = "高危"; }
                else if (fail >= 1 || gpa < 2.0) { level = "2"; label = "预警"; }
                else if (avg < 72) { level = "1"; label = "关注"; }
                else { level = "0"; label = "平稳"; }
                Map<String, Object> r = new LinkedHashMap<>(row);
                r.put("riskLevel", level);
                r.put("riskLabel", label);
                list.add(r);
                dist.merge(label, 1, Integer::sum);
            }
        }
        result.put("list", list);
        result.put("distribution", dist);
        result.put("total", list.size());
        return result;
    }

    // ================= 辅助方法 =================

    private List<String> buildSuggestions(String riskLevel, double slope, int recentFail, double lastScore)
    {
        List<String> tips = new ArrayList<>();
        if ("3".equals(riskLevel))
        {
            tips.add("已触发高危，建议立即联系学业导师介入，制定补修与重修计划；");
            if (recentFail > 0) { tips.add("近期存在不及格课程，优先安排重修并跟进出勤与作业。"); }
        }
        else if ("2".equals(riskLevel))
        {
            tips.add("存在学业风险，建议加强薄弱课程辅导，控制选课门数避免负荷过载；");
            if (slope < declineSlope) { tips.add("成绩呈下滑趋势，需排查学习方法与出勤情况。"); }
        }
        else if ("1".equals(riskLevel))
        {
            tips.add("整体平稳但需关注，建议保持当前节奏并针对低分课程巩固。");
        }
        else
        {
            tips.add("学业表现良好，可视培养方案进度适度挑战更高学分或拓展课程。");
        }
        if (lastScore >= 85 && slope >= 0) { tips.add("学有余力，可考虑参与辅修或竞赛类课程。"); }
        return tips;
    }

    private Map<String, Object> factor(String name, Object value, String desc)
    {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("value", value);
        m.put("desc", desc);
        return m;
    }

    private double toDouble(Object v)
    {
        if (v == null) { return 0; }
        if (v instanceof Number) { return ((Number) v).doubleValue(); }
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0; }
    }

    private double clamp(double v, double min, double max)
    {
        if (v < min) { return min; }
        if (v > max) { return max; }
        return v;
    }

    private double round(double v, int scale)
    {
        double f = Math.pow(10, scale);
        return Math.round(v * f) / f;
    }
}
