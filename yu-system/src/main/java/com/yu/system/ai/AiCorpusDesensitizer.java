package com.yu.system.ai;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.yu.common.utils.StringUtils;
import com.yu.system.service.ISysConfigService;

/**
 * 垂直语料脱敏器（K4 AI 治理 · 知识库入库前的敏感信息去标识化口径）。
 *
 * <p>教务政策知识库是智能问答（RAG）的唯一事实来源，条目正文会被注入检索上下文乃至大模型提示，
 * 一旦其中残留真实身份证号/手机号/邮箱等个人信息，将随问答回答对外泄露，违背《人工智能+教育行动
 * 计划》与等保对训练/检索语料「去标识化」的要求。本类把「哪些形态属个人信息、如何掩码」收敛为
 * 单一执行口径，与 {@code AiContentModerator} 同属规则式、零外部依赖、结果确定可审计：
 * <ul>
 *   <li>扫描：识别正文中内嵌的身份证号、手机号、电子邮箱 token 并计数；</li>
 *   <li>脱敏：按与运行期 {@code DesensitizedType} 一致的星号口径就地掩码，保留周边政策文字；</li>
 *   <li>模式：{@code ai.corpus.desensitize.mode}=MASK（默认，入库前自动掩码）或 BLOCK（命中即拒绝入库，
 *       由编辑显式去除个人信息后再提交）；{@code ai.corpus.desensitize.enabled}=false 可整体关闭。</li>
 * </ul>
 *
 * <p>掩码只作用于「嵌入在成段文字中的个人信息 token」，不改动正常政策表述中的数字（如年份、条款号、
 * 学时数），因此对知识库内容无破坏性，属合规优先的安全侧默认。
 *
 * @author yu
 * @date 2026-10-08
 */
@Component
public class AiCorpusDesensitizer
{
    /** 语料脱敏总开关（默认开启，合规优先） */
    private static final String CFG_ENABLED = "ai.corpus.desensitize.enabled";

    /** 脱敏模式：MASK=入库前自动掩码（默认），BLOCK=命中个人信息即拒绝入库 */
    private static final String CFG_MODE = "ai.corpus.desensitize.mode";

    public static final String MODE_MASK = "MASK";
    public static final String MODE_BLOCK = "BLOCK";

    /** 类别：身份证号 */
    public static final String CAT_ID_CARD = "ID_CARD";
    /** 类别：手机号 */
    public static final String CAT_PHONE = "PHONE";
    /** 类别：电子邮箱 */
    public static final String CAT_EMAIL = "EMAIL";

    // 18 位身份证（前后不接其它数字，避免误伤更长的数字串）；保留前 4 与后段，中间 10 位掩码
    private static final Pattern ID_CARD = Pattern.compile("(?<!\\d)(\\d{4})\\d{10}(\\d{3}[Xx]|\\d{4})(?!\\d)");
    // 11 位手机号（1 开头）；保留前 3 后 4，中间 4 位掩码
    private static final Pattern PHONE = Pattern.compile("(?<!\\d)(1\\d{2})\\d{4}(\\d{4})(?!\\d)");
    // 电子邮箱；保留首字符与 @ 之后域名，本地名掩码
    private static final Pattern EMAIL = Pattern.compile("(\\b[A-Za-z0-9._%+-])[A-Za-z0-9._%+-]*(@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b)");

    @Autowired
    private ISysConfigService configService;

    /** 是否启用语料脱敏（未显式配置时默认开启） */
    public boolean isEnabled()
    {
        String v = configService.selectConfigByKey(CFG_ENABLED);
        return StringUtils.isEmpty(v) || !"false".equalsIgnoreCase(v.trim());
    }

    /** 当前脱敏模式 */
    public String mode()
    {
        String v = configService.selectConfigByKey(CFG_MODE);
        return MODE_BLOCK.equalsIgnoreCase(StringUtils.trim(v)) ? MODE_BLOCK : MODE_MASK;
    }

    /**
     * 扫描文本中内嵌的个人信息 token，返回「类别 -> 命中次数」（无命中则为空 Map）。
     */
    public Map<String, Integer> scan(String text)
    {
        Map<String, Integer> hits = new LinkedHashMap<>();
        if (StringUtils.isBlank(text))
        {
            return hits;
        }
        count(hits, CAT_ID_CARD, ID_CARD, text);
        count(hits, CAT_PHONE, PHONE, text);
        count(hits, CAT_EMAIL, EMAIL, text);
        return hits;
    }

    private void count(Map<String, Integer> hits, String cat, Pattern p, String text)
    {
        Matcher m = p.matcher(text);
        int c = 0;
        while (m.find())
        {
            c++;
        }
        if (c > 0)
        {
            hits.put(cat, c);
        }
    }

    /**
     * 对文本做去标识化掩码：身份证号/手机号/邮箱就地星号化，其余文字原样保留。
     * 顺序为先掩码身份证（掩码后数字被打断，避免再被手机号规则二次命中），再手机号、邮箱。
     */
    public String mask(String text)
    {
        if (StringUtils.isBlank(text))
        {
            return text;
        }
        String out = ID_CARD.matcher(text).replaceAll("$1**********$2");
        out = PHONE.matcher(out).replaceAll("$1****$2");
        out = EMAIL.matcher(out).replaceAll("$1****$2");
        return out;
    }

    /**
     * 供服务层调用：命中个人信息返回其类别统计，未命中返回空 Map。
     * BLOCK 模式下调用方应据返回的非空结果拒绝入库；MASK 模式下调用方改用 {@link #mask(String)} 落库。
     */
    public Map<String, Integer> detectIfEnabled(String text)
    {
        if (!isEnabled())
        {
            return new LinkedHashMap<>();
        }
        return scan(text);
    }
}
