package com.yu.dis.controller;

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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

import com.yu.dis.domain.DisSyncTask;
import com.yu.dis.service.IDisSyncTaskService;

/**
 * 数据同步任务接口层测试（Q1 第十四批：MockMvc standalone，首次覆盖 yu-dis 业务域控制器）。
 * 校验路由 /dis/task、@Validated 必填校验（taskName @NotBlank、systemId/interfaceId @NotNull）、
 * D1 手动执行 execute/{taskId} 与 D2 人工重推 repush/{taskId} 的路径变量绑定及结果 Map 落 data（非 msg）、
 * 逗号数组批量删除、list 查询条件绑定。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class DisSyncTaskControllerMockMvcTest {

    @Mock
    private IDisSyncTaskService disSyncTaskService;

    @InjectMocks
    private DisSyncTaskController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：路径变量绑定taskId并返回任务数据体")
    void getInfo_returnsTask() throws Exception {
        DisSyncTask task = new DisSyncTask();
        task.setTaskId(9L);
        task.setTaskName("学籍同步");
        when(disSyncTaskService.selectDisSyncTaskByTaskId(eq(9L))).thenReturn(task);

        mockMvc.perform(get("/dis/task/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.taskId").value(9))
                .andExpect(jsonPath("$.data.taskName").value("学籍同步"));
    }

    @Test
    @DisplayName("add：合法请求体通过@Validated并落库")
    void add_validBodyPersists() throws Exception {
        when(disSyncTaskService.insertDisSyncTask(any(DisSyncTask.class))).thenReturn(1);

        String body = "{\"taskName\":\"成绩同步\",\"systemId\":1,\"interfaceId\":2}";
        mockMvc.perform(post("/dis/task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<DisSyncTask> captor = ArgumentCaptor.forClass(DisSyncTask.class);
        verify(disSyncTaskService).insertDisSyncTask(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(1L), captor.getValue().getSystemId());
        org.junit.jupiter.api.Assertions.assertEquals(Long.valueOf(2L), captor.getValue().getInterfaceId());
    }

    @Test
    @DisplayName("add：缺失必填taskName被@NotBlank拦截返回400，不落库")
    void add_missingTaskNameRejected() throws Exception {
        String body = "{\"systemId\":1,\"interfaceId\":2}";
        mockMvc.perform(post("/dis/task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(disSyncTaskService, never()).insertDisSyncTask(any());
    }

    @Test
    @DisplayName("add：缺失必填interfaceId被@NotNull拦截返回400，不落库")
    void add_missingInterfaceIdRejected() throws Exception {
        String body = "{\"taskName\":\"成绩同步\",\"systemId\":1}";
        mockMvc.perform(post("/dis/task")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        verify(disSyncTaskService, never()).insertDisSyncTask(any());
    }

    @Test
    @DisplayName("execute：D1手动执行路径变量绑定taskId，结果Map落data而非msg")
    void execute_bindsIdAndReturnsMapInData() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("success", true);
        result.put("count", 10);
        when(disSyncTaskService.executeSyncTask(eq(9L))).thenReturn(result);

        mockMvc.perform(post("/dis/task/execute/9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.success").value(true))
                .andExpect(jsonPath("$.data.count").value(10));

        verify(disSyncTaskService).executeSyncTask(eq(9L));
    }

    @Test
    @DisplayName("repush：D2人工重推路径变量绑定taskId，结果Map落data")
    void repush_bindsIdAndReturnsMapInData() throws Exception {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("retryFlag", 1);
        when(disSyncTaskService.rePushTask(eq(3L))).thenReturn(result);

        mockMvc.perform(post("/dis/task/repush/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.retryFlag").value(1));

        verify(disSyncTaskService).rePushTask(eq(3L));
    }

    @Test
    @DisplayName("remove：逗号路径变量批量删除绑定Long[]")
    void remove_bindsCommaArray() throws Exception {
        when(disSyncTaskService.deleteDisSyncTaskByTaskIds(any(Long[].class))).thenReturn(1);

        mockMvc.perform(delete("/dis/task/1,2,3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        ArgumentCaptor<Long[]> captor = ArgumentCaptor.forClass(Long[].class);
        verify(disSyncTaskService).deleteDisSyncTaskByTaskIds(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals(3, captor.getValue().length);
    }

    @Test
    @DisplayName("list：查询条件绑定进域对象并返回表格分页结构")
    void list_bindsQueryFields() throws Exception {
        when(disSyncTaskService.selectDisSyncTaskList(any(DisSyncTask.class))).thenReturn(List.of());

        mockMvc.perform(get("/dis/task/list").param("taskName", "同步"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.rows").isArray());

        ArgumentCaptor<DisSyncTask> captor = ArgumentCaptor.forClass(DisSyncTask.class);
        verify(disSyncTaskService).selectDisSyncTaskList(captor.capture());
        org.junit.jupiter.api.Assertions.assertEquals("同步", captor.getValue().getTaskName());
    }
}
