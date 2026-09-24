package com.yu.sam.service.impl;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.sam.mapper.SamCertificateMapper;
import com.yu.sam.domain.SamCertificate;
import com.yu.sam.service.ISamCertificateService;

/**
 * 证书编号管理Service业务层处理
 * S7a：证书编号按规则自动生成并做唯一性校验，支持批量生成。
 *
 * @author ruoyi
 * @date 2026-09-24
 */
@Service
public class SamCertificateServiceImpl implements ISamCertificateService
{
    @Autowired
    private SamCertificateMapper samCertificateMapper;

    /** 院校码（证书编号前缀），默认长江大学 10489 */
    @Value("${sam.cert.schoolCode:10489}")
    private String schoolCode;

    /** 编号流水号位数 */
    @Value("${sam.cert.seqLen:4}")
    private int seqLen;

    @Override
    public SamCertificate selectSamCertificateByCertId(Long certId) { return samCertificateMapper.selectSamCertificateByCertId(certId); }

    @Override
    public List<SamCertificate> selectSamCertificateList(SamCertificate samCertificate) { return samCertificateMapper.selectSamCertificateList(samCertificate); }

    @Override
    public List<SamCertificate> selectByStudentId(Long studentId) { return samCertificateMapper.selectByStudentId(studentId); }

    @Override
    @Transactional
    public int insertSamCertificate(SamCertificate samCertificate)
    {
        // 编号为空则自动生成；非空则做唯一性校验
        if (StringUtils.isBlank(samCertificate.getCertNumber()))
        {
            samCertificate.setCertNumber(generateUniqueNumber(samCertificate.getCertType(), resolveYear(samCertificate)));
        }
        else if (samCertificateMapper.countByCertNumber(samCertificate.getCertNumber(), null) > 0)
        {
            throw new ServiceException("证书编号 " + samCertificate.getCertNumber() + " 已存在，请更换或留空由系统自动生成");
        }
        if (StringUtils.isBlank(samCertificate.getReissueType()))
        {
            samCertificate.setReissueType("0");
        }
        samCertificate.setCreateTime(DateUtils.getNowDate());
        return samCertificateMapper.insertSamCertificate(samCertificate);
    }

    @Override
    @Transactional
    public int updateSamCertificate(SamCertificate samCertificate)
    {
        if (StringUtils.isNotBlank(samCertificate.getCertNumber())
                && samCertificateMapper.countByCertNumber(samCertificate.getCertNumber(), samCertificate.getCertId()) > 0)
        {
            throw new ServiceException("证书编号 " + samCertificate.getCertNumber() + " 已被其他证书记录占用");
        }
        samCertificate.setUpdateTime(DateUtils.getNowDate());
        return samCertificateMapper.updateSamCertificate(samCertificate);
    }

    @Override
    @Transactional
    public int deleteSamCertificateByCertId(Long certId) { return samCertificateMapper.deleteSamCertificateByCertId(certId); }

    @Override
    @Transactional
    public int deleteSamCertificateByCertIds(Long[] certIds) { return samCertificateMapper.deleteSamCertificateByCertIds(certIds); }

    @Override
    public String previewCertNumber(String certType, Integer year)
    {
        int y = year != null ? year : Calendar.getInstance().get(Calendar.YEAR);
        return generateUniqueNumber(certType, y);
    }

    @Override
    @Transactional
    public Map<String, Object> batchGenerateCertificates(String certType, String gradYear, String operator)
    {
        if (StringUtils.isBlank(certType))
        {
            throw new ServiceException("请选择要生成的证书类型");
        }
        // 毕业/结业证书以毕业审核通过(1)为口径，学位证书以学位审核通过(1)为口径
        String reviewPassStatus = "1";
        List<Map<String, Object>> students = samCertificateMapper.selectStudentsWithoutCert(certType, reviewPassStatus, gradYear);
        int generated = 0;
        Date now = DateUtils.getNowDate();
        for (Map<String, Object> st : students)
        {
            SamCertificate cert = new SamCertificate();
            cert.setStudentId(toLong(st.get("studentId")));
            cert.setCertType(certType);
            cert.setMajorId(toLong(st.get("majorId")));
            Object eduLevel = st.get("educationLevel");
            cert.setEducationLevel(eduLevel == null ? null : String.valueOf(eduLevel));
            cert.setCertDate(now);
            cert.setIsIssued("0");
            cert.setStatus("0");
            cert.setReissueType("0");
            cert.setCreateBy(operator);
            // 编号自动生成（按发证年份=当前年分桶自增）
            cert.setCertNumber(generateUniqueNumber(certType, Calendar.getInstance().get(Calendar.YEAR)));
            samCertificateMapper.insertSamCertificate(cert);
            generated++;
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("certType", certType);
        result.put("candidates", students.size());
        result.put("generated", generated);
        return result;
    }

    /** 解析发证年份：优先 certDate，否则当前年 */
    private int resolveYear(SamCertificate cert)
    {
        if (cert.getCertDate() != null)
        {
            Calendar c = Calendar.getInstance();
            c.setTime(cert.getCertDate());
            return c.get(Calendar.YEAR);
        }
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    /**
     * 生成唯一编号：院校码 + 年份(4) + 证书类型码(1) + 流水号(seqLen，左补零)。
     * 在同分桶内取最大流水号自增，并循环校验全局唯一，规避并发/删除造成的重复。
     */
    private String generateUniqueNumber(String certType, int year)
    {
        String typeCode = StringUtils.isBlank(certType) ? "9" : certType.trim();
        String bucket = schoolCode + year + typeCode;
        Long max = samCertificateMapper.selectMaxSeqByBucket(bucket, seqLen);
        long seq = (max == null ? 0L : max) + 1;
        String number;
        int guard = 0;
        do
        {
            number = bucket + leftPad(seq, seqLen);
            seq++;
            // 极端情况下（编号被占用）继续自增，guard 防止无限循环
            if (++guard > 100000)
            {
                throw new ServiceException("证书编号生成失败：分桶 " + bucket + " 流水号已耗尽");
            }
        }
        while (samCertificateMapper.countByCertNumber(number, null) > 0);
        return number;
    }

    private String leftPad(long v, int len)
    {
        String s = Long.toString(v);
        StringBuilder sb = new StringBuilder();
        for (int i = s.length(); i < len; i++)
        {
            sb.append('0');
        }
        sb.append(s);
        return sb.toString();
    }

    private Long toLong(Object v)
    {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (NumberFormatException e) { return null; }
    }
}
