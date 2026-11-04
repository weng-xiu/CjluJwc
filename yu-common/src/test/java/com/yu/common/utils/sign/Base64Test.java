package com.yu.common.utils.sign;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * G5：{@link Base64} 的契约测试（137 行、零覆盖）。
 *
 * <p>这是若依自带的 Base64 实现，不是 {@code java.util.Base64} 的别名 —— 它被
 * {@code Base64Encoder}/{@code EncryptUtils} 等签名链路直接调用。一旦它对填充位、
 * 空输入的处理跟标准库分叉，签名校验就会在跨端联调时才暴露。故这里用标准库的
 * 期望值反向钉住它。</p>
 */
class Base64Test
{
    @Test
    @DisplayName("encode：与标准 Base64 完全一致（含 = 填充）")
    void encode()
    {
        assertEquals("aGVsbG8=", Base64.encode("hello".getBytes(StandardCharsets.UTF_8)));
        assertEquals("aGk=", Base64.encode("hi".getBytes(StandardCharsets.UTF_8)), "余 1 字节要用一个 = 补齐");
        assertEquals("aGVs", Base64.encode("hel".getBytes(StandardCharsets.UTF_8)), "整 3 字节不需要填充");
        assertEquals("", Base64.encode(new byte[0]), "空数组编码成空串而不是 null");
    }

    @Test
    @DisplayName("decode：还原字节，空串返回空数组")
    void decode()
    {
        assertArrayEquals("hello".getBytes(StandardCharsets.UTF_8), Base64.decode("aGVsbG8="));
        assertArrayEquals("hel".getBytes(StandardCharsets.UTF_8), Base64.decode("aGVs"));
        assertArrayEquals(new byte[0], Base64.decode(""), "空串解码不能是 null，否则下游 NPE");
    }

    @Test
    @DisplayName("回环：中文与二进制任意长度都能无损还原")
    void roundTrip()
    {
        String[] samples = { "中", "长江大学教务系统", "a", "ab", "abc", "abcd" };
        for (String s : samples)
        {
            byte[] raw = s.getBytes(StandardCharsets.UTF_8);
            assertArrayEquals(raw, Base64.decode(Base64.encode(raw)), "回环失败：" + s);
        }

        byte[] binary = new byte[256];
        for (int i = 0; i < binary.length; i++)
        {
            binary[i] = (byte) i;
        }
        assertArrayEquals(binary, Base64.decode(Base64.encode(binary)), "全字节域回环失败");
    }
}
