package com.yu.aem.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
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
        // 5. 分配监考（每个考场1主1副，回避冲突）
        List<AemExamInvigilation> dispatchList = new ArrayList<>();
        Set<Long> usedTeacherIds = new HashSet<>(); // 已分配的教师（避免同一场考试重复分配）
        int assignedCount = 0;
        int failCount = 0;
        List<String> failReasons = new ArrayList<>();
        for (Long classroomId : classroomIds)
        {
            // 分配主监考
            Long mainTeacherId = findAvailableTeacher(teachers, usedTeacherIds);
            if (mainTeacherId == null)
            {
                failCount++;
                failReasons.add("教室[" + classroomId + "]无可用主监考教师");
                continue;
            }
            usedTeacherIds.add(mainTeacherId);
            AemExamInvigilation mainInvigilation = buildInvigilation(examId, classroomId, mainTeacherId, examPlan, "0");
            dispatchList.add(mainInvigilation);
            // 分配副监考
            Long assistTeacherId = findAvailableTeacher(teachers, usedTeacherIds);
            if (assistTeacherId != null)
            {
                usedTeacherIds.add(assistTeacherId);
                AemExamInvigilation assistInvigilation = buildInvigilation(examId, classroomId, assistTeacherId, examPlan, "1");
                dispatchList.add(assistInvigilation);
            }
            assignedCount++;
        }
        // 6. 批量插入
        for (AemExamInvigilation inv : dispatchList)
        {
            aemExamInvigilationMapper.insertAemExamInvigilation(inv);
        }
        result.put("totalClassrooms", classroomIds.size());
        result.put("assignedCount", assignedCount);
        result.put("failCount", failCount);
        result.put("failReasons", failReasons);
        result.put("message", String.format("监考派发完成：安排%d个考场，成功%d个，失败%d个", classroomIds.size(), assignedCount, failCount));
        log.info("考试[{}]监考派发完成：成功{}个考场", examId, assignedCount);
        return result;
    }

    /**
     * 从教师列表中查找未使用的教师
     */
    private Long findAvailableTeacher(List<BrmTeacher> teachers, Set<Long> usedTeacherIds)
    {
        for (BrmTeacher teacher : teachers)
        {
            if (!usedTeacherIds.contains(teacher.getTeacherId()))
            {
                return teacher.getTeacherId();
            }
        }
        return null;
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
