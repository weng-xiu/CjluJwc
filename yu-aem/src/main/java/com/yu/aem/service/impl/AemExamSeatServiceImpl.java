package com.yu.aem.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.aem.mapper.AemExamSeatMapper;
import com.yu.aem.domain.AemExamSeat;
import com.yu.aem.domain.AemExamPlan;
import com.yu.aem.mapper.AemExamPlanMapper;
import com.yu.brm.domain.BrmClassroom;
import com.yu.brm.mapper.BrmClassroomMapper;
import com.yu.aem.service.IAemExamSeatService;

/**
 * 考场座位编排Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@Service
public class AemExamSeatServiceImpl implements IAemExamSeatService
{
    private static final Logger log = LoggerFactory.getLogger(AemExamSeatServiceImpl.class);

    @Autowired
    private AemExamSeatMapper aemExamSeatMapper;

    @Autowired
    private AemExamPlanMapper aemExamPlanMapper;

    @Autowired
    private BrmClassroomMapper brmClassroomMapper;

    @Override
    public AemExamSeat selectAemExamSeatBySeatId(Long seatId)
    {
        return aemExamSeatMapper.selectAemExamSeatBySeatId(seatId);
    }

    @Override
    public List<AemExamSeat> selectAemExamSeatList(AemExamSeat aemExamSeat)
    {
        return aemExamSeatMapper.selectAemExamSeatList(aemExamSeat);
    }

    @Override
    @Transactional
    public int insertAemExamSeat(AemExamSeat aemExamSeat)
    {
        // 数据范围校验：座位号必须为正整数
        if (aemExamSeat.getSeatNumber() != null && aemExamSeat.getSeatNumber() <= 0)
        {
            throw new ServiceException("座位号必须为正整数");
        }
        aemExamSeat.setCreateTime(DateUtils.getNowDate());
        return aemExamSeatMapper.insertAemExamSeat(aemExamSeat);
    }

    @Override
    @Transactional
    public int updateAemExamSeat(AemExamSeat aemExamSeat)
    {
        aemExamSeat.setUpdateTime(DateUtils.getNowDate());
        return aemExamSeatMapper.updateAemExamSeat(aemExamSeat);
    }

    @Override
    @Transactional
    public int deleteAemExamSeatBySeatId(Long seatId)
    {
        return aemExamSeatMapper.deleteAemExamSeatBySeatId(seatId);
    }

    @Override
    @Transactional
    public int deleteAemExamSeatBySeatIds(Long[] seatIds)
    {
        return aemExamSeatMapper.deleteAemExamSeatBySeatIds(seatIds);
    }

    /**
     * 自动编排考场座位
     * 算法：按学号排序，蛇形分配座位，每列间隔一个座位（错开相邻座位防作弊）
     */
    @Override
    @Transactional
    public Map<String, Object> autoArrangeSeats(Long examId, Long classroomId)
    {
        Map<String, Object> result = new HashMap<>();
        // 1. 获取考试信息
        AemExamPlan examPlan = aemExamPlanMapper.selectAemExamPlanByExamId(examId);
        if (examPlan == null)
        {
            result.put("message", "考试不存在");
            return result;
        }
        // 2. 获取教室信息
        BrmClassroom classroom = brmClassroomMapper.selectBrmClassroomByClassroomId(classroomId);
        if (classroom == null)
        {
            result.put("message", "教室不存在");
            return result;
        }
        int capacity = classroom.getCapacity() != null ? classroom.getCapacity() : 0;
        if (capacity <= 0)
        {
            result.put("message", "教室容量无效");
            return result;
        }
        // 3. 获取考生名单（从选课名单中查询该课程的学生）
        // 通过跨表查询获取选课学生ID列表
        List<Long> studentIds = aemExamSeatMapper.selectStudentIdsByCourseAndSemester(
                examPlan.getCourseId(), examPlan.getSemesterId());
        if (studentIds == null || studentIds.isEmpty())
        {
            result.put("message", "无考生名单");
            return result;
        }
        if (studentIds.size() > capacity)
        {
            result.put("message", "考生人数(" + studentIds.size() + ")超过教室容量(" + capacity + ")");
            return result;
        }
        // 4. 按学号排序
        studentIds.sort(Long::compareTo);
        // 5. 计算行列布局（蛇形排座，每列间隔一个座位）
        // 估算行列：按教室容量计算，列数取平方根的整数
        int totalSeats = studentIds.size();
        int cols = (int) Math.ceil(Math.sqrt(capacity));
        if (cols < 1) cols = 1;
        // 为了错开相邻座位，实际使用双倍列数，隔列入座
        int actualCols = cols * 2;
        int rows = (int) Math.ceil((double) totalSeats / cols);
        if (rows < 1) rows = 1;
        // 6. 清除原有座位记录
        aemExamSeatMapper.deleteByExamId(examId);
        // 7. 蛇形分配座位
        List<AemExamSeat> seats = new ArrayList<>();
        int seatNumber = 1;
        int studentIndex = 0;
        for (int row = 1; row <= rows && studentIndex < totalSeats; row++)
        {
            // 偶数行正序，奇数行反序（蛇形）
            if (row % 2 == 1)
            {
                for (int col = 1; col <= cols && studentIndex < totalSeats; col++)
                {
                    AemExamSeat seat = new AemExamSeat();
                    seat.setExamId(examId);
                    seat.setClassroomId(classroomId);
                    seat.setStudentId(studentIds.get(studentIndex));
                    seat.setSeatNumber(seatNumber++);
                    seat.setRowNumber(row);
                    seat.setColNumber(col * 2 - 1); // 隔列入座：1,3,5...
                    seat.setStatus("0");
                    seat.setCreateTime(DateUtils.getNowDate());
                    seats.add(seat);
                    studentIndex++;
                }
            }
            else
            {
                for (int col = cols; col >= 1 && studentIndex < totalSeats; col--)
                {
                    AemExamSeat seat = new AemExamSeat();
                    seat.setExamId(examId);
                    seat.setClassroomId(classroomId);
                    seat.setStudentId(studentIds.get(studentIndex));
                    seat.setSeatNumber(seatNumber++);
                    seat.setRowNumber(row);
                    seat.setColNumber(col * 2 - 1);
                    seat.setStatus("0");
                    seat.setCreateTime(DateUtils.getNowDate());
                    seats.add(seat);
                    studentIndex++;
                }
            }
        }
        // 8. 批量插入
        if (!seats.isEmpty())
        {
            aemExamSeatMapper.batchInsert(seats);
        }
        result.put("totalStudents", totalSeats);
        result.put("arrangedSeats", seats.size());
        result.put("classroomName", classroom.getClassroomName());
        result.put("rows", rows);
        result.put("cols", cols);
        result.put("message", String.format("座位编排完成：共%d人，%d排%d列", totalSeats, rows, cols));
        log.info("考试[{}]座位编排完成：教室[{}]，共{}人", examId, classroom.getClassroomName(), totalSeats);
        return result;
    }
}
