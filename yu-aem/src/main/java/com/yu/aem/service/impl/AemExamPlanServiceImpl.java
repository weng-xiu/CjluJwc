package com.yu.aem.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.yu.common.annotation.DataScope;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.aem.mapper.AemExamPlanMapper;
import com.yu.aem.mapper.AemExamSeatMapper;
import com.yu.aem.mapper.AemExamInvigilationMapper;
import com.yu.aem.domain.AemExamPlan;
import com.yu.aem.domain.AemExamSeat;
import com.yu.aem.domain.AemExamInvigilation;
import com.yu.brm.domain.BrmClassroom;
import com.yu.brm.mapper.BrmClassroomMapper;
import com.yu.aem.service.IAemExamPlanService;

/**
 * 考试安排Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-11
 */
@Service
public class AemExamPlanServiceImpl implements IAemExamPlanService
{
    @Autowired
    private AemExamPlanMapper aemExamPlanMapper;

    @Autowired
    private AemExamSeatMapper aemExamSeatMapper;

    @Autowired
    private AemExamInvigilationMapper aemExamInvigilationMapper;

    @Autowired
    private BrmClassroomMapper brmClassroomMapper;

    @Override
    public AemExamPlan selectAemExamPlanByExamId(Long examId)
    {
        return aemExamPlanMapper.selectAemExamPlanByExamId(examId);
    }

    @Override
    public AemExamPlan selectAemExamPlanDetail(Long examId)
    {
        AemExamPlan plan = aemExamPlanMapper.selectAemExamPlanByExamId(examId);
        if (plan == null)
        {
            return null;
        }
        AemExamSeat seatQuery = new AemExamSeat();
        seatQuery.setExamId(examId);
        plan.setSeats(aemExamSeatMapper.selectAemExamSeatList(seatQuery));

        AemExamInvigilation invQuery = new AemExamInvigilation();
        invQuery.setExamId(examId);
        plan.setInvigilations(aemExamInvigilationMapper.selectAemExamInvigilationList(invQuery));
        return plan;
    }

    @Override
    @DataScope(deptAlias = "d")
    public List<AemExamPlan> selectAemExamPlanList(AemExamPlan aemExamPlan)
    {
        return aemExamPlanMapper.selectAemExamPlanList(aemExamPlan);
    }

    @Override
    public List<AemExamPlan> selectAemExamPlanListForPortal(AemExamPlan aemExamPlan)
    {
        // 门户端无部门视角，显式置空 dataScope，避免“仅本人”数据范围把考试列表过滤为空
        aemExamPlan.getParams().put("dataScope", "");
        return aemExamPlanMapper.selectAemExamPlanList(aemExamPlan);
    }

    @Override
    @Transactional
    public int insertAemExamPlan(AemExamPlan aemExamPlan)
    {
        aemExamPlan.setCreateTime(DateUtils.getNowDate());
        return aemExamPlanMapper.insertAemExamPlan(aemExamPlan);
    }

    @Override
    @Transactional
    public int updateAemExamPlan(AemExamPlan aemExamPlan)
    {
        // 级联校验：发布状态的考试安排必须至少完成座位或监考编排
        if ("2".equals(aemExamPlan.getPlanStatus()))
        {
            AemExamSeat seatQuery = new AemExamSeat();
            seatQuery.setExamId(aemExamPlan.getExamId());
            List<AemExamSeat> seats = aemExamSeatMapper.selectAemExamSeatList(seatQuery);
            AemExamInvigilation invQuery = new AemExamInvigilation();
            invQuery.setExamId(aemExamPlan.getExamId());
            List<AemExamInvigilation> invs = aemExamInvigilationMapper.selectAemExamInvigilationList(invQuery);
            if ((seats == null || seats.isEmpty()) && (invs == null || invs.isEmpty()))
            {
                throw new ServiceException("发布前请先完成座位编排或监考安排");
            }
        }
        aemExamPlan.setUpdateTime(DateUtils.getNowDate());
        return aemExamPlanMapper.updateAemExamPlan(aemExamPlan);
    }

    @Override
    @Transactional
    public int deleteAemExamPlanByExamId(Long examId)
    {
        // 级联删除子表，保证主子表数据一致性
        aemExamSeatMapper.deleteByExamId(examId);
        aemExamInvigilationMapper.deleteByExamId(examId);
        return aemExamPlanMapper.deleteAemExamPlanByExamId(examId);
    }

    @Override
    @Transactional
    public int deleteAemExamPlanByExamIds(Long[] examIds)
    {
        for (Long examId : examIds)
        {
            aemExamSeatMapper.deleteByExamId(examId);
            aemExamInvigilationMapper.deleteByExamId(examId);
        }
        return aemExamPlanMapper.deleteAemExamPlanByExamIds(examIds);
    }

    /**
     * A1：考试自动编排。
     * 流程：取考生名单 → 查询同时段未占用教室（按容量降序贪心） → 超容量自动拆分到多教室
     *      → 各教室蛇形排座 → 置为已安排。
     */
    @Override
    @Transactional
    public Map<String, Object> autoArrangeExam(Long examId)
    {
        Map<String, Object> result = new HashMap<>();
        AemExamPlan plan = aemExamPlanMapper.selectAemExamPlanByExamId(examId);
        if (plan == null)
        {
            result.put("message", "考试不存在");
            return result;
        }
        if (plan.getExamDate() == null || plan.getStartTime() == null || plan.getEndTime() == null
                || plan.getStartTime().trim().isEmpty() || plan.getEndTime().trim().isEmpty())
        {
            result.put("message", "请先设置考试日期与起止时间");
            return result;
        }
        // 1. 考生名单
        List<Long> studentIds = aemExamSeatMapper.selectStudentIdsByCourseAndSemester(
                plan.getCourseId(), plan.getSemesterId());
        if (studentIds == null || studentIds.isEmpty())
        {
            result.put("message", "无考生名单");
            return result;
        }
        studentIds.sort(Long::compareTo);
        // 2. 可用教室 = 全部正常教室 - 同时段已占用教室
        Set<Long> occupied = new HashSet<>();
        List<Long> occupiedIds = aemExamSeatMapper.selectOccupiedClassroomIds(
                plan.getExamDate(), plan.getStartTime(), plan.getEndTime(), examId);
        if (occupiedIds != null) occupied.addAll(occupiedIds);
        // 使用 resultMap 映射的列表查询（全局关闭了驼峰转换，resultType 无法映射 classroom_id/capacity）
        BrmClassroom roomQuery = new BrmClassroom();
        roomQuery.setStatus("0");
        List<BrmClassroom> rooms = brmClassroomMapper.selectBrmClassroomList(roomQuery);
        List<BrmClassroom> available = new ArrayList<>();
        if (rooms != null)
        {
            for (BrmClassroom r : rooms)
            {
                if (r.getClassroomId() == null || r.getCapacity() == null || r.getCapacity() <= 0) continue;
                if (occupied.contains(r.getClassroomId())) continue;
                available.add(r);
            }
        }
        if (available.isEmpty())
        {
            result.put("message", "无可用教室（同时段均已占用或无正常教室）");
            return result;
        }
        // 3. 按容量降序，贪心拆分（超容量自动拆分到多教室）
        available.sort((a, b) -> b.getCapacity().compareTo(a.getCapacity()));
        int total = studentIds.size();
        int totalCap = 0;
        for (BrmClassroom r : available) totalCap += r.getCapacity();
        if (totalCap < total)
        {
            result.put("message", "可用教室总容量(" + totalCap + ")不足，无法容纳考生(" + total + "人)");
            return result;
        }
        aemExamSeatMapper.deleteByExamId(examId);
        List<AemExamSeat> allSeats = new ArrayList<>();
        List<Map<String, Object>> roomDetails = new ArrayList<>();
        int cursor = 0;
        int seatNumber = 1;
        for (BrmClassroom room : available)
        {
            if (cursor >= total) break;
            int take = Math.min(room.getCapacity(), total - cursor);
            List<Long> chunk = studentIds.subList(cursor, cursor + take);
            buildRoomSeats(allSeats, examId, room, chunk, seatNumber);
            seatNumber += take;
            cursor += take;
            Map<String, Object> rd = new HashMap<>();
            rd.put("classroomId", room.getClassroomId());
            rd.put("classroomName", room.getClassroomName());
            rd.put("capacity", room.getCapacity());
            rd.put("assigned", take);
            roomDetails.add(rd);
        }
        if (!allSeats.isEmpty())
        {
            aemExamSeatMapper.batchInsert(allSeats);
        }
        // 4. 更新计划状态
        plan.setTotalStudents(total);
        plan.setPlanStatus("1");
        plan.setUpdateTime(DateUtils.getNowDate());
        aemExamPlanMapper.updateAemExamPlan(plan);
        result.put("totalStudents", total);
        result.put("roomCount", roomDetails.size());
        result.put("rooms", roomDetails);
        result.put("message", String.format("自动编排完成：%d名考生拆分至%d个教室，无教室冲突", total, roomDetails.size()));
        return result;
    }

    /** 为单个教室按蛇形（隔列入座）生成座位。 */
    private void buildRoomSeats(List<AemExamSeat> collector, Long examId, BrmClassroom room,
                                List<Long> students, int startSeatNumber)
    {
        int capacity = room.getCapacity();
        int totalSeats = students.size();
        int cols = (int) Math.ceil(Math.sqrt(capacity));
        if (cols < 1) cols = 1;
        int rows = (int) Math.ceil((double) totalSeats / cols);
        if (rows < 1) rows = 1;
        int seatNumber = startSeatNumber;
        int idx = 0;
        for (int row = 1; row <= rows && idx < totalSeats; row++)
        {
            if (row % 2 == 1)
            {
                for (int col = 1; col <= cols && idx < totalSeats; col++)
                {
                    collector.add(newSeat(examId, room.getClassroomId(), students.get(idx++), seatNumber++, row, col));
                }
            }
            else
            {
                for (int col = cols; col >= 1 && idx < totalSeats; col--)
                {
                    collector.add(newSeat(examId, room.getClassroomId(), students.get(idx++), seatNumber++, row, col));
                }
            }
        }
    }

    private AemExamSeat newSeat(Long examId, Long classroomId, Long studentId, int seatNumber, int row, int col)
    {
        AemExamSeat seat = new AemExamSeat();
        seat.setExamId(examId);
        seat.setClassroomId(classroomId);
        seat.setStudentId(studentId);
        seat.setSeatNumber(seatNumber);
        seat.setRowNumber(row);
        seat.setColNumber(col * 2 - 1); // 隔列入座防作弊
        seat.setStatus("0");
        seat.setCreateTime(DateUtils.getNowDate());
        return seat;
    }

    /**
     * A2：考试冲突检测。两两比较时间重叠的考试，输出教室/学生/监考教师冲突。
     */
    @Override
    public List<Map<String, Object>> detectExamConflicts(Long semesterId)
    {
        List<Map<String, Object>> conflicts = new ArrayList<>();
        List<AemExamPlan> plans = aemExamPlanMapper.selectBySemesterForConflict(semesterId);
        if (plans == null || plans.size() < 2)
        {
            return conflicts;
        }
        // 预取每个考试的占用集合，避免两两比较时重复查库
        Map<Long, Set<Long>> roomMap = new HashMap<>();
        Map<Long, Set<Long>> stuMap = new HashMap<>();
        Map<Long, Set<Long>> teaMap = new HashMap<>();
        for (AemExamPlan p : plans)
        {
            roomMap.put(p.getExamId(), toSet(aemExamSeatMapper.selectClassroomIdsByExamId(p.getExamId())));
            stuMap.put(p.getExamId(), toSet(aemExamSeatMapper.selectStudentIdsByExamId(p.getExamId())));
            teaMap.put(p.getExamId(), toSet(aemExamInvigilationMapper.selectTeacherIdsByExamId(p.getExamId())));
        }
        int n = plans.size();
        for (int i = 0; i < n; i++)
        {
            for (int j = i + 1; j < n; j++)
            {
                AemExamPlan a = plans.get(i);
                AemExamPlan b = plans.get(j);
                if (!sameDate(a.getExamDate(), b.getExamDate())) continue;
                if (!timeOverlap(a.getStartTime(), a.getEndTime(), b.getStartTime(), b.getEndTime())) continue;
                addConflict(conflicts, a, b, "教室冲突", intersect(roomMap.get(a.getExamId()), roomMap.get(b.getExamId())));
                addConflict(conflicts, a, b, "学生冲突", intersect(stuMap.get(a.getExamId()), stuMap.get(b.getExamId())));
                addConflict(conflicts, a, b, "监考教师冲突", intersect(teaMap.get(a.getExamId()), teaMap.get(b.getExamId())));
            }
        }
        return conflicts;
    }

    private void addConflict(List<Map<String, Object>> conflicts, AemExamPlan a, AemExamPlan b,
                             String type, List<Long> overlap)
    {
        if (overlap.isEmpty()) return;
        Map<String, Object> c = new HashMap<>();
        c.put("examIdA", a.getExamId());
        c.put("examNameA", a.getExamName());
        c.put("examIdB", b.getExamId());
        c.put("examNameB", b.getExamName());
        c.put("conflictType", type);
        c.put("count", overlap.size());
        c.put("relatedIds", overlap);
        conflicts.add(c);
    }

    private Set<Long> toSet(List<Long> list)
    {
        return list == null ? new HashSet<>() : new HashSet<>(list);
    }

    private List<Long> intersect(Set<Long> a, Set<Long> b)
    {
        List<Long> r = new ArrayList<>();
        if (a == null || b == null) return r;
        for (Long id : a) if (b.contains(id)) r.add(id);
        r.sort(Long::compareTo);
        return r;
    }

    private boolean sameDate(Date d1, Date d2)
    {
        if (d1 == null || d2 == null) return false;
        java.util.Calendar c1 = java.util.Calendar.getInstance();
        java.util.Calendar c2 = java.util.Calendar.getInstance();
        c1.setTime(d1);
        c2.setTime(d2);
        return c1.get(java.util.Calendar.YEAR) == c2.get(java.util.Calendar.YEAR)
                && c1.get(java.util.Calendar.DAY_OF_YEAR) == c2.get(java.util.Calendar.DAY_OF_YEAR);
    }

    /** 判断两个 HH:mm 时间段是否重叠。 */
    private boolean timeOverlap(String s1, String e1, String s2, String e2)
    {
        int a1 = toMinutes(s1), a2 = toMinutes(e1);
        int b1 = toMinutes(s2), b2 = toMinutes(e2);
        if (a1 < 0 || a2 < 0 || b1 < 0 || b2 < 0) return false;
        return a1 < b2 && b1 < a2;
    }

    private int toMinutes(String hhmm)
    {
        if (hhmm == null) return -1;
        String t = hhmm.trim();
        if (t.isEmpty()) return -1;
        String[] parts = t.split(":");
        try
        {
            int h = Integer.parseInt(parts[0]);
            int m = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            return h * 60 + m;
        }
        catch (NumberFormatException e)
        {
            return -1;
        }
    }
}
