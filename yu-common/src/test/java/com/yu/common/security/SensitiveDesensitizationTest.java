package com.yu.common.security;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import tools.jackson.databind.json.JsonMapper;

import com.yu.common.annotation.Sensitive;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.domain.model.LoginUser;
import com.yu.common.enums.DesensitizedType;

/**
 * A4 字段级脱敏：集中策略 {@link SensitivePolicy} 与 Jackson3 序列化链路的单元验证。
 *
 * @author A4
 */
class SensitiveDesensitizationTest
{
    /** 带脱敏注解的最小 POJO，用于验证序列化端到端效果 */
    static class Contact
    {
        private final String phone;

        Contact(String phone)
        {
            this.phone = phone;
        }

        @Sensitive(desensitizedType = DesensitizedType.PHONE)
        public String getPhone()
        {
            return phone;
        }
    }

    @AfterEach
    void clearContext()
    {
        SecurityContextHolder.clearContext();
    }

    private void login(Long userId)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        LoginUser loginUser = new LoginUser();
        loginUser.setUser(user);
        SecurityContextHolder.getContext()
                .setAuthentication(new UsernamePasswordAuthenticationToken(loginUser, null));
    }

    @Test
    void admin_shouldNotDesensitize()
    {
        login(1L);
        assertFalse(SensitivePolicy.shouldDesensitize(), "超级管理员应可见明文");
    }

    @Test
    void nonAdmin_shouldDesensitize()
    {
        login(2L);
        assertTrue(SensitivePolicy.shouldDesensitize(), "普通用户应脱敏");
    }

    @Test
    void anonymous_shouldDesensitize_failSafe()
    {
        SecurityContextHolder.clearContext();
        assertTrue(SensitivePolicy.shouldDesensitize(), "无登录上下文应故障安全脱敏");
    }

    @Test
    void serialize_masksPhone_forNonAdmin()
    {
        login(2L);
        String json = JsonMapper.builder().build().writeValueAsString(new Contact("13812345678"));
        assertTrue(json.contains("138****5678"), "普通用户手机号应脱敏, 实际=" + json);
    }

    @Test
    void serialize_keepsRaw_forAdmin()
    {
        login(1L);
        String json = JsonMapper.builder().build().writeValueAsString(new Contact("13812345678"));
        assertTrue(json.contains("13812345678"), "管理员手机号应保留明文, 实际=" + json);
    }

    @Test
    void emailDesensitize_masksLocalPart()
    {
        login(2L);
        String json = JsonMapper.builder().build().writeValueAsString(new Email());
        assertTrue(json.contains("a****@example.com"), "普通用户邮箱应脱敏, 实际=" + json);
    }

    /** 邮箱脱敏 POJO */
    static class Email
    {
        @Sensitive(desensitizedType = DesensitizedType.EMAIL)
        public String getEmail()
        {
            return "abcdef@example.com";
        }
    }
}
