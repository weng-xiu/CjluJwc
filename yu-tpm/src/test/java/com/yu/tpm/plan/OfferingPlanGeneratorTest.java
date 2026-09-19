package com.yu.tpm.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.tpm.domain.TpmCourseLibrary;
import com.yu.tpm.domain.TpmCourseOffering;

/**
 * {@link OfferingPlanGenerator} 单元测试（T4 批量生成开课核心逻辑）。
 */
class OfferingPlanGeneratorTest
{
    private final OfferingPlanGenerator generator = new OfferingPlanGenerator();

    private TpmCourseLibrary course(long id)
    {
        TpmCourseLibrary c = new TpmCourseLibrary();
        c.setCourseId(id);
        return c;
    }

    @Test
    @DisplayName("按方案课程一键生成待确认开课并应用容量与教学班数")
    void generateFromCourses()
    {
        List<TpmCourseLibrary> courses = Arrays.asList(course(1), course(2), course(3));
        OfferingGenerationResult r = generator.generate(courses, new HashSet<>(), new ArrayList<>(), 1L, 9L, 60, 2, true);

        assertEquals(3, r.getGenerated());
        for (TpmCourseOffering o : r.getOfferings())
        {
            assertEquals("0", o.getOfferingStatus());
            assertEquals(1L, o.getSemesterId());
            assertEquals(9L, o.getCampusId());
            assertEquals(60, o.getMaxStudents());
            assertEquals(2, o.getClassCount());
            assertNull(o.getTeacherId());
        }
    }

    @Test
    @DisplayName("已存在开课的课程被跳过（幂等）")
    void skipExisting()
    {
        List<TpmCourseLibrary> courses = Arrays.asList(course(1), course(2), course(3));
        Set<Long> existing = new HashSet<>(Arrays.asList(2L));
        OfferingGenerationResult r = generator.generate(courses, existing, new ArrayList<>(), 1L, null, 40, 1, true);

        assertEquals(2, r.getGenerated());
        assertEquals(1, r.getSkippedExisting());
        assertTrue(r.getOfferings().stream().noneMatch(o -> o.getCourseId() == 2L));
    }

    @Test
    @DisplayName("教师轮询预分配并均匀分布")
    void teacherRoundRobin()
    {
        List<TpmCourseLibrary> courses = Arrays.asList(course(1), course(2), course(3), course(4));
        List<Long> teachers = Arrays.asList(101L, 202L);
        OfferingGenerationResult r = generator.generate(courses, new HashSet<>(), teachers, 1L, null, 30, 1, true);

        assertEquals(4, r.getGenerated());
        assertEquals(4, r.getTeacherAssigned());
        // 101,202,101,202
        assertEquals(101L, r.getOfferings().get(0).getTeacherId());
        assertEquals(202L, r.getOfferings().get(1).getTeacherId());
        assertEquals(101L, r.getOfferings().get(2).getTeacherId());
        assertEquals(202L, r.getOfferings().get(3).getTeacherId());
    }

    @Test
    @DisplayName("关闭跳过开关时已存在开课仍会生成")
    void noSkipWhenDisabled()
    {
        List<TpmCourseLibrary> courses = Arrays.asList(course(1), course(2));
        Set<Long> existing = new HashSet<>(Arrays.asList(1L, 2L));
        OfferingGenerationResult r = generator.generate(courses, existing, new ArrayList<>(), 1L, null, 30, 1, false);
        assertEquals(2, r.getGenerated());
        assertEquals(0, r.getSkippedExisting());
    }

    @Test
    @DisplayName("非正数的容量与教学班数回退到默认值(30/1)")
    void fallbackDefaults()
    {
        List<TpmCourseLibrary> courses = Arrays.asList(course(1));
        OfferingGenerationResult r = generator.generate(courses, new HashSet<>(), new ArrayList<>(), 1L, null, 0, -5, true);
        assertEquals(30, r.getOfferings().get(0).getMaxStudents());
        assertEquals(1, r.getOfferings().get(0).getClassCount());
    }

    @Test
    @DisplayName("空课程列表与无课程ID记录不产出结果")
    void emptyAndInvalid()
    {
        assertTrue(generator.generate(new ArrayList<>(), new HashSet<>(), null, 1L, null, 30, 1, true).getGenerated() == 0);
        assertTrue(generator.generate(null, new HashSet<>(), null, 1L, null, 30, 1, true).getGenerated() == 0);

        List<TpmCourseLibrary> withNull = new ArrayList<>();
        withNull.add(new TpmCourseLibrary()); // courseId 为空
        withNull.add(course(5));
        OfferingGenerationResult r = generator.generate(withNull, new HashSet<>(), new ArrayList<>(), 1L, null, 30, 1, true);
        assertEquals(1, r.getGenerated());
    }
}
