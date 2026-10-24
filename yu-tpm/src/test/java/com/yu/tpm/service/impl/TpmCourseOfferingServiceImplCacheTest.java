package com.yu.tpm.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import com.yu.framework.config.CacheConfig;

/**
 * P3 缓存分层扩展 · 开课 service 缓存注解契约测试。
 *
 * <p>目的：锁定 {@link TpmCourseOfferingServiceImpl} 的 {@code @Cacheable}/{@code @CacheEvict} 元数据，
 * 防止后续重构误删注解导致开课单条读缓存静默失效（选课/抽签/改选热点读回退到 DB）。
 * 采用纯反射断言，不依赖 Spring 上下文与 Redis，稳定且零副作用。</p>
 *
 * <p>覆盖：
 * <ul>
 *   <li>selectTpmCourseOfferingByOfferingId 命中 CACHE_COURSE_OFFERING，key=参数，unless 屏蔽 null</li>
 *   <li>insert/update/confirm/cancel/delete×2/batchGenerate 六大变更面 allEntries=true 全清</li>
 *   <li>list 类查询（带 @DataScope 或 portal 分支）不加缓存，避免动态条件与数据范围污染</li>
 * </ul>
 */
class TpmCourseOfferingServiceImplCacheTest
{
    /** 通过反射读取指定方法上的注解；若不存在返回 null。 */
    private Method findMethod(String name, Class<?>... params)
    {
        try
        {
            return TpmCourseOfferingServiceImpl.class.getDeclaredMethod(name, params);
        }
        catch (NoSuchMethodException e)
        {
            throw new AssertionError("方法签名缺失：" + name, e);
        }
    }

    @Test
    @DisplayName("selectTpmCourseOfferingByOfferingId 应命中 CACHE_COURSE_OFFERING 且 key 绑定入参")
    void selectByIdShouldBeCacheable()
    {
        Method m = findMethod("selectTpmCourseOfferingByOfferingId", Long.class);
        Cacheable c = m.getAnnotation(Cacheable.class);
        assertNotNull(c, "单条读缺失 @Cacheable");
        assertEquals(1, c.cacheNames().length);
        assertEquals(CacheConfig.CACHE_COURSE_OFFERING, c.cacheNames()[0]);
        assertEquals("#offeringId", c.key());
        assertEquals("#result == null", c.unless(), "应屏蔽 null 结果，防止空对象长期占据");
    }

    @Test
    @DisplayName("六大变更方法必须 allEntries=true 全量失效，避免任意 key 脏读")
    void allMutationsShouldEvict()
    {
        Method[] required = {
                findMethod("insertTpmCourseOffering", com.yu.tpm.domain.TpmCourseOffering.class),
                findMethod("updateTpmCourseOffering", com.yu.tpm.domain.TpmCourseOffering.class),
                findMethod("deleteTpmCourseOfferingByOfferingId", Long.class),
                findMethod("deleteTpmCourseOfferingByOfferingIds", Long[].class),
                findMethod("confirmOffering", Long.class),
                findMethod("cancelOffering", Long.class),
                findMethod("batchGenerateOfferings", com.yu.tpm.domain.dto.BatchOfferingRequest.class),
        };
        for (Method m : required)
        {
            CacheEvict evict = m.getAnnotation(CacheEvict.class);
            assertNotNull(evict, m.getName() + " 缺失 @CacheEvict");
            assertEquals(CacheConfig.CACHE_COURSE_OFFERING, evict.cacheNames()[0],
                    m.getName() + " 缓存名不匹配");
            assertTrue(evict.allEntries(), m.getName() + " 应 allEntries=true（可能批量生成或多 key 脏化）");
        }
    }

    @Test
    @DisplayName("list 类查询不应加缓存：@DataScope 与门户分支依赖调用者上下文")
    void listMethodsShouldNotBeCacheable()
    {
        Method withScope = findMethod("selectTpmCourseOfferingList", com.yu.tpm.domain.TpmCourseOffering.class);
        assertFalse(withScope.isAnnotationPresent(Cacheable.class),
                "带 @DataScope 的 list 不可缓存，否则跨用户/部门数据范围将污染");

        Method portalList = findMethod("selectTpmCourseOfferingListForPortal", com.yu.tpm.domain.TpmCourseOffering.class);
        assertFalse(portalList.isAnnotationPresent(Cacheable.class),
                "门户 list 走动态过滤，不加缓存");
    }

    @Test
    @DisplayName("CacheConfig 已注册 course_offering 缓存名与短 TTL")
    void cacheConfigExposesCourseOfferingName()
    {
        assertEquals("course_offering", CacheConfig.CACHE_COURSE_OFFERING);
    }
}
