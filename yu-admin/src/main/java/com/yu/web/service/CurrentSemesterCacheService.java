package com.yu.web.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import com.yu.framework.config.CacheConfig;
import com.yu.web.mapper.DashboardStatMapper;

/**
 * A3：当前学期热点读缓存提供者。
 *
 * <p>驾驶舱、主题统计等页面每次加载都会查询「覆盖今天的学期，无则最新启用学期」，
 * 而该结果全年仅切换约两次，属典型读多写极少的热点。此处独立成 Bean 是为了让
 * {@code @Cacheable} 走 Spring 代理生效——若在 {@code DashboardStatServiceImpl} 内部
 * 自调用则代理失效。TTL 由 {@link CacheConfig} 统一配置（10 分钟），
 * 学期增删改由 yu-brm 侧 {@code @CacheEvict} 主动失效。</p>
 *
 * @author cjlu
 */
@Service
public class CurrentSemesterCacheService
{
    @Autowired
    private DashboardStatMapper dashboardStatMapper;

    @Cacheable(cacheNames = CacheConfig.CACHE_CURRENT_SEMESTER)
    public Map<String, Object> getCurrentSemester()
    {
        return dashboardStatMapper.selectCurrentSemester();
    }
}
