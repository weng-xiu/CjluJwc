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

import com.yu.brm.domain.BrmClassroomType;
import com.yu.brm.service.IBrmClassroomTypeService;

/**
 * 教室类型接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-brm 长尾基础数据控制器）。
 * 标准 CRUD，控制器未使用登录上下文。校验路由 /brm/roomtype、@Validated 必填校验（typeName @NotBlank）、
 * add/edit 域对象透传、逗号数组批量删除、list 查询条件绑定与表格结构。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrmClassroomTypeControllerMockMvcTest {

    @Mock
    private IBrmClassroomTypeService brmClassroomTypeService;

    @InjectMocks
    private BrmClassroomTypeController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定typeId并返回教室类型")
    void getInfo_returnsType() throws Exception {
        BrmClassroomType type = new BrmClassroomType();
        type.setTypeId(4L);
        type.setTypeName("多媒体教室");
        when(brmClassroomTypeService.selectBrmClassroomTypeByTypeId(eq(4L))).thenReturn(type);

        mockMvc.perform(get("/brm/roomtype/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.typeName").value("多媒体教室"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(brmClassroomTypeService.insertBrmClassroomType(any(BrmClassroomType.class))).thenReturn(1);

        mockMvc.perform(post("/brm/roomtype")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeName\":\"多媒体教室\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<BrmClassroomType> captor = ArgumentCaptor.forClass(BrmClassroomType.class);
        verify(brmClassroomTypeService).insertBrmClassroomType(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("多媒体教室", captor.getValue().getTypeName());
    }

    @Test
    @DisplayName("add：缺失必填typeName被@NotBlank拦截返回400，不落库")
    void add_missingTypeNameRejected() throws Exception {
        mockMvc.perform(post("/brm/roomtype")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verify(brmClassroomTypeService, never()).insertBrmClassroomType(any());
    }

    @Test
    @DisplayName("edit：影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(brmClassroomTypeService.updateBrmClassroomType(any(BrmClassroomType.class))).thenReturn(0);

        mockMvc.perform(put("/brm/roomtype")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"typeId\":4,\"typeName\":\"多媒体教室A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(brmClassroomTypeService.deleteBrmClassroomTypeByTypeIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/brm/roomtype/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(brmClassroomTypeService).deleteBrmClassroomTypeByTypeIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(brmClassroomTypeService.selectBrmClassroomTypeList(any(BrmClassroomType.class))).thenReturn(List.of());

        mockMvc.perform(get("/brm/roomtype/list").param("typeName", "多媒体"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<BrmClassroomType> captor = ArgumentCaptor.forClass(BrmClassroomType.class);
        verify(brmClassroomTypeService).selectBrmClassroomTypeList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("多媒体", captor.getValue().getTypeName());
    }
}
