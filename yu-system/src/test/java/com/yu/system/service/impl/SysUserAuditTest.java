package com.yu.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.redis.RedisCache;
import com.yu.common.utils.bean.FieldChange;
import com.yu.brm.service.IBrmTeacherService;
import com.yu.system.mapper.SysPostMapper;
import com.yu.system.mapper.SysRoleMapper;
import com.yu.system.mapper.SysUserMapper;
import com.yu.system.mapper.SysUserPostMapper;
import com.yu.system.mapper.SysUserRoleMapper;
import com.yu.system.service.IDataChangeAuditService;
import com.yu.system.service.ISysConfigService;
import com.yu.system.service.ISysDeptService;

/**
 * 系统用户（含教师整合）修改字段级留痕服务层测试（V4.1 §6.2 K1 合规③接线扩展）。
 *
 * <p>校验 SysUserServiceImpl.updateUser 在修改成功后，以「库中旧值 vs 提交新值」比对产出差异并写流水；
 * 手机号/邮箱等敏感字段旧/新值须脱敏后落库；无差异或更新 0 行时不留痕。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SysUserAuditTest {

    @Mock
    private SysUserMapper userMapper;
    @Mock
    private SysRoleMapper roleMapper;
    @Mock
    private SysPostMapper postMapper;
    @Mock
    private SysUserRoleMapper userRoleMapper;
    @Mock
    private SysUserPostMapper userPostMapper;
    @Mock
    private ISysConfigService configService;
    @Mock
    private ISysDeptService deptService;
    @Mock
    private RedisCache redisCache;
    @Mock
    private IBrmTeacherService brmTeacherService;
    @Mock
    private IDataChangeAuditService dataChangeAuditService;

    @InjectMocks
    private SysUserServiceImpl service;

    private SysUser oldUser() {
        SysUser old = new SysUser();
        old.setUserId(1L);
        old.setUserName("T2026001");
        old.setNickName("李教师");
        old.setPhonenumber("13800138000");
        old.setEmail("old@cjlu.edu.cn");
        old.setStatus("0");
        old.setUserCategory("student");
        old.setTeacherCode("T2026001");
        return old;
    }

    @Test
    @DisplayName("手机号变更 → 留痕且旧/新值脱敏，明文不落库")
    void updatePhoneAuditedMasked() {
        when(userMapper.selectUserById(1L)).thenReturn(oldUser());
        when(userMapper.updateUser(any(SysUser.class))).thenReturn(1);

        SysUser upd = new SysUser();
        upd.setUserId(1L);
        upd.setUserName("T2026001");
        upd.setUserCategory("student"); // 非 teacher，跳过教师同步分支
        upd.setPhonenumber("13900139000"); // 仅提交手机号变更

        int rows = service.updateUser(upd);
        assertEquals(1, rows);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FieldChange>> captor = ArgumentCaptor.forClass(List.class);
        verify(dataChangeAuditService).recordDiff(eq("sys_user"), eq("系统用户"), eq("1"), captor.capture());

        List<FieldChange> changes = captor.getValue();
        assertEquals(1, changes.size());
        FieldChange c = changes.get(0);
        assertEquals("phonenumber", c.getFieldName());
        assertEquals("手机号码", c.getFieldLabel(), "标签应来自 @Excel(name)");
        assertFalse(c.getOldValue().contains("80013800"), "旧值须脱敏");
        assertFalse(c.getNewValue().contains("90013900"), "新值须脱敏");
        assertTrue(c.getOldValue().startsWith("1") && c.getOldValue().endsWith("0"), "掩码保留首尾");
    }

    @Test
    @DisplayName("非敏感字段账号状态变更 → 明文留痕旧0新1")
    void updateStatusAuditedPlain() {
        when(userMapper.selectUserById(1L)).thenReturn(oldUser());
        when(userMapper.updateUser(any(SysUser.class))).thenReturn(1);

        SysUser upd = new SysUser();
        upd.setUserId(1L);
        upd.setUserName("T2026001");
        upd.setUserCategory("student");
        upd.setStatus("1"); // 正常→停用

        service.updateUser(upd);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<FieldChange>> captor = ArgumentCaptor.forClass(List.class);
        verify(dataChangeAuditService).recordDiff(eq("sys_user"), eq("系统用户"), eq("1"), captor.capture());

        List<FieldChange> changes = captor.getValue();
        assertEquals(1, changes.size());
        assertEquals("status", changes.get(0).getFieldName());
        assertEquals("0", changes.get(0).getOldValue());
        assertEquals("1", changes.get(0).getNewValue());
    }

    @Test
    @DisplayName("提交值与库中旧值一致 → 无差异，留空列表")
    void updateNoChangeProducesEmptyDiff() {
        when(userMapper.selectUserById(1L)).thenReturn(oldUser());
        when(userMapper.updateUser(any(SysUser.class))).thenReturn(1);

        SysUser upd = new SysUser();
        upd.setUserId(1L);
        upd.setUserName("T2026001");
        upd.setUserCategory("student");
        upd.setStatus("0");        // 与旧值相同
        upd.setNickName("李教师");  // 与旧值相同

        service.updateUser(upd);

        ArgumentCaptor<List<FieldChange>> captor = ArgumentCaptor.forClass(List.class);
        verify(dataChangeAuditService).recordDiff(eq("sys_user"), eq("系统用户"), eq("1"), captor.capture());
        assertTrue(captor.getValue().isEmpty(), "无差异应产出空列表");
    }

    @Test
    @DisplayName("更新影响 0 行（未真正修改）→ 不写流水")
    void updateZeroRowsSkipsAudit() {
        when(userMapper.selectUserById(anyLong())).thenReturn(oldUser());
        when(userMapper.updateUser(any(SysUser.class))).thenReturn(0);

        SysUser upd = new SysUser();
        upd.setUserId(1L);
        upd.setUserName("T2026001");
        upd.setUserCategory("student");
        upd.setStatus("1");

        service.updateUser(upd);

        verify(dataChangeAuditService, never()).recordDiff(any(), any(), any(), any());
    }
}
