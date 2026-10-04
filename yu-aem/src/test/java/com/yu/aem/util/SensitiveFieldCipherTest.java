package com.yu.aem.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.common.utils.sign.SensitiveFieldCipher;

/**
 * 敏感字段静态加密工具测试（V4.0 K1 合规②：AES-GCM 原语）。
 */
class SensitiveFieldCipherTest {

    private static final String KEY = "cjlu-test-key-2026";

    @Test
    @DisplayName("加密后可用同一密钥解密还原明文")
    void roundTrip() {
        String plain = "420101199001011234";
        String cipher = SensitiveFieldCipher.encrypt(plain, KEY);
        assertNotEquals(plain, cipher, "密文不应等于明文");
        assertEquals(plain, SensitiveFieldCipher.decrypt(cipher, KEY));
    }

    @Test
    @DisplayName("每次加密使用随机 IV，同一明文密文不同但均可解密")
    void randomIvProducesDifferentCiphertext() {
        String plain = "13800138000";
        String c1 = SensitiveFieldCipher.encrypt(plain, KEY);
        String c2 = SensitiveFieldCipher.encrypt(plain, KEY);
        assertNotEquals(c1, c2, "随机 IV 应使两次密文不同");
        assertEquals(plain, SensitiveFieldCipher.decrypt(c1, KEY));
        assertEquals(plain, SensitiveFieldCipher.decrypt(c2, KEY));
    }

    @Test
    @DisplayName("错误密钥解密失败（GCM 认证标签校验不通过）")
    void wrongKeyFails() {
        String cipher = SensitiveFieldCipher.encrypt("secret", KEY);
        assertThrows(IllegalStateException.class, () -> SensitiveFieldCipher.decrypt(cipher, "another-key"));
    }

    @Test
    @DisplayName("密文被篡改时解密失败（认证加密防篡改）")
    void tamperDetected() {
        String cipher = SensitiveFieldCipher.encrypt("important-data", KEY);
        char[] arr = cipher.toCharArray();
        arr[arr.length - 3] = arr[arr.length - 3] == 'A' ? 'B' : 'A';
        String tampered = new String(arr);
        assertThrows(IllegalStateException.class, () -> SensitiveFieldCipher.decrypt(tampered, KEY));
    }

    @Test
    @DisplayName("空值原样返回，null/empty 不加密")
    void nullAndEmptyPassthrough() {
        assertNull(SensitiveFieldCipher.encrypt(null, KEY));
        assertEquals("", SensitiveFieldCipher.encrypt("", KEY));
        assertNull(SensitiveFieldCipher.decrypt(null, KEY));
        assertEquals("", SensitiveFieldCipher.decrypt("", KEY));
    }

    @Test
    @DisplayName("空密钥调用加密抛异常")
    void blankKeyRejected() {
        assertThrows(IllegalArgumentException.class, () -> SensitiveFieldCipher.encrypt("x", ""));
    }
}
