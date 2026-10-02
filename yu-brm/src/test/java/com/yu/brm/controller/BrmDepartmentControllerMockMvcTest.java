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

import com.yu.brm.domain.BrmDepartment;
import com.yu.brm.service.IBrmDepartmentService;

/**
 * 院系接口层测试（Q1 第十五批：MockMvc standalone，覆盖 yu-brm 院系域控制器）。
 * 校验 list 与常规分页表格不同——直接 success(List) 返回 data 数组的契约差异、
 * @Validated 必填校验（deptCode/deptName @NotBlank）、新增/修改的名称唯一短路、
 * 修改上级院系自环拦截、停用含子院系拦截、根节点与子节点/教师存在性删除守卫、
 * syncFromSys 同步计数提示语；
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrmDepartmentControllerMockMvcTest {

    @Mock
    private IBrmDepartmentService brmDepartmentService;

    @InjectMocks
    private BrmDepartmentController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定deptId并返回院系记录")
    void getInfo_returnsDept() throws Exception {
        BrmDepartment dept = new BrmDepartment();
        dept.setDeptId(101L);
        dept.setDeptName("计算机学院");
        when(brmDepartmentService.selectBrmDepartmentByDeptId(eq(101L))).thenReturn(dept);

        mockMvc.perform(get("/brm/dept/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.deptName").value("计算机学院"));
    }

    @Test
    @DisplayName("list：查询条件透传并返回data数组（非rows分页结构）")
    void list_returnsDataArray() throws Exception {
        BrmDepartment dept = new BrmDepartment();
        dept.setDeptId(101L);
        when(brmDepartmentService.selectBrmDepartmentList(any(BrmDepartment.class))).thenReturn(List.of(dept));

        mockMvc.perform(get("/brm/dept/list").param("deptName", "计算机"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].deptId").value(101));

        ArgumentCaptor<BrmDepartment> captor = ArgumentCaptor.forClass(BrmDepartment.class);
        verify(brmDepartmentService).selectBrmDepartmentList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("计算机", captor.getValue().getDeptName());
    }

    @Test
    @DisplayName("add：合法且名称唯一时落库")
    void add_validBodyPersists() throws Exception {
        when(brmDepartmentService.checkDeptNameUnique(any(BrmDepartment.class))).thenReturn("1");
        when(brmDepartmentService.insertBrmDepartment(any(BrmDepartment.class))).thenReturn(1);

        String body = "{\"parentId\":100,\"deptCode\":\"CS\",\"deptName\":\"计算机学院\"}";
        mockMvc.perform(post("/brm/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(brmDepartmentService).insertBrmDepartment(any(BrmDepartment.class));
    }

    @Test
    @DisplayName("add：缺失必填deptCode被@NotBlank拦截返回400，不落库")
    void add_missingDeptCodeRejected() throws Exception {
        String body = "{\"parentId\":100,\"deptName\":\"计算机学院\"}";
        mockMvc.perform(post("/brm/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(brmDepartmentService, never()).insertBrmDepartment(any());
    }

    @Test
    @DisplayName("add：院系名称已存在时短路报错，不落库")
    void add_duplicateNameShortCircuits() throws Exception {
        when(brmDepartmentService.checkDeptNameUnique(any(BrmDepartment.class))).thenReturn("0");

        String body = "{\"parentId\":100,\"deptCode\":\"CS\",\"deptName\":\"计算机学院\"}";
        mockMvc.perform(post("/brm/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("新增院系'计算机学院'失败，院系名称已存在"));

        verify(brmDepartmentService, never()).insertBrmDepartment(any());
    }

    @Test
    @DisplayName("edit：上级院系为自身被拦截")
    void edit_parentSelfRejected() throws Exception {
        when(brmDepartmentService.checkDeptNameUnique(any(BrmDepartment.class))).thenReturn("1");

        String body = "{\"deptId\":101,\"parentId\":101,\"deptCode\":\"CS\",\"deptName\":\"计算机学院\"}";
        mockMvc.perform(put("/brm/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("修改院系'计算机学院'失败，上级院系不能是自己"));

        verify(brmDepartmentService, never()).updateBrmDepartment(any());
    }

    @Test
    @DisplayName("edit：停用存在未停用子院系的节点被拦截")
    void edit_stopWithActiveChildrenRejected() throws Exception {
        when(brmDepartmentService.checkDeptNameUnique(any(BrmDepartment.class))).thenReturn("1");
        when(brmDepartmentService.selectChildrenDeptById(eq(101L))).thenReturn(List.of(new BrmDepartment()));

        String body = "{\"deptId\":101,\"parentId\":100,\"deptCode\":\"CS\",\"deptName\":\"计算机学院\",\"status\":\"1\"}";
        mockMvc.perform(put("/brm/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("该院系包含未停用的子院系！"));

        verify(brmDepartmentService, never()).updateBrmDepartment(any());
    }

    @Test
    @DisplayName("edit：合法更新透传，影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(brmDepartmentService.checkDeptNameUnique(any(BrmDepartment.class))).thenReturn("1");
        when(brmDepartmentService.updateBrmDepartment(any(BrmDepartment.class))).thenReturn(0);

        String body = "{\"deptId\":101,\"parentId\":100,\"deptCode\":\"CS\",\"deptName\":\"计算机学院\",\"status\":\"0\"}";
        mockMvc.perform(put("/brm/dept")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：根节点100混入批量删除被直接拒绝")
    void remove_rootNodeGuarded() throws Exception {
        mockMvc.perform(delete("/brm/dept/100,101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500))
                .andExpect(jsonPath("$.msg").value("根节点不能删除"));

        verify(brmDepartmentService, never()).deleteBrmDepartmentByDeptIds(any());
    }

    @Test
    @DisplayName("remove：存在下级院系时删除被拦截，无守卫时批量删除落库")
    void remove_childrenGuardAndSuccess() throws Exception {
        when(brmDepartmentService.selectChildrenDeptById(eq(101L))).thenReturn(List.of(new BrmDepartment()));
        mockMvc.perform(delete("/brm/dept/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("存在下级院系,不允许删除"));

        when(brmDepartmentService.selectChildrenDeptById(eq(102L))).thenReturn(List.of());
        when(brmDepartmentService.checkDeptExistUser(eq(102L))).thenReturn(false);
        when(brmDepartmentService.deleteBrmDepartmentByDeptIds(any(Long[].class))).thenReturn(1);
        mockMvc.perform(delete("/brm/dept/102"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    @Test
    @DisplayName("syncFromSys：返回同步计数提示语")
    void syncFromSys_returnsCount() throws Exception {
        when(brmDepartmentService.syncFromSysDept()).thenReturn(3);

        mockMvc.perform(post("/brm/dept/syncFromSys"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.msg").value("已从部门管理同步 3 条院系数据"));
    }
}
