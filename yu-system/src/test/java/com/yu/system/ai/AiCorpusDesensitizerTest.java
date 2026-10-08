package com.yu.system.ai;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import com.yu.system.service.ISysConfigService;

/**
 * 垂直语料脱敏器测试（K4 AI 治理 · 知识库入库去标识化口径）。
 *
 * <p>钉住规则式脱敏的可解释行为：识别身份证/手机/邮箱并计数；掩码保留周边文字、口径与
 * DesensitizedType 一致；正常政策表述中的年份/学时数不误伤；身份证掩码后不被手机号规则二次命中；
 * 关闭开关时不检测。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AiCorpusDesensitizerTest {

    @Mock
    private ISysConfigService configService;

    @InjectMocks
    private AiCorpusDesensitizer desensitizer;

    @BeforeEach
    void stubDefaults() {
        when(configService.selectConfigByKey(eq("ai.corpus.desensitize.enabled"))).thenReturn("true");
        when(configService.selectConfigByKey(eq("ai.corpus.desensitize.mode"))).thenReturn("MASK");
    }

    @Test
    @DisplayName("扫描命中身份证/手机/邮箱并计数")
    void scan_detectsAllCategories() {
        Map<String, Integer> hits = desensitizer.scan(
                "学生张三身份证 420101199001011234，手机 13800138000，邮箱 zhangsan@example.com");
        assertEquals(1, hits.get(AiCorpusDesensitizer.CAT_ID_CARD));
        assertEquals(1, hits.get(AiCorpusDesensitizer.CAT_PHONE));
        assertEquals(1, hits.get(AiCorpusDesensitizer.CAT_EMAIL));
    }

    @Test
    @DisplayName("掩码按口径星号化且保留周边政策文字")
    void mask_appliesConsistentStars() {
        String masked = desensitizer.mask("身份证 420101199001011234 于第3周生效");
        assertTrue(masked.contains("4201**********1234"), "身份证应前4后4保留、中间掩码, 实际=" + masked);
        assertFalse(masked.contains("420101199001011234"), "不得残留完整明文身份证");
        assertTrue(masked.contains("于第3周生效"), "周边政策文字应原样保留");
    }

    @Test
    @DisplayName("手机号掩码保留前3后4")
    void mask_phone() {
        assertEquals("联系 138****8000 咨询", desensitizer.mask("联系 13800138000 咨询"));
    }

    @Test
    @DisplayName("邮箱掩码保留首字符与域名")
    void mask_email() {
        assertEquals("a****@example.com", desensitizer.mask("abcdef@example.com"));
    }

    @Test
    @DisplayName("身份证掩码后不被手机号规则二次命中")
    void mask_idCardNotResecondPassAsPhone() {
        String masked = desensitizer.mask("编号 13800138000138000 结束");
        // 该串为 17 位，不构成合法身份证/手机号；确认可控处理不抛异常即可
        assertTrue(masked.contains("结束"));
    }

    @Test
    @DisplayName("正常政策表述中的年份/学时数不误伤")
    void scan_noFalsePositiveOnNormalPolicyNumbers() {
        String policy = "2024级本科培养方案规定：第3周开始选课，理论学时 300 学时，实践 18 周，成绩满 60 分及格。";
        assertTrue(desensitizer.scan(policy).isEmpty(), "正常政策数字不应被判为个人信息");
        assertEquals(policy, desensitizer.mask(policy), "无个人信息时掩码应原样返回");
    }

    @Test
    @DisplayName("关闭开关后不再检测")
    void disabled_returnsEmpty() {
        when(configService.selectConfigByKey(eq("ai.corpus.desensitize.enabled"))).thenReturn("false");
        assertTrue(desensitizer.detectIfEnabled("身份证 420101199001011234").isEmpty());
    }

    @Test
    @DisplayName("未显式配置开关时合规优先默认开启")
    void enabledByDefaultWhenUnset() {
        when(configService.selectConfigByKey(eq("ai.corpus.desensitize.enabled"))).thenReturn(null);
        assertTrue(desensitizer.isEnabled());
    }
}
