package com.yu.sam.service;

import java.util.List;
import java.util.Map;
import com.yu.sam.domain.SamCertificate;

public interface ISamCertificateService 
{
    public SamCertificate selectSamCertificateByCertId(Long certId);
    public List<SamCertificate> selectSamCertificateList(SamCertificate samCertificate);
    public int insertSamCertificate(SamCertificate samCertificate);
    public int updateSamCertificate(SamCertificate samCertificate);
    public int deleteSamCertificateByCertIds(Long[] certIds);
    public int deleteSamCertificateByCertId(Long certId);

    /** S7a：按规则预生成一个唯一证书编号（不落库），供前端预览/手工新增默认值 */
    public String previewCertNumber(String certType, Integer year);

    /** S7a：批量为已通过审核且无该类型证书的学生生成证书（编号自动生成、唯一），返回统计 */
    public Map<String, Object> batchGenerateCertificates(String certType, String gradYear, String operator);

    /** 学生已持有证书列表（补办选择原证书用） */
    public List<SamCertificate> selectByStudentId(Long studentId);
}
