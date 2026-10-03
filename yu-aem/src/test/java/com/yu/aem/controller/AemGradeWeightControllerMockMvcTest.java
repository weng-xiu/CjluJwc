package com.yu.aem.controller;

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

import com.yu.aem.domain.AemGradeWeight;
import com.yu.aem.service.IAemGradeWeightService;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;

/**
 * 成绩权重配置接口层测试（Q1 第十七批：MockMvc standalone）。
 * 校验 /aem/gradeWeight 路由：add/edit 无 @Validated（不拦截请求体），但透传登录用户名写入 createBy/updateBy
 * （SecurityContext）；effective 预览端点将 resolveRatios 的 double[] 拆解为顶层 regularRatio/examRatio 字段。
 * 权重归口与解析算法由服务层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AemGradeWeightControllerMockMvcTest {

    @Mock
    private IAemGradeWeightService aemGradeWeightService;

    @InjectMocks
    private AemGradeWeightController controller;

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

    @Test
    @DisplayName("getInfo：路径变量绑定weightId并返回数据体")
    void getInfo_returnsWeight() throws Exception {
        AemGradeWeight w = new AemGradeWeight();
        w.setWeightId(2L);
        w.setCourseId(5L);
        when(aemGradeWeightService.selectAemGradeWeightByWeightId(eq(2L))).thenReturn(w);

        mockMvc.perform(get("/aem/gradeWeight/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.weightId").value(2))
                .andExpect(jsonPath("$.data.courseId").value(5));
    }

    @Test
    @DisplayName("add：无@Validated，透传请求体并以登录名写入createBy")
    void add_setsCreateByFromLogin() throws Exception {
        when(aemGradeWeightService.insertAemGradeWeight(any(AemGradeWeight.class))).thenReturn(1);

        mockMvc.perform(post("/aem/gradeWeight")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"courseId\":5,\"regularRatio\":0.3,\"examRatio\":0.7}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<AemGradeWeight> captor = ArgumentCaptor.forClass(AemGradeWeight.class);
        verify(aemGradeWeightService).insertAemGradeWeight(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("tester", captor.getValue().getCreateBy());
    }

    @Test
    @DisplayName("edit：无@Validated，透传请求体并以登录名写入updateBy")
    void edit_setsUpdateByFromLogin() throws Exception {
        when(aemGradeWeightService.updateAemGradeWeight(any(AemGradeWeight.class))).thenReturn(1);

        mockMvc.perform(put("/aem/gradeWeight")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"weightId\":2,\"courseId\":5,\"regularRatio\":0.4,\"examRatio\":0.6}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<AemGradeWeight> captor = ArgumentCaptor.forClass(AemGradeWeight.class);
        verify(aemGradeWeightService).updateAemGradeWeight(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("tester", captor.getValue().getUpdateBy());
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(aemGradeWeightService.deleteAemGradeWeightByWeightIds(any(Long[].class))).thenReturn(2);

        mockMvc.perform(delete("/aem/gradeWeight/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(aemGradeWeightService).deleteAemGradeWeightByWeightIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("effective：double[]拆解为顶层regularRatio/examRatio")
    void effective_splitsRatiosToTopLevel() throws Exception {
        when(aemGradeWeightService.resolveRatios(eq(5L))).thenReturn(new double[] {0.3, 0.7});

        mockMvc.perform(get("/aem/gradeWeight/effective/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.regularRatio").value(0.3))
                .andExpect(jsonPath("$.examRatio").value(0.7));
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(aemGradeWeightService.selectAemGradeWeightList(any(AemGradeWeight.class)))
                .thenReturn(List.of());

        mockMvc.perform(get("/aem/gradeWeight/list").param("courseId", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<AemGradeWeight> captor = ArgumentCaptor.forClass(AemGradeWeight.class);
        verify(aemGradeWeightService).selectAemGradeWeightList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(5L), captor.getValue().getCourseId());
    }
}
