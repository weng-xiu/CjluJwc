package com.yu.common.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * S-9 操作日志脱敏：{@link OperLogRedactor} 的两级判据单元验证。
 *
 * <p>覆盖三类必须成立的行为：敏感键整值掩码、键名没命中时的形态兜底、
 * 以及「不该掩的不能掩」（学号、13 位时间戳）——最后这类是防止脱敏过度把日志变成废纸。
 *
 * @author yu
 */
class OperLogRedactorTest
{
    private static final String ID_PLAIN = "990101199001011234";
    private static final String PHONE_PLAIN = "13800138000";

    @Test
    void masksValueWhenKeyIsSensitive()
    {
        String out = OperLogRedactor.redact("{\"idCard\":\"" + ID_PLAIN + "\",\"studentNo\":\"2023001234\"}");
        assertFalse(out.contains(ID_PLAIN), "身份证明文不得出现在日志参数里：" + out);
        assertTrue(out.contains("\"idCard\":\"" + OperLogRedactor.MASK + "\""), "敏感键应整值替换：" + out);
        assertTrue(out.contains("2023001234"), "学号属 L2 非 PII，必须保留可追溯性：" + out);
    }

    @Test
    void normalizesKeySeparatorsAndCase()
    {
        String snake = OperLogRedactor.redact("{\"id_card\":\"" + ID_PLAIN + "\"}");
        String camel = OperLogRedactor.redact("{\"phoneNumber\":\"" + PHONE_PLAIN + "\"}");
        String kebab = OperLogRedactor.redact("{\"access-token\":\"abc123def456\"}");
        assertFalse(snake.contains(ID_PLAIN), "snake_case 键也要命中：" + snake);
        assertFalse(camel.contains(PHONE_PLAIN), "camelCase 键也要命中：" + camel);
        assertFalse(kebab.contains("abc123def456"), "kebab-case 键也要命中：" + kebab);
    }

    @Test
    void masksByShapeWhenKeyIsNotSensitive()
    {
        String out = OperLogRedactor.redact("{\"remark\":\"请联系 " + PHONE_PLAIN + " 或 990101199001011234\"}");
        assertFalse(out.contains(PHONE_PLAIN), "自由文本里的手机号也要掩：" + out);
        assertFalse(out.contains(ID_PLAIN), "自由文本里的身份证也要掩：" + out);
        assertTrue(out.contains("138****8000"), "手机号保留前 3 后 4：" + out);
        assertTrue(out.contains("9901** **** ****1234"), "身份证保留前 4 后 4：" + out);
    }

    @Test
    void masksNumericValueUnderSensitiveKey()
    {
        // 表单以数字提交时 JSON 里没有引号，键名一级不适用，靠形态兜底
        String out = OperLogRedactor.redact("{\"phone\":" + PHONE_PLAIN + "}");
        assertFalse(out.contains(PHONE_PLAIN), "数字形态的手机号也要掩：" + out);
        assertTrue(out.contains("138****8000"), "数字值应走形态掩码：" + out);
    }

    @Test
    void masksEmailByShape()
    {
        String out = OperLogRedactor.redact("{\"note\":\"邮箱 zhangsan@cjlu.edu.cn 待确认\"}");
        assertFalse(out.contains("zhangsan@cjlu.edu.cn"), "邮箱明文不得落库：" + out);
        assertTrue(out.contains("z****@cjlu.edu.cn"), "邮箱只保留首字符与域名：" + out);
    }

    @Test
    void masksSensitiveFragmentInsideErrorMessage()
    {
        String out = OperLogRedactor.redact("Duplicate entry '990101199001011234' for key 'uk_id_card'");
        assertFalse(out.contains(ID_PLAIN), "异常消息里的身份证同样要掩：" + out);
    }

    @Test
    void keepsNonPiiNumbersIntact()
    {
        // 13 位毫秒时间戳、8 位学号、6 位课程号：都不该被当成手机号/身份证
        String text = "{\"ts\":1735689600000,\"studentNo\":\"20230012\",\"courseNo\":\"B01001\"}";
        assertEquals(text, OperLogRedactor.redact(text), "非 PII 数字不得被误掩");
    }

    @Test
    void keepsMaskedValueStable()
    {
        // 已是掩码形态时不重复处理，避免把 ****** 变成别的样子
        String text = "{\"idCard\":\"" + OperLogRedactor.MASK + "\"}";
        assertEquals(text, OperLogRedactor.redact(text), "重复脱敏应幂等");
    }

    @Test
    void handlesNullAndBlank()
    {
        assertNull(OperLogRedactor.redact(null), "null 原样返回");
        assertEquals("", OperLogRedactor.redact(""), "空串原样返回");
        assertEquals("   ", OperLogRedactor.redact("   "), "空白原样返回");
    }

    @Test
    void worksOnConcatenatedArgJson()
    {
        // LogAspect.argsArrayToString 会把多个入参 JSON 用空格拼起来，整体不是合法 JSON
        String out = OperLogRedactor.redact("{\"idCard\":\"" + ID_PLAIN + "\"} 12 \"x\"");
        assertFalse(out.contains(ID_PLAIN), "拼接串也要掩：" + out);
        assertTrue(out.contains("\"x\""), "非敏感片段应原样保留：" + out);
    }
}
