package com.yu.brm.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.brm.domain.BrmClass;
import com.yu.brm.service.IBrmClassService;

/**
 * 班级接口层测试（Q1 第十四批：MockMvc standalone）。
 * 校验路由 /brm/clazz、@Validated 必填校验（classCode/className @NotBlank、majorId @NotNull）、
 * add/edit 域对象透传、逗号数组批量删除、list 查询条件绑定与表格结构。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrmClassControllerMockMvcTest {

    @Mock
    private IBrmClassService brmClassService;

    @InjectMocks
    private BrmClassController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定classId并返回班级数据体")
    void getInfo_returnsClass() throws Exception {
        BrmClass clazz = new BrmClass();
        clazz.setClassId(5L);
        clazz.setClassName("计科21001");
        when(brmClassService.selectBrmClassByClassId(eq(5L))).thenReturn(clazz);

        mockMvc.perform(get("/brm/clazz/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.classId").value(5))
                .andExpect(jsonPath("$.data.className").value("计科21001"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(brmClassService.insertBrmClass(any(BrmClass.class))).thenReturn(1);

        String body = "{\"classCode\":\"CS2101\",\"className\":\"计科2101\",\"majorId\":2}";
        mockMvc.perform(post("/brm/clazz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<BrmClass> captor = ArgumentCaptor.forClass(BrmClass.class);
        verify(brmClassService).insertBrmClass(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("CS2101", captor.getValue().getClassCode());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(2L), captor.getValue().getMajorId());
    }

    @Test
    @DisplayName("add：缺失必填className被@NotBlank拦截返回400，不落库")
    void add_missingClassNameRejected() throws Exception {
        String body = "{\"classCode\":\"CS2101\",\"majorId\":2}";
        mockMvc.perform(post("/brm/clazz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(brmClassService, never()).insertBrmClass(any());
    }

    @Test
    @DisplayName("add：缺失必填majorId被@NotNull拦截返回400，不落库")
    void add_missingMajorIdRejected() throws Exception {
        String body = "{\"classCode\":\"CS2101\",\"className\":\"计科2101\"}";
        mockMvc.perform(post("/brm/clazz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(brmClassService, never()).insertBrmClass(any());
    }

    @Test
    @DisplayName("edit：合法请求体透传更新，影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(brmClassService.updateBrmClass(any(BrmClass.class))).thenReturn(0);

        String body = "{\"classId\":5,\"classCode\":\"CS2101\",\"className\":\"计科2101A\",\"majorId\":2}";
        mockMvc.perform(put("/brm/clazz")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(brmClassService).updateBrmClass(any(BrmClass.class));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(brmClassService.deleteBrmClassByClassIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/brm/clazz/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(brmClassService).deleteBrmClassByClassIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(brmClassService.selectBrmClassList(any(BrmClass.class))).thenReturn(List.of());

        mockMvc.perform(get("/brm/clazz/list").param("className", "计科"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<BrmClass> captor = ArgumentCaptor.forClass(BrmClass.class);
        verify(brmClassService).selectBrmClassList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("计科", captor.getValue().getClassName());
    }
}
