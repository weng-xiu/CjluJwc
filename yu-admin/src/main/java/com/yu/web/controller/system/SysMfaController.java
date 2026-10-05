package com.yu.web.controller.system;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.utils.SecurityUtils;
import com.yu.common.utils.StringUtils;
import com.yu.common.utils.TotpUtils;
import com.yu.system.domain.SysUserMfa;
import com.yu.system.service.ISysUserMfaService;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

/**
 * 多因子鉴别（MFA / TOTP）绑定与状态接口（K3-2 后端地基）。
 *
 * <p>所有接口均需已登录（第一因子密码已通过）。用于用户自助绑定/确认/解绑身份验证器，
 * 以及查询当前 MFA 状态。绑定成功后，后续登录将强制校验一次性口令。</p>
 *
 * @author cjlu
 */
@RestController
@RequestMapping("/mfa")
public class SysMfaController
{
    /** otpauth URI 中的发行方标识 */
    private static final String ISSUER = "CJLU-JWC";

    @Autowired
    private ISysUserMfaService mfaService;

    /**
     * 查询当前用户 MFA 状态。
     */
    @GetMapping("/status")
    public AjaxResult status()
    {
        Long userId = SecurityUtils.getUserId();
        SysUserMfa mfa = mfaService.getByUserId(userId);
        AjaxResult ajax = AjaxResult.success();
        boolean enabled = mfa != null && SysUserMfa.STATUS_ENABLED.equals(mfa.getStatus());
        boolean pending = mfa != null && SysUserMfa.STATUS_PENDING.equals(mfa.getStatus());
        ajax.put("enabled", enabled);
        ajax.put("pending", pending);
        ajax.put("bindTime", mfa != null ? mfa.getBindTime() : null);
        return ajax;
    }

    /**
     * 发起绑定：生成密钥并返回 otpauth URI，供前端渲染二维码。
     */
    @PostMapping("/bind")
    public AjaxResult bind()
    {
        Long userId = SecurityUtils.getUserId();
        String secret = mfaService.startBind(userId);
        String uri = TotpUtils.buildOtpauthUri(ISSUER, SecurityUtils.getUsername(), secret);
        AjaxResult ajax = AjaxResult.success("请使用身份验证器扫码或手动录入密钥，随后确认启用");
        ajax.put("secret", secret);
        ajax.put("otpauthUri", uri);
        return ajax;
    }

    /**
     * 确认绑定：校验一次性口令，通过后启用 MFA。
     */
    @PostMapping("/confirm")
    public AjaxResult confirm(@RequestBody Map<String, String> body)
    {
        String code = body == null ? null : body.get("code");
        if (StringUtils.isBlank(code))
        {
            return AjaxResult.error("请输入身份验证器中的 6 位口令");
        }
        Long userId = SecurityUtils.getUserId();
        boolean ok = mfaService.confirmBind(userId, code.trim());
        return ok ? AjaxResult.success("MFA 已启用") : AjaxResult.error("口令不正确或已过期，请重试");
    }

    /**
     * 解绑：校验当前口令后移除 MFA 绑定。
     */
    @PostMapping("/unbind")
    public AjaxResult unbind(@RequestBody Map<String, String> body)
    {
        String code = body == null ? null : body.get("code");
        if (StringUtils.isBlank(code))
        {
            return AjaxResult.error("请输入身份验证器中的 6 位口令以确认解绑");
        }
        Long userId = SecurityUtils.getUserId();
        boolean ok = mfaService.unbind(userId, code.trim());
        return ok ? AjaxResult.success("MFA 已解绑") : AjaxResult.error("口令不正确，解绑失败");
    }

    /**
     * 获取当前用户待绑定密钥的二维码（PNG base64，与验证码同形态回传）。
     *
     * <p>密钥仅在服务端渲染为图片，不经前端/第三方二维码服务中转，避免明文密钥泄露。</p>
     */
    @GetMapping("/qrcode")
    public AjaxResult qrcode()
    {
        Long userId = SecurityUtils.getUserId();
        SysUserMfa mfa = mfaService.getByUserId(userId);
        if (mfa == null || StringUtils.isBlank(mfa.getSecret()))
        {
            return AjaxResult.error("请先发起绑定");
        }
        String uri = TotpUtils.buildOtpauthUri(ISSUER, SecurityUtils.getUsername(), mfa.getSecret());
        try
        {
            AjaxResult ajax = AjaxResult.success();
            ajax.put("img", toQrPngBase64(uri));
            return ajax;
        }
        catch (Exception e)
        {
            return AjaxResult.error("二维码生成失败");
        }
    }

    /** 将文本编码为 QR 矩阵并渲染为 PNG 的 base64（zxing core + AWT，无 javase 依赖） */
    private static String toQrPngBase64(String content) throws Exception
    {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 240, 240, hints);
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < width; x++)
        {
            for (int y = 0; y < height; y++)
            {
                image.setRGB(x, y, matrix.get(x, y) ? 0xFF000000 : 0xFFFFFFFF);
            }
        }
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(image, "png", bos);
        return Base64.getEncoder().encodeToString(bos.toByteArray());
    }
}
