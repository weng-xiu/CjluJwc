package com.yu.sam.mapper;

import java.util.List;
import com.yu.sam.domain.SamCertificate;

/**
 * 证书编号管理Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-13
 */
public interface SamCertificateMapper 
{
    public SamCertificate selectSamCertificateByCertId(Long certId);
    public List<SamCertificate> selectSamCertificateList(SamCertificate samCertificate);
    public int insertSamCertificate(SamCertificate samCertificate);
    public int updateSamCertificate(SamCertificate samCertificate);
    public int deleteSamCertificateByCertId(Long certId);
    public int deleteSamCertificateByCertIds(Long[] certIds);
}
