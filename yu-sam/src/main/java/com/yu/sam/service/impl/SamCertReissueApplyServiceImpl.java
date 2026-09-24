package com.yu.sam.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.sam.domain.SamCertificate;
import com.yu.sam.domain.SamCertReissueApply;
import com.yu.sam.mapper.SamCertificateMapper;
import com.yu.sam.mapper.SamCertReissueApplyMapper;
import com.yu.sam.mapper.SamWarningDataMapper;
import com.yu.sam.service.ISamCertReissueApplyService;
import com.yu.system.domain.SysMessage;
import com.yu.system.service.ISysMessageService;

/**
 * 证书补办申请Service业务层处理（S7b）
 * 流程：学生/管理员提交申请(0待受理) → 教务处受理通过(1，自动生成补办证书并通知) / 驳回(2，通知)。
 *
 * @author ruoyi
 * @date 2026-09-24
 */
@Service
public class SamCertReissueApplyServiceImpl implements ISamCertReissueApplyService
{
    private static final Logger log = LoggerFactory.getLogger(SamCertReissueApplyServiceImpl.class);

    @Autowired
    private SamCertReissueApplyMapper samCertReissueApplyMapper;

    @Autowired
    private SamCertificateMapper samCertificateMapper;

    @Autowired
    private SamWarningDataMapper samWarningDataMapper;

    /** 复用证书Service的编号自动生成逻辑创建补办证书 */
    @Autowired
    private com.yu.sam.service.ISamCertificateService samCertificateService;

    @Autowired
    private ISysMessageService sysMessageService;

    @Override
    public SamCertReissueApply selectSamCertReissueApplyByApplyId(Long applyId)
    {
        return samCertReissueApplyMapper.selectSamCertReissueApplyByApplyId(applyId);
    }

    @Override
    public List<SamCertReissueApply> selectSamCertReissueApplyList(SamCertReissueApply samCertReissueApply)
    {
        return samCertReissueApplyMapper.selectSamCertReissueApplyList(samCertReissueApply);
    }

    @Override
    public int deleteSamCertReissueApplyByApplyIds(Long[] applyIds)
    {
        return samCertReissueApplyMapper.deleteSamCertReissueApplyByApplyIds(applyIds);
    }

    @Override
    public List<SamCertReissueApply> selectCertsForStudent(Long studentId)
    {
        return samCertReissueApplyMapper.selectCertsByStudentId(studentId);
    }

    @Override
    @Transactional
    public int submitApply(SamCertReissueApply apply)
    {
        if (apply.getStudentId() == null || apply.getOrigCertId() == null)
        {
            throw new ServiceException("学生与原证书不能为空");
        }
        SamCertificate orig = samCertificateMapper.selectSamCertificateByCertId(apply.getOrigCertId());
        if (orig == null)
        {
            throw new ServiceException("原证书不存在");
        }
        if (!orig.getStudentId().equals(apply.getStudentId()))
        {
            throw new ServiceException("原证书不属于该学生");
        }
        if ("1".equals(orig.getReissueType()))
        {
            throw new ServiceException("补办证书不可再次补办");
        }
        if (StringUtils.isBlank(apply.getReason()))
        {
            throw new ServiceException("请填写补办原因");
        }
        if (samCertReissueApplyMapper.countPendingByOrigCertId(apply.getOrigCertId()) > 0)
        {
            throw new ServiceException("该证书已有待受理的补办申请，请勿重复提交");
        }
        apply.setApplyStatus("0");
        apply.setCreateBy(apply.getCreateBy());
        apply.setCreateTime(DateUtils.getNowDate());
        return samCertReissueApplyMapper.insertSamCertReissueApply(apply);
    }

    @Override
    @Transactional
    public int approve(Long applyId, String auditOpinion, String operator)
    {
        SamCertReissueApply apply = samCertReissueApplyMapper.selectSamCertReissueApplyByApplyId(applyId);
        if (apply == null)
        {
            throw new ServiceException("补办申请不存在");
        }
        if (!"0".equals(apply.getApplyStatus()))
        {
            throw new ServiceException("仅待受理的申请可受理");
        }
        SamCertificate orig = samCertificateMapper.selectSamCertificateByCertId(apply.getOrigCertId());
        if (orig == null)
        {
            throw new ServiceException("原证书不存在，无法补办");
        }
        // 以原证书为模板生成补办证书：编号自动生成、来源标记为补办
        SamCertificate reissue = new SamCertificate();
        reissue.setStudentId(orig.getStudentId());
        reissue.setCertType(orig.getCertType());
        reissue.setMajorId(orig.getMajorId());
        reissue.setEducationLevel(orig.getEducationLevel());
        reissue.setCertDate(DateUtils.getNowDate());
        reissue.setIsIssued("0");
        reissue.setStatus("0");
        reissue.setReissueType("1");
        reissue.setCertSourceId(orig.getCertId());
        reissue.setCreateBy(operator);
        reissue.setRemark("由补办申请" + applyId + "生成，原证书编号" + orig.getCertNumber());
        samCertificateService.insertSamCertificate(reissue);

        SamCertReissueApply update = new SamCertReissueApply();
        update.setApplyId(applyId);
        update.setApplyStatus("1");
        update.setNewCertId(reissue.getCertId());
        update.setAuditBy(operator);
        update.setAuditTime(new Date());
        update.setAuditOpinion(auditOpinion);
        update.setUpdateTime(DateUtils.getNowDate());
        int rows = samCertReissueApplyMapper.updateSamCertReissueApply(update);

        notifyStudent(apply.getStudentId(), "【证书补办已受理】",
                "您的证书补办申请已受理通过，补办证书编号：" + reissue.getCertNumber() + "，请到教务处领取。");
        return rows;
    }

    @Override
    @Transactional
    public int reject(Long applyId, String auditOpinion, String operator)
    {
        SamCertReissueApply apply = samCertReissueApplyMapper.selectSamCertReissueApplyByApplyId(applyId);
        if (apply == null)
        {
            throw new ServiceException("补办申请不存在");
        }
        if (!"0".equals(apply.getApplyStatus()))
        {
            throw new ServiceException("仅待受理的申请可驳回");
        }
        SamCertReissueApply update = new SamCertReissueApply();
        update.setApplyId(applyId);
        update.setApplyStatus("2");
        update.setAuditBy(operator);
        update.setAuditTime(new Date());
        update.setAuditOpinion(auditOpinion);
        update.setUpdateTime(DateUtils.getNowDate());
        int rows = samCertReissueApplyMapper.updateSamCertReissueApply(update);

        notifyStudent(apply.getStudentId(), "【证书补办申请被驳回】",
                "您的证书补办申请未通过，原因：" + (StringUtils.isBlank(auditOpinion) ? "无" : auditOpinion));
        return rows;
    }

    /** 向学生关联账号推送站内消息（异常不阻断主流程） */
    private void notifyStudent(Long studentId, String title, String content)
    {
        try
        {
            Map<String, Object> contact = samWarningDataMapper.selectStudentContact(studentId);
            Object userIdObj = contact == null ? null : contact.get("userId");
            if (userIdObj == null)
            {
                return;
            }
            SysMessage msg = new SysMessage();
            msg.setReceiverId(((Number) userIdObj).longValue());
            msg.setMsgType("1");
            msg.setTitle(title);
            msg.setContent(content);
            msg.setBusinessType("certReissue");
            msg.setBusinessId(studentId);
            sysMessageService.sendMessage(msg);
        }
        catch (Exception e)
        {
            log.error("证书补办消息推送失败（studentId={}）", studentId, e);
        }
    }
}
