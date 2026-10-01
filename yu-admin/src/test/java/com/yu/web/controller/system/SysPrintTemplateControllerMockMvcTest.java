package com.yu.web.controller.system;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.system.domain.SysPrintTemplate;
import com.yu.system.service.ISysPrintService;

/**
 * 打印凭证模板接口层测试（Q1 第十二批：MockMvc standalone）。
 * 校验 list 表格结构、getInfo 路径变量绑定 templateId、add/edit 回填 createBy/updateBy（SecurityContext 登录用户）、
 * 影响行数 0 经 toAjax 降级 500、remove 逗号路径变量绑定 Long[]、preview 走 previewTemplate 并把 HTML 落 data；
 * 同时钉住 {@code /list}、{@code /preview/{templateId}} 等字面量段优先于 {@code /{templateId}} 的消歧契约。
 * 模板解析渲染逻辑由 ISysPrintService 覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysPrintTemplateControllerMockMvcTest {

    @Mock
    private ISysPrintService sysPrintService;

    @InjectMocks
    private SysPrintTemplateController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        SysUser sysUser = new SysUser();
        sysUser.setUserName("tester");
        LoginUser loginUser = new LoginUser(1L, 2L, sysUser, Collections.emptySet());
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private SysPrintTemplate template(long id) {
        SysPrintTemplate t = new SysPrintTemplate();
        t.setTemplateId(id);
        return t;
    }

    @Test
    @DisplayName("list：返回模板表格结构")
    void list_returnsTable() throws Exception {
        when(sysPrintService.selectTemplateList(any(SysPrintTemplate.class))).thenReturn(List.of(template(1L)));

        mockMvc.perform(get("/system/printTemplate/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows.length()").value(1));
    }

    @Test
    @DisplayName("getInfo：路径变量绑定 templateId")
    void getInfo_bindsTemplateId() throws Exception {
        when(sysPrintService.selectTemplateById(eq(7L))).thenReturn(template(7L));

        mockMvc.perform(get("/system/printTemplate/7"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.templateId").value(7));
    }

    @Test
    @DisplayName("add：回填 createBy 为登录用户后落库")
    void add_setsCreateBy() throws Exception {
        when(sysPrintService.insertTemplate(any(SysPrintTemplate.class))).thenReturn(1);

        mockMvc.perform(post("/system/printTemplate")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysPrintTemplate> captor = ArgumentCaptor.forClass(SysPrintTemplate.class);
        verify(sysPrintService).insertTemplate(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getCreateBy());
    }

    @Test
    @DisplayName("add：影响行数 0 经 toAjax 降级 500")
    void add_zeroRowsReturnsError() throws Exception {
        when(sysPrintService.insertTemplate(any(SysPrintTemplate.class))).thenReturn(0);

        mockMvc.perform(post("/system/printTemplate")
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("操作失败"));
    }

    @Test
    @DisplayName("edit：回填 updateBy 并透传 templateId")
    void edit_setsUpdateBy() throws Exception {
        when(sysPrintService.updateTemplate(any(SysPrintTemplate.class))).thenReturn(1);

        mockMvc.perform(put("/system/printTemplate")
                        .contentType(MediaType.APPLICATION_JSON).content("{\"templateId\":3}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<SysPrintTemplate> captor = ArgumentCaptor.forClass(SysPrintTemplate.class);
        verify(sysPrintService).updateTemplate(captor.capture());
        Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
        Assertions.assertEquals(3L, captor.getValue().getTemplateId());
    }

    @Test
    @DisplayName("remove：逗号路径变量绑定 Long[]")
    void remove_bindsCommaIds() throws Exception {
        when(sysPrintService.deleteTemplateByIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/system/printTemplate/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(sysPrintService).deleteTemplateByIds(captor.capture());
        Assertions.assertArrayEquals(new Long[] {1L, 2L}, captor.getValue());
    }

    @Test
    @DisplayName("preview：测试渲染 HTML 落 data")
    void preview_htmlToData() throws Exception {
        when(sysPrintService.previewTemplate(eq(4L))).thenReturn("<HTML>PREVIEW</HTML>");

        mockMvc.perform(get("/system/printTemplate/preview/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").value("<HTML>PREVIEW</HTML>"));
    }
}
