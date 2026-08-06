package com.yu.tpm.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.yu.common.core.domain.AjaxResult;
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

    @Transactional
    @Override
    public int insertTpmSelectionEnrollment(TpmSelectionEnrollment tpmSelectionEnrollment)
    {
        tpmSelectionEnrollment.setCreateTime(DateUtils.getNowDate());
        return tpmSelectionEnrollmentMapper.insertTpmSelectionEnrollment(tpmSelectionEnrollment);
    }

    @Transactional
    @Override
    public int updateTpmSelectionEnrollment(TpmSelectionEnrollment tpmSelectionEnrollment)
    {
        tpmSelectionEnrollment.setUpdateTime(DateUtils.getNowDate());
        return tpmSelectionEnrollmentMapper.updateTpmSelectionEnrollment(tpmSelectionEnrollment);
    }

    @Transactional
    @Override
    public int deleteTpmSelectionEnrollmentByEnrollId(Long enrollId)
    {
        return tpmSelectionEnrollmentMapper.deleteTpmSelectionEnrollmentByEnrollId(enrollId);
    }

    @Transactional
    @Override
    public int deleteTpmSelectionEnrollmentByEnrollIds(Long[] enrollIds)
    {
        return tpmSelectionEnrollmentMapper.deleteTpmSelectionEnrollmentByEnrollIds(enrollIds);
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
        List<TpmSchedule> targetSchedules = tpmScheduleMapper.selectTpmScheduleList(new TpmSchedule() {{
            setOfferingId(courseOfferingId);
        }});

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

        // 6. 检测课程容量是否已满
        int enrolledCount = tpmSelectionEnrollmentMapper.selectCountByOffering(courseOfferingId);
        TpmCourseOffering offering = tpmCourseOfferingMapper.selectTpmCourseOfferingByOfferingId(courseOfferingId);
        if (offering != null && offering.getMaxStudents() != null && enrolledCount >= offering.getMaxStudents())
        {
            warnings.add(new ConflictWarning(
                    "COURSE_FULL",
                    "课程已满：当前已选" + enrolledCount + "人，容量上限" + offering.getMaxStudents() + "人"
            ));
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

            // 检查容量
            int altEnrolledCount = tpmSelectionEnrollmentMapper.selectCountByOffering(alt.getOfferingId());
            if (alt.getMaxStudents() != null && altEnrolledCount >= alt.getMaxStudents())
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
     * 流程：冲突检测 -> Redis原子扣减容量 -> 保存选课记录 -> 清除缓存
     *
     * @param studentId        学生ID
     * @param courseOfferingId 开课ID
     * @param roundId          轮次ID
     * @return 操作结果
     */
    @Transactional
    @Override
    public AjaxResult enrollWithValidation(Long studentId, Long courseOfferingId, Long roundId)
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

        // 2. Redis原子扣减容量
        // 先确保容量缓存存在
        Long capacity = selectionCacheManager.getCapacity(courseOfferingId);
        if (capacity == null)
        {
            // 缓存不存在，从数据库初始化
            TpmCourseOffering offering = tpmCourseOfferingMapper.selectTpmCourseOfferingByOfferingId(courseOfferingId);
            if (offering != null && offering.getMaxStudents() != null)
            {
                int currentCount = tpmSelectionEnrollmentMapper.selectCountByOffering(courseOfferingId);
                int remaining = offering.getMaxStudents() - currentCount;
                selectionCacheManager.setCapacity(courseOfferingId, remaining);
            }
            else
            {
                return AjaxResult.error("课程信息异常");
            }
        }

        Long remaining = selectionCacheManager.decrementCapacity(courseOfferingId);
        if (remaining < 0)
        {
            // 容量不足，恢复扣减
            selectionCacheManager.incrementCapacity(courseOfferingId);
            return AjaxResult.error("课程已满，选课失败");
        }

        try
        {
            // 3. 保存选课记录
            TpmSelectionEnrollment enrollment = new TpmSelectionEnrollment();
            enrollment.setStudentId(studentId);
            enrollment.setCourseOfferingId(courseOfferingId);
            enrollment.setRoundId(roundId);
            enrollment.setSelectTime(new Date());
            enrollment.setResultStatus("1"); // 选中
            enrollment.setLotteryResult("0"); // 未抽签
            enrollment.setCreateTime(DateUtils.getNowDate());
            tpmSelectionEnrollmentMapper.insertTpmSelectionEnrollment(enrollment);

            // 4. 清除学生选课缓存
            selectionCacheManager.clearStudentCache(studentId, roundId);

            return AjaxResult.success("选课成功");
        }
        catch (Exception e)
        {
            // 选课失败，恢复容量
            selectionCacheManager.incrementCapacity(courseOfferingId);
            throw e;
        }
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
    @Transactional
    @Override
    public Map<String, Object> runLottery(Long roundId)
    {
        Map<String, Object> result = new HashMap<>();
        TpmSelectionRound round = tpmSelectionRoundMapper.selectTpmSelectionRoundByRoundId(roundId);
        if (round == null)
        {
            result.put("message", "轮次不存在");
            return result;
        }
        // 查询轮次下所有选课记录（状态为已选但未抽签的）
        TpmSelectionEnrollment query = new TpmSelectionEnrollment();
        query.setRoundId(roundId);
        query.setResultStatus("1"); // 选中状态
        List<TpmSelectionEnrollment> allEnrollments = tpmSelectionEnrollmentMapper.selectTpmSelectionEnrollmentList(query);

        if (allEnrollments == null || allEnrollments.isEmpty())
        {
            result.put("message", "无选课记录需要抽签");
            return result;
        }

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

            // 如果未超容量，全部中签
            if (enrollments.size() <= maxCapacity)
            {
                for (TpmSelectionEnrollment e : enrollments)
                {
                    e.setLotteryResult("1"); // 中签
                    e.setResultStatus("1");  // 选中
                    toUpdate.add(e);
                    successCount++;
                }
                continue;
            }

            // 超容量，执行随机抽签
            Collections.shuffle(enrollments);
            lotteryCount++;

            for (int i = 0; i < enrollments.size(); i++)
            {
                TpmSelectionEnrollment e = enrollments.get(i);
                if (i < maxCapacity)
                {
                    e.setLotteryResult("1"); // 中签
                    e.setResultStatus("1");  // 选中
                    successCount++;
                }
                else
                {
                    e.setLotteryResult("2"); // 落选
                    e.setResultStatus("2");  // 未选中
                    failCount++;
                }
                toUpdate.add(e);
            }
        }

        // 批量更新
        for (TpmSelectionEnrollment e : toUpdate)
        {
            e.setUpdateTime(new Date());
            tpmSelectionEnrollmentMapper.updateTpmSelectionEnrollment(e);
        }

        // 更新轮次状态为已结束
        round.setRoundStatus("2");
        round.setUpdateTime(new Date());
        tpmSelectionRoundMapper.updateTpmSelectionRound(round);

        result.put("lotteryCourses", lotteryCount);
        result.put("successCount", successCount);
        result.put("failCount", failCount);
        result.put("message", String.format("抽签完成：涉及%d门课程，中签%d人，落选%d人", lotteryCount, successCount, failCount));
        return result;
    }
}
