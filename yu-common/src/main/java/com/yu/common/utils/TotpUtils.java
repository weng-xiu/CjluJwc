package com.yu.common.utils;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/**
 * 基于时间的一次性口令（TOTP，RFC 6238 / RFC 4226 HOTP 衍生）工具。
 *
 * <p>为 K3 MFA 二次鉴别提供纯 JDK 实现（HmacSHA1 + Base32），<b>不引入任何第三方依赖</b>，
 * 与主流身份验证器（Google Authenticator、Microsoft Authenticator、FreeOTP 等）默认参数互通：
 * 算法 SHA1、位数 6、周期 30 秒。校验时默认允许前后各 1 个周期的时钟漂移窗口，
 * 以容忍客户端与服务器的时钟不同步。</p>
 *
 * @author cjlu
 */
public class TotpUtils
{
    /** 默认口令位数 */
    public static final int DIGITS = 6;

    /** 默认时间周期（秒） */
    public static final int PERIOD_SECONDS = 30;

    /** 默认密钥字节数（20 字节 = 160 位，匹配 SHA1 输出，业界推荐） */
    private static final int SECRET_BYTES = 20;

    /** RFC 4648 Base32 字母表（无填充） */
    private static final char[] BASE32_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567".toCharArray();

    /** Base32 反查表：'A'..'Z','2'..'7' -> 0..31，其余为 -1 */
    private static final int[] BASE32_LOOKUP = new int[128];

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    static
    {
        java.util.Arrays.fill(BASE32_LOOKUP, -1);
        for (int i = 0; i < BASE32_CHARS.length; i++)
        {
            BASE32_LOOKUP[BASE32_CHARS[i]] = i;
        }
    }

    /**
     * 生成一个随机的 Base32 密钥（供绑定使用，返回字符串形态即身份验证器录入的 Secret）。
     *
     * @return Base32 编码的随机密钥
     */
    public static String generateSecret()
    {
        byte[] bytes = new byte[SECRET_BYTES];
        SECURE_RANDOM.nextBytes(bytes);
        return base32Encode(bytes);
    }

    /**
     * 计算指定时刻的 TOTP 口令。
     *
     * @param base32Secret Base32 密钥
     * @return 6 位口令（可能含前导零）
     */
    public static String getCode(String base32Secret)
    {
        return getCode(base32Secret, System.currentTimeMillis() / 1000L);
    }

    /**
     * 计算指定 Unix 秒的 TOTP 口令（便于测试与自定义时钟）。
     *
     * @param base32Secret Base32 密钥
     * @param unixSeconds  Unix 时间戳（秒）
     * @return 6 位口令
     */
    public static String getCode(String base32Secret, long unixSeconds)
    {
        byte[] key = base32Decode(base32Secret);
        long counter = unixSeconds / PERIOD_SECONDS;
        byte[] counterBytes = new byte[8];
        for (int i = 7; i >= 0; i--)
        {
            counterBytes[i] = (byte) (counter & 0xff);
            counter >>>= 8;
        }
        int hash;
        try
        {
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(key, "HmacSHA1"));
            byte[] digest = mac.doFinal(counterBytes);
            int offset = digest[digest.length - 1] & 0x0f;
            hash = ((digest[offset] & 0x7f) << 24)
                    | ((digest[offset + 1] & 0xff) << 16)
                    | ((digest[offset + 2] & 0xff) << 8)
                    | (digest[offset + 3] & 0xff);
        }
        catch (GeneralSecurityException e)
        {
            throw new IllegalStateException("TOTP 计算失败", e);
        }
        int otp = hash % (int) Math.pow(10, DIGITS);
        return String.format("%0" + DIGITS + "d", otp);
    }

    /**
     * 校验 TOTP 口令，默认允许前后各 1 个周期的时钟漂移。
     *
     * @param base32Secret Base32 密钥
     * @param code         用户提交的口令
     * @return 是否匹配
     */
    public static boolean verify(String base32Secret, String code)
    {
        return verify(base32Secret, code, 1);
    }

    /**
     * 校验 TOTP 口令。
     *
     * @param base32Secret Base32 密钥
     * @param code         用户提交的口令
     * @param window       允许向前/向后漂移的周期数（0 表示仅当前周期）
     * @return 是否匹配
     */
    public static boolean verify(String base32Secret, String code, int window)
    {
        if (StringUtils.isEmpty(base32Secret) || StringUtils.isEmpty(code))
        {
            return false;
        }
        String normalized = code.trim();
        long currentStep = System.currentTimeMillis() / 1000L / PERIOD_SECONDS;
        for (long i = -window; i <= window; i++)
        {
            String expected = getCode(base32Secret, (currentStep + i) * PERIOD_SECONDS);
            if (constantTimeEquals(expected, normalized))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * 构造 {@code otpauth://} 配置 URI，供身份验证器扫码/手动录入。
     *
     * @param issuer   发行方（一般为系统名，如 "长江大学教务系统"）
     * @param account  账户标识（一般为用户名）
     * @param base32Secret Base32 密钥
     * @return otpauth URI
     */
    public static String buildOtpauthUri(String issuer, String account, String base32Secret)
     {
        String label = urlEncode(issuer) + ":" + urlEncode(account);
        return "otpauth://totp/" + label
                + "?secret=" + base32Secret
                + "&issuer=" + urlEncode(issuer)
                + "&algorithm=SHA1&digits=" + DIGITS + "&period=" + PERIOD_SECONDS;
    }

    // ------------------------------------------------------------------------
    // 内部工具
    // ------------------------------------------------------------------------

    /** 定长时间比较，避免口令校验被时序侧信道探测 */
    private static boolean constantTimeEquals(String a, String b)
    {
        if (a == null || b == null || a.length() != b.length())
        {
            return false;
        }
        int diff = 0;
        for (int i = 0; i < a.length(); i++)
        {
            diff |= a.charAt(i) ^ b.charAt(i);
        }
        return diff == 0;
    }

    /** RFC 4648 Base32 编码（无填充） */
    public static String base32Encode(byte[] data)
    {
        StringBuilder sb = new StringBuilder();
        int buffer = 0;
        int bitsLeft = 0;
        for (byte b : data)
        {
            buffer = (buffer << 8) | (b & 0xff);
            bitsLeft += 8;
            while (bitsLeft >= 5)
            {
                int index = (buffer >> (bitsLeft - 5)) & 0x1f;
                bitsLeft -= 5;
                sb.append(BASE32_CHARS[index]);
            }
        }
        if (bitsLeft > 0)
        {
            int index = (buffer << (5 - bitsLeft)) & 0x1f;
            sb.append(BASE32_CHARS[index]);
        }
        return sb.toString();
    }

    /** RFC 4648 Base32 解码（忽略填充与分隔符，大小写不敏感） */
    public static byte[] base32Decode(String secret)
    {
        String s = secret.trim().toUpperCase().replace("=", "").replace(" ", "").replace("-", "");
        int buffer = 0;
        int bitsLeft = 0;
        byte[] out = new byte[s.length() * 5 / 8 + 1];
        int count = 0;
        for (int i = 0; i < s.length(); i++)
        {
            char c = s.charAt(i);
            int val = c < 128 ? BASE32_LOOKUP[c] : -1;
            if (val < 0)
            {
                throw new IllegalArgumentException("非法的 Base32 字符: " + c);
            }
            buffer = (buffer << 5) | val;
            bitsLeft += 5;
            if (bitsLeft >= 8)
            {
                out[count++] = (byte) ((buffer >> (bitsLeft - 8)) & 0xff);
                bitsLeft -= 8;
            }
        }
        byte[] result = new byte[count];
        System.arraycopy(out, 0, result, 0, count);
        return result;
    }

    private static String urlEncode(String value)
    {
        try
        {
            return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8.name())
                    .replace("+", "%20");
        }
        catch (Exception e)
        {
            return value;
        }
    }
}
