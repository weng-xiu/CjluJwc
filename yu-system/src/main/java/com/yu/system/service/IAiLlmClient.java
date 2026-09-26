package com.yu.system.service;

/**
 * 可插拔大模型网关（Phase34 AI应用试点）
 *
 * 采用 OpenAI 兼容的 chat/completions 契约，参数全部来自 sys_config（ai.llm.*）。
 * 未开启或未配置完整时 isEnabled() 返回 false，调用方须降级为本地知识库抽取式回答，
 * 因此 AI 能力不对外部模型服务形成硬依赖。
 *
 * @author yu
 * @date 2026-09-26
 */
public interface IAiLlmClient
{
    /**
     * 是否已启用且配置完整（开关 + 服务地址 + 模型名 + 密钥）
     */
    public boolean isEnabled();

    /**
     * 引擎说明（供前端披露当前回答口径，密钥不外泄）
     */
    public String describeEngine();

    /**
     * 以「系统提示 + 用户提示」发起一次对话补全
     *
     * @param systemPrompt 系统提示（角色与作答约束）
     * @param userPrompt   用户提示（含检索到的知识上下文与问题）
     * @return 模型生成的回答文本
     * @throws Exception 网络、鉴权或响应格式异常时抛出，由调用方降级处理
     */
    public String complete(String systemPrompt, String userPrompt) throws Exception;
}
