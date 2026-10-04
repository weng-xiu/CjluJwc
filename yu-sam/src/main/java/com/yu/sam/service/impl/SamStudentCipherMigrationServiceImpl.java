package com.yu.sam.service.impl;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.yu.common.exception.ServiceException;
import com.yu.common.utils.StringUtils;
import com.yu.common.utils.sign.SensitiveFieldCipher;
import com.yu.sam.domain.SamStudent;
import com.yu.sam.mapper.SamStudentMapper;
import com.yu.sam.service.ISamStudentCipherMigrationService;

/**
 * 身份证号列加密灰度迁移服务实现（V4.1 §6.2 K1 合规②）。
 *
 * <p>加解密原语复用 {@link SensitiveFieldCipher}（AES-256/GCM）。密钥来自配置
 * {@code data.encrypt.key}（应由密钥库/环境变量注入，禁止入库、禁止硬编码默认值）。</p>
 *
 * @author yu
 */
@Service
public class SamStudentCipherMigrationServiceImpl implements ISamStudentCipherMigrationService
{
    @Autowired
    private SamStudentMapper samStudentMapper;

    /** 灰度开关：默认关闭，未开启前绝不触碰密文列（存量库可能尚未应用 V20261004.3 DDL） */
    @Value("${data.encrypt.enabled:false}")
    private boolean enabled;

    /** 加密密钥：来自环境变量/密钥库，默认空（空则拒绝加密操作，避免用弱默认密钥加密后无法解密） */
    @Value("${data.encrypt.key:}")
    private String key;

    @Override
    public boolean isEnabled()
    {
        return enabled;
    }

    @Override
    public int backfill(int limit)
    {
        if (!enabled)
        {
            return 0;
        }
        if (StringUtils.isBlank(key))
        {
            throw new ServiceException("未配置 data.encrypt.key，无法执行身份证列加密回填");
        }
        List<SamStudent> pending = samStudentMapper.selectStudentsForCipherBackfill(limit);
        if (pending == null || pending.isEmpty())
        {
            return 0;
        }
        int migrated = 0;
        for (SamStudent student : pending)
        {
            if (StringUtils.isBlank(student.getIdCard()))
            {
                continue;
            }
            String cipher = SensitiveFieldCipher.encrypt(student.getIdCard(), key);
            // 仅写密文列，保留明文列，任何时刻可回滚
            migrated += samStudentMapper.updateStudentIdCardCipher(student.getStudentId(), cipher);
        }
        return migrated;
    }

    @Override
    public String resolveIdCard(Long studentId)
    {
        // 明文列读取走原有查询（resultMap 不含密文列，存量库无该列亦安全）
        SamStudent student = samStudentMapper.selectSamStudentByStudentId(studentId);
        if (student == null)
        {
            return null;
        }
        if (!enabled || StringUtils.isBlank(key))
        {
            return student.getIdCard();
        }
        // 灰度开启后才会查询密文列；密文优先、缺失或解密失败回退明文
        String cipher = samStudentMapper.selectIdCardCipherByStudentId(studentId);
        if (StringUtils.isNotBlank(cipher))
        {
            try
            {
                String plain = SensitiveFieldCipher.decrypt(cipher, key);
                if (StringUtils.isNotBlank(plain))
                {
                    return plain;
                }
            }
            catch (Exception ignore)
            {
                // 密钥不符/密文损坏 → 回退明文列，保证读路径不因迁移而失败
            }
        }
        return student.getIdCard();
    }
}
