package com.yu.sam.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.sam.mapper.SamCertificateMapper;
import com.yu.sam.domain.SamCertificate;
import com.yu.sam.service.ISamCertificateService;

@Service
public class SamCertificateServiceImpl implements ISamCertificateService 
{
    @Autowired
    private SamCertificateMapper samCertificateMapper;

    @Override
    public SamCertificate selectSamCertificateByCertId(Long certId) { return samCertificateMapper.selectSamCertificateByCertId(certId); }
    @Override
    public List<SamCertificate> selectSamCertificateList(SamCertificate samCertificate) { return samCertificateMapper.selectSamCertificateList(samCertificate); }
    @Override
    @Transactional
    public int insertSamCertificate(SamCertificate samCertificate) { samCertificate.setCreateTime(DateUtils.getNowDate()); return samCertificateMapper.insertSamCertificate(samCertificate); }
    @Override
    @Transactional
    public int updateSamCertificate(SamCertificate samCertificate) { samCertificate.setUpdateTime(DateUtils.getNowDate()); return samCertificateMapper.updateSamCertificate(samCertificate); }
    @Override
    @Transactional
    public int deleteSamCertificateByCertId(Long certId) { return samCertificateMapper.deleteSamCertificateByCertId(certId); }
    @Override
    @Transactional
    public int deleteSamCertificateByCertIds(Long[] certIds) { return samCertificateMapper.deleteSamCertificateByCertIds(certIds); }
}
