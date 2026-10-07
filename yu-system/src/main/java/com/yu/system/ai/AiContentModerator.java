package com.yu.system.ai;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.yu.common.utils.StringUtils;
import com.yu.system.service.ISysConfigService;

/**
 * AI 内容治理审核器（K4 AI 治理 · 输入/输出审核与算法留痕）
 *
 * 定位为「可解释、零外部依赖」的规则式审核，与 Phase34 AI 试点「本地可解释 + 可插拔大模型」的
 * 技术取向一致，绝不对第三方审核服务形成硬依赖：命中内置风险规则即拦截或改写，规则清单可经
 * sys_config 扩充。合规口径对应《人工智能+教育行动计划》与等保对「AI 内容审核与算法留痕」的要求，
 * 覆盖三类风险：
 * 1) 越狱 / 提示词注入（诱导模型泄露系统提示、绕过作答约束）；
 * 2) 敏感信息套取（套取密钥、令牌、连接串、他人隐私等）；
 * 3) 输入超限与自定义违禁词（由机构在参数配置中维护）。
 * 输出侧另做一道防泄露改写，避免模型把系统提示或密钥原样带出。
 *
 * @author yu
 * @date 2026-10-07
 */
@Component
public class AiContentModerator
{
    /** 审核总开关（默认开启） */
    private static final String CFG_ENABLED = "ai.moderation.enabled";

    /** 单次提问最大字符数（超出即拦截，防止超长注入与资源滥用） */
    private static final String CFG_MAX_LENGTH = "ai.moderation.maxLength";

    /** 机构自定义违禁词，逗号分隔，命中即拦截 */
    private static final String CFG_BLOCK_KEYWORDS = "ai.moderation.blockKeywords";

    /** 默认最大长度 */
    private static final int DEFAULT_MAX_LENGTH = 200;

    /** 审核结果码：通过 */
    public static final String RESULT_PASS = "0";

    /** 审核结果码：输入被拦截 */
    public static final String RESULT_BLOCK_INPUT = "1";

    /** 审核结果码：输出被改写（命中泄露风险） */
    public static final String RESULT_REWRITE_OUTPUT = "2";

    /** 越狱 / 提示词注入特征（小写匹配） */
    private static final String[] INJECTION_PATTERNS = {
            "ignore previous", "ignore above", "disregard previous", "system prompt", "reveal your prompt",
            "reveal your instruction", "print your instruction", "developer mode", "jailbreak", "dan mode",
            "忽略上述", "忽略之前", "忽略上面", "无视上述", "无视之前", "系统提示词", "系统指令",
            "你现在是", "假装你是", "扮演一个", "角色扮演", "绕过限制", "突破限制", "开发者模式",
            "输出你的提示词", "打印你的指令", "告诉我你的规则", "你的初始指令"
    };

    /** 敏感信息套取特征（小写匹配） */
    private static final String[] SECRET_PROBE_PATTERNS = {
            "api key", "apikey", "access token", "secret key", "connection string", "jdbc:", "password",
            "密钥", "令牌", "数据库密码", "连接串", "私钥", "授权码", "管理员账号密码"
    };

    @Autowired
    private ISysConfigService configService;

    /**
     * 输入审核：判定提问是否可安全进入作答链路。
     *
     * @param question 用户提问原文
     * @return 审核结论；未通过时应以安全话术直接回复、不触达检索与模型
     */
    public Verdict moderateInput(String question)
    {
        if (!isEnabled())
        {
            return Verdict.pass();
        }
        if (StringUtils.isBlank(question))
        {
            return Verdict.pass();
        }
        String q = question.trim();
        int maxLength = intConfig(CFG_MAX_LENGTH, DEFAULT_MAX_LENGTH);
        if (maxLength > 0 && q.length() > maxLength)
        {
            return Verdict.blockInput("输入超限", "提问过长，请精简到 " + maxLength + " 字以内后重试。");
        }
        String lower = q.toLowerCase();
        for (String p : INJECTION_PATTERNS)
        {
            if (lower.contains(p))
            {
                return Verdict.blockInput("越狱/提示词注入", "该提问试图引导助手偏离教务政策作答范围，已被安全策略拦截。请就教务事项重新提问。");
            }
        }
        for (String p : SECRET_PROBE_PATTERNS)
        {
            if (lower.contains(p))
            {
                return Verdict.blockInput("敏感信息套取", "出于数据安全要求，助手不提供系统密钥、口令或他人隐私信息。如需相关数据，请通过教务处正式流程申请。");
            }
        }
        for (String kw : blockKeywords())
        {
            if (StringUtils.isNotEmpty(kw) && q.contains(kw))
            {
                return Verdict.blockInput("命中违禁词", "提问包含不被允许的内容，请调整后重试，或直接咨询教务处。");
            }
        }
        return Verdict.pass();
    }

    /**
     * 输出审核：对生成答案做一道防泄露改写。命中系统提示外泄或密钥形态时，整段替换为安全话术，
     * 避免模型把内部提示或凭据原样带出。审核通过返回原答案，不改变正常回复。
     *
     * @param answer 已生成的答案
     * @return 审核结论；未通过时应以返回的 safeReply 覆盖原答案
     */
    public Verdict moderateOutput(String answer)
    {
        if (!isEnabled() || StringUtils.isBlank(answer))
        {
            return Verdict.pass();
        }
        String lower = answer.toLowerCase();
        // 疑似泄露 API Key（sk- 形态）或 JDBC 连接串
        if (lower.matches("(?s).*(sk-[a-z0-9]{16,}).*") || lower.contains("jdbc:"))
        {
            return Verdict.rewriteOutput("输出疑似包含密钥或连接串，已按安全策略屏蔽。请重新提问或联系教务处。");
        }
        return Verdict.pass();
    }

    private boolean isEnabled()
    {
        String v = configService.selectConfigByKey(CFG_ENABLED);
        // 未显式配置时默认开启（合规优先），仅当显式配置为 false 才关闭
        return StringUtils.isEmpty(v) || !"false".equalsIgnoreCase(v.trim());
    }

    private List<String> blockKeywords()
    {
        List<String> list = new ArrayList<>();
        String v = configService.selectConfigByKey(CFG_BLOCK_KEYWORDS);
        if (StringUtils.isNotEmpty(v))
        {
            for (String part : v.split("[,，;；]"))
            {
                String kw = part.trim();
                if (!kw.isEmpty())
                {
                    list.add(kw);
                }
            }
        }
        return list;
    }

    private int intConfig(String key, int defaultValue)
    {
        String v = configService.selectConfigByKey(key);
        try
        {
            return StringUtils.isEmpty(v) ? defaultValue : Integer.parseInt(v.trim());
        }
        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }

    /**
     * 审核结论：pass=是否放行，category=命中类别（留痕用），result=结果码，safeReply=拦截/改写话术。
     */
    public static class Verdict
    {
        private final boolean pass;
        private final String category;
        private final String result;
        private final String safeReply;

        private Verdict(boolean pass, String category, String result, String safeReply)
        {
            this.pass = pass;
            this.category = category;
            this.result = result;
            this.safeReply = safeReply;
        }

        public static Verdict pass()
        {
            return new Verdict(true, null, RESULT_PASS, null);
        }

        public static Verdict blockInput(String category, String safeReply)
        {
            return new Verdict(false, category, RESULT_BLOCK_INPUT, safeReply);
        }

        public static Verdict rewriteOutput(String safeReply)
        {
            return new Verdict(false, "输出泄露防护", RESULT_REWRITE_OUTPUT, safeReply);
        }

        public boolean isPass()
        {
            return pass;
        }

        public String getCategory()
        {
            return category;
        }

        public String getResult()
        {
            return result;
        }

        public String getSafeReply()
        {
            return safeReply;
        }
    }
}
