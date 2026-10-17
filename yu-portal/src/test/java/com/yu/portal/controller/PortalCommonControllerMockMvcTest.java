package com.yu.portal.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.brm.domain.BrmSemester;
import com.yu.brm.service.IBrmSemesterService;

/**
 * 门户公共字典接口层测试（Q1 覆盖率爬坡：MockMvc standalone，补齐 PortalCommonController 缺口）。
 *
 * <p>E2 学期供给契约：/portal/common/semesters 供门户各页学期下拉。校验三点——
 * ①仅查启用学期（透传查询条件 status="0"）；②结果按学期ID倒序（最新在前）；
 * ③回传体仅裁剪为 semesterId + semesterName 两字段（不外泄整域对象）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PortalCommonControllerMockMvcTest {

    @Mock
    private IBrmSemesterService brmSemesterService;

    @InjectMocks
    private PortalCommonController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    private BrmSemester semester(long id, String name) {
        BrmSemester s = new BrmSemester();
        s.setSemesterId(id);
        s.setSemesterName(name);
        s.setStatus("0");
        s.setAcademicYearId(1L);
        return s;
    }

    @Test
    @DisplayName("semesters：仅查启用学期，按学期ID倒序且只回传下拉两字段")
    void semesters_enabledOnlySortedDescAndTrimmed() throws Exception {
        // 控制器会对返回列表执行 sort，须给可变列表（与真实 Mapper 返回一致）
        List<BrmSemester> list = new ArrayList<>();
        list.add(semester(1L, "2025-2026第一学期"));
        list.add(semester(3L, "2026-2027第一学期"));
        list.add(semester(2L, "2025-2026第二学期"));
        when(brmSemesterService.selectBrmSemesterList(any(BrmSemester.class))).thenReturn(list);

        mockMvc.perform(get("/portal/common/semesters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(3))
                // 倒序：最新学期ID在前
                .andExpect(jsonPath("$.data[0].semesterId").value(3))
                .andExpect(jsonPath("$.data[1].semesterId").value(2))
                .andExpect(jsonPath("$.data[2].semesterId").value(1))
                .andExpect(jsonPath("$.data[0].semesterName").value("2026-2027第一学期"))
                // 字段裁剪：不外泄 academicYearId 等非下拉字段
                .andExpect(jsonPath("$.data[0].academicYearId").doesNotExist());

        ArgumentCaptor<BrmSemester> captor = ArgumentCaptor.forClass(BrmSemester.class);
        verify(brmSemesterService).selectBrmSemesterList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("0", captor.getValue().getStatus());
    }

    @Test
    @DisplayName("semesters：无启用学期时返回空数组而非报错")
    void semesters_emptySafe() throws Exception {
        when(brmSemesterService.selectBrmSemesterList(any(BrmSemester.class))).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/portal/common/semesters"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data.length()").value(0));
    }
}
