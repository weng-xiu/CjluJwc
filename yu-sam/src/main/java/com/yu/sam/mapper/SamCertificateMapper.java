package com.yu.sam.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
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

    /** 证书编号唯一性校验（excludeCertId 修改时排除自身） */
    public int countByCertNumber(@Param("certNumber") String certNumber, @Param("excludeCertId") Long excludeCertId);

    /** 某分桶（前缀+年份+类型码）下的最大流水号，用于自动编号自增 */
    public Long selectMaxSeqByBucket(@Param("bucket") String bucket, @Param("seqLen") int seqLen);

    /** 查询已通过毕业/学位审核但尚无指定类型证书的学生（批量生成候选） */
    public List<Map<String, Object>> selectStudentsWithoutCert(@Param("certType") String certType, @Param("reviewPassStatus") String reviewPassStatus, @Param("gradYear") String gradYear);

    /** 按学生ID+证书类型查询证书（补办申请选择原证书用） */
    public List<SamCertificate> selectByStudentId(@Param("studentId") Long studentId);
}
