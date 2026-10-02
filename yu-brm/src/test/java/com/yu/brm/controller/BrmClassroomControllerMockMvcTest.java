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

import com.yu.brm.domain.BrmClassroom;
import com.yu.brm.service.IBrmClassroomService;

/**
 * 教室接口层测试（Q1 第十五批：MockMvc standalone，覆盖 yu-brm 教室域控制器）。
 * 校验 CRUD 路由与 @Validated 必填校验（classroomName @NotBlank、buildingId/typeId @NotNull）、
 * add/edit 域对象透传、逗号数组批量删除、list 查询条件绑定；
 * P7 Excel 导入导出依赖真实 POI 流与登录上下文，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrmClassroomControllerMockMvcTest {

    @Mock
    private IBrmClassroomService brmClassroomService;

    @InjectMocks
    private BrmClassroomController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定classroomId并返回教室记录")
    void getInfo_returnsClassroom() throws Exception {
        BrmClassroom classroom = new BrmClassroom();
        classroom.setClassroomId(4L);
        classroom.setClassroomName("一教101");
        when(brmClassroomService.selectBrmClassroomByClassroomId(eq(4L))).thenReturn(classroom);

        mockMvc.perform(get("/brm/classroom/4"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.classroomName").value("一教101"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(brmClassroomService.insertBrmClassroom(any(BrmClassroom.class))).thenReturn(1);

        String body = "{\"classroomName\":\"一教101\",\"buildingId\":2,\"typeId\":3,\"capacity\":60}";
        mockMvc.perform(post("/brm/classroom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<BrmClassroom> captor = ArgumentCaptor.forClass(BrmClassroom.class);
        verify(brmClassroomService).insertBrmClassroom(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Integer.valueOf(60), captor.getValue().getCapacity());
    }

    @Test
    @DisplayName("add：缺失必填classroomName被@NotBlank拦截返回400，不落库")
    void add_missingNameRejected() throws Exception {
        String body = "{\"buildingId\":2,\"typeId\":3}";
        mockMvc.perform(post("/brm/classroom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(brmClassroomService, never()).insertBrmClassroom(any());
    }

    @Test
    @DisplayName("add：缺失必填buildingId被@NotNull拦截返回400，不落库")
    void add_missingBuildingRejected() throws Exception {
        String body = "{\"classroomName\":\"一教101\",\"typeId\":3}";
        mockMvc.perform(post("/brm/classroom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(brmClassroomService, never()).insertBrmClassroom(any());
    }

    @Test
    @DisplayName("add：缺失必填typeId被@NotNull拦截返回400，不落库")
    void add_missingTypeRejected() throws Exception {
        String body = "{\"classroomName\":\"一教101\",\"buildingId\":2}";
        mockMvc.perform(post("/brm/classroom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(brmClassroomService, never()).insertBrmClassroom(any());
    }

    @Test
    @DisplayName("edit：合法请求体透传更新，影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(brmClassroomService.updateBrmClassroom(any(BrmClassroom.class))).thenReturn(0);

        String body = "{\"classroomId\":4,\"classroomName\":\"一教102\",\"buildingId\":2,\"typeId\":3}";
        mockMvc.perform(put("/brm/classroom")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));

        verify(brmClassroomService).updateBrmClassroom(any(BrmClassroom.class));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(brmClassroomService.deleteBrmClassroomByClassroomIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/brm/classroom/1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(brmClassroomService).deleteBrmClassroomByClassroomIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(3, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(brmClassroomService.selectBrmClassroomList(any(BrmClassroom.class))).thenReturn(List.of());

        mockMvc.perform(get("/brm/classroom/list").param("classroomName", "一教"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<BrmClassroom> captor = ArgumentCaptor.forClass(BrmClassroom.class);
        verify(brmClassroomService).selectBrmClassroomList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("一教", captor.getValue().getClassroomName());
    }
}
