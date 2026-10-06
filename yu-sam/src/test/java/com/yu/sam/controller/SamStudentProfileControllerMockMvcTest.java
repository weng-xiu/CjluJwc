package com.yu.sam.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
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

import com.yu.sam.service.ISamStudentProfileService;

/**
 * N6 学生数字档案接口层测试（MockMvc standalone）。
 *
 * @author N6
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamStudentProfileControllerMockMvcTest
{
    @Mock
    private ISamStudentProfileService studentProfileService;

    @InjectMocks
    private SamStudentProfileController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("profile：路径变量绑定并返回 available")
    void profile_bindsPathVariable() throws Exception
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("available", true);
        data.put("studentId", 1L);
        when(studentProfileService.buildProfile(eq(1L))).thenReturn(data);

        mockMvc.perform(get("/sam/studentProfile/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.studentId").value(1));

        verify(studentProfileService).buildProfile(eq(1L));
    }

    @Test
    @DisplayName("profile：学生不存在时 available=false")
    void profile_notAvailable() throws Exception
    {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("available", false);
        when(studentProfileService.buildProfile(eq(99L))).thenReturn(data);

        mockMvc.perform(get("/sam/studentProfile/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.available").value(false));
    }
}
