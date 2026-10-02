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

import com.yu.brm.domain.BrmClassroomBorrow;
import com.yu.brm.service.IBrmClassroomBorrowService;

/**
 * 教室借用接口层测试（Q1 第十六批：MockMvc standalone，覆盖 yu-brm 长尾业务控制器）。
 * 标准 CRUD，控制器未使用登录上下文。校验路由 /brm/borrow、@Validated 必填校验
 * （classroomId/borrowDate @NotNull、applicant @NotBlank）、add/edit 域对象透传（借用日期按 yyyy-MM-dd 反序列化）、
 * 逗号数组批量删除、list 查询条件绑定与表格结构。
 * Excel 导出依赖真实 POI 输出流，不属纯接口层契约故不模拟。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BrmClassroomBorrowControllerMockMvcTest {

    @Mock
    private IBrmClassroomBorrowService brmClassroomBorrowService;

    @InjectMocks
    private BrmClassroomBorrowController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定borrowId并返回借用申请")
    void getInfo_returnsBorrow() throws Exception {
        BrmClassroomBorrow borrow = new BrmClassroomBorrow();
        borrow.setBorrowId(5L);
        borrow.setApplicant("张三");
        when(brmClassroomBorrowService.selectBrmClassroomBorrowByBorrowId(eq(5L))).thenReturn(borrow);

        mockMvc.perform(get("/brm/borrow/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.applicant").value("张三"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(brmClassroomBorrowService.insertBrmClassroomBorrow(any(BrmClassroomBorrow.class))).thenReturn(1);

        mockMvc.perform(post("/brm/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classroomId\":7,\"applicant\":\"张三\",\"borrowDate\":\"2026-10-02\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<BrmClassroomBorrow> captor = ArgumentCaptor.forClass(BrmClassroomBorrow.class);
        verify(brmClassroomBorrowService).insertBrmClassroomBorrow(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(7L), captor.getValue().getClassroomId());
        org.junit.jupiter.api.Assertions.assertNotNull(captor.getValue().getBorrowDate());
    }

    @Test
    @DisplayName("add：缺失必填字段被校验拦截返回400，不落库")
    void add_missingRequiredRejected() throws Exception {
        mockMvc.perform(post("/brm/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"classroomId\":7}"))
                .andExpect(status().isBadRequest());

        verify(brmClassroomBorrowService, never()).insertBrmClassroomBorrow(any());
    }

    @Test
    @DisplayName("edit：影响行数0时toAjax降级500")
    void edit_zeroRowsDegrades() throws Exception {
        when(brmClassroomBorrowService.updateBrmClassroomBorrow(any(BrmClassroomBorrow.class))).thenReturn(0);

        mockMvc.perform(put("/brm/borrow")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"borrowId\":5,\"classroomId\":7,\"applicant\":\"李四\",\"borrowDate\":\"2026-10-03\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(500));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(brmClassroomBorrowService.deleteBrmClassroomBorrowByBorrowIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/brm/borrow/1,2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(brmClassroomBorrowService).deleteBrmClassroomBorrowByBorrowIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(2, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(brmClassroomBorrowService.selectBrmClassroomBorrowList(any(BrmClassroomBorrow.class))).thenReturn(List.of());

        mockMvc.perform(get("/brm/borrow/list").param("applicant", "张"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<BrmClassroomBorrow> captor = ArgumentCaptor.forClass(BrmClassroomBorrow.class);
        verify(brmClassroomBorrowService).selectBrmClassroomBorrowList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("张", captor.getValue().getApplicant());
    }
}
