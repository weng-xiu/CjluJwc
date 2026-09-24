package com.yu.sam.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.sam.domain.SamCertReissueApply;

/**
 * 证书补办申请Mapper接口（S7）
 *
 * @author ruoyi
 * @date 2026-09-24
 */
public interface SamCertReissueApplyMapper
{
    public SamCertReissueApply selectSamCertReissueApplyByApplyId(Long applyId);

    public List<SamCertReissueApply> selectSamCertReissueApplyList(SamCertReissueApply samCertReissueApply);

    public int insertSamCertReissueApply(SamCertReissueApply samCertReissueApply);

    public int updateSamCertReissueApply(SamCertReissueApply samCertReissueApply);

    public int deleteSamCertReissueApplyByApplyIds(Long[] applyIds);

    /** 该证书是否已有待受理申请（防重复提交） */
    public int countPendingByOrigCertId(@Param("origCertId") Long origCertId);

    /** 学生已持有的证书列表（补办申请时选择原证书，联查编号） */
    public List<SamCertReissueApply> selectCertsByStudentId(@Param("studentId") Long studentId);
}
