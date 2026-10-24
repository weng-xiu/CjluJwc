package com.yu.sam.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.sam.service.ISamProgramQualityService;

/**
 * F3 专业质量档案接口层测试（MockMvc standalone）。
 *
 * @author F3
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamProgramQualityControllerMockMvcTest
{
    @Mock
    private ISamProgramQualityService programQualityService;

    @InjectMocks
    private SamProgramQualityController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("majors：返回专业下拉清单")
    void majors_returnsList() throws Exception
    {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("majorId", 1L);
        m.put("majorName", "计算机科学与技术");
        list.add(m);
        when(programQualityService.listMajors()).thenReturn(list);

        mockMvc.perform(get("/sam/programQuality/majors"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].majorName").value("计算机科学与技术"));

        verify(programQualityService).listMajors();
    }

    @Test
    @DisplayName("profile：路径变量绑定并返回 available")
    void profile_bindsPathVariable() throws Exception
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("available", true);
        data.put("majorId", 1L);
        when(programQualityService.buildProfile(eq(1L))).thenReturn(data);

        mockMvc.perform(get("/sam/programQuality/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.majorId").value(1));

        verify(programQualityService).buildProfile(eq(1L));
    }

    @Test
    @DisplayName("profile：专业不存在时 available=false")
    void profile_notAvailable() throws Exception
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("available", false);
        when(programQualityService.buildProfile(eq(99L))).thenReturn(data);

        mockMvc.perform(get("/sam/programQuality/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));
    }
}
