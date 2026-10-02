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

import com.yu.brm.domain.BrmMajor;
import com.yu.brm.service.IBrmMajorService;

/**
 * 专业接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-brm 长尾基础数据控制器）。
 * 标准 CRUD，控制器未使用登录上下文。校验路由 /brm/major、@Validated 必填校验
 * （majorCode/majorName @NotBlank、deptId @NotNull）、add/edit 域对象透传、
 * 逗号数组批量删除、list 查询条件绑定与表格结构。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrmMajorControllerMockMvcTest {

    @Mock
    private IBrmMajorService brmMajorService;

    @InjectMocks
    private BrmMajorController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定majorId并返回专业")
    void getInfo_returnsMajor() throws Exception {
        BrmMajor major = new BrmMajor();
        major.setMajorId(4L);
        major.setMajorName("计算机科学与技术");
        when(brmMajorService.selectBrmMajorByMajorId(eq(4L))).thenReturn(major);

        mockMvc.perform(get("/brm/major/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.majorName").value("计算机科学与技术"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(brmMajorService.insertBrmMajor(any(BrmMajor.class))).thenReturn(1);

        mockMvc.perform(post("/brm/major")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"majorCode\":\"CS\",\"majorName\":\"计算机科学与技术\",\"deptId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<BrmMajor> captor = ArgumentCaptor.forClass(BrmMajor.class);
        verify(brmMajorService).insertBrmMajor(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("CS", captor.getValue().getMajorCode());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(2L), captor.getValue().getDeptId());
    }

    @Test
    @DisplayName("add：缺失必填字段被校验拦截返回400，不落库")
    void add_missingRequiredRejected() throws Exception {
        mockMvc.perform(post("/brm/major")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"majorCode\":\"CS\"}"))
                .andExpect(status().isBadRequest());

        verify(brmMajorService, never()).insertBrmMajor(any());
    }

    @Test
    @DisplayName("edit：影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(brmMajorService.updateBrmMajor(any(BrmMajor.class))).thenReturn(0);

        mockMvc.perform(put("/brm/major")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"majorId\":4,\"majorCode\":\"CS\",\"majorName\":\"计科\",\"deptId\":2}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(brmMajorService.deleteBrmMajorByMajorIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/brm/major/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(brmMajorService).deleteBrmMajorByMajorIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(brmMajorService.selectBrmMajorList(any(BrmMajor.class))).thenReturn(List.of());

        mockMvc.perform(get("/brm/major/list").param("majorName", "计算机"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<BrmMajor> captor = ArgumentCaptor.forClass(BrmMajor.class);
        verify(brmMajorService).selectBrmMajorList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("计算机", captor.getValue().getMajorName());
    }
}
