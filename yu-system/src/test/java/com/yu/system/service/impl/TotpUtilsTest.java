package com.yu.system.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.common.utils.TotpUtils;

/**
 * {@link TotpUtils} 单元测试，使用 RFC 6238 官方测试向量（ASCII 密钥 "12345678901234567890"）验证算法正确性。
 *
 * @author cjlu
 */
class TotpUtilsTest
{
    /** RFC 6238 附录 B 的种子密钥（ASCII），其 Base32 表示如下 */
    private static final String RFC_SECRET = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";

    @Test
    @DisplayName("Base32 编解码往返一致，且与 RFC 6238 种子密钥匹配")
    void base32RoundTrip()
    {
        byte[] seed = "12345678901234567890".getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        assertEquals(RFC_SECRET, TotpUtils.base32Encode(seed));
        String decoded = new String(TotpUtils.base32Decode(RFC_SECRET), java.nio.charset.StandardCharsets.US_ASCII);
        assertEquals("12345678901234567890", decoded);
    }

    @Test
    @DisplayName(" getCode 命中 RFC 6238 官方向量（SHA1 / 6 位）")
    void getCodeMatchesRfc6238Vectors()
    {
        // 官方 8 位向量截断为 6 位：94287082 -> 287082、14050471 -> 050471 等
        assertEquals("287082", TotpUtils.getCode(RFC_SECRET, 59L));
        assertEquals("081804", TotpUtils.getCode(RFC_SECRET, 1111111109L));
        assertEquals("050471", TotpUtils.getCode(RFC_SECRET, 1111111111L));
        assertEquals("005924", TotpUtils.getCode(RFC_SECRET, 1234567890L));
        assertEquals("279037", TotpUtils.getCode(RFC_SECRET, 2000000000L));
        assertEquals("353130", TotpUtils.getCode(RFC_SECRET, 20000000000L));
    }

    @Test
    @DisplayName("随机密钥长度合法且两次生成不同")
    void generateSecretIsRandomAndValid()
    {
        String s1 = TotpUtils.generateSecret();
        String s2 = TotpUtils.generateSecret();
        assertFalse(s1.isEmpty());
        assertFalse(s1.equals(s2));
        // 生成的密钥应可被解码（纯 Base32 字符）
        assertTrue(TotpUtils.base32Decode(s1).length >= 16);
    }

    @Test
    @DisplayName("otpauth URI 结构完整")
    void otpauthUriWellFormed()
    {
        String uri = TotpUtils.buildOtpauthUri("CJLU-JWC", "admin", RFC_SECRET);
        assertTrue(uri.startsWith("otpauth://totp/"));
        assertTrue(uri.contains("secret=" + RFC_SECRET));
        assertTrue(uri.contains("issuer=CJLU-JWC"));
        assertTrue(uri.contains("algorithm=SHA1"));
        assertTrue(uri.contains("digits=6"));
    }

    @Test
    @DisplayName("verify 对空/非法口令返回 false")
    void verifyRejectsBlank()
    {
        assertFalse(TotpUtils.verify(RFC_SECRET, null));
        assertFalse(TotpUtils.verify(RFC_SECRET, ""));
        assertFalse(TotpUtils.verify(null, "123456"));
    }

    @Test
    @DisplayName("verify 命中当前周期口令")
    void verifyAcceptsCurrentCode()
    {
        String now = TotpUtils.getCode(RFC_SECRET);
        assertTrue(TotpUtils.verify(RFC_SECRET, now));
    }
}
