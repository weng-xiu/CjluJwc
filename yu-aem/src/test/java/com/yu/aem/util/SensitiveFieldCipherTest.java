package com.yu.aem.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.common.utils.sign.SensitiveFieldCipher;

/**
 * 敏感字段静态加密工具测试（K3 国密：SM4-CBC + HMAC-SM3 原语，兼顾 legacy AES-GCM 兼容读取）。
 */
class SensitiveFieldCipherTest {

    private static final String KEY = "cjlu-test-key-2026";

    @Test
    @DisplayName("SM4 加密后可用同一密钥解密还原明文，且带国密前缀")
    void roundTrip() {
        String plain = "420101199001011234";
        String cipher = SensitiveFieldCipher.encrypt(plain, KEY);
        assertNotEquals(plain, cipher, "密文不应等于明文");
        assertTrue(cipher.startsWith("SM4:"), "新写入应为国密前缀形态");
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
    @DisplayName("错误密钥解密失败（HMAC-SM3 认证标签校验不通过）")
    void wrongKeyFails() {
        String cipher = SensitiveFieldCipher.encrypt("secret", KEY);
        assertThrows(IllegalStateException.class, () -> SensitiveFieldCipher.decrypt(cipher, "another-key"));
    }

    @Test
    @DisplayName("密文被篡改时解密失败（encrypt-then-MAC 防篡改）")
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

    @Test
    @DisplayName("legacy AES-GCM 无前缀密文仍可解密（灰度兼容读取）")
    void legacyAesGcmCipherStillReadable() throws Exception {
        String plain = "420101199001015678";
        // 按旧版原语手工构造 legacy 密文：base64(iv12 || ciphertext+tag)，密钥为 SHA-256(口令)
        byte[] iv = new byte[12];
        new SecureRandom().nextBytes(iv);
        byte[] k = MessageDigest.getInstance("SHA-256").digest(KEY.getBytes(StandardCharsets.UTF_8));
        Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(k, "AES"), new GCMParameterSpec(128, iv));
        byte[] ct = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
        byte[] out = new byte[iv.length + ct.length];
        System.arraycopy(iv, 0, out, 0, iv.length);
        System.arraycopy(ct, 0, out, iv.length, ct.length);
        String legacy = Base64.getEncoder().encodeToString(out);
        assertEquals(plain, SensitiveFieldCipher.decrypt(legacy, KEY), "无前缀密文应回退 legacy AES-GCM 路径");
    }
}
