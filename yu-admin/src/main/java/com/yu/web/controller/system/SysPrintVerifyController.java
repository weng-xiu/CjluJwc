package com.yu.web.controller.system;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.annotation.Anonymous;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.system.service.ISysPrintService;

/**
 * 电子凭证公开验真Controller（P4，匿名访问）
 *
 * @author yu
 * @date 2026-09-25
 */
@Anonymous
@RestController
@RequestMapping("/system/credential")
public class SysPrintVerifyController extends BaseController
{
    @Autowired
    private ISysPrintService sysPrintService;

    /**
     * 凭证验真：按凭证编号 + 验证码校验真伪（返回脱敏信息）
     */
    @GetMapping("/verify")
    public AjaxResult verify(@RequestParam String serialNo, @RequestParam String verifyCode)
    {
        return success(sysPrintService.verify(serialNo, verifyCode));
    }
}
