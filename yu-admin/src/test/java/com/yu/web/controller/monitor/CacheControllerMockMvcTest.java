package com.yu.web.controller.monitor;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.HashSet;
import java.util.Properties;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

/**
 * 缓存监控接口层测试（Q1 第六批：MockMvc standalone）。
 * 校验缓存信息聚合（Redis info/commandstats/dbSize 经回调装配为 map）、缓存名清单、
 * 按键名模式检索（TreeSet 去重排序）、按 key 取值封装 SysCache、删除单键五类端点契约；
 * RedisTemplate 全部隔离为 Mock，真实 Redis 交互由集成层覆盖。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CacheControllerMockMvcTest {

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private CacheController controller;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("getInfo：聚合 info/dbSize/commandStats 为 map")
    void getInfo_aggregatesRedisInfo() throws Exception {
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(new Properties());

        mockMvc.perform(get("/monitor/cache"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.commandStats").isArray());
    }

    @Test
    @DisplayName("getNames：返回预置缓存名清单")
    void getNames_returnsCacheList() throws Exception {
        mockMvc.perform(get("/monitor/cache/getNames"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    @DisplayName("getKeys：按 cacheName 前缀检索并去重排序")
    void getKeys_returnsSortedSet() throws Exception {
        when(redisTemplate.keys(org.mockito.ArgumentMatchers.<String>any()))
                .thenReturn(new HashSet<>(java.util.List.of("sys_config:b", "sys_config:a")));

        mockMvc.perform(get("/monitor/cache/getKeys/sys_config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.length()").value(2));
    }

    @Test
    @DisplayName("getValue：按 key 取值并封装为 SysCache")
    void getValue_wrapsSysCache() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("sys_config:abc")).thenReturn("123");

        mockMvc.perform(get("/monitor/cache/getValue/sys_config/sys_config:abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.cacheValue").value("123"));
    }

    @Test
    @DisplayName("clearCacheKey：删除指定缓存键")
    void clearCacheKey_deletesKey() throws Exception {
        mockMvc.perform(delete("/monitor/cache/clearCacheKey/sys_dict:abc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(redisTemplate).delete("sys_dict:abc");
    }
}
