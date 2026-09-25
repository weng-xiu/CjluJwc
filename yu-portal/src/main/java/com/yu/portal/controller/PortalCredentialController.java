package com.yu.portal.controller;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Log;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.enums.BusinessType;
import com.yu.sam.domain.SamCertificate;
import com.yu.sam.service.ISamCertificateService;
import com.yu.system.service.ISysPrintService;

/**
 * 门户端打印与电子凭证控制器（P4）
 *
 * 防越权：业务主键强制绑定登录用户（checkOwner=true，Service 层校验归属），
 * 学生可打印成绩证明单/课表/准考证/证书，教师可打印课表/监考通知单。
 *
 * @author yu
 * @date 2026-09-25
 */
@RestController
@RequestMapping("/portal/credential")
public class PortalCredentialController extends BaseController
{
    /** 门户允许自助的凭证类型 */
    private static final List<String> SELF_TYPES = Arrays.asList("GRADE", "SCHEDULE", "CERTIFICATE", "EXAM_TICKET", "INVIGILATION");

    @Autowired
    private ISysPrintService sysPrintService;

    @Autowired
    private ISamCertificateService samCertificateService;

    /** 我的证书列表（按登录学生过滤，供选择打印） */
    @PreAuthorize("@ss.hasPermi('portal:credential:list') and @ss.hasAnyRoles('admin,student')")
    @GetMapping("/myCertificates")
    public TableDataInfo myCertificates()
    {
        Long studentId = sysPrintService.selectStudentIdByUserId(getUserId());
        SamCertificate query = new SamCertificate();
        query.setStudentId(studentId != null ? studentId : -1L);
        startPage();
        List<SamCertificate> list = samCertificateService.selectSamCertificateList(query);
        return getDataTable(list);
    }

    /** 本人考场座位列表（准考证选择源） */
    @PreAuthorize("@ss.hasPermi('portal:credential:list') and @ss.hasAnyRoles('admin,student')")
    @GetMapping("/mySeats")
    public AjaxResult mySeats()
    {
        return success(sysPrintService.selectMySeats(getUserId()));
    }

    /** 本人监考安排列表（监考通知单选择源） */
    @PreAuthorize("@ss.hasPermi('portal:credential:list') and @ss.hasAnyRoles('admin,teacher')")
    @GetMapping("/myInvigilations")
    public AjaxResult myInvigilations()
    {
        return success(sysPrintService.selectMyInvigilations(getUserId()));
    }

    /** 凭证预览（按登录人装配数据，不落库） */
    @PreAuthorize("@ss.hasPermi('portal:credential:list')")
    @GetMapping("/render")
    public AjaxResult render(@RequestParam String bizType, @RequestParam Long bizId,
                             @RequestParam(required = false) Long semesterId)
    {
        checkSelfType(bizType);
        return success((Object) sysPrintService.render(bizType, bizId, semesterId, true));
    }

    /** 自助发放电子凭证（生成验证码/编号，落发放记录） */
    @PreAuthorize("@ss.hasPermi('portal:credential:issue')")
    @Log(title = "门户电子凭证发放", businessType = BusinessType.INSERT)
    @PostMapping("/issue")
    public AjaxResult issue(@RequestParam String bizType, @RequestParam Long bizId,
                            @RequestParam(required = false) Long semesterId)
    {
        checkSelfType(bizType);
        return success((Object) sysPrintService.issue(bizType, bizId, semesterId, "1", true));
    }

    /** 打开本人已发放凭证 */
    @PreAuthorize("@ss.hasPermi('portal:credential:list')")
    @GetMapping("/print/{recordId}")
    public AjaxResult print(@org.springframework.web.bind.annotation.PathVariable("recordId") Long recordId)
    {
        com.yu.system.domain.SysPrintRecord record = sysPrintService.selectRecordById(recordId);
        if (record == null || !SELF_TYPES.contains(record.getBizType()))
        {
            return error("凭证不存在");
        }
        // 归属校验：非管理员仅可打开本人凭证
        Long receiveId = record.getReceiveId();
        if (receiveId == null || !(receiveId.equals(getUserId()) || com.yu.common.utils.SecurityUtils.isAdmin(receiveId)))
        {
            return error("无权访问该凭证");
        }
        return success((Object) sysPrintService.renderByRecord(recordId));
    }

    /** 本人凭证发放记录 */
    @PreAuthorize("@ss.hasPermi('portal:credential:list')")
    @GetMapping("/myRecords")
    public TableDataInfo myRecords(com.yu.system.domain.SysPrintRecord query)
    {
        query.setReceiveId(getUserId());
        startPage();
        List<com.yu.system.domain.SysPrintRecord> list = sysPrintService.selectRecordList(query);
        return getDataTable(list);
    }

    private void checkSelfType(String bizType)
    {
        if (!SELF_TYPES.contains(bizType))
        {
            throw new com.yu.common.exception.ServiceException("不支持的凭证类型：" + bizType);
        }
    }
}
