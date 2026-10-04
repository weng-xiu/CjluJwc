package com.yu.sam.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
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
import org.springframework.test.util.ReflectionTestUtils;

import com.yu.common.exception.ServiceException;
import com.yu.common.utils.sign.SensitiveFieldCipher;
import com.yu.sam.domain.SamStudent;
import com.yu.sam.mapper.SamStudentMapper;

/**
 * 身份证号列加密灰度迁移服务测试（V4.1 §6.2 K1 合规②）。
 *
 * <p>校验灰度开关门控、非破坏性回填（只写密文列、保留明文）、双读优先取密文与解密失败回退明文。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SamStudentCipherMigrationServiceTest {

    private static final String KEY = "cjlu-test-key-2026";

    @Mock
    private SamStudentMapper samStudentMapper;

    @InjectMocks
    private SamStudentCipherMigrationServiceImpl service;

    private SamStudent student(long id, String idCard) {
        SamStudent s = new SamStudent();
        s.setStudentId(id);
        s.setIdCard(idCard);
        return s;
    }

    @Test
    @DisplayName("灰度未开启 → 回填直接返回 0 且不触碰密文列")
    void backfillDisabledNoOp() {
        ReflectionTestUtils.setField(service, "enabled", false);
        ReflectionTestUtils.setField(service, "key", KEY);

        assertEquals(0, service.backfill(100));
        verify(samStudentMapper, never()).selectStudentsForCipherBackfill(anyInt());
        verify(samStudentMapper, never()).updateStudentIdCardCipher(any(), any());
    }

    @Test
    @DisplayName("灰度开启但密钥缺失 → 回填拒绝执行并抛异常")
    void backfillMissingKeyThrows() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "key", "");

        assertThrows(ServiceException.class, () -> service.backfill(100));
        verify(samStudentMapper, never()).updateStudentIdCardCipher(any(), any());
    }

    @Test
    @DisplayName("灰度开启 → 非破坏性回填：密文列写入可解密还原的值，跳过空明文行")
    void backfillEncryptsNonDestructively() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "key", KEY);

        List<SamStudent> pending = new ArrayList<>();
        pending.add(student(1L, "420101199001011234"));
        pending.add(student(2L, "")); // 空明文跳过
        when(samStudentMapper.selectStudentsForCipherBackfill(100)).thenReturn(pending);
        when(samStudentMapper.updateStudentIdCardCipher(eq(1L), any())).thenReturn(1);

        int migrated = service.backfill(100);
        assertEquals(1, migrated);

        ArgumentCaptor<String> cipherCap = ArgumentCaptor.forClass(String.class);
        verify(samStudentMapper).updateStudentIdCardCipher(eq(1L), cipherCap.capture());
        String cipher = cipherCap.getValue();
        assertNotEquals("420101199001011234", cipher, "写入的应是密文而非明文");
        assertEquals("420101199001011234", SensitiveFieldCipher.decrypt(cipher, KEY), "密文应可还原为原明文");
        verify(samStudentMapper, never()).updateStudentIdCardCipher(eq(2L), any());
    }

    @Test
    @DisplayName("双读 → 密文列有值时解密返回明文")
    void resolvePrefersCipher() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "key", KEY);

        String cipher = SensitiveFieldCipher.encrypt("420101199001015678", KEY);
        when(samStudentMapper.selectSamStudentByStudentId(1L)).thenReturn(student(1L, "420101199001015678"));
        when(samStudentMapper.selectIdCardCipherByStudentId(1L)).thenReturn(cipher);

        assertEquals("420101199001015678", service.resolveIdCard(1L));
    }

    @Test
    @DisplayName("双读 → 密文列缺失时回退明文列")
    void resolveFallsBackToPlain() {
        ReflectionTestUtils.setField(service, "enabled", true);
        ReflectionTestUtils.setField(service, "key", KEY);

        when(samStudentMapper.selectSamStudentByStudentId(1L)).thenReturn(student(1L, "420101199001011234"));
        when(samStudentMapper.selectIdCardCipherByStudentId(1L)).thenReturn(null);

        assertEquals("420101199001011234", service.resolveIdCard(1L));
    }

    @Test
    @DisplayName("双读 → 灰度未开启时不查密文列，直接返回明文；学生不存在返回 null")
    void resolveDisabledReturnsPlain() {
        ReflectionTestUtils.setField(service, "enabled", false);
        ReflectionTestUtils.setField(service, "key", KEY);

        when(samStudentMapper.selectSamStudentByStudentId(1L)).thenReturn(student(1L, "420101199001011234"));
        assertEquals("420101199001011234", service.resolveIdCard(1L));
        verify(samStudentMapper, never()).selectIdCardCipherByStudentId(any());

        when(samStudentMapper.selectSamStudentByStudentId(99L)).thenReturn(null);
        assertNull(service.resolveIdCard(99L));
    }
}
