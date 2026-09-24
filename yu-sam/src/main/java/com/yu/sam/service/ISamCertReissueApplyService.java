package com.yu.sam.service;

import java.util.List;
import com.yu.sam.domain.SamCertReissueApply;

/**
 * 证书补办申请Service接口（S7b）
 *
 * @author ruoyi
 * @date 2026-09-24
 */
public interface ISamCertReissueApplyService
{
    public SamCertReissueApply selectSamCertReissueApplyByApplyId(Long applyId);

    public List<SamCertReissueApply> selectSamCertReissueApplyList(SamCertReissueApply samCertReissueApply);

    public int deleteSamCertReissueApplyByApplyIds(Long[] applyIds);

    /** 提交补办申请（校验原证书属于该学生、无重复待受理申请，状态置0待受理） */
    public int submitApply(SamCertReissueApply apply);

    /** 受理通过：以原证书为模板生成补办证书（编号自动生成，reissue_type=1），回填 newCertId 并通知学生 */
    public int approve(Long applyId, String auditOpinion, String operator);

    /** 驳回：置状态2并通知学生 */
    public int reject(Long applyId, String auditOpinion, String operator);

    /** 学生可补办的原证书列表 */
    public List<SamCertReissueApply> selectCertsForStudent(Long studentId);
}
