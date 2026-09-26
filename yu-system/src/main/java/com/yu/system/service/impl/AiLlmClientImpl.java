package com.yu.system.service.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.yu.common.utils.StringUtils;
import com.yu.system.service.IAiLlmClient;
import com.yu.system.service.ISysConfigService;

/**
 * 大模型网关实现（Phase34 AI应用试点）
 *
 * 走 OpenAI 兼容 /chat/completions：baseUrl 形如 https://host/v1，鉴权 Bearer apiKey。
 * 所有参数每次调用即时从 sys_config 读取（与项目内 tpm./aem./sam. 参数口径一致），
 * 改参数无需重启；任一参数缺失即视为未启用，由上层降级为本地抽取式回答。
 *
 * @author yu
 * @date 2026-09-26
 */
@Service
public class AiLlmClientImpl implements IAiLlmClient
{
    private static final Logger log = LoggerFactory.getLogger(AiLlmClientImpl.class);

    /** 参数键 */
    private static final String CFG_ENABLED = "ai.llm.enabled";
    private static final String CFG_BASE_URL = "ai.llm.baseUrl";
    private static final String CFG_API_KEY = "ai.llm.apiKey";
    private static final String CFG_MODEL = "ai.llm.model";
    private static final String CFG_TEMPERATURE = "ai.llm.temperature";
    private static final String CFG_MAX_TOKENS = "ai.llm.maxTokens";
    private static final String CFG_TIMEOUT = "ai.llm.timeoutSeconds";

    @Autowired
    private ISysConfigService configService;

    @Override
    public boolean isEnabled()
    {
        return "true".equalsIgnoreCase(trim(configService.selectConfigByKey(CFG_ENABLED)))
                && StringUtils.isNotEmpty(trim(configService.selectConfigByKey(CFG_BASE_URL)))
                && StringUtils.isNotEmpty(trim(configService.selectConfigByKey(CFG_MODEL)))
                && StringUtils.isNotEmpty(trim(configService.selectConfigByKey(CFG_API_KEY)));
    }

    @Override
    public String describeEngine()
    {
        if (!isEnabled())
        {
            return "本地知识库检索（未接入大模型，回答由命中条目原文抽取生成）";
        }
        return "大模型：" + trim(configService.selectConfigByKey(CFG_MODEL));
    }

    @Override
    public String complete(String systemPrompt, String userPrompt) throws Exception
    {
        String baseUrl = trim(configService.selectConfigByKey(CFG_BASE_URL));
        String apiKey = trim(configService.selectConfigByKey(CFG_API_KEY));
        String model = trim(configService.selectConfigByKey(CFG_MODEL));
        int timeoutSeconds = intConfig(CFG_TIMEOUT, 20);
        double temperature = doubleConfig(CFG_TEMPERATURE, 0.3d);
        int maxTokens = intConfig(CFG_MAX_TOKENS, 1024);

        List<Map<String, Object>> messages = new ArrayList<>();
        messages.add(Map.of("role", "system", "content", systemPrompt == null ? "" : systemPrompt));
        messages.add(Map.of("role", "user", "content", userPrompt == null ? "" : userPrompt));
        JSONObject payload = new JSONObject();
        payload.put("model", model);
        payload.put("messages", messages);
        payload.put("temperature", temperature);
        payload.put("max_tokens", maxTokens);
        payload.put("stream", false);

        String url = joinUrl(baseUrl);
        HttpClient client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(timeoutSeconds)).build();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + apiKey)
                .POST(HttpRequest.BodyPublishers.ofString(payload.toJSONString()))
                .build();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
        int code = response.statusCode();
        if (code < 200 || code >= 300)
        {
            throw new IllegalStateException("大模型服务返回状态码 " + code + "：" + abbreviate(response.body()));
        }
        JSONObject body = JSON.parseObject(response.body());
        if (body == null)
        {
            throw new IllegalStateException("大模型服务响应不是合法 JSON");
        }
        JSONArray choices = body.getJSONArray("choices");
        if (choices == null || choices.isEmpty())
        {
            throw new IllegalStateException("大模型响应缺少 choices：" + abbreviate(response.body()));
        }
        JSONObject message = choices.getJSONObject(0).getJSONObject("message");
        String content = message == null ? null : message.getString("content");
        if (StringUtils.isBlank(content))
        {
            throw new IllegalStateException("大模型返回空回答");
        }
        log.info("AI大模型调用成功，model={}, 用量={}", model, body.getJSONObject("usage"));
        return content.trim();
    }

    /** baseUrl 允许带或不带尾部斜杠，自动补全 chat/completions */
    private String joinUrl(String baseUrl)
    {
        String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        if (base.endsWith("/chat/completions"))
        {
            return base;
        }
        return base + "/chat/completions";
    }

    private String trim(String v)
    {
        return v == null ? "" : v.trim();
    }

    private int intConfig(String key, int defaultValue)
    {
        String v = trim(configService.selectConfigByKey(key));
        try
        {
            return v.isEmpty() ? defaultValue : Integer.parseInt(v);
        }
        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }

    private double doubleConfig(String key, double defaultValue)
    {
        String v = trim(configService.selectConfigByKey(key));
        try
        {
            return v.isEmpty() ? defaultValue : Double.parseDouble(v);
        }
        catch (NumberFormatException e)
        {
            return defaultValue;
        }
    }

    private String abbreviate(String v)
    {
        if (v == null)
        {
            return "";
        }
        return v.length() > 200 ? v.substring(0, 200) + "……" : v;
    }
}
