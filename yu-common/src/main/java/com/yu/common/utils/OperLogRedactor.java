package com.yu.common.utils;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 操作日志脱敏（缺陷报告 S-9）。
 *
 * <p>{@code sys_oper_log.oper_param / json_result} 由 {@code LogAspect} 直接序列化入参和响应体，
 * 修改学生/教师/用户信息时身份证号、手机号会原样落库，且这张表长期留存、DBA 与审计人员都能查。
 * 本类负责在写库前把这类值掩掉。
 *
 * <p><b>为什么不复用 {@code SensitivePolicy}</b>：后者按"当前请求的身份"决定是否脱敏
 * （管理员看明文），这是接口响应的语义；操作日志是事后取证材料，无论谁操作的都必须脱敏，
 * 否则管理员做一次「编辑学生」就会把明文身份证写进日志表。两者判定维度不同，不能共用。
 *
 * <p><b>两级判据</b>：
 * <ol>
 *   <li>键名命中（{@link #SENSITIVE_KEYS}）→ 值整体替换为 {@code ******}，
 *       不看形态：{@code phone} 里填了邮箱、{@code idCard} 里填了工号，同样掩掉；</li>
 *   <li>形态兜底 → 键名没命中但值长得像身份证/手机号/邮箱（如自由文本备注、
 *       异常消息里的 SQL 片段），按 {@link #ID_CARD}/{@link #PHONE}/{@link #EMAIL} 掩中间段。</li>
 * </ol>
 *
 * <p><b>刻意不掩的东西</b>：学号/工号（{@code 20xxxxxx} 这类 8~12 位数字）按台账属 L2 而非 PII，
 * 掩掉只会让日志失去可追溯性；姓名保留——审计要看"谁被改了"。
 *
 * <p>身份证不校验校验位：仓库门禁（{@code bin/pii-scan.js}）要求校验位合法是为了「宁可少报」，
 * 而日志是「宁可多掩」——掩错一个 18 位订单号的中间 10 位，代价远小于漏一个真证件号。
 *
 * @author yu
 */
public final class OperLogRedactor
{
    /**
     * 键名命中即整值掩码：凭证、证件、联系方式。
     *
     * <p>比对前会把键名里的 {@code _} / {@code -} 去掉并转小写，
     * 所以 {@code idCard}、{@code id_card}、{@code ID-CARD} 命中同一条。
     */
    private static final Set<String> SENSITIVE_KEYS = Set.of(
            "password", "passwd", "pwd", "oldpassword", "newpassword", "confirmpassword",
            "idcard", "idno", "idnumber", "certno", "certcode", "certificateno",
            "bankcard", "bankno", "cardno",
            "phone", "phonenumber", "mobile", "moble", "tel", "telephone",
            "email", "mail",
            "token", "accesstoken", "refreshtoken",
            "secret", "secretkey", "privatekey", "apikey");

    /** 整值掩码占位（定长，不泄露原文长度） */
    public static final String MASK = "******";

    /** JSON 字符串字段：组1 = `"key":`，组2 = key 名，组3 = 值（含转义序列） */
    private static final Pattern STRING_FIELD =
            Pattern.compile("(\"([A-Za-z0-9_$-]+)\"\\s*:\\s*)\"((?:[^\"\\\\]|\\\\.)*)\"");

    /** 18 位身份证形态（末位可为 X），保留前 4 与后 4 */
    private static final Pattern ID_CARD =
            Pattern.compile("(?<![0-9])([0-9]{4})[0-9]{10}([0-9]{3}[0-9Xx])(?![0-9])");

    /** 11 位手机号，保留前 3 与后 4 */
    private static final Pattern PHONE =
            Pattern.compile("(?<![0-9])(1[3-9][0-9])[0-9]{4}([0-9]{4})(?![0-9])");

    /** 邮箱，仅保留首字符与域名 */
    private static final Pattern EMAIL =
            Pattern.compile("([A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*(@[A-Za-z0-9.-]+\\.[A-Za-z]{2,})");

    private OperLogRedactor()
    {
    }

    /**
     * 对一段 JSON 文本做脱敏，非 JSON 文本（异常消息、拼接后的参数串）同样适用。
     *
     * @param text 原始文本，可为 null
     * @return 脱敏后的文本；入参为 null 时返回 null
     */
    public static String redact(String text)
    {
        if (StringUtils.isEmpty(text))
        {
            return text;
        }
        String masked = maskSensitiveKeys(text);
        return maskByShape(masked);
    }

    /** 第一级：按键名整值掩码 */
    private static String maskSensitiveKeys(String text)
    {
        Matcher matcher = STRING_FIELD.matcher(text);
        StringBuilder out = new StringBuilder(text.length());
        int last = 0;
        boolean hit = false;
        while (matcher.find())
        {
            String key = normalizeKey(matcher.group(2));
            if (!SENSITIVE_KEYS.contains(key))
            {
                continue;
            }
            out.append(text, last, matcher.start());
            out.append(matcher.group(1)).append('"').append(MASK).append('"');
            last = matcher.end();
            hit = true;
        }
        if (!hit)
        {
            return text;
        }
        out.append(text, last, text.length());
        return out.toString();
    }

    /** 键名归一化：小写 + 去掉分隔符，让 camelCase / snake_case / kebab-case 命中同一份名单 */
    private static String normalizeKey(String key)
    {
        return key.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
    }

    /** 第二级：按值的形态兜底掩码（键名没命中时也不让明文漏出去） */
    private static String maskByShape(String text)
    {
        String masked = ID_CARD.matcher(text).replaceAll("$1** **** ****$2");
        masked = PHONE.matcher(masked).replaceAll("$1****$2");
        return EMAIL.matcher(masked).replaceAll("$1****$2");
    }
}
