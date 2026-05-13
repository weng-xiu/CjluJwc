package com.yu.sam.service;

import java.util.List;
import com.yu.sam.domain.SamCertificate;

public interface ISamCertificateService 
{
    public SamCertificate selectSamCertificateByCertId(Long certId);
    public List<SamCertificate> selectSamCertificateList(SamCertificate samCertificate);
    public int insertSamCertificate(SamCertificate samCertificate);
    public int updateSamCertificate(SamCertificate samCertificate);
    public int deleteSamCertificateByCertIds(Long[] certIds);
    public int deleteSamCertificateByCertId(Long certId);
}
