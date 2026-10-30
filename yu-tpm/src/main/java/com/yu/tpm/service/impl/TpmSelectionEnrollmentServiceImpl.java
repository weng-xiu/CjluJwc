package com.yu.tpm.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.yu.common.core.domain.AjaxResult;
import com.yu.common.annotation.BizMetric;
import com.yu.common.monitor.BizMetrics;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.schedule.TimeSlotUtils;
import com.yu.framework.cache.SelectionCacheManager;
import com.yu.tpm.domain.TpmCourseLibrary;
import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.domain.TpmSelectionEnrollment;
import com.yu.tpm.domain.TpmSelectionRound;
import com.yu.tpm.domain.dto.ConflictWarning;
import com.yu.tpm.domain.dto.CourseSuggestion;
import com.yu.tpm.mapper.TpmCourseLibraryMapper;
import com.yu.tpm.mapper.TpmCourseOfferingMapper;
import com.yu.tpm.mapper.TpmScheduleMapper;
import com.yu.tpm.mapper.TpmSelectionRoundMapper;
import com.yu.tpm.service.ITpmSelectionRuleService;
import com.yu.brm.mapper.BrmTeacherMapper;
import com.yu.brm.domain.BrmTeacher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.tpm.mapper.TpmSelectionEnrollmentMapper;
import com.yu.tpm.service.ITpmSelectionEnrollmentService;

/**
 * 选课名单Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmSelectionEnrollmentServiceImpl implements ITpmSelectionEnrollmentService 
{
    /** 默认学分上限（从配置读取） */
    @org.springframework.beans.factory.annotation.Value("${tpm.selection.maxCredits:30.0}")
    private double defaultMaxCredits;

    /** 星期名称映射 */
    private static final String[] WEEK_DAY_NAMES = {"", "周一", "周二", "周三", "周四", "周五", "周六", "周日"};

    /**
     * P-3：抽签结果批量落库的分块行数。
     * 仅限制单条 SQL 的体积与锁持有粒度，**不是分段提交边界**——抽签必须整体原子，
     * 中途失败要能全量回滚后重抽，拆成多段独立事务会留下「部分中签、部分未抽签」的半成品。
     */
    static final int LOTTERY_UPDATE_CHUNK = 500;

    @Autowired
    private TpmSelectionEnrollmentMapper tpmSelectionEnrollmentMapper;

    @Autowired
    private TpmScheduleMapper tpmScheduleMapper;

    @Autowired
    private TpmCourseOfferingMapper tpmCourseOfferingMapper;

    @Autowired
    private TpmCourseLibraryMapper tpmCourseLibraryMapper;

    @Autowired
    private TpmSelectionRoundMapper tpmSelectionRoundMapper;

    @Autowired
    private BrmTeacherMapper brmTeacherMapper;

    @Autowired
    private SelectionCacheManager selectionCacheManager;

    @Autowired
    private ITpmSelectionRuleService tpmSelectionRuleService;

    /** Q3/P2 可观测性：超卖尝试埋点门面（写入 Micrometer，失败自行降级不影响主流程） */
    @Autowired
    private BizMetrics bizMetrics;

    @Override
    public TpmSelectionEnrollment selectTpmSelectionEnrollmentByEnrollId(Long enrollId)
    {
        return tpmSelectionEnrollmentMapper.selectTpmSelectionEnrollmentByEnrollId(enrollId);
    }

    @Override
    public List<TpmSelectionEnrollment> selectTpmSelectionEnrollmentList(TpmSelectionEnrollment tpmSelectionEnrollment)
    {
        return tpmSelectionEnrollmentMapper.selectTpmSelectionEnrollmentList(tpmSelectionEnrollment);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int insertTpmSelectionEnrollment(TpmSelectionEnrollment tpmSelectionEnrollment)
    {
        tpmSelectionEnrollment.setCreateTime(DateUtils.getNowDate());
        int rows = tpmSelectionEnrollmentMapper.insertTpmSelectionEnrollment(tpmSelectionEnrollment);
        // T-3：管理端直接补录报名行不经过 atomic 抢名额路径，按报名表真相重算该开课的占用数
        recountSeats(tpmSelectionEnrollment.getCourseOfferingId());
        return rows;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int updateTpmSelectionEnrollment(TpmSelectionEnrollment tpmSelectionEnrollment)
    {
        // 改选/改状态可能跨开课，两个开课都要重算（旧开课改前取，删完就查不到了）
        TpmSelectionEnrollment before = tpmSelectionEnrollment.getEnrollId() == null ? null
                : tpmSelectionEnrollmentMapper.selectTpmSelectionEnrollmentByEnrollId(tpmSelectionEnrollment.getEnrollId());
        tpmSelectionEnrollment.setUpdateTime(DateUtils.getNowDate());
        int rows = tpmSelectionEnrollmentMapper.updateTpmSelectionEnrollment(tpmSelectionEnrollment);
        recountSeats(tpmSelectionEnrollment.getCourseOfferingId());
        if (before != null)
        {
            recountSeats(before.getCourseOfferingId());
        }
        return rows;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int deleteTpmSelectionEnrollmentByEnrollId(Long enrollId)
    {
        TpmSelectionEnrollment before = tpmSelectionEnrollmentMapper.selectTpmSelectionEnrollmentByEnrollId(enrollId);
        int rows = tpmSelectionEnrollmentMapper.deleteTpmSelectionEnrollmentByEnrollId(enrollId);
        if (before != null)
        {
            recountSeats(before.getCourseOfferingId());
        }
        return rows;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public int deleteTpmSelectionEnrollmentByEnrollIds(Long[] enrollIds)
    {
        // 软删后查不到开课ID，先取回再删；管理端批量操作，N 为 UI 勾选行数（个位数）
        List<Long> affectedOfferingIds = new ArrayList<>();
        if (enrollIds != null)
        {
            for (Long enrollId : enrollIds)
            {
                TpmSelectionEnrollment before = tpmSelectionEnrollmentMapper.selectTpmSelectionEnrollmentByEnrollId(enrollId);
                if (before != null && before.getCourseOfferingId() != null)
                {
                    affectedOfferingIds.add(before.getCourseOfferingId());
                }
            }
        }
        int rows = tpmSelectionEnrollmentMapper.deleteTpmSelectionEnrollmentByEnrollIds(enrollIds);
        recountSeats(affectedOfferingIds.toArray(new Long[0]));
        return rows;
    }

    /**
     * T-3：按报名表真相重算若干开课的占用数（幂等）。
     *
     * <p>热路径（选课/退课/改选/递补）用原子增量；管理端直接增删改报名行这类低频路径用重算，
     * 既不会把并发增量覆盖掉，也顺手修正了历史上可能的计数漂移。</p>
     */
    private void recountSeats(Long... offeringIds)
    {
        List<Long> ids = new ArrayList<>();
        for (Long offeringId : offeringIds)
        {
            if (offeringId != null && !ids.contains(offeringId))
            {
                ids.add(offeringId);
            }
        }
        if (!ids.isEmpty())
        {
            tpmCourseOfferingMapper.recountEnrolled(ids);
        }
    }

    /**
     * 检测选课冲突
     * 包括：时间冲突、学分超限、课程容量已满
     *
     * @param studentId        学生ID
     * @param courseOfferingId 待选开课ID
     * @param roundId          轮次ID
     * @return 冲突警告列表，为空表示无冲突
     */
    @Override
    public List<ConflictWarning> checkSelectionConflicts(Long studentId, Long courseOfferingId, Long roundId)
    {
        List<ConflictWarning> warnings = new ArrayList<>();

        // 1. 获取待选课程的排课信息
        // Q-4：不用双括号匿名内部类（每次调用都会生成匿名子类，序列化与内存开销大）
        TpmSchedule scheduleQuery = new TpmSchedule();
        scheduleQuery.setOfferingId(courseOfferingId);
        List<TpmSchedule> targetSchedules = tpmScheduleMapper.selectTpmScheduleList(scheduleQuery);

        // 2. 查询学生在该轮次已选课程
        List<TpmSelectionEnrollment> enrolledList = tpmSelectionEnrollmentMapper.selectByStudentAndRound(studentId, roundId);

        if (enrolledList != null && !enrolledList.isEmpty())
        {
            // 提取已选课程的开课ID列表
            List<Long> enrolledOfferingIds = enrolledList.stream()
                    .map(TpmSelectionEnrollment::getCourseOfferingId)
                    .collect(Collectors.toList());

            // 3. 批量获取已选课程的排课信息
            List<TpmSchedule> enrolledSchedules = tpmScheduleMapper.selectByOfferingIds(enrolledOfferingIds);

            // 按开课ID分组
            Map<Long, List<TpmSchedule>> scheduleMap = enrolledSchedules.stream()
                    .collect(Collectors.groupingBy(TpmSchedule::getOfferingId));

            // 4. 逐门检测时间冲突
            for (TpmSelectionEnrollment enrolled : enrolledList)
            {
                Long enrolledOfferingId = enrolled.getCourseOfferingId();
                List<TpmSchedule> courseSchedules = scheduleMap.get(enrolledOfferingId);
                if (courseSchedules == null || courseSchedules.isEmpty())
                {
                    continue;
                }

                // 检测待选课程与已选课程的时间冲突
                for (TpmSchedule targetSchedule : targetSchedules)
                {
                    for (TpmSchedule enrolledSchedule : courseSchedules)
                    {
                        boolean conflict = TimeSlotUtils.hasTimeConflict(
                                enrolledSchedule.getWeekDay(), enrolledSchedule.getStartPeriod(), enrolledSchedule.getEndPeriod(),
                                enrolledSchedule.getStartWeek(), enrolledSchedule.getEndWeek(),
                                targetSchedule.getWeekDay(), targetSchedule.getStartPeriod(), targetSchedule.getEndPeriod(),
                                targetSchedule.getStartWeek(), targetSchedule.getEndWeek());

                        if (conflict)
                        {
                            // 获取冲突课程名称
                            TpmCourseOffering conflictOffering = tpmCourseOfferingMapper.selectTpmCourseOfferingByOfferingId(enrolledOfferingId);
                            String courseName = "";
                            if (conflictOffering != null)
                            {
                                TpmCourseLibrary course = tpmCourseLibraryMapper.selectTpmCourseLibraryByCourseId(conflictOffering.getCourseId());
                                if (course != null)
                                {
                                    courseName = course.getCourseName();
                                }
                            }

                            String timeDesc = buildScheduleDesc(enrolledSchedule);
                            String targetTimeDesc = buildScheduleDesc(targetSchedule);
                            warnings.add(new ConflictWarning(
                                    "TIME_CONFLICT",
                                    "与已选课程【" + courseName + "】时间冲突：" + targetTimeDesc + " 与 " + timeDesc + " 冲突",
                                    enrolledOfferingId,
                                    courseName,
                                    timeDesc
                            ));
                            break; // 一门已选课程只报告一次冲突
                        }
                    }
                }
            }

            // 5. 检测学分超限
            double totalCredits = 0.0;
            for (Long offeringId : enrolledOfferingIds)
            {
                TpmCourseOffering offering = tpmCourseOfferingMapper.selectTpmCourseOfferingByOfferingId(offeringId);
                if (offering != null)
                {
                    TpmCourseLibrary course = tpmCourseLibraryMapper.selectTpmCourseLibraryByCourseId(offering.getCourseId());
                    if (course != null && course.getCredit() != null)
                    {
                        totalCredits += course.getCredit();
                    }
                }
            }

            // 获取待选课程学分
            TpmCourseOffering targetOffering = tpmCourseOfferingMapper.selectTpmCourseOfferingByOfferingId(courseOfferingId);
            double targetCredit = 0.0;
            if (targetOffering != null)
            {
                TpmCourseLibrary targetCourse = tpmCourseLibraryMapper.selectTpmCourseLibraryByCourseId(targetOffering.getCourseId());
                if (targetCourse != null && targetCourse.getCredit() != null)
                {
                    targetCredit = targetCourse.getCredit();
                }
            }

            // 获取学分上限（从配置读取）
            double maxCredits = defaultMaxCredits;

            if (totalCredits + targetCredit > maxCredits)
            {
                warnings.add(new ConflictWarning(
                        "CREDIT_OVERFLOW",
                        "学分超限：当前已选" + totalCredits + "学分，加上本课程" + targetCredit + "学分将超过上限" + maxCredits + "学分"
                ));
            }
        }

        // 6. 检测课程容量是否已满（T-3：读 enrolled_count 真相列。落选/候补 result_status='2' 不占名额，
        //    不能拿报名行数判满，否则抽签后大量候补会把还有名额的课判成已满）
        TpmCourseOffering offering = tpmCourseOfferingMapper.selectTpmCourseOfferingByOfferingId(courseOfferingId);
        if (offering != null && offering.getMaxStudents() != null && offering.getMaxStudents() > 0
                && offering.getEnrolledCount() != null
                && offering.getEnrolledCount() >= offering.getMaxStudents())
        {
            warnings.add(new ConflictWarning(
                    "COURSE_FULL",
                    "课程已满：当前已选" + offering.getEnrolledCount() + "人，容量上限" + offering.getMaxStudents() + "人"
            ));
        }

        // 7. 选课规则校验（专业/年级/院系/人数上限/先修课程）
        List<String> ruleViolations = tpmSelectionRuleService.validate(roundId, studentId, courseOfferingId);
        if (ruleViolations != null && !ruleViolations.isEmpty())
        {
            for (String violation : ruleViolations)
            {
                warnings.add(new ConflictWarning("RULE_VIOLATION", violation));
            }
        }

        return warnings;
    }

    /**
     * 获取替代课程建议
     * 查找与待选课程相同课程代码的其他教学班，过滤掉时间冲突和已满的
     *
     * @param studentId        学生ID
     * @param courseOfferingId 待选开课ID
     * @param roundId          轮次ID
     * @return 替代课程建议列表
     */
    @Override
    public List<CourseSuggestion> getAlternativeCourses(Long studentId, Long courseOfferingId, Long roundId)
    {
        List<CourseSuggestion> suggestions = new ArrayList<>();

        // 1. 获取待选课程信息
        TpmCourseOffering targetOffering = tpmCourseOfferingMapper.selectTpmCourseOfferingByOfferingId(courseOfferingId);
        if (targetOffering == null)
        {
            return suggestions;
        }

        // 2. 查询同课程的其他开课
        TpmCourseOffering query = new TpmCourseOffering();
        query.setCourseId(targetOffering.getCourseId());
        query.setSemesterId(targetOffering.getSemesterId());
        query.setOfferingStatus("1"); // 只查已确认的开课
        List<TpmCourseOffering> alternativeOfferings = tpmCourseOfferingMapper.selectTpmCourseOfferingList(query);

        if (alternativeOfferings == null || alternativeOfferings.isEmpty())
        {
            return suggestions;
        }

        // 3. 获取学生已选课程及其排课信息
        List<TpmSelectionEnrollment> enrolledList = tpmSelectionEnrollmentMapper.selectByStudentAndRound(studentId, roundId);
        List<TpmSchedule> enrolledSchedules = new ArrayList<>();
        if (enrolledList != null && !enrolledList.isEmpty())
        {
            List<Long> enrolledOfferingIds = enrolledList.stream()
                    .map(TpmSelectionEnrollment::getCourseOfferingId)
                    .collect(Collectors.toList());
            enrolledSchedules = tpmScheduleMapper.selectByOfferingIds(enrolledOfferingIds);
        }

        // 4. 获取课程信息（学分/名称）
        TpmCourseLibrary course = tpmCourseLibraryMapper.selectTpmCourseLibraryByCourseId(targetOffering.getCourseId());

        // 5. 遍历替代课程，过滤冲突和已满的
        for (TpmCourseOffering alt : alternativeOfferings)
        {
            // 排除自己
            if (alt.getOfferingId().equals(courseOfferingId))
            {
                continue;
            }

            // 检查容量（T-3：列表查询的 enrolled_count 子查询已按 result_status='1' 现算）
            Integer altEnrolledCount = alt.getEnrolledCount();
            if (alt.getMaxStudents() != null && alt.getMaxStudents() > 0
                    && altEnrolledCount != null && altEnrolledCount >= alt.getMaxStudents())
            {
                continue; // 已满，跳过
            }

            // 获取替代课程排课
            List<TpmSchedule> altSchedules = tpmScheduleMapper.selectTpmScheduleList(new TpmSchedule() {{
                setOfferingId(alt.getOfferingId());
            }});

            // 检查时间冲突
            boolean hasConflict = false;
            for (TpmSchedule altSchedule : altSchedules)
            {
                for (TpmSchedule enrolledSchedule : enrolledSchedules)
                {
                    if (TimeSlotUtils.hasTimeConflict(
                            enrolledSchedule.getWeekDay(), enrolledSchedule.getStartPeriod(), enrolledSchedule.getEndPeriod(),
                            enrolledSchedule.getStartWeek(), enrolledSchedule.getEndWeek(),
                            altSchedule.getWeekDay(), altSchedule.getStartPeriod(), altSchedule.getEndPeriod(),
                            altSchedule.getStartWeek(), altSchedule.getEndWeek()))
                    {
                        hasConflict = true;
                        break;
                    }
                }
                if (hasConflict)
                {
                    break;
                }
            }
            if (hasConflict)
            {
                continue;
            }

            // 构建建议对象
            CourseSuggestion suggestion = new CourseSuggestion();
            suggestion.setCourseOfferingId(alt.getOfferingId());
            suggestion.setCourseName(course != null ? course.getCourseName() : "");
            suggestion.setCredit(course != null ? course.getCredit() : null);
            suggestion.setMaxStudents(alt.getMaxStudents());
            suggestion.setEnrolledCount(altEnrolledCount);

            // 获取教师名称
            if (alt.getTeacherId() != null)
            {
                BrmTeacher teacher = brmTeacherMapper.selectBrmTeacherByTeacherId(alt.getTeacherId());
                if (teacher != null)
                {
                    suggestion.setTeacherName(teacher.getTeacherName());
                }
            }

            // 构建排课时间描述
            suggestion.setScheduleDesc(buildScheduleDescList(altSchedules));

            suggestions.add(suggestion);
        }

        return suggestions;
    }

    /**
     * 带验证的选课
     * 流程：冲突检测 -> 原子占用名额（DB 条件更新） -> 保存选课记录 -> 清除缓存
     *
     * @param studentId        学生ID
     * @param courseOfferingId 开课ID
     * @param roundId          轮次ID
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    @BizMetric("selection")
    public AjaxResult enrollWithValidation(Long studentId, Long courseOfferingId, Long roundId)
    {
        // 保持既有口径：无志愿优先级
        return enrollWithValidation(studentId, courseOfferingId, roundId, null);
    }

    /**
     * F2-2：带志愿优先级的选课。priority 为志愿序号（1=第一志愿，越小越优先），
     * 供轮次采用 weighted 抽签模式时按志愿权重中签；为空则等同普通选课。
     */
    // T-1：控制器实际调用的是本 4 参重载，事务注解必须落在真正被执行的方法上，
    // 否则容量扣减、选课落库、异常回补三步不在同一事务内（历史缺陷）。
    // T-2：显式 rollbackFor，避免受检异常导致部分提交。
    @Transactional(rollbackFor = Exception.class)
    @Override
    @BizMetric("selection")
    public AjaxResult enrollWithValidation(Long studentId, Long courseOfferingId, Long roundId, Integer priority)
    {
        // 0. 校验轮次状态
        TpmSelectionRound round = tpmSelectionRoundMapper.selectTpmSelectionRoundByRoundId(roundId);
        if (round == null)
        {
            return AjaxResult.error("选课轮次不存在");
        }
        String roundStatus = round.getRoundStatus();
        if (!"1".equals(roundStatus))
        {
            String statusDesc = "0".equals(roundStatus) ? "未开始" : "2".equals(roundStatus) ? "已结束" : "未知";
            return AjaxResult.error("选课轮次" + statusDesc + "，无法选课");
        }
        // 校验选课门数上限
        List<TpmSelectionEnrollment> enrolledList = tpmSelectionEnrollmentMapper.selectByStudentAndRound(studentId, roundId);
        if (round.getMaxCoursesPerStudent() != null && enrolledList != null
                && enrolledList.size() >= round.getMaxCoursesPerStudent())
        {
            return AjaxResult.error("已达到本轮次选课门数上限" + round.getMaxCoursesPerStudent() + "门");
        }

        // 1. 冲突检测
        List<ConflictWarning> conflicts = checkSelectionConflicts(studentId, courseOfferingId, roundId);
        if (!conflicts.isEmpty())
        {
            StringBuilder msg = new StringBuilder("选课失败，存在以下冲突：");
            for (ConflictWarning w : conflicts)
            {
                msg.append(w.getMessage()).append("；");
            }
            return AjaxResult.error(msg.toString());
        }

        // 2. 原子占用名额（T-3）：容量的唯一真相是 tpm_course_offering.enrolled_count。
        //    条件更新把「是否还有名额」与「占一个名额」压成一条语句，由 InnoDB 行锁串行化并发请求，
        //    不存在「先查后改」的窗口。原实现走 Redis DECR/INCR：进程崩在 DECR 与 INSERT 之间会
        //    永久少一个名额，键被清空或主从切换则会超卖，两边不一致时也判断不出谁对。
        if (tpmCourseOfferingMapper.occupySeat(courseOfferingId) == 0)
        {
            // 只在失败路径多查一次开课，用于区分「真的满了」与「开课不存在/容量未配置」
            TpmCourseOffering offering = tpmCourseOfferingMapper.selectTpmCourseOfferingByOfferingId(courseOfferingId);
            if (offering == null || offering.getMaxStudents() == null || offering.getMaxStudents() <= 0)
            {
                return AjaxResult.error("课程信息异常");
            }
            bizMetrics.recordOversell(); // P2 超卖尝试埋点
            return AjaxResult.error("课程已满，选课失败");
        }

        // 3. 保存选课记录。整个方法处在同一事务内：insert 抛错会连同上面占用的名额一起回滚，
        //    不需要 catch 里手工补偿（那条补偿在进程被杀时根本不会执行）。
        TpmSelectionEnrollment enrollment = new TpmSelectionEnrollment();
        enrollment.setStudentId(studentId);
        enrollment.setCourseOfferingId(courseOfferingId);
        enrollment.setRoundId(roundId);
        enrollment.setSelectTime(new Date());
        enrollment.setResultStatus("1"); // 选中
        enrollment.setLotteryResult("0"); // 未抽签
        if (priority != null && priority > 0)
        {
            enrollment.setPriority(priority); // F2-2 志愿优先级
        }
        enrollment.setCreateTime(DateUtils.getNowDate());
        tpmSelectionEnrollmentMapper.insertTpmSelectionEnrollment(enrollment);

        // 4. 清除学生选课缓存
        selectionCacheManager.clearStudentCache(studentId, roundId);

        return AjaxResult.success("选课成功");
    }

    /**
     * 构建单条排课时间描述
     *
     * @param schedule 排课信息
     * @return 时间描述字符串
     */
    private String buildScheduleDesc(TpmSchedule schedule)
    {
        if (schedule == null)
        {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        if (schedule.getWeekDay() != null && schedule.getWeekDay() >= 1 && schedule.getWeekDay() <= 7)
        {
            sb.append(WEEK_DAY_NAMES[schedule.getWeekDay()]);
        }
        if (schedule.getStartPeriod() != null && schedule.getEndPeriod() != null)
        {
            sb.append(" 第").append(schedule.getStartPeriod()).append("-").append(schedule.getEndPeriod()).append("节");
        }
        if (schedule.getStartWeek() != null && schedule.getEndWeek() != null)
        {
            sb.append(" [").append(schedule.getStartWeek()).append("-").append(schedule.getEndWeek()).append("周]");
        }
        return sb.toString();
    }

    /**
     * 构建多条排课时间描述
     *
     * @param schedules 排课列表
     * @return 时间描述字符串
     */
    private String buildScheduleDescList(List<TpmSchedule> schedules)
    {
        if (schedules == null || schedules.isEmpty())
        {
            return "暂无排课信息";
        }
        return schedules.stream()
                .map(this::buildScheduleDesc)
                .collect(Collectors.joining("; "));
    }

    /**
     * 执行抽签（超容量课程公平抽签）
     * 对轮次下所有超容量的开课，随机抽取学生至容量上限，未中签学生标记为落选
     *
     * @param roundId 轮次ID
     * @return 抽签结果统计
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, Object> runLottery(Long roundId)
    {
        return runLottery(roundId, null);
    }

    /**
     * T6：执行抽签（可指定随机种子以支持结果复现审计）。
     * 相比旧实现新增：① 记录并复用随机种子，逐开课用 seed+offeringId 派生确定性随机序列，结果可复现；
     * ② 落选学生写入候补排名 waitlist_rank（按抽签顺序），供后续递补；
     * ③ 抽签完成后按报名表重算各开课已占名额（容量真相在 DB 列，不再回写 Redis）。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    @BizMetric("lottery")
    public Map<String, Object> runLottery(Long roundId, Long seed)
    {
        Map<String, Object> result = new HashMap<>();
        TpmSelectionRound round = tpmSelectionRoundMapper.selectTpmSelectionRoundByRoundId(roundId);
        if (round == null)
        {
            result.put("message", "轮次不存在");
            return result;
        }
        // T-4 幂等闸门：抽签是轮次级全量重算，只允许在“进行中(1)”的轮次执行；
        // 抽签成功后轮次将被置为已结束(2)，重复点击/前端重试/任务重跑一律被状态机挡住，
        // 不会对已产生的中签结果重新洗牌覆盖（学生已按结果缴费排课的事故形态）。
        if (!"1".equals(round.getRoundStatus()))
        {
            result.put("message", "轮次当前状态不允许抽签（仅进行中可抽签，已结束请先重置轮次状态）");
            return result;
        }
        // 查询轮次下所有选课记录（状态为已选但未抽签的）
        TpmSelectionEnrollment query = new TpmSelectionEnrollment();
        query.setRoundId(roundId);
        query.setResultStatus("1"); // 选中状态
        // T-4 幂等：只纳入 lottery_result='0'（未抽签）的记录，
        // 即使状态闸门被绕过（如手工改回进行中），已中签/已落选的记录也不会被重新洗牌。
        query.setLotteryResult("0");
        List<TpmSelectionEnrollment> allEnrollments = tpmSelectionEnrollmentMapper.selectTpmSelectionEnrollmentList(query);

        if (allEnrollments == null || allEnrollments.isEmpty())
        {
            result.put("message", "无选课记录需要抽签");
            return result;
        }

        // 随机种子：为空则以当前毫秒生成，保证可复现（记录到轮次）
        long effectiveSeed = (seed != null) ? seed : System.currentTimeMillis();

        // 按开课ID分组
        Map<Long, List<TpmSelectionEnrollment>> offeringMap = allEnrollments.stream()
                .collect(Collectors.groupingBy(TpmSelectionEnrollment::getCourseOfferingId));

        int lotteryCount = 0;
        int successCount = 0;
        int failCount = 0;
        List<TpmSelectionEnrollment> toUpdate = new ArrayList<>();

        for (Map.Entry<Long, List<TpmSelectionEnrollment>> entry : offeringMap.entrySet())
        {
            Long offeringId = entry.getKey();
            List<TpmSelectionEnrollment> enrollments = entry.getValue();

            // 获取开课容量
            TpmCourseOffering offering = tpmCourseOfferingMapper.selectTpmCourseOfferingByOfferingId(offeringId);
            if (offering == null || offering.getMaxStudents() == null)
            {
                continue;
            }
            int maxCapacity = offering.getMaxStudents();

            // F2-2 弹性扩容：超容量且开启弹性时，按步长扩容至上限（不超过实际超出人数），缓解热门课容量紧张
            if (isElasticEnabled(offering) && enrollments.size() > maxCapacity)
            {
                int added = computeElasticAdd(offering, enrollments.size() - maxCapacity);
                if (added > 0)
                {
                    maxCapacity += added;
                    offering.setMaxStudents(maxCapacity);
                    tpmCourseOfferingMapper.updateTpmCourseOffering(offering);
                }
            }

            // 如果未超容量，全部中签
            if (enrollments.size() <= maxCapacity)
            {
                for (TpmSelectionEnrollment e : enrollments)
                {
                    e.setLotteryResult("1"); // 中签
                    e.setResultStatus("1");  // 选中
                    e.setWaitlistRank(null);
                    toUpdate.add(e);
                    successCount++;
                }
                continue;
            }

            // 超容量，执行确定性抽签：
            //  - weighted 模式（F2-2）：先按志愿优先级排序，同志愿内用 seed 派生随机序列，优先级高者优先中签
            //  - random/默认（T6）：整体用 seed+offeringId 派生随机序列公平洗牌，同种子同结果可复现、可审计
            if ("weighted".equals(round.getLotteryMode()))
            {
                orderForWeightedLottery(enrollments, effectiveSeed, offeringId);
            }
            else
            {
                Collections.shuffle(enrollments, new java.util.Random(effectiveSeed + offeringId));
            }
            lotteryCount++;

            for (int i = 0; i < enrollments.size(); i++)
            {
                TpmSelectionEnrollment e = enrollments.get(i);
                if (i < maxCapacity)
                {
                    e.setLotteryResult("1"); // 中签
                    e.setResultStatus("1");  // 选中
                    e.setWaitlistRank(null);
                    successCount++;
                }
                else
                {
                    e.setLotteryResult("2"); // 落选
                    e.setResultStatus("2");  // 未选中
                    e.setWaitlistRank(i - maxCapacity + 1); // 候补排名：1 为最优递补
                    failCount++;
                }
                toUpdate.add(e);
            }
        }

        // P-3：批量落库。原实现逐行 updateTpmSelectionEnrollment，单轮抽签要发 N 次 DB 往返
        // （备份库实测该轮 13957 条选课记录），每次一条 SQL 各自往返与加锁，耗时线性放大。
        // 改为按块切片，每块一条 CASE WHEN 同时更新 lottery_result / result_status / waitlist_rank。
        // toUpdate 为空时不调用 Mapper：空列表会拼出 `case enroll_id end` 这种非法 SQL。
        Date lotteryUpdateTime = new Date();
        for (int from = 0; from < toUpdate.size(); from += LOTTERY_UPDATE_CHUNK)
        {
            int to = Math.min(from + LOTTERY_UPDATE_CHUNK, toUpdate.size());
            tpmSelectionEnrollmentMapper.batchUpdateLotteryResult(toUpdate.subList(from, to), lotteryUpdateTime);
        }

        // T-3：抽签把部分「已选中」改成了「落选/候补」，占用数必须按报名表全量重算（幂等）。
        // 不再回写 Redis：缓存不是真相，且中签计数与报名行数在并发下可能不同源。
        if (!offeringMap.isEmpty())
        {
            tpmCourseOfferingMapper.recountEnrolled(new ArrayList<>(offeringMap.keySet()));
        }

        // 记录种子与抽签时间，并更新轮次状态为已结束
        round.setLotterySeed(effectiveSeed);
        round.setLotteryTime(new Date());
        round.setRoundStatus("2");
        round.setUpdateTime(new Date());
        tpmSelectionRoundMapper.updateTpmSelectionRound(round);

        result.put("lotteryCourses", lotteryCount);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("seed", effectiveSeed);
        result.put("message", String.format("抽签完成：涉及%d门课程，中签%d人，落选%d人（候补已按序入列，随机种子=%d，可用于结果复现审计）",
                lotteryCount, successCount, failCount, effectiveSeed));
        return result;
    }

    /**
     * F2-2：判断开课是否开启弹性扩容（elastic_enabled='1'）。
     */
    private boolean isElasticEnabled(TpmCourseOffering offering)
    {
        return offering != null && "1".equals(offering.getElasticEnabled());
    }

    /**
     * F2-2：计算弹性扩容增量。按步长逐步扩容，不超过扩容上限（elastic_max），且不超过实际超出人数（excess）。
     * 缺省：步长 5，上限为原容量上浮 50%（未显式配置 elastic_max 时）。
     *
     * @param offering 开课
     * @param excess   超出原容量的人数
     * @return 可扩容的人数增量（>=0）
     */
    private int computeElasticAdd(TpmCourseOffering offering, int excess)
    {
        if (excess <= 0) { return 0; }
        int base = offering.getMaxStudents() != null ? offering.getMaxStudents() : 0;
        int step = offering.getElasticStep() != null && offering.getElasticStep() > 0 ? offering.getElasticStep() : 5;
        int ceiling = offering.getElasticMax() != null && offering.getElasticMax() > base
                ? offering.getElasticMax()
                : base + base / 2; // 未配置上限时默认允许上浮 50%
        int room = Math.max(0, ceiling - base);
        int byStep = (excess + step - 1) / step * step; // 按步长向上取整
        return Math.max(0, Math.min(Math.min(byStep, room), excess));
    }

    /**
     * F2-2：志愿优先级权重抽签排序。先按 priority 升序（数值小=志愿优先级高，null 视为最低），
     * 同一优先级子区间内用 seed 派生的确定性随机序列洗牌，保证同志愿公平且结果可复现。
     * 直接对传入列表原地重排。
     */
    private void orderForWeightedLottery(List<TpmSelectionEnrollment> enrollments, long seed, Long offeringId)
    {
        final long baseSeed = seed + (offeringId == null ? 0L : offeringId);
        enrollments.sort((a, b) -> Integer.compare(priorityKey(a), priorityKey(b)));
        int i = 0;
        java.util.Random rnd = new java.util.Random(baseSeed);
        while (i < enrollments.size())
        {
            int j = i;
            int pi = priorityKey(enrollments.get(i));
            while (j < enrollments.size() && priorityKey(enrollments.get(j)) == pi) { j++; }
            if (j - i > 1)
            {
                Collections.shuffle(enrollments.subList(i, j), new java.util.Random(rnd.nextLong()));
            }
            i = j;
        }
    }

    /** 志愿优先级键：null 视为最低优先级（排最后） */
    private int priorityKey(TpmSelectionEnrollment e)
    {
        return e.getPriority() == null ? Integer.MAX_VALUE : e.getPriority();
    }

    /**
     * T6：候补递补。当中签人数低于容量时，按候补排名顺序将落选学生递补为中签。
     * T-3：递补前先做一次 DB 条件更新占名额（occupySeat），占到才改记录；占不到说明名额被并发抢走，
     * 立即停止递补（剩余候补等下一次触发），不会出现“记录已递补、名额没占到”的越界。
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public Map<String, Object> promoteWaitlist(Long offeringId)
    {
        Map<String, Object> result = new HashMap<>();
        TpmCourseOffering offering = tpmCourseOfferingMapper.selectTpmCourseOfferingByOfferingId(offeringId);
        if (offering == null || offering.getMaxStudents() == null)
        {
            result.put("promoted", 0);
            result.put("message", "开课不存在或容量未配置");
            return result;
        }
        int maxCapacity = offering.getMaxStudents();
        int admitted = tpmSelectionEnrollmentMapper.countAdmittedByOffering(offeringId);
        int free = maxCapacity - admitted;
        if (free <= 0)
        {
            result.put("promoted", 0);
            result.put("admitted", admitted);
            result.put("capacity", maxCapacity);
            result.put("message", "无空余容量，无需递补");
            return result;
        }
        List<TpmSelectionEnrollment> waitlist = tpmSelectionEnrollmentMapper.selectWaitlistByOffering(offeringId);
        if (waitlist == null || waitlist.isEmpty())
        {
            result.put("promoted", 0);
            result.put("admitted", admitted);
            result.put("capacity", maxCapacity);
            result.put("message", "无候补学生");
            return result;
        }
        int promoted = 0;
        Date now = new Date();
        for (TpmSelectionEnrollment e : waitlist)
        {
            if (promoted >= free)
            {
                break;
            }
            // 先占名额再改记录：占不到（返回 0）说明并发请求已把这批空位吃掉，本轮到此为止
            if (tpmCourseOfferingMapper.occupySeat(offeringId) == 0)
            {
                break;
            }
            tpmSelectionEnrollmentMapper.promoteFromWaitlist(e.getEnrollId(), now);
            selectionCacheManager.clearStudentCache(e.getStudentId(), e.getRoundId());
            promoted++;
        }
        result.put("promoted", promoted);
        result.put("admitted", admitted + promoted);
        result.put("capacity", maxCapacity);
        result.put("message", String.format("候补递补完成：本次递补%d人，累计中签%d人（容量%d）", promoted, admitted + promoted, maxCapacity));
        return result;
    }

    /**
     * 学生退课
     * 校验记录存在、当前为选中状态、轮次进行中；置为退课状态并释放 DB 名额（releaseSeat）
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AjaxResult dropCourse(Long enrollId)
    {
        TpmSelectionEnrollment enrollment = tpmSelectionEnrollmentMapper.selectTpmSelectionEnrollmentByEnrollId(enrollId);
        if (enrollment == null)
        {
            return AjaxResult.error("选课记录不存在");
        }
        if (!"1".equals(enrollment.getResultStatus()))
        {
            return AjaxResult.error("当前选课记录状态不允许退课");
        }
        TpmSelectionRound round = tpmSelectionRoundMapper.selectTpmSelectionRoundByRoundId(enrollment.getRoundId());
        if (round == null)
        {
            return AjaxResult.error("选课轮次不存在");
        }
        // 轮次非进行中：仅抽签中签（lotteryResult=1）的学生可退课（用于触发候补递补闭环），其余不允许
        boolean admittedByLottery = "1".equals(enrollment.getLotteryResult());
        if (!"1".equals(round.getRoundStatus()) && !admittedByLottery)
        {
            return AjaxResult.error("选课轮次非进行中，不允许退课");
        }

        // 更新为退课状态
        enrollment.setResultStatus("3");
        enrollment.setDropTime(new Date());
        enrollment.setUpdateTime(DateUtils.getNowDate());
        tpmSelectionEnrollmentMapper.updateTpmSelectionEnrollment(enrollment);

        // T-3：释放名额走 DB 条件更新（带 enrolled_count > 0 守卫，计数不会被减成负数），
        // 与选课占名额同处一张表的行锁域内，不再有 Redis 与 DB 两套计数需要对齐。
        tpmCourseOfferingMapper.releaseSeat(enrollment.getCourseOfferingId());
        // 清除学生选课缓存
        selectionCacheManager.clearStudentCache(enrollment.getStudentId(), enrollment.getRoundId());

        // T6：抽签后中签学生退课释放的容量，自动触发候补递补
        if (admittedByLottery)
        {
            promoteWaitlist(enrollment.getCourseOfferingId());
        }

        return AjaxResult.success("退课成功");
    }

    /**
     * F2-2 退改选窗口：将学生已选课程改选为另一开课（保持选课门数不变）。
     * 仅当轮次开放退改选（allow_drop_adjust='1'）且当前时间处于 [dropAdjustStart, dropAdjustEnd] 窗口内时允许；
     * T-3：顺序为「冲突检测 → 占新名额 → 改记录 → 释放旧名额」。先占后放保证任何一步失败都由事务
     * 整体回滚，不会出现“旧名额已放、新名额没抢到”导致学生无课可上的中间态（原实现要先释放再抢，
     * 失败时靠 catch 反向补偿，补偿本身也可能抢不到）。
     *
     * @param enrollId      原选课记录ID
     * @param newOfferingId 目标开课ID
     * @return 操作结果
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public AjaxResult swapCourse(Long enrollId, Long newOfferingId)
    {
        TpmSelectionEnrollment enrollment = tpmSelectionEnrollmentMapper.selectTpmSelectionEnrollmentByEnrollId(enrollId);
        if (enrollment == null)
        {
            return AjaxResult.error("选课记录不存在");
        }
        if (!"1".equals(enrollment.getResultStatus()))
        {
            return AjaxResult.error("当前选课记录状态不允许改选");
        }
        if (newOfferingId == null || newOfferingId.equals(enrollment.getCourseOfferingId()))
        {
            return AjaxResult.error("请选择与原课程不同的目标课程");
        }
        TpmSelectionRound round = tpmSelectionRoundMapper.selectTpmSelectionRoundByRoundId(enrollment.getRoundId());
        if (round == null)
        {
            return AjaxResult.error("选课轮次不存在");
        }
        // 退改选窗口校验
        AjaxResult windowCheck = checkDropAdjustWindow(round);
        if (windowCheck != null)
        {
            return windowCheck;
        }

        Long oldOfferingId = enrollment.getCourseOfferingId();
        Long studentId = enrollment.getStudentId();
        Long roundId = enrollment.getRoundId();

        // 1. 新课程冲突检测（原记录尚未改动，冲突检测的口径与学生当前已选状态一致）
        List<ConflictWarning> conflicts = checkSelectionConflicts(studentId, newOfferingId, roundId);
        if (!conflicts.isEmpty())
        {
            StringBuilder msg = new StringBuilder("改选失败，新课程存在以下冲突：");
            for (ConflictWarning w : conflicts) { msg.append(w.getMessage()).append("；"); }
            return AjaxResult.error(msg.toString());
        }

        // 2. 先占新名额（DB 条件更新）。占不到时多查一次开课，区分「已满」与「开课不存在/容量未配置」
        if (tpmCourseOfferingMapper.occupySeat(newOfferingId) == 0)
        {
            TpmCourseOffering target = tpmCourseOfferingMapper.selectTpmCourseOfferingByOfferingId(newOfferingId);
            if (target == null || target.getMaxStudents() == null || target.getMaxStudents() <= 0)
            {
                return AjaxResult.error("目标课程信息异常");
            }
            bizMetrics.recordOversell(); // P2 超卖尝试埋点（改选抢已满课程）
            return AjaxResult.error("目标课程已满，改选失败");
        }

        // 3. 原地改指向目标开课，重置为未抽签选中
        enrollment.setCourseOfferingId(newOfferingId);
        enrollment.setResultStatus("1");
        enrollment.setLotteryResult("0");
        enrollment.setWaitlistRank(null);
        enrollment.setDropTime(null);
        enrollment.setSelectTime(new Date());
        enrollment.setUpdateTime(DateUtils.getNowDate());
        tpmSelectionEnrollmentMapper.updateTpmSelectionEnrollment(enrollment);

        // 4. 记录已指向新课后释放旧名额；若本步或上一步抛错，整个事务回滚，新名额自动归还
        tpmCourseOfferingMapper.releaseSeat(oldOfferingId);

        selectionCacheManager.clearStudentCache(studentId, roundId);
        return AjaxResult.success("改选成功");
    }

    /**
     * 退改选窗口校验：返回 null 表示允许；否则返回错误结果。
     * 需轮次开放退改选（allow_drop_adjust='1'），且当前时间落在窗口区间（未配置起止则不限制时间）。
     */
    private AjaxResult checkDropAdjustWindow(TpmSelectionRound round)
    {
        if (!"1".equals(round.getAllowDropAdjust()))
        {
            return AjaxResult.error("本轮次未开放退改选窗口");
        }
        Date now = new Date();
        if (round.getDropAdjustStart() != null && now.before(round.getDropAdjustStart()))
        {
            return AjaxResult.error("退改选窗口尚未开始");
        }
        if (round.getDropAdjustEnd() != null && now.after(round.getDropAdjustEnd()))
        {
            return AjaxResult.error("退改选窗口已结束");
        }
        return null;
    }
}
