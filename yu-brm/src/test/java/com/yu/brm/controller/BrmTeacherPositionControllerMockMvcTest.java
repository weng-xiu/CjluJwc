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

import com.yu.brm.domain.BrmTeacherPosition;
import com.yu.brm.service.IBrmTeacherPositionService;

/**
 * 教师任职信息接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-brm 长尾教师子域控制器）。
 * 标准 CRUD，控制器未使用登录上下文。校验路由 /brm/position、@Validated 必填校验
 * （teacherId/deptId @NotNull、positionTitle @NotBlank）、add/edit 域对象透传、
 * 逗号数组批量删除、list 查询条件绑定与表格结构。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrmTeacherPositionControllerMockMvcTest {

    @Mock
    private IBrmTeacherPositionService brmTeacherPositionService;

    @InjectMocks
    private BrmTeacherPositionController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定posId并返回任职记录")
    void getInfo_returnsPosition() throws Exception {
        BrmTeacherPosition pos = new BrmTeacherPosition();
        pos.setPosId(3L);
        pos.setPositionTitle("系主任");
        when(brmTeacherPositionService.selectBrmTeacherPositionByPosId(eq(3L))).thenReturn(pos);

        mockMvc.perform(get("/brm/position/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.positionTitle").value("系主任"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(brmTeacherPositionService.insertBrmTeacherPosition(any(BrmTeacherPosition.class))).thenReturn(1);

        mockMvc.perform(post("/brm/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teacherId\":21,\"deptId\":2,\"positionTitle\":\"系主任\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<BrmTeacherPosition> captor = ArgumentCaptor.forClass(BrmTeacherPosition.class);
        verify(brmTeacherPositionService).insertBrmTeacherPosition(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(21L), captor.getValue().getTeacherId());
        org.junit.jupiter.api.Assertions.assertEquals("系主任", captor.getValue().getPositionTitle());
    }

    @Test
    @DisplayName("add：缺失必填字段被校验拦截返回400，不落库")
    void add_missingRequiredRejected() throws Exception {
        mockMvc.perform(post("/brm/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"teacherId\":21,\"deptId\":2}"))
                .andExpect(status().isBadRequest());

        verify(brmTeacherPositionService, never()).insertBrmTeacherPosition(any());
    }

    @Test
    @DisplayName("edit：影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(brmTeacherPositionService.updateBrmTeacherPosition(any(BrmTeacherPosition.class))).thenReturn(0);

        mockMvc.perform(put("/brm/position")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"posId\":3,\"teacherId\":21,\"deptId\":2,\"positionTitle\":\"副主任\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(brmTeacherPositionService.deleteBrmTeacherPositionByPosIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/brm/position/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(brmTeacherPositionService).deleteBrmTeacherPositionByPosIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(brmTeacherPositionService.selectBrmTeacherPositionList(any(BrmTeacherPosition.class))).thenReturn(List.of());

        mockMvc.perform(get("/brm/position/list").param("teacherId", "21"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<BrmTeacherPosition> captor = ArgumentCaptor.forClass(BrmTeacherPosition.class);
        verify(brmTeacherPositionService).selectBrmTeacherPositionList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(21L), captor.getValue().getTeacherId());
    }
}
