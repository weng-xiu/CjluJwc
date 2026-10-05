package com.yu.common.utils.sign;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bouncycastle.jce.provider.BouncyCastleProvider;

/**
 * 敏感字段静态加密工具（K1 合规②：重要字段加密存储的基础密码学原语）。
 *
 * <p><b>国密 K3：主算法为 SM4/CBC/PKCS7Padding + HMAC-SM3（encrypt-then-MAC）</b>——
 * 随机 16 字节 IV + 32 字节 SM3 认证标签 + SM4 密文，输出 {@code "SM4:" + base64(iv || mac || ciphertext)}。
 * SM4/SM3 由 BouncyCastle 提供者实现（单 jar 零传递依赖）。旧版 AES-256/GCM 密文（无前缀的
 * Base64 形态）仍可通过 {@link #decrypt} 读取，保证灰度兼容；新写入一律使用国密。</p>
 *
 * <p>密钥以任意长度口令经 SHA-256 域分隔派生（加密密钥取前 16 字节，MAC 密钥为
 * SHA-256("mac||"||口令)），实际部署密钥应来自密钥库/环境变量（如 {@code DATA_ENCRYPT_KEY}），禁止入库。</p>
 *
 * <p>本类仅提供无状态原语，列加密的灰度迁移（新增密文列 + 双读）由具体业务在改造时接入，
 * 不在工具层隐式改写存量数据。</p>
 *
 * @author yu
 */
public class SensitiveFieldCipher
{
    /** 国密密文前缀（区别于 legacy AES-GCM 的无前缀 Base64 形态） */
    private static final String SM4_PREFIX = "SM4:";

    /** SM4 分组长度 = IV 长度：16 字节 */
    private static final int IV_LENGTH = 16;

    /** HMAC-SM3 认证标签长度：32 字节 */
    private static final int MAC_LENGTH = 32;

    private static final String SM4_TRANSFORMATION = "SM4/CBC/PKCS7Padding";

    private static final String SM3_HMAC_ALGORITHM = "HMACSM3";

    /** legacy 原语：AES-256/GCM（仅保留解密路径以兼容存量密文） */
    private static final String LEGACY_TRANSFORMATION = "AES/GCM/NoPadding";

    private static final int LEGACY_IV_LENGTH = 12;

    private static final int LEGACY_TAG_LENGTH_BIT = 128;

    private static final SecureRandom RANDOM = new SecureRandom();

    /** 按需注册 BC 提供者（幂等，JDK 无 SM4/SM3 实现） */
    static
    {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null)
        {
            Security.addProvider(new BouncyCastleProvider());
        }
    }

    private SensitiveFieldCipher()
    {
    }

    /** 由口令派生 SM4 密钥（SHA-256 前 16 字节，域分隔避免与 MAC 密钥同值） */
    private static SecretKeySpec deriveSm4Key(String key) throws Exception
    {
        byte[] h = MessageDigest.getInstance("SHA-256").digest(("enc||" + key).getBytes(StandardCharsets.UTF_8));
        byte[] k = new byte[16];
        System.arraycopy(h, 0, k, 0, 16);
        return new SecretKeySpec(k, "SM4");
    }

    /** 由口令派生 HMAC-SM3 密钥（encrypt-then-MAC 独立密钥） */
    private static SecretKeySpec deriveMacKey(String key) throws Exception
    {
        byte[] k = MessageDigest.getInstance("SHA-256").digest(("mac||" + key).getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(k, SM3_HMAC_ALGORITHM);
    }

    /** 计算 HMAC-SM3(iv || ciphertext) */
    private static byte[] mac(String key, byte[] iv, byte[] cipherText) throws Exception
    {
        Mac mac = Mac.getInstance(SM3_HMAC_ALGORITHM, BouncyCastleProvider.PROVIDER_NAME);
        mac.init(deriveMacKey(key));
        byte[] input = new byte[iv.length + cipherText.length];
        System.arraycopy(iv, 0, input, 0, iv.length);
        System.arraycopy(cipherText, 0, input, iv.length, cipherText.length);
        return mac.doFinal(input);
    }

    /**
     * legacy 密钥派生：SHA-256 归一 32 字节 AES 密钥（仅解密路径使用）。
     */
    private static SecretKeySpec deriveAesKey(String key) throws Exception
    {
        byte[] k = MessageDigest.getInstance("SHA-256").digest(key.getBytes(StandardCharsets.UTF_8));
        return new SecretKeySpec(k, "AES");
    }

    private static void requireKey(String key)
    {
        if (key == null || key.isEmpty())
        {
            throw new IllegalArgumentException("加密密钥不能为空");
        }
    }

    /**
     * 加密：返回 {@code "SM4:" + base64(iv || mac || ciphertext)}。明文为空时原样返回。
     */
    public static String encrypt(String plainText, String key)
    {
        requireKey(key);
        if (plainText == null || plainText.isEmpty())
        {
            return plainText;
        }
        try
        {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(SM4_TRANSFORMATION, BouncyCastleProvider.PROVIDER_NAME);
            cipher.init(Cipher.ENCRYPT_MODE, deriveSm4Key(key), new IvParameterSpec(iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] tag = mac(key, iv, cipherText);
            byte[] out = new byte[iv.length + tag.length + cipherText.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(tag, 0, out, iv.length, tag.length);
            System.arraycopy(cipherText, 0, out, iv.length + tag.length, cipherText.length);
            return SM4_PREFIX + Base64.getEncoder().encodeToString(out);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("字段加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解密 encrypt 产物：SM4 前缀走国密路径（先验 MAC 再解密），无前缀回退 legacy AES-GCM。
     * 入参为空时原样返回。
     */
    public static String decrypt(String cipherTextBase64, String key)
    {
        requireKey(key);
        if (cipherTextBase64 == null || cipherTextBase64.isEmpty())
        {
            return cipherTextBase64;
        }
        try
        {
            if (cipherTextBase64.startsWith(SM4_PREFIX))
            {
                return decryptSm4(cipherTextBase64.substring(SM4_PREFIX.length()), key);
            }
            return decryptAesGcmLegacy(cipherTextBase64, key);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("字段解密失败: " + e.getMessage(), e);
        }
    }

    /** 国密路径：base64(iv || mac || ciphertext)，HMAC-SM3 认证失败即拒绝（防篡改/防错误密钥） */
    private static String decryptSm4(String body, String key) throws Exception
    {
        byte[] all = Base64.getDecoder().decode(body);
        if (all.length <= IV_LENGTH + MAC_LENGTH)
        {
            throw new IllegalArgumentException("密文长度非法");
        }
        byte[] iv = new byte[IV_LENGTH];
        byte[] tag = new byte[MAC_LENGTH];
        byte[] cipherText = new byte[all.length - IV_LENGTH - MAC_LENGTH];
        System.arraycopy(all, 0, iv, 0, IV_LENGTH);
        System.arraycopy(all, IV_LENGTH, tag, 0, MAC_LENGTH);
        System.arraycopy(all, IV_LENGTH + MAC_LENGTH, cipherText, 0, cipherText.length);
        byte[] expect = mac(key, iv, cipherText);
        if (!MessageDigest.isEqual(tag, expect))
        {
            throw new IllegalArgumentException("认证标签校验失败");
        }
        Cipher cipher = Cipher.getInstance(SM4_TRANSFORMATION, BouncyCastleProvider.PROVIDER_NAME);
        cipher.init(Cipher.DECRYPT_MODE, deriveSm4Key(key), new IvParameterSpec(iv));
        return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    }

    /** legacy 路径：AES-256/GCM 无前缀密文读取（灰度兼容，不再用于新写入） */
    private static String decryptAesGcmLegacy(String cipherTextBase64, String key) throws Exception
    {
        byte[] all = Base64.getDecoder().decode(cipherTextBase64);
        if (all.length <= LEGACY_IV_LENGTH)
        {
            throw new IllegalArgumentException("密文长度非法");
        }
        byte[] iv = new byte[LEGACY_IV_LENGTH];
        System.arraycopy(all, 0, iv, 0, LEGACY_IV_LENGTH);
        byte[] cipherText = new byte[all.length - LEGACY_IV_LENGTH];
        System.arraycopy(all, LEGACY_IV_LENGTH, cipherText, 0, cipherText.length);
        Cipher cipher = Cipher.getInstance(LEGACY_TRANSFORMATION);
        cipher.init(Cipher.DECRYPT_MODE, deriveAesKey(key), new GCMParameterSpec(LEGACY_TAG_LENGTH_BIT, iv));
        return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
    }
}
