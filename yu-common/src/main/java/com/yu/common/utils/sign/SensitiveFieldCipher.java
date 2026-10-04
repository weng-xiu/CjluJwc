package com.yu.common.utils.sign;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

/**
 * 敏感字段静态加密工具（K1 合规②：重要字段加密存储的基础密码学原语）。
 *
 * <p>采用 <b>AES-256/GCM/NoPadding</b> 认证加密：随机 12 字节 IV + 密文 + 16 字节认证标签，
 * 输出统一 Base64 编码（{@code base64(iv || ciphertext||tag)}）。GCM 相比 CBC 提供完整性校验，
 * 篡改密文会解密失败，符合等保对「密码技术」的要求。密钥以任意长度口令经 SHA-256 归一为
 * 32 字节 AES 密钥，实际部署密钥应来自密钥库/环境变量（如 {@code DATA_ENCRYPT_KEY}），禁止入库。</p>
 *
 * <p>本类仅提供无状态原语，列加密的灰度迁移（新增密文列 + 双读）由具体业务在改造时接入，
 * 不在工具层隐式改写存量数据。</p>
 *
 * @author yu
 */
public class SensitiveFieldCipher
{
    /** GCM 推荐 IV 长度：12 字节 */
    private static final int IV_LENGTH = 12;

    /** GCM 认证标签长度：128 bit */
    private static final int TAG_LENGTH_BIT = 128;

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";

    private static final SecureRandom RANDOM = new SecureRandom();

    private SensitiveFieldCipher()
    {
    }

    /**
     * 由任意长度口令派生 256 bit AES 密钥（SHA-256）。
     */
    private static SecretKeySpec deriveKey(String key)
    {
        if (key == null || key.isEmpty())
        {
            throw new IllegalArgumentException("加密密钥不能为空");
        }
        try
        {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] k = sha.digest(key.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(k, "AES");
        }
        catch (Exception e)
        {
            throw new IllegalStateException("AES 密钥派生失败: " + e.getMessage(), e);
        }
    }

    /**
     * 加密：返回 Base64(iv || ciphertext||tag)。明文为空时原样返回。
     */
    public static String encrypt(String plainText, String key)
    {
        if (key == null || key.isEmpty())
        {
            throw new IllegalArgumentException("加密密钥不能为空");
        }
        if (plainText == null || plainText.isEmpty())
        {
            return plainText;
        }
        try
        {
            byte[] iv = new byte[IV_LENGTH];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, deriveKey(key), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            byte[] cipherText = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] out = new byte[iv.length + cipherText.length];
            System.arraycopy(iv, 0, out, 0, iv.length);
            System.arraycopy(cipherText, 0, out, iv.length, cipherText.length);
            return Base64.getEncoder().encodeToString(out);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("字段加密失败: " + e.getMessage(), e);
        }
    }

    /**
     * 解密 encrypt 产物。入参为空时原样返回。
     */
    public static String decrypt(String cipherTextBase64, String key)
    {
        if (key == null || key.isEmpty())
        {
            throw new IllegalArgumentException("解密密钥不能为空");
        }
        if (cipherTextBase64 == null || cipherTextBase64.isEmpty())
        {
            return cipherTextBase64;
        }
        try
        {
            byte[] all = Base64.getDecoder().decode(cipherTextBase64);
            if (all.length <= IV_LENGTH)
            {
                throw new IllegalArgumentException("密文长度非法");
            }
            byte[] iv = new byte[IV_LENGTH];
            System.arraycopy(all, 0, iv, 0, IV_LENGTH);
            byte[] cipherText = new byte[all.length - IV_LENGTH];
            System.arraycopy(all, IV_LENGTH, cipherText, 0, cipherText.length);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(key), new GCMParameterSpec(TAG_LENGTH_BIT, iv));
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("字段解密失败: " + e.getMessage(), e);
        }
    }
}
