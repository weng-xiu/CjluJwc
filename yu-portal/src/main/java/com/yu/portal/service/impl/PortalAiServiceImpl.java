package com.yu.portal.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yu.common.utils.StringUtils;
import com.yu.portal.mapper.PortalAiMapper;
import com.yu.portal.service.IPortalAiService;
import com.yu.system.service.ISysConfigService;
import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.domain.TpmSelectionEnrollment;
import com.yu.tpm.domain.TpmSelectionRound;
import com.yu.tpm.domain.dto.ConflictWarning;
import com.yu.tpm.service.ITpmCourseOfferingService;
import com.yu.tpm.service.ITpmSelectionEnrollmentService;
import com.yu.tpm.service.ITpmSelectionRoundService;

/**
 * 门户 AI 分析实现（Phase34 AI应用试点）
 *
 * 选课推荐采用「硬约束复用 + 多信号加权」口径：
 * 硬约束直接调用既有选课校验（时间冲突/学分超限/容量已满/规则限制），保证推荐结果一定可选，
 * 不会出现「推荐了却选不上」；打分信号全部来自培养方案、学分模块、修读进度、课程评教与容量余量，
 * 缺数据的信号不计入分母，每个推荐项都给出可核对的中文理由。
 *
 * @author yu
 * @date 2026-09-26
 */
@Service
public class PortalAiServiceImpl implements IPortalAiService
{
    /** 推荐返回条数参数 */
    private static final String CFG_TOP_N = "ai.recommend.topN";

    /** 推荐学分上限参数（与 tpm.selection.maxCredits 同口径） */
    private static final String CFG_MAX_CREDITS = "ai.recommend.maxCredits";

    /** 打分信号权重 */
    private static final double W_PLAN = 0.30;
    private static final double W_MODULE_GAP = 0.25;
    private static final double W_PROGRESS = 0.15;
    private static final double W_QUALITY = 0.15;
    private static final double W_CAPACITY = 0.15;

    /** 参与打分的候选上限（防止极端数据量下逐条冲突校验耗时） */
    private static final int MAX_CANDIDATES = 40;

    @Autowired
    private PortalAiMapper portalAiMapper;

    @Autowired
    private ITpmSelectionRoundService tpmSelectionRoundService;

    @Autowired
    private ITpmCourseOfferingService tpmCourseOfferingService;

    @Autowired
    private ITpmSelectionEnrollmentService tpmSelectionEnrollmentService;

    @Autowired
    private ISysConfigService configService;

    @Override
    public Map<String, Object> recommendCourses(Long studentId)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        Map<String, Object> profile = studentId == null ? null : portalAiMapper.selectStudentProfile(studentId);
        if (profile == null)
        {
            result.put("available", false);
            result.put("note", "未找到本人学籍信息，无法进行个性化推荐。");
            return result;
        }
        result.put("student", profile);

        TpmSelectionRound round = findOngoingRound();
        if (round == null)
        {
            result.put("available", false);
            result.put("note", "当前没有进行中的选课轮次，推荐功能仅在选课时段内可用。");
            return result;
        }
        result.put("round", describeRound(round));

        Map<String, Object> plan = portalAiMapper.selectPublishedPlan(num(profile, "majorId") == null
                ? null : num(profile, "majorId").longValue());

        // 已选清单（退课记录不计入），门数与学分同时用于余量计算
        List<TpmSelectionEnrollment> selected = listSelected(studentId, round.getRoundId());
        Set<Long> selectedOfferingIds = new LinkedHashSet<>();
        double selectedCredit = 0d;
        for (TpmSelectionEnrollment e : selected)
        {
            if (e.getCourseOfferingId() != null)
            {
                selectedOfferingIds.add(e.getCourseOfferingId());
            }
        }

        TpmCourseOffering query = new TpmCourseOffering();
        query.setSemesterId(round.getSemesterId());
        query.setOfferingStatus("1");
        List<TpmCourseOffering> offerings = tpmCourseOfferingService.selectTpmCourseOfferingListForPortal(query);
        if (offerings == null)
        {
            offerings = new ArrayList<>();
        }
        // 候选 = 本轮已确认开课 - 本人已选
        List<TpmCourseOffering> candidates = new ArrayList<>();
        List<Map<String, Object>> selectedViews = new ArrayList<>();
        Set<Long> selectedCourseIds = new LinkedHashSet<>();
        for (TpmCourseOffering o : offerings)
        {
            if (selectedOfferingIds.contains(o.getOfferingId()))
            {
                selectedCredit += dbl(o.getCredit());
                Map<String, Object> sv = new LinkedHashMap<>();
                sv.put("offeringId", o.getOfferingId());
                sv.put("courseId", o.getCourseId());
                sv.put("courseName", o.getCourseName());
                sv.put("credit", o.getCredit());
                sv.put("teacherName", o.getTeacherName());
                selectedViews.add(sv);
                if (o.getCourseId() != null)
                {
                    selectedCourseIds.add(o.getCourseId());
                }
                continue;
            }
            if (o.getCourseId() != null && selectedCourseIds.contains(o.getCourseId()))
            {
                // 同一课程已有其它教学班在选，不再重复推荐同课程班次
                continue;
            }
            candidates.add(o);
        }
        result.put("selected", selectedViews);

        int maxCourses = round.getMaxCoursesPerStudent() == null ? 0 : round.getMaxCoursesPerStudent();
        int remainingCourses = maxCourses > 0 ? maxCourses - selectedViews.size() : Integer.MAX_VALUE;
        double maxCredits = doubleConfig(CFG_MAX_CREDITS, 30d);
        double remainingCredit = maxCredits - selectedCredit;
        Map<String, Object> quota = new LinkedHashMap<>();
        quota.put("maxCourses", round.getMaxCoursesPerStudent());
        quota.put("maxCredits", maxCredits);
        quota.put("selectedCourses", selectedViews.size());
        quota.put("selectedCredit", round2(selectedCredit));
        quota.put("remainingCourses", remainingCourses == Integer.MAX_VALUE ? null : remainingCourses);
        quota.put("remainingCredit", round2(remainingCredit));
        result.put("quota", quota);

        if (candidates.isEmpty())
        {
            result.put("available", true);
            result.put("items", new ArrayList<>());
            result.put("note", "本轮已确认开课的课程均已在本人选课清单中，暂无新的推荐候选。");
            return result;
        }
        if (remainingCourses <= 0 || remainingCredit <= 0)
        {
            result.put("available", true);
            result.put("items", new ArrayList<>());
            result.put("note", "本人本轮已选门数或学分已达上限，请先退课或联系教务处调整后再选。");
            return result;
        }

        // 批量取信号数据，避免逐条查询
        List<Long> courseIds = new ArrayList<>();
        for (TpmCourseOffering o : candidates)
        {
            if (o.getCourseId() != null)
            {
                courseIds.add(o.getCourseId());
            }
        }
        Map<Long, Map<String, Object>> courseInfo = courseIds.isEmpty()
                ? new LinkedHashMap<>() : mapById(portalAiMapper.selectCourseInfoByIds(courseIds), "courseId");
        Map<String, Map<String, Object>> moduleRequire = new LinkedHashMap<>();
        Long planId = plan == null ? null : longOf(num(plan, "planId"));
        if (planId != null)
        {
            for (Map<String, Object> m : portalAiMapper.selectPlanModuleRequire(planId))
            {
                moduleRequire.put(str(m, "creditType"), m);
            }
        }
        Map<String, Double> earnedByCategory = new LinkedHashMap<>();
        for (Map<String, Object> e : portalAiMapper.selectStudentEarnedByCategory(studentId))
        {
            earnedByCategory.put(str(e, "creditType"), dbl(num(e, "earnedCredit")));
        }
        Map<Long, Map<String, Object>> evalStat = courseIds.isEmpty()
                ? new LinkedHashMap<>() : mapById(portalAiMapper.selectCourseEvalStat(courseIds), "courseId");
        Map<String, Object> evalGlobal = portalAiMapper.selectEvalGlobalAvg();
        Double globalEvalAvg = evalGlobal == null ? null : num(evalGlobal, "globalAvgScore");
        Double studied = null;
        Map<String, Object> progress = portalAiMapper.selectStudentSemesterProgress(studentId);
        if (progress != null)
        {
            studied = num(progress, "studiedSemesters");
        }
        int nextSemesterOrder = studied == null ? 1 : studied.intValue() + 1;

        List<Map<String, Object>> items = new ArrayList<>();
        List<Map<String, Object>> excluded = new ArrayList<>();
        int checked = 0;
        for (TpmCourseOffering o : candidates)
        {
            if (checked >= MAX_CANDIDATES)
            {
                break;
            }
            checked++;
            List<ConflictWarning> conflicts = tpmSelectionEnrollmentService
                    .checkSelectionConflicts(studentId, o.getOfferingId(), round.getRoundId());
            if (conflicts != null && !conflicts.isEmpty())
            {
                Map<String, Object> ex = new LinkedHashMap<>();
                ex.put("offeringId", o.getOfferingId());
                ex.put("courseName", o.getCourseName());
                ex.put("teacherName", o.getTeacherName());
                List<String> reasons = new ArrayList<>();
                for (ConflictWarning w : conflicts)
                {
                    reasons.add(w.getMessage());
                }
                ex.put("reasons", reasons);
                excluded.add(ex);
                continue;
            }
            items.add(scoreCandidate(o, courseInfo, planId, moduleRequire, earnedByCategory,
                    evalStat, globalEvalAvg, nextSemesterOrder));
        }
        items.sort((a, b) -> Double.compare(dbl(num(b, "score")), dbl(num(a, "score"))));
        int topN = intConfig(CFG_TOP_N, 8);
        if (items.size() > topN)
        {
            items = new ArrayList<>(items.subList(0, topN));
        }
        int rank = 1;
        for (Map<String, Object> item : items)
        {
            item.put("rank", rank++);
        }
        result.put("available", true);
        result.put("items", items);
        result.put("excluded", excluded);
        result.put("signalNote", buildSignalNote(plan, globalEvalAvg, studied));
        if (items.isEmpty())
        {
            result.put("note", "本轮剩余候选课程均与本人已选课程存在冲突或不再适用，详见「未推荐原因」。");
        }
        return result;
    }

    /**
     * 单个候选课程打分，返回可直接渲染的视图（含分数、信号明细与中文理由）
     */
    private Map<String, Object> scoreCandidate(TpmCourseOffering o, Map<Long, Map<String, Object>> courseInfo,
            Long planId, Map<String, Map<String, Object>> moduleRequire, Map<String, Double> earnedByCategory,
            Map<Long, Map<String, Object>> evalStat, Double globalEvalAvg, int nextSemesterOrder)
    {
        Map<String, Object> info = o.getCourseId() == null ? null : courseInfo.get(o.getCourseId());
        List<Map<String, Object>> signals = new ArrayList<>();
        List<String> reasons = new ArrayList<>();
        double weighted = 0d;
        double weightSum = 0d;

        // 1) 培养方案一致性：课程是否属于本人所属方案
        if (planId != null && info != null)
        {
            Long coursePlanId = longOf(num(info, "planId"));
            boolean inPlan = coursePlanId != null && coursePlanId.equals(planId);
            double v = inPlan ? 1d : 0d;
            signals.add(signal("PLAN", "培养方案匹配", v, W_PLAN, inPlan ? "属于本人培养方案内的课程" : "不在本人培养方案内"));
            weighted += v * W_PLAN;
            weightSum += W_PLAN;
            if (inPlan)
            {
                reasons.add("本人专业培养方案内课程");
            }
        }

        // 2) 学分模块缺口：该课程所属模块距离方案要求还差多少学分
        String category = info == null ? null : str(info, "courseCategory");
        if (StringUtils.isNotEmpty(category) && moduleRequire.containsKey(category))
        {
            double required = dbl(num(moduleRequire.get(category), "requiredCredit"));
            double earned = earnedByCategory.getOrDefault(category, 0d);
            if (required > 0)
            {
                double gapRatio = clamp((required - earned) / required);
                String typeName = str(moduleRequire.get(category), "creditTypeName");
                signals.add(signal("MODULE", "模块学分缺口", gapRatio, W_MODULE_GAP,
                        (StringUtils.isEmpty(typeName) ? category : typeName) + "模块要求 " + trim(required)
                                + " 学分，已获得 " + trim(earned) + " 学分"));
                weighted += gapRatio * W_MODULE_GAP;
                weightSum += W_MODULE_GAP;
                if (gapRatio > 0)
                {
                    reasons.add("可补「" + (StringUtils.isEmpty(typeName) ? category : typeName) + "」模块学分 "
                            + trim(required - earned) + " 分");
                }
            }
        }

        // 3) 修读进度：建议学期与本人当前进度的贴近程度
        Double semesterOrder = info == null ? null : num(info, "semesterOrder");
        if (semesterOrder != null && semesterOrder > 0)
        {
            double diff = Math.abs(semesterOrder - nextSemesterOrder);
            double v = clamp(1d - diff / 3d);
            signals.add(signal("PROGRESS", "修读进度契合", v, W_PROGRESS,
                    "方案建议第 " + trim(semesterOrder) + " 学期修读，本人当前为第 " + nextSemesterOrder + " 学期"));
            weighted += v * W_PROGRESS;
            weightSum += W_PROGRESS;
            if (v >= 0.99d)
            {
                reasons.add("与当前修读进度一致");
            }
        }

        // 4) 课程质量：以该课程历史评教均分相对全校均分的差值折算
        Map<String, Object> ev = o.getCourseId() == null ? null : evalStat.get(o.getCourseId());
        Double courseEvalAvg = ev == null ? null : num(ev, "avgScore");
        if (courseEvalAvg != null && globalEvalAvg != null && globalEvalAvg > 0)
        {
            double v = clamp(0.5d + (courseEvalAvg - globalEvalAvg) / 20d);
            signals.add(signal("QUALITY", "课程评教水平", v, W_QUALITY,
                    "评教均分 " + trim(courseEvalAvg) + "，全校均分 " + trim(globalEvalAvg)
                            + "（" + trim(dbl(num(ev, "evalCount"))) + " 份评价）"));
            weighted += v * W_QUALITY;
            weightSum += W_QUALITY;
            if (courseEvalAvg > globalEvalAvg)
            {
                reasons.add("评教分高于全校均值");
            }
        }

        // 5) 容量可得性：剩余容量占比，余量越大越容易选中
        Double maxStudents = o.getMaxStudents() == null ? null : o.getMaxStudents().doubleValue();
        if (maxStudents != null && maxStudents > 0)
        {
            int enrolled = o.getEnrolledCount() == null ? 0 : o.getEnrolledCount();
            double v = clamp((maxStudents - enrolled) / maxStudents);
            signals.add(signal("CAPACITY", "容量余量", v, W_CAPACITY,
                    "已选 " + enrolled + " 人，容量 " + trim(maxStudents) + " 人"));
            weighted += v * W_CAPACITY;
            weightSum += W_CAPACITY;
            if (v < 0.15d)
            {
                reasons.add("剩余容量偏少，可能进入候补抽签");
            }
        }

        Map<String, Object> item = new LinkedHashMap<>();
        item.put("offeringId", o.getOfferingId());
        item.put("courseId", o.getCourseId());
        item.put("courseName", o.getCourseName());
        item.put("courseCode", o.getCourseCode());
        item.put("teacherName", o.getTeacherName());
        item.put("credit", o.getCredit());
        item.put("maxStudents", o.getMaxStudents());
        item.put("enrolledCount", o.getEnrolledCount());
        item.put("courseCategory", category);
        item.put("semesterOrder", semesterOrder);
        item.put("score", weightSum == 0 ? null : round1(weighted / weightSum * 100d));
        item.put("signals", signals);
        item.put("reasons", reasons.isEmpty() ? listDefaultReasons(o) : reasons);
        return item;
    }

    private List<String> listDefaultReasons(TpmCourseOffering o)
    {
        List<String> reasons = new ArrayList<>();
        reasons.add("本轮已确认开课且与本人已选课程无冲突");
        if (o.getCredit() != null)
        {
            reasons.add("计 " + trim(o.getCredit().doubleValue()) + " 学分");
        }
        return reasons;
    }

    private String buildSignalNote(Map<String, Object> plan, Double globalEvalAvg, Double studiedSemesters)
    {
        List<String> notes = new ArrayList<>();
        notes.add(plan == null ? "未找到本人专业的已发布培养方案，「培养方案匹配」「模块学分缺口」两项信号不参与评分"
                : "培养方案：" + str(plan, "planName") + "（要求 " + trim(dbl(num(plan, "totalCredits"))) + " 学分）");
        notes.add(globalEvalAvg == null ? "暂无可用的课程评教数据，「课程评教水平」不参与评分"
                : "全校评教均分 " + trim(globalEvalAvg));
        notes.add(studiedSemesters == null || studiedSemesters == 0
                ? "尚无成绩记录，修读进度按第 1 学期估算" : "已修读 " + studiedSemesters.intValue() + " 个学期");
        return String.join("；", notes) + "。";
    }

    @Override
    public Map<String, Object> buildPortrait(Long studentId)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("studentId", studentId);
        Map<String, Object> profile = studentId == null ? null : portalAiMapper.selectStudentProfile(studentId);
        if (profile == null)
        {
            result.put("available", false);
            result.put("note", "未找到本人学籍信息，无法生成学业画像。");
            return result;
        }
        result.put("student", profile);
        Map<String, Object> plan = portalAiMapper.selectPublishedPlan(longOf(num(profile, "majorId")));
        result.put("plan", plan);
        Map<String, Object> summary = portalAiMapper.selectStudentGradeSummary(studentId);
        result.put("gradeSummary", summary);
        Map<String, Object> peer = portalAiMapper.selectPeerGradeSummary(longOf(num(profile, "majorId")),
                integerOf(num(profile, "enrollmentYear")));
        result.put("peerSummary", peer);
        List<Map<String, Object>> grades = portalAiMapper.selectRecentGrades(studentId, 20);
        result.put("grades", grades);
        result.put("warnings", portalAiMapper.selectStudentWarnings(studentId));

        // 模块达成情况（要求学分 / 已获得学分 / 达成率）
        List<Map<String, Object>> modules = new ArrayList<>();
        Long planId = plan == null ? null : longOf(num(plan, "planId"));
        if (planId != null)
        {
            Map<String, Double> earned = new LinkedHashMap<>();
            for (Map<String, Object> e : portalAiMapper.selectStudentEarnedByCategory(studentId))
            {
                earned.put(str(e, "creditType"), dbl(num(e, "earnedCredit")));
            }
            for (Map<String, Object> r : portalAiMapper.selectPlanModuleRequire(planId))
            {
                double required = dbl(num(r, "requiredCredit"));
                double got = earned.getOrDefault(str(r, "creditType"), 0d);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("creditType", str(r, "creditType"));
                m.put("creditTypeName", str(r, "creditTypeName"));
                m.put("requiredCredit", required);
                m.put("earnedCredit", round2(got));
                m.put("rate", required > 0 ? round1(Math.min(got / required, 1d) * 100) : null);
                m.put("courseCount", num(r, "courseCount"));
                modules.add(m);
            }
        }
        result.put("modules", modules);
        result.put("categoryScores", portalAiMapper.selectCategoryScoreStat(studentId));

        List<Map<String, Object>> dims = buildDims(profile, plan, summary, peer, modules, studentId);
        result.put("dims", dims);
        result.put("overallScore", overall(dims));
        result.put("suggestions", buildSuggestions(dims, modules, summary));
        result.put("available", true);
        result.put("note", "画像各维度均由成绩、培养方案、选课与预警表实数计算，缺少数据的维度会标注原因。");
        return result;
    }

    /**
     * 六维画像：成绩水平、学分达成、结构均衡、学习进度、学业风险、修读积极性
     */
    private List<Map<String, Object>> buildDims(Map<String, Object> profile, Map<String, Object> plan,
            Map<String, Object> summary, Map<String, Object> peer, List<Map<String, Object>> modules, Long studentId)
    {
        List<Map<String, Object>> dims = new ArrayList<>();
        Double avgScore = summary == null ? null : num(summary, "avgScore");
        Double peerAvg = peer == null ? null : num(peer, "peerAvgScore");
        Integer peerCount = peer == null ? null : integerOf(num(peer, "peerCount"));
        if (avgScore == null)
        {
            dims.add(dim("SCORE", "成绩水平", null, "尚无任何成绩记录，暂无法评价"));
        }
        else
        {
            StringBuilder text = new StringBuilder("加权平均成绩 ").append(trim(avgScore)).append(" 分");
            if (peerAvg != null)
            {
                double diff = round1(avgScore - peerAvg);
                text.append("，").append(diff >= 0 ? "高于" : "低于").append("同专业同年级均分 ")
                        .append(trim(Math.abs(diff))).append(" 分（对比样本 ")
                        .append(peerCount == null ? 0 : peerCount).append(" 人）");
            }
            else
            {
                text.append("，暂无同专业同年级可对比样本");
            }
            dims.add(dim("SCORE", "成绩水平", round1(avgScore), text.toString()));
        }

        double earnedCredit = summary == null ? 0d : dbl(num(summary, "earnedCredit"));
        double planCredit = plan == null ? 0d : dbl(num(plan, "totalCredits"));
        if (planCredit > 0)
        {
            dims.add(dim("CREDIT", "学分达成", round1(Math.min(earnedCredit / planCredit, 1d) * 100),
                    "已获 " + trim(earnedCredit) + " 学分 / 方案要求 " + trim(planCredit) + " 学分"));
        }
        else
        {
            dims.add(dim("CREDIT", "学分达成", null, "未找到已发布培养方案的总学分要求，暂无法评价"));
        }

        // 结构均衡以「最短板模块达成率」衡量，任一模块滞后都会拉低该维度
        Double worst = null;
        String worstName = null;
        StringBuilder moduleText = new StringBuilder();
        for (Map<String, Object> m : modules)
        {
            double required = dbl(num(m, "requiredCredit"));
            if (required <= 0)
            {
                continue;
            }
            double rate = Math.min(dbl(num(m, "earnedCredit")) / required, 1d);
            if (worst == null || rate < worst)
            {
                worst = rate;
                worstName = StringUtils.isEmpty(str(m, "creditTypeName")) ? str(m, "creditType") : str(m, "creditTypeName");
            }
            if (moduleText.length() > 0)
            {
                moduleText.append("、");
            }
            moduleText.append(StringUtils.isEmpty(str(m, "creditTypeName")) ? str(m, "creditType") : str(m, "creditTypeName"))
                    .append(" ").append(trim(rate * 100)).append("%");
        }
        if (worst == null)
        {
            dims.add(dim("BALANCE", "结构均衡", null, "培养方案未配置学分模块要求，暂无法评价结构均衡度"));
        }
        else
        {
            dims.add(dim("BALANCE", "结构均衡", round1(worst * 100),
                    "最短板为「" + worstName + "」，各模块达成率：" + moduleText));
        }

        // 学习进度：实际完成比例与按学期数推算的理论进度对比
        Map<String, Object> progress = portalAiMapper.selectStudentSemesterProgress(studentId);
        Double studied = progress == null ? null : num(progress, "studiedSemesters");
        if (planCredit > 0 && studied != null && studied > 0)
        {
            double expected = Math.min(studied / 8d, 1d);
            double actual = Math.min(earnedCredit / planCredit, 1d);
            double value = expected <= 0 ? 100d : round1(Math.min(actual / expected, 1.2d) / 1.2d * 100d);
            dims.add(dim("PROGRESS", "学习进度", value,
                    "已修 " + studied.intValue() + " 个学期，完成 " + trim(actual * 100) + "% 学分，理论进度 "
                            + trim(expected * 100) + "%"));
        }
        else
        {
            dims.add(dim("PROGRESS", "学习进度", null, "缺少成绩或方案数据，暂无法计算学习进度"));
        }

        Map<String, Object> warn = portalAiMapper.selectStudentWarningSummary(studentId);
        int warnTotal = warn == null ? 0 : (int) dbl(num(warn, "total"));
        int unresolved = warn == null ? 0 : (int) dbl(num(warn, "unresolved"));
        Double maxLevel = warn == null ? null : num(warn, "maxLevel");
        double risk = 100d - unresolved * 20d;
        if (maxLevel != null && maxLevel >= 2)
        {
            risk -= 20d;
        }
        risk = Math.max(risk, 0d);
        dims.add(dim("RISK", "学业风险", round1(risk), warnTotal == 0
                ? "无任何学业预警记录" : "累计预警 " + warnTotal + " 条，其中未解决 " + unresolved + " 条"
                        + (maxLevel == null ? "" : "，最高等级 " + maxLevel.intValue())));

        TpmSelectionRound round = findOngoingRound();
        Long statRoundId = round == null ? null : round.getRoundId();
        int selectedCourses = 0;
        int droppedCourses = 0;
        TpmSelectionEnrollment eq = new TpmSelectionEnrollment();
        eq.setStudentId(studentId);
        if (statRoundId != null)
        {
            eq.setRoundId(statRoundId);
        }
        List<TpmSelectionEnrollment> enrollments = tpmSelectionEnrollmentService.selectTpmSelectionEnrollmentList(eq);
        if (enrollments != null)
        {
            for (TpmSelectionEnrollment e : enrollments)
            {
                if ("3".equals(e.getResultStatus()))
                {
                    droppedCourses++;
                }
                else if ("1".equals(e.getResultStatus()))
                {
                    selectedCourses++;
                }
            }
        }
        Integer maxCourses = round == null || round.getMaxCoursesPerStudent() == null ? 0 : round.getMaxCoursesPerStudent();
        if (maxCourses > 0)
        {
            dims.add(dim("ENGAGE", "修读积极性", round1(Math.min(selectedCourses * 1.0 / maxCourses, 1d) * 100),
                    "本轮选中 " + selectedCourses + " 门（上限 " + maxCourses + " 门），退课 " + droppedCourses + " 次"));
        }
        else
        {
            dims.add(dim("ENGAGE", "修读积极性", null,
                    selectedCourses > 0 ? "当前无进行中轮次，已选 " + selectedCourses + " 门不作进度评价"
                            : "尚无选课记录，暂无法评价修读积极性"));
        }
        return dims;
    }

    private Double overall(List<Map<String, Object>> dims)
    {
        double sum = 0d;
        int n = 0;
        for (Map<String, Object> d : dims)
        {
            Object v = d.get("value");
            if (v instanceof Number)
            {
                sum += ((Number) v).doubleValue();
                n++;
            }
        }
        return n == 0 ? null : round1(sum / n);
    }

    /**
     * 建议按维度短板生成，全部为可核对的规则结论（不由模型生成）
     */
    private List<String> buildSuggestions(List<Map<String, Object>> dims, List<Map<String, Object>> modules,
            Map<String, Object> summary)
    {
        List<String> tips = new ArrayList<>();
        for (Map<String, Object> d : dims)
        {
            Object v = d.get("value");
            if (!(v instanceof Number))
            {
                continue;
            }
            double value = ((Number) v).doubleValue();
            String code = str(d, "code");
            if (value >= 80)
            {
                continue;
            }
            if ("SCORE".equals(code))
            {
                tips.add(value < 60 ? "平均成绩低于 60 分，建议优先复核不及格课程并申请重修或补考安排。"
                        : "平均成绩 " + trim(value) + " 分，仍有提升空间，建议关注平时成绩占比高的课程。");
            }
            else if ("CREDIT".equals(code))
            {
                tips.add("总学分达成率 " + trim(value) + "%，请按培养方案剩余学分倒排各学期选课门数。");
            }
            else if ("BALANCE".equals(code))
            {
                String lag = null;
                double lagRate = 2;
                for (Map<String, Object> m : modules)
                {
                    double required = dbl(num(m, "requiredCredit"));
                    if (required <= 0)
                    {
                        continue;
                    }
                    double rate = dbl(num(m, "earnedCredit")) / required;
                    if (rate < lagRate)
                    {
                        lagRate = rate;
                        lag = StringUtils.isEmpty(str(m, "creditTypeName")) ? str(m, "creditType") : str(m, "creditTypeName");
                    }
                }
                tips.add("「" + lag + "」模块学分完成度最低，建议优先补足该模块课程，避免毕业审核时出现结构缺漏。");
            }
            else if ("PROGRESS".equals(code))
            {
                tips.add("学分完成进度落后于按学期推算的理论进度，建议与教学秘书核对后续学期的开课安排。");
            }
            else if ("RISK".equals(code))
            {
                tips.add("存在未处理的学业预警，请及时联系辅导员或教学秘书确认帮扶措施并跟进解除条件。");
            }
            else if ("ENGAGE".equals(code))
            {
                tips.add("本轮选课门数未达上限，可在剩余容量内补选契合培养方案缺口的课程。");
            }
        }
        if (tips.isEmpty())
        {
            Double fail = summary == null ? null : num(summary, "failCount");
            tips.add(fail != null && fail > 0
                    ? "各维度表现均达到 80 分以上，但仍有 " + fail.intValue() + " 门不及格课程，请留意重修安排。"
                    : "各维度表现均达到 80 分以上，保持当前修读节奏即可。");
        }
        return tips;
    }

    /** 进行中轮次（按开始时间取最近一条） */
    private TpmSelectionRound findOngoingRound()
    {
        TpmSelectionRound query = new TpmSelectionRound();
        query.setRoundStatus("1");
        List<TpmSelectionRound> rounds = tpmSelectionRoundService.selectTpmSelectionRoundList(query);
        if (rounds == null || rounds.isEmpty())
        {
            return null;
        }
        return rounds.get(0);
    }

    /** 本人在该轮次的有效选课（排除退课） */
    private List<TpmSelectionEnrollment> listSelected(Long studentId, Long roundId)
    {
        TpmSelectionEnrollment query = new TpmSelectionEnrollment();
        query.setStudentId(studentId);
        query.setRoundId(roundId);
        List<TpmSelectionEnrollment> list = tpmSelectionEnrollmentService.selectTpmSelectionEnrollmentList(query);
        List<TpmSelectionEnrollment> valid = new ArrayList<>();
        if (list != null)
        {
            for (TpmSelectionEnrollment e : list)
            {
                if (!"3".equals(e.getResultStatus()))
                {
                    valid.add(e);
                }
            }
        }
        return valid;
    }

    private Map<String, Object> describeRound(TpmSelectionRound round)
    {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("roundId", round.getRoundId());
        view.put("roundName", round.getRoundName());
        view.put("semesterId", round.getSemesterId());
        view.put("maxCoursesPerStudent", round.getMaxCoursesPerStudent());
        view.put("startTime", round.getStartTime());
        view.put("endTime", round.getEndTime());
        view.put("roundStatus", round.getRoundStatus());
        return view;
    }

    private Map<String, Object> dim(String code, String name, Double value, String text)
    {
        Map<String, Object> d = new LinkedHashMap<>();
        d.put("code", code);
        d.put("name", name);
        d.put("value", value);
        d.put("text", text);
        return d;
    }

    private Map<String, Object> signal(String code, String name, double value, double weight, String detail)
    {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("code", code);
        s.put("name", name);
        s.put("value", round1(value * 100));
        s.put("weight", weight);
        s.put("detail", detail);
        return s;
    }

    private Map<Long, Map<String, Object>> mapById(List<Map<String, Object>> rows, String key)
    {
        Map<Long, Map<String, Object>> map = new LinkedHashMap<>();
        if (rows == null)
        {
            return map;
        }
        for (Map<String, Object> row : rows)
        {
            Long id = longOf(num(row, key));
            if (id != null)
            {
                map.put(id, row);
            }
        }
        return map;
    }

    private Double clamp(double v)
    {
        return Math.max(0d, Math.min(1d, v));
    }

    private double dbl(Double v)
    {
        return v == null ? 0d : v;
    }

    private Double num(Map<String, Object> map, String key)
    {
        if (map == null)
        {
            return null;
        }
        Object v = map.get(key);
        if (v == null)
        {
            return null;
        }
        if (v instanceof Number)
        {
            return ((Number) v).doubleValue();
        }
        try
        {
            return Double.parseDouble(v.toString());
        }
        catch (NumberFormatException e)
        {
            return null;
        }
    }

    private String str(Map<String, Object> map, String key)
    {
        if (map == null || map.get(key) == null)
        {
            return null;
        }
        return map.get(key).toString();
    }

    private Long longOf(Double v)
    {
        return v == null ? null : v.longValue();
    }

    private Integer integerOf(Double v)
    {
        return v == null ? null : v.intValue();
    }

    private double round1(double v)
    {
        return Math.round(v * 10d) / 10d;
    }

    private double round2(double v)
    {
        return Math.round(v * 100d) / 100d;
    }

    /** 去掉无意义的小数尾零，便于中文理由阅读 */
    private String trim(double v)
    {
        if (v == Math.floor(v))
        {
            return String.valueOf((long) v);
        }
        return String.valueOf(round2(v));
    }

    private int intConfig(String key, int defaultValue)
    {
        return parseConfig(configService.selectConfigByKey(key), defaultValue);
    }

    private double doubleConfig(String key, double defaultValue)
    {
        String v = configService.selectConfigByKey(key);
        if (StringUtils.isEmpty(v))
        {
            return defaultValue;
        }
        try
        {
            return Double.parseDouble(v.trim());
        }
        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }

    private int parseConfig(String v, int defaultValue)
    {
        if (StringUtils.isEmpty(v))
        {
            return defaultValue;
        }
        try
        {
            return Integer.parseInt(v.trim());
        }
        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }
}
