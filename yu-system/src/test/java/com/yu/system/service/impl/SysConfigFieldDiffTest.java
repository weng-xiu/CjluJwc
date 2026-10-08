package com.yu.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.common.utils.bean.FieldChange;
import com.yu.common.utils.bean.FieldDiffUtils;
import com.yu.system.domain.SysConfig;

/**
 * 参数配置字段级差异比对测试（A4/K1 配置项级审计的比对内核）。
 *
 * <p>直接对两个 {@link SysConfig} 实例调用 {@link FieldDiffUtils#diff}，验证 sys_config 接入
 * data_change_audit 所依赖的差异产出语义：改值留痕、无差不记、选择性更新（null 跳过）、
 * 跟踪字段收敛。刻意不经 Spring 上下文、不 mock RedisCache，避免既有内联 mock 环境级失败。</p>
 */
class SysConfigFieldDiffTest
{
    /** 与 SysConfigServiceImpl 保持一致的跟踪字段口径 */
    private static final Set<String> TRACKED = new HashSet<>(Arrays.asList(
            "configName", "configKey", "configValue", "configType"));

    private SysConfig persisted()
    {
        SysConfig old = new SysConfig();
        old.setConfigId(100L);
        old.setConfigName("验证码开关");
        old.setConfigKey("sys.account.captchaEnabled");
        old.setConfigValue("true");
        old.setConfigType("Y");
        return old;
    }

    @Test
    @DisplayName("改参数键值应产出 configValue 旧→新 的差异")
    void diff_detectsConfigValueChange()
    {
        SysConfig old = persisted();
        SysConfig neu = persisted();
        neu.setConfigValue("false");

        List<FieldChange> changes = FieldDiffUtils.diff(old, neu, TRACKED, Collections.emptySet());

        assertEquals(1, changes.size());
        FieldChange c = changes.get(0);
        assertEquals("configValue", c.getFieldName());
        assertEquals("true", c.getOldValue());
        assertEquals("false", c.getNewValue());
    }

    @Test
    @DisplayName("无字段变化时应返回空差异（不留痕）")
    void diff_noChange_returnsEmpty()
    {
        SysConfig old = persisted();
        SysConfig neu = persisted();

        List<FieldChange> changes = FieldDiffUtils.diff(old, neu, TRACKED, Collections.emptySet());

        assertTrue(changes.isEmpty());
    }

    @Test
    @DisplayName("多字段同时修改应逐条留痕（名称+键名+键值）")
    void diff_multipleFields()
    {
        SysConfig old = persisted();
        SysConfig neu = persisted();
        neu.setConfigName("登录验证码开关");
        neu.setConfigKey("sys.account.captcha");
        neu.setConfigValue("false");

        List<FieldChange> changes = FieldDiffUtils.diff(old, neu, TRACKED, Collections.emptySet());

        Set<String> names = new HashSet<>();
        for (FieldChange c : changes)
        {
            names.add(c.getFieldName());
        }
        assertEquals(3, changes.size());
        assertTrue(names.contains("configName"));
        assertTrue(names.contains("configKey"));
        assertTrue(names.contains("configValue"));
    }

    @Test
    @DisplayName("选择性更新语义：新值为 null 视为未提交，不记为改成空")
    void diff_nullNewValue_skipped()
    {
        SysConfig old = persisted();
        SysConfig neu = new SysConfig();
        neu.setConfigId(100L);
        // 仅提交 configValue，其余字段为 null（表单未提交）
        neu.setConfigValue("false");

        List<FieldChange> changes = FieldDiffUtils.diff(old, neu, TRACKED, Collections.emptySet());

        assertEquals(1, changes.size());
        assertEquals("configValue", changes.get(0).getFieldName());
    }

    @Test
    @DisplayName("非跟踪字段（如 configId）差异不纳入留痕")
    void diff_ignoresNonTrackedField()
    {
        SysConfig old = persisted();
        SysConfig neu = persisted();
        neu.setConfigId(999L); // 主键变化但不在 TRACKED 内

        List<FieldChange> changes = FieldDiffUtils.diff(old, neu, TRACKED, Collections.emptySet());

        assertTrue(changes.isEmpty());
        assertFalse(changes.stream().anyMatch(c -> "configId".equals(c.getFieldName())));
    }

    @Test
    @DisplayName("remark 属框架噪声列，即便不同也不留痕")
    void diff_remarkExcluded()
    {
        SysConfig old = persisted();
        old.setRemark("旧备注");
        SysConfig neu = persisted();
        neu.setRemark("新备注");

        List<FieldChange> changes = FieldDiffUtils.diff(old, neu, null, Collections.emptySet());

        assertFalse(changes.stream().anyMatch(c -> "remark".equals(c.getFieldName())));
    }
}
