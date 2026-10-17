package com.yu.web.controller.system;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.yu.system.service.ISysPrintService;

/**
 * 电子凭证公开验真控制器契约测试（G5，P4 匿名端点）。
 * 校验按“凭证编号 + 验证码”验真时，脱敏结果经统一 {code/data} 响应序列化，
 * 且视图层不感知服务层的真伪判定与脱敏细节；验真失败同样以 200 + data.valid=false 回传（非报错）。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysPrintVerifyControllerMockMvcTest
{
    @Mock
    private ISysPrintService sysPrintService;

    @InjectMocks
    private SysPrintVerifyController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("GET /system/credential/verify：验真通过回传脱敏信息")
    void verifyValid() throws Exception
    {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("valid", true);
        r.put("holderName", "张*");
        when(sysPrintService.verify("CN2026001", "8888")).thenReturn(r);

        mockMvc.perform(get("/system/credential/verify")
                        .param("serialNo", "CN2026001").param("verifyCode", "8888"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.valid").value(true))
                .andExpect(jsonPath("$.data.holderName").value("张*"));

        verify(sysPrintService).verify("CN2026001", "8888");
    }

    @Test
    @DisplayName("验真不通过以 200 + valid=false 回传而非报错")
    void verifyInvalid() throws Exception
    {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("valid", false);
        when(sysPrintService.verify("CN2026001", "bad")).thenReturn(r);

        mockMvc.perform(get("/system/credential/verify")
                        .param("serialNo", "CN2026001").param("verifyCode", "bad"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.valid").value(false));

        verify(sysPrintService).verify("CN2026001", "bad");
    }
}
