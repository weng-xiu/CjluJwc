package com.yu.aem.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.aem.mapper.AemExamInvigilationMapper;
import com.yu.aem.domain.AemExamInvigilation;
import com.yu.aem.domain.AemExamPlan;
import com.yu.aem.domain.AemExamSeat;
import com.yu.aem.mapper.AemExamPlanMapper;
import com.yu.aem.mapper.AemExamSeatMapper;
import com.yu.brm.domain.BrmTeacher;
import com.yu.brm.mapper.BrmTeacherMapper;
import com.yu.aem.service.IAemExamInvigilationService;

/**
 * 监考教师分配Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@Service
public class AemExamInvigilationServiceImpl implements IAemExamInvigilationService
{
    private static final Logger log = LoggerFactory.getLogger(AemExamInvigilationServiceImpl.class);

    @Autowired
    private AemExamInvigilationMapper aemExamInvigilationMapper;

    @Autowired
    private AemExamPlanMapper aemExamPlanMapper;

    @Autowired
    private AemExamSeatMapper aemExamSeatMapper;

    @Autowired
    private BrmTeacherMapper brmTeacherMapper;

    /** A3：单教师本学期监考工作量上限（历史+本轮），<=0 表示不限制。 */
    @Value("${aem.invigilation.maxPerTeacher:6}")
    private int maxPerTeacher;

    /** A3：是否硬性回避本课程任课教师（任课教师不派往本课程考场）。 */
    @Value("${aem.invigilation.courseTeacherAvoid:true}")
    private boolean courseTeacherAvoid;

    /** A3：是否软性回避任课教师所属院系（无其他可选时可放宽）。 */
    @Value("${aem.invigilation.deptAvoid:true}")
    private boolean deptAvoid;

    @Override
    public AemExamInvigilation selectAemExamInvigilationByInvigilationId(Long invigilationId)
    {
        return aemExamInvigilationMapper.selectAemExamInvigilationByInvigilationId(invigilationId);
    }

    @Override
    public List<AemExamInvigilation> selectAemExamInvigilationList(AemExamInvigilation aemExamInvigilation)
    {
        return aemExamInvigilationMapper.selectAemExamInvigilationList(aemExamInvigilation);
    }

    @Override
    @Transactional
    public int insertAemExamInvigilation(AemExamInvigilation aemExamInvigilation)
    {
        aemExamInvigilation.setCreateTime(DateUtils.getNowDate());
        return aemExamInvigilationMapper.insertAemExamInvigilation(aemExamInvigilation);
    }

    @Override
    @Transactional
    public int updateAemExamInvigilation(AemExamInvigilation aemExamInvigilation)
    {
        aemExamInvigilation.setUpdateTime(DateUtils.getNowDate());
        return aemExamInvigilationMapper.updateAemExamInvigilation(aemExamInvigilation);
    }

    @Override
    @Transactional
    public int deleteAemExamInvigilationByInvigilationId(Long invigilationId)
    {
        return aemExamInvigilationMapper.deleteAemExamInvigilationByInvigilationId(invigilationId);
    }

    @Override
    @Transactional
    public int deleteAemExamInvigilationByInvigilationIds(Long[] invigilationIds)
    {
        return aemExamInvigilationMapper.deleteAemExamInvigilationByInvigilationIds(invigilationIds);
    }

    /**
     * 自动派发监考教师
     * 算法：
     * 1. 获取考试的所有考场（从座位编排中提取教室）
     * 2. 获取可用教师列表
     * 3. 每个考场分配1名主监考+1名副监考
     * 4. 回避同一教师在同一时间的冲突
     */
    @Override
    @Transactional
    public Map<String, Object> autoDispatchInvigilators(Long examId)
    {
        Map<String, Object> result = new HashMap<>();
        // 1. 获取考试信息
        AemExamPlan examPlan = aemExamPlanMapper.selectAemExamPlanByExamId(examId);
        if (examPlan == null)
        {
            result.put("message", "考试不存在");
            return result;
        }
        // 2. 获取考场列表（从已编排的座位中提取教室）
        AemExamSeat seatQuery = new AemExamSeat();
        seatQuery.setExamId(examId);
        List<AemExamSeat> seats = aemExamSeatMapper.selectAemExamSeatList(seatQuery);
        if (seats == null || seats.isEmpty())
        {
            result.put("message", "请先完成座位编排");
            return result;
        }
        // 提取去重的教室ID
        Set<Long> classroomIds = new HashSet<>();
        for (AemExamSeat seat : seats)
        {
            if (seat.getClassroomId() != null)
            {
                classroomIds.add(seat.getClassroomId());
            }
        }
        if (classroomIds.isEmpty())
        {
            result.put("message", "无考场信息");
            return result;
        }
        // 3. 获取可用教师列表
        BrmTeacher teacherQuery = new BrmTeacher();
        teacherQuery.setStatus("0");
        List<BrmTeacher> teachers = brmTeacherMapper.selectBrmTeacherList(teacherQuery);
        if (teachers == null || teachers.isEmpty())
        {
            result.put("message", "无可用教师");
            return result;
        }
        // 4. 清除原有监考记录
        aemExamInvigilationMapper.deleteByExamId(examId);
        // 4.1 一次性查询该时段已存在监考冲突的教师ID集合（避免逐教师N+1查询）
        List<Long> busyIds = aemExamInvigilationMapper.selectBusyTeacherIds(
                examPlan.getExamDate(), examPlan.getStartTime(), examPlan.getEndTime(), examId);
        Set<Long> busyTeacherIds = busyIds == null ? new HashSet<>() : new HashSet<>(busyIds);
        // 4.2 A3：本课程任课教师（硬回避，不派往本课程考场）
        Set<Long> courseTeacherIds = new HashSet<>();
        if (courseTeacherAvoid && examPlan.getCourseId() != null)
        {
            List<Long> ct = aemExamInvigilationMapper.selectCourseTeacherIds(examPlan.getCourseId(), examPlan.getSemesterId());
            if (ct != null) courseTeacherIds.addAll(ct);
        }
        // 4.3 A3：任课教师所属院系（软回避）
        Set<Long> courseDeptIds = new HashSet<>();
        if (deptAvoid && examPlan.getCourseId() != null)
        {
            List<Long> cd = aemExamInvigilationMapper.selectCourseTeacherDeptIds(examPlan.getCourseId(), examPlan.getSemesterId());
            if (cd != null) courseDeptIds.addAll(cd);
        }
        // 4.4 A3：各教师本学期已监考次数（次数均衡 + 工作量上限基数）
        Map<Long, Integer> load = new HashMap<>();
        if (examPlan.getSemesterId() != null)
        {
            List<Map<String, Object>> counts = aemExamInvigilationMapper.selectInvigilationCountBySemester(examPlan.getSemesterId(), examId);
            if (counts != null)
            {
                for (Map<String, Object> row : counts)
                {
                    Object tid = row.get("teacherId");
                    Object cnt = row.get("cnt");
                    if (tid != null && cnt != null)
                    {
                        load.put(Long.valueOf(tid.toString()), Integer.valueOf(cnt.toString()));
                    }
                }
            }
        }
        // 5. 分配监考（每个考场1主1副）
        //    硬约束：同场不重复 + 跨考试时间不冲突 + 回避本课程任课教师；
        //    软偏好：回避任课教师院系、工作量上限、优先选取本学期累计监考次数最少者（次数均衡）。
        List<AemExamInvigilation> dispatchList = new ArrayList<>();
        Set<Long> usedTeacherIds = new HashSet<>(); // 已分配的教师（避免同一场考试重复分配）
        int assignedCount = 0;
        int failCount = 0;
        List<String> failReasons = new ArrayList<>();
        for (Long classroomId : classroomIds)
        {
            // 分配主监考
            Long mainTeacherId = findAvailableTeacher(teachers, usedTeacherIds, busyTeacherIds,
                    courseTeacherIds, courseDeptIds, load);
            if (mainTeacherId == null)
            {
                failCount++;
                failReasons.add("教室[" + classroomId + "]无可用主监考教师（均时间冲突/回避）");
                continue;
            }
            usedTeacherIds.add(mainTeacherId);
            bumpLoad(load, mainTeacherId);
            dispatchList.add(buildInvigilation(examId, classroomId, mainTeacherId, examPlan, "0"));
            // 分配副监考
            Long assistTeacherId = findAvailableTeacher(teachers, usedTeacherIds, busyTeacherIds,
                    courseTeacherIds, courseDeptIds, load);
            if (assistTeacherId != null)
            {
                usedTeacherIds.add(assistTeacherId);
                bumpLoad(load, assistTeacherId);
                dispatchList.add(buildInvigilation(examId, classroomId, assistTeacherId, examPlan, "1"));
            }
            assignedCount++;
        }
        // 6. 批量插入（替代循环单条插入，显著减少数据库往返）
        if (!dispatchList.isEmpty())
        {
            aemExamInvigilationMapper.batchInsert(dispatchList);
        }
        result.put("totalClassrooms", classroomIds.size());
        result.put("assignedCount", assignedCount);
        result.put("failCount", failCount);
        result.put("failReasons", failReasons);
        result.put("courseTeacherAvoided", courseTeacherIds.size());
        result.put("maxPerTeacher", maxPerTeacher);
        result.put("message", String.format("监考派发完成：安排%d个考场，成功%d个，失败%d个", classroomIds.size(), assignedCount, failCount));
        log.info("考试[{}]监考派发完成：成功{}个考场，回避任课教师{}人", examId, assignedCount, courseTeacherIds.size());
        return result;
    }

    /** 本轮派发计数自增（用于次数均衡与工作量上限的实时评估）。 */
    private void bumpLoad(Map<Long, Integer> load, Long teacherId)
    {
        load.merge(teacherId, 1, Integer::sum);
    }

    /**
     * 从教师列表中查找可用教师（A3 增强）。
     * 硬排除：本轮已用、时段冲突(busy)、本课程任课教师。
     * 软偏好：回避任课教师院系、未超工作量上限；在可选集合中优先选取本学期累计监考次数最少者，实现次数均衡。
     * 若软偏好集合为空则放宽软约束（仍严格遵守硬排除），保证可派发。
     */
    private Long findAvailableTeacher(List<BrmTeacher> teachers, Set<Long> usedTeacherIds,
                                     Set<Long> busyTeacherIds, Set<Long> courseTeacherIds,
                                     Set<Long> courseDeptIds, Map<Long, Integer> load)
    {
        BrmTeacher preferred = null;   // 满足软偏好者
        BrmTeacher fallback = null;    // 仅满足硬约束者
        for (BrmTeacher teacher : teachers)
        {
            Long tid = teacher.getTeacherId();
            if (tid == null || usedTeacherIds.contains(tid) || busyTeacherIds.contains(tid)
                    || courseTeacherIds.contains(tid))
            {
                continue; // 硬排除
            }
            int cnt = load.getOrDefault(tid, 0);
            boolean withinCap = maxPerTeacher <= 0 || cnt < maxPerTeacher;
            boolean deptOk = !deptAvoid || teacher.getDeptId() == null
                    || !courseDeptIds.contains(teacher.getDeptId());
            // 次数均衡：累计监考次数最少者优先
            if (preferred == null || cnt < load.getOrDefault(preferred.getTeacherId(), 0))
            {
                if (withinCap && deptOk)
                {
                    preferred = teacher;
                }
            }
            if (fallback == null || cnt < load.getOrDefault(fallback.getTeacherId(), 0))
            {
                fallback = teacher;
            }
        }
        if (preferred != null) return preferred.getTeacherId();
        if (fallback != null) return fallback.getTeacherId();
        return null;
    }

    @Override
    @Transactional
    public int importInvigilation(List<AemExamInvigilation> list, String operator)
    {
        if (list == null || list.isEmpty())
        {
            throw new ServiceException("导入数据不能为空");
        }
        for (AemExamInvigilation inv : list)
        {
            if (inv.getExamId() == null)
            {
                throw new ServiceException("考试ID不能为空");
            }
            if (inv.getClassroomId() == null)
            {
                throw new ServiceException("教室ID不能为空");
            }
            if (inv.getTeacherId() == null)
            {
                throw new ServiceException("监考教师不能为空");
            }
            if (inv.getExamDate() == null)
            {
                throw new ServiceException("考试日期不能为空");
            }
            if (StringUtils.isEmpty(inv.getStartTime()) || StringUtils.isEmpty(inv.getEndTime()))
            {
                throw new ServiceException("开始时间和结束时间不能为空");
            }
            if (StringUtils.isEmpty(inv.getDutyType()))
            {
                inv.setDutyType("0");
            }
            if (StringUtils.isEmpty(inv.getStatus()))
            {
                inv.setStatus("0");
            }
            inv.setCreateBy(operator);
            inv.setCreateTime(DateUtils.getNowDate());
        }
        return aemExamInvigilationMapper.batchInsert(list);
    }

    /**
     * 构建监考记录
     */
    private AemExamInvigilation buildInvigilation(Long examId, Long classroomId, Long teacherId, AemExamPlan examPlan, String dutyType)
    {
        AemExamInvigilation inv = new AemExamInvigilation();
        inv.setExamId(examId);
        inv.setClassroomId(classroomId);
        inv.setTeacherId(teacherId);
        inv.setExamDate(examPlan.getExamDate());
        inv.setStartTime(examPlan.getStartTime());
        inv.setEndTime(examPlan.getEndTime());
        inv.setDutyType(dutyType);
        inv.setStatus("0");
        inv.setCreateTime(DateUtils.getNowDate());
        return inv;
    }
}
