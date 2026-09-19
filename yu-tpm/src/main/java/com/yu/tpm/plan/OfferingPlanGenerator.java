package com.yu.tpm.plan;

import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Component;
import com.yu.tpm.domain.TpmCourseLibrary;
import com.yu.tpm.domain.TpmCourseOffering;

/**
 * 开课计划生成器（T4 核心逻辑，纯函数式，便于离线单元测试）。
 *
 * 输入：培养方案下的课程清单、当前学期已存在的开课课程ID集合（用于幂等去重）、
 * 可用教师池、以及生成参数（学期/校区/容量/教学班数）。
 * 输出：待插入的开课计划列表（状态为待确认 0），并统计跳过与教师预分配情况。
 *
 * 教师预分配采用轮询（round-robin）策略，将课程均匀分配到教师池中的教师；
 * 容量取传入的默认容量（调用方保证已解析配置/兜底值）。
 *
 * @author ruoyi
 */
@Component
public class OfferingPlanGenerator
{
    /**
     * @param courses         培养方案课程（已按学期序号等条件过滤）
     * @param existingCourseIds 该学期已存在开课（未取消）的课程ID集合，可为空
     * @param teacherIds      可用教师ID池，可为空（则不预分配教师）
     * @param semesterId      目标学期ID
     * @param campusId        校区ID（可空）
     * @param capacity        单开课容量（须为正数，调用方兜底）
     * @param classCount      每门课教学班数（须为正数，调用方兜底）
     * @param skipExisting    是否跳过已存在开课的课程
     * @return 生成结果
     */
    public OfferingGenerationResult generate(List<TpmCourseLibrary> courses,
                                             Set<Long> existingCourseIds,
                                             List<Long> teacherIds,
                                             Long semesterId,
                                             Long campusId,
                                             int capacity,
                                             int classCount,
                                             boolean skipExisting)
    {
        OfferingGenerationResult result = new OfferingGenerationResult();
        if (courses == null || courses.isEmpty())
        {
            return result;
        }
        int safeCapacity = capacity > 0 ? capacity : 30;
        int safeClassCount = classCount > 0 ? classCount : 1;
        boolean hasTeachers = teacherIds != null && !teacherIds.isEmpty();
        int assignCursor = 0;

        for (TpmCourseLibrary course : courses)
        {
            if (course == null || course.getCourseId() == null)
            {
                continue;
            }
            if (skipExisting && existingCourseIds != null && existingCourseIds.contains(course.getCourseId()))
            {
                result.incrementSkippedExisting();
                continue;
            }
            TpmCourseOffering offering = new TpmCourseOffering();
            offering.setSemesterId(semesterId);
            offering.setCourseId(course.getCourseId());
            offering.setCampusId(campusId);
            offering.setClassCount(safeClassCount);
            offering.setMaxStudents(safeCapacity);
            // 生成结果默认待确认，供用户编辑后再确认
            offering.setOfferingStatus("0");
            offering.setStatus("0");
            if (hasTeachers)
            {
                Long teacherId = teacherIds.get(assignCursor % teacherIds.size());
                offering.setTeacherId(teacherId);
                result.incrementTeacherAssigned();
                assignCursor++;
            }
            result.add(offering);
        }
        return result;
    }
}
