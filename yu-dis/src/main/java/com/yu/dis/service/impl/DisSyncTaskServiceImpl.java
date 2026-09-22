package com.yu.dis.service.impl;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.system.domain.SysMessage;
import com.yu.system.service.ISysMessageService;
import com.yu.dis.mapper.DisSyncTaskMapper;
import com.yu.dis.mapper.DisInterfaceConfigMapper;
import com.yu.dis.mapper.DisExternalSystemMapper;
import com.yu.dis.mapper.DisFieldMappingMapper;
import com.yu.dis.mapper.DisDataExchangeLogMapper;
import com.yu.dis.mapper.SyncPersistenceMapper;
import com.yu.dis.domain.DisSyncTask;
import com.yu.dis.domain.DisInterfaceConfig;
import com.yu.dis.domain.DisExternalSystem;
import com.yu.dis.domain.DisFieldMapping;
import com.yu.dis.domain.DisDataExchangeLog;
import com.yu.dis.service.IDisSyncTaskService;
import com.yu.dis.sync.SyncResponseParser;

/**
 * 数据同步任务Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-14
 */
@Service
public class DisSyncTaskServiceImpl implements IDisSyncTaskService
{
    private static final Logger log = LoggerFactory.getLogger(DisSyncTaskServiceImpl.class);

    @Autowired
    private DisSyncTaskMapper disSyncTaskMapper;

    @Autowired
    private DisInterfaceConfigMapper disInterfaceConfigMapper;

    @Autowired
    private DisExternalSystemMapper disExternalSystemMapper;

    @Autowired
    private DisFieldMappingMapper disFieldMappingMapper;

    @Autowired
    private DisDataExchangeLogMapper disDataExchangeLogMapper;

    @Autowired
    private SyncPersistenceMapper syncPersistenceMapper;

    @Autowired
    private ISysMessageService sysMessageService;

    /** 同步异常告警接收人用户ID（默认 admin） */
    @Value("${dis.alert.receiverId:1}")
    private Long alertReceiverId;

    private final SyncResponseParser responseParser = new SyncResponseParser();

    @Override
    public DisSyncTask selectDisSyncTaskByTaskId(Long taskId)
    {
        return disSyncTaskMapper.selectDisSyncTaskByTaskId(taskId);
    }

    @Override
    public List<DisSyncTask> selectDisSyncTaskList(DisSyncTask disSyncTask)
    {
        return disSyncTaskMapper.selectDisSyncTaskList(disSyncTask);
    }

    @Transactional
    @Override
    public int insertDisSyncTask(DisSyncTask disSyncTask)
    {
        disSyncTask.setCreateTime(DateUtils.getNowDate());
        return disSyncTaskMapper.insertDisSyncTask(disSyncTask);
    }

    @Transactional
    @Override
    public int updateDisSyncTask(DisSyncTask disSyncTask)
    {
        disSyncTask.setUpdateTime(DateUtils.getNowDate());
        return disSyncTaskMapper.updateDisSyncTask(disSyncTask);
    }

    @Transactional
    @Override
    public int deleteDisSyncTaskByTaskId(Long taskId)
    {
        return disSyncTaskMapper.deleteDisSyncTaskByTaskId(taskId);
    }

    @Transactional
    @Override
    public int deleteDisSyncTaskByTaskIds(Long[] taskIds)
    {
        return disSyncTaskMapper.deleteDisSyncTaskByTaskIds(taskIds);
    }

    /**
     * 执行同步任务（D1 完整链路 + D2 可靠性增强：调用—解析—落库—留痕）
     * 1. 加载任务/接口/外部系统配置
     * 2. D2：生成同步批次号；增量模式下以水位(lastWatermark)替换 ${watermark} 占位符
     * 3. 携带鉴权头调用外部接口（失败按配置重试）
     * 4. 依字段映射解析响应并 upsert 落库（幂等）
     * 5. 仅整体成功时推进增量水位；更新任务执行记录并写入数据交换日志（含批次号/重推标记）
     * 6. 失败时推送 SysMessage 站内告警
     */
    @Override
    public Map<String, Object> executeSyncTask(Long taskId)
    {
        return executeSyncTaskInternal(taskId, false);
    }

    /**
     * 人工重推（D2）：手动触发一次补偿同步，交换日志标记 retryFlag=1，便于全链路留痕
     */
    @Override
    public Map<String, Object> rePushTask(Long taskId)
    {
        return executeSyncTaskInternal(taskId, true);
    }

    private Map<String, Object> executeSyncTaskInternal(Long taskId, boolean isRepush)
    {
        Map<String, Object> result = new HashMap<>();
        // 1. 获取任务配置
        DisSyncTask task = disSyncTaskMapper.selectDisSyncTaskByTaskId(taskId);
        if (task == null)
        {
            result.put("success", false);
            result.put("message", "任务不存在");
            return result;
        }
        // 2. 获取接口配置
        DisInterfaceConfig interfaceConfig = disInterfaceConfigMapper.selectDisInterfaceConfigByInterfaceId(task.getInterfaceId());
        if (interfaceConfig == null)
        {
            result.put("success", false);
            result.put("message", "接口配置不存在");
            return result;
        }
        if (interfaceConfig.getRequestPath() == null || interfaceConfig.getRequestPath().trim().isEmpty())
        {
            result.put("success", false);
            result.put("message", "接口请求路径未配置");
            return result;
        }
        DisExternalSystem externalSystem = task.getSystemId() != null
                ? disExternalSystemMapper.selectDisExternalSystemBySystemId(task.getSystemId()) : null;
        // 3. D2：批次号 + 增量水位；D3：拼装变量表并对 URL/请求模板做通用 ${变量} 替换
        java.util.Date execStart = new java.util.Date();
        String batchNo = "SYNC-" + taskId + "-" + new SimpleDateFormat("yyyyMMddHHmmss").format(execStart)
                + (isRepush ? "-R" : "");
        Map<String, String> vars = new HashMap<>();
        vars.put("batchNo", batchNo);
        vars.put("taskId", String.valueOf(taskId));
        vars.put("timestamp", String.valueOf(execStart.getTime()));
        vars.put("date", new SimpleDateFormat("yyyy-MM-dd").format(execStart));
        vars.put("datetime", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(execStart));
        if ("1".equals(task.getSyncMode()))
        {
            java.util.Date wmBase = task.getLastWatermark() != null ? task.getLastWatermark()
                    : (task.getLastExecuteTime() != null ? task.getLastExecuteTime() : new java.util.Date(0));
            vars.put("watermark", new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(wmBase));
        }
        String requestPath = resolveTemplate(interfaceConfig.getRequestPath(), vars, true);
        String requestTemplate = resolveTemplate(interfaceConfig.getRequestTemplate(), vars, false);
        // D3：请求路径为相对路径时拼接外部系统 baseUrl
        String requestUrl = buildRequestUrl(externalSystem != null ? externalSystem.getBaseUrl() : null, requestPath);
        // 4. 执行HTTP调用（含重试）
        int maxRetry = interfaceConfig.getRetryCount() != null ? interfaceConfig.getRetryCount() : 0;
        int timeoutSeconds = interfaceConfig.getTimeoutSeconds() != null ? interfaceConfig.getTimeoutSeconds() : 30;
        String requestMethod = interfaceConfig.getRequestMethod() != null
                ? interfaceConfig.getRequestMethod().toUpperCase() : "GET";
        int attempt = 0;
        boolean callSuccess = false;
        int statusCode = 0;
        String lastError = "";
        String responseBody = "";
        long startTime = System.currentTimeMillis();
        while (attempt <= maxRetry && !callSuccess)
        {
            attempt++;
            try
            {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                        .build();
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(requestUrl))
                        .timeout(Duration.ofSeconds(timeoutSeconds));
                applyAuthHeaders(requestBuilder, externalSystem, timeoutSeconds);
                if ("GET".equals(requestMethod))
                {
                    requestBuilder.GET();
                }
                else if ("POST".equals(requestMethod))
                {
                    String body = requestTemplate != null ? requestTemplate : "";
                    requestBuilder.header("Content-Type", "application/json");
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
                }
                else
                {
                    requestBuilder.method(requestMethod, HttpRequest.BodyPublishers.noBody());
                }
                HttpResponse<String> response = client.send(requestBuilder.build(),
                        HttpResponse.BodyHandlers.ofString());
                statusCode = response.statusCode();
                responseBody = response.body();
                if (statusCode >= 200 && statusCode < 300)
                {
                    callSuccess = true;
                    log.info("同步任务[{}]第{}次调用成功，状态码：{}", task.getTaskName(), attempt, statusCode);
                }
                else
                {
                    lastError = "HTTP状态码：" + statusCode;
                    log.warn("同步任务[{}]第{}次调用失败，状态码：{}", task.getTaskName(), attempt, statusCode);
                }
            }
            catch (Exception e)
            {
                lastError = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
                log.warn("同步任务[{}]第{}次调用异常：{}", task.getTaskName(), attempt, e.toString());
            }
            if (!callSuccess && attempt <= maxRetry)
            {
                try { Thread.sleep(1000L * attempt); } catch (InterruptedException ie) { break; }
            }
        }
        long elapsed = System.currentTimeMillis() - startTime;

        // 5. 解析—落库
        int persistedRows = 0;
        List<String> tables = new ArrayList<>();
        String persistError = null;
        if (callSuccess)
        {
            try
            {
                List<DisFieldMapping> mappings = disFieldMappingMapper.selectEnabledByInterfaceId(interfaceConfig.getInterfaceId());
                Map<String, List<Map<String, Object>>> parsed = responseParser.parse(responseBody, mappings);
                for (Map.Entry<String, List<Map<String, Object>>> entry : parsed.entrySet())
                {
                    String table = entry.getKey();
                    List<Map<String, Object>> rows = entry.getValue();
                    Set<String> colSet = new LinkedHashSet<>();
                    for (Map<String, Object> r : rows) { colSet.addAll(r.keySet()); }
                    List<String> columns = new ArrayList<>(colSet);
                    List<List<Object>> valueRows = new ArrayList<>();
                    for (Map<String, Object> r : rows)
                    {
                        List<Object> vr = new ArrayList<>();
                        for (String c : columns) { vr.add(r.get(c)); }
                        valueRows.add(vr);
                    }
                    if (columns.isEmpty() || valueRows.isEmpty()) { continue; }
                    syncPersistenceMapper.upsertBatch(table, columns, valueRows);
                    persistedRows += rows.size();
                    tables.add(table);
                    log.info("同步任务[{}]落库表 {} 行数={}", task.getTaskName(), table, rows.size());
                }
            }
            catch (Exception pe)
            {
                persistError = pe.getMessage();
                log.error("同步任务[{}]落库异常：{}", task.getTaskName(), pe.getMessage(), pe);
            }
        }

        // 6. 更新任务执行记录
        java.util.Date now = new java.util.Date();
        task.setLastExecuteTime(now);
        task.setNextExecuteTime(computeNextExecuteTime(task.getCronExpression(), now));
        task.setExecuteCount((task.getExecuteCount() != null ? task.getExecuteCount() : 0) + 1);
        if (!callSuccess)
        {
            task.setFailCount((task.getFailCount() != null ? task.getFailCount() : 0) + 1);
        }
        // D2：仅整体成功（调用+落库）时推进增量水位，失败保持旧水位以便重推补数
        if (callSuccess && persistError == null && "1".equals(task.getSyncMode()))
        {
            task.setLastWatermark(execStart);
        }
        task.setUpdateTime(now);
        disSyncTaskMapper.updateDisSyncTask(task);

        // 7. 写入数据交换日志（留痕，无论成功失败；含批次号与重推标记）
        String error = (lastError != null && !lastError.isEmpty()) ? lastError : persistError;
        writeExchangeLog(task, requestMethod, statusCode, callSuccess, responseBody, error, elapsed,
                requestUrl, requestTemplate, batchNo, isRepush);

        // 8. 返回结果
        boolean overallSuccess = callSuccess && persistError == null;
        if (!overallSuccess)
        {
            // D2：异常告警——推送站内消息给运维负责人，便于及时人工重推
            sendSyncAlert(task, batchNo, error, isRepush);
        }
        result.put("success", overallSuccess);
        result.put("batchNo", batchNo);
        result.put("repush", isRepush);
        result.put("callSuccess", callSuccess);
        result.put("attempts", attempt);
        result.put("elapsedMs", elapsed);
        result.put("statusCode", statusCode);
        result.put("persistedRows", persistedRows);
        result.put("tables", tables);
        if (persistError != null) { result.put("persistError", persistError); }
        result.put("message", buildMessage(callSuccess, attempt, elapsed, persistedRows, persistError, lastError));
        log.info("同步任务[{}]执行完成：调用成功={}，落库={}行，耗时={}ms", task.getTaskName(), callSuccess, persistedRows, elapsed);
        return result;
    }

    /** OAuth2 令牌缓存：systemId -> (accessToken, 过期时间戳ms)，提前 30s 视为过期 */
    private static final Map<Long, OAuth2Token> OAUTH2_TOKEN_CACHE = new ConcurrentHashMap<>();

    /** OAuth2 令牌缓存条目 */
    private static class OAuth2Token
    {
        final String accessToken;
        final long expireAt;
        OAuth2Token(String accessToken, long expireAt)
        {
            this.accessToken = accessToken;
            this.expireAt = expireAt;
        }
    }

    /** 依据外部系统认证配置注入鉴权头（TOKEN/BASIC/OAUTH2） */
    private void applyAuthHeaders(HttpRequest.Builder builder, DisExternalSystem system, int timeoutSeconds) throws Exception
    {
        if (system == null || system.getAuthType() == null || "NONE".equalsIgnoreCase(system.getAuthType()))
        {
            return;
        }
        JSONObject cfg = null;
        try
        {
            if (system.getAuthConfig() != null && !system.getAuthConfig().trim().isEmpty())
            {
                cfg = JSON.parseObject(system.getAuthConfig());
            }
        }
        catch (Exception ignore) { /* authConfig 非法 JSON，忽略 */ }
        String type = system.getAuthType().toUpperCase();
        if ("TOKEN".equals(type))
        {
            String headerName = cfg != null && cfg.getString("headerName") != null ? cfg.getString("headerName") : "Authorization";
            String prefix = cfg != null && cfg.getString("tokenPrefix") != null ? cfg.getString("tokenPrefix") : "";
            String token = cfg != null ? cfg.getString("token") : null;
            if (token != null)
            {
                builder.header(headerName, (prefix.isEmpty() ? "" : prefix + " ") + token);
            }
        }
        else if ("BASIC".equals(type) && cfg != null)
        {
            String user = cfg.getString("username");
            String pass = cfg.getString("password");
            if (user != null)
            {
                String raw = user + ":" + (pass == null ? "" : pass);
                builder.header("Authorization", "Basic " + Base64.getEncoder().encodeToString(raw.getBytes(StandardCharsets.UTF_8)));
            }
        }
        else if ("OAUTH2".equals(type))
        {
            // D3：client_credentials 模式令牌交换，令牌按系统缓存至过期前 30s
            if (cfg == null || cfg.getString("tokenUrl") == null || cfg.getString("clientId") == null)
            {
                throw new Exception("OAUTH2 认证配置不完整：authConfig 需包含 tokenUrl/clientId/clientSecret");
            }
            String token = fetchOAuth2Token(system.getSystemId(), cfg, timeoutSeconds);
            String headerName = cfg.getString("headerName") != null ? cfg.getString("headerName") : "Authorization";
            String prefix = cfg.getString("tokenPrefix") != null ? cfg.getString("tokenPrefix") : "Bearer";
            builder.header(headerName, (prefix.isEmpty() ? "" : prefix + " ") + token);
        }
    }

    /**
     * 获取 OAuth2 访问令牌（D3）：缓存命中直接返回；否则以 client_credentials 向 tokenUrl
     * POST 换取令牌，并按 expires_in 缓存（提前 30s 过期）。失败抛异常由调用方重试。
     */
    private String fetchOAuth2Token(Long systemId, JSONObject cfg, int timeoutSeconds) throws Exception
    {
        OAuth2Token cached = OAUTH2_TOKEN_CACHE.get(systemId);
        if (cached != null && cached.expireAt > System.currentTimeMillis())
        {
            return cached.accessToken;
        }
        StringBuilder form = new StringBuilder("grant_type=client_credentials")
                .append("&client_id=").append(URLEncoder.encode(cfg.getString("clientId"), StandardCharsets.UTF_8))
                .append("&client_secret=").append(URLEncoder.encode(cfg.getString("clientSecret") == null ? "" : cfg.getString("clientSecret"), StandardCharsets.UTF_8));
        if (cfg.getString("scope") != null && !cfg.getString("scope").isEmpty())
        {
            form.append("&scope=").append(URLEncoder.encode(cfg.getString("scope"), StandardCharsets.UTF_8));
        }
        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds)).build();
        HttpRequest tokenRequest = HttpRequest.newBuilder()
                .uri(URI.create(cfg.getString("tokenUrl")))
                .timeout(Duration.ofSeconds(timeoutSeconds))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
                .build();
        HttpResponse<String> tokenResponse = client.send(tokenRequest, HttpResponse.BodyHandlers.ofString());
        if (tokenResponse.statusCode() < 200 || tokenResponse.statusCode() >= 300)
        {
            throw new Exception("OAuth2 令牌获取失败，状态码：" + tokenResponse.statusCode());
        }
        JSONObject tokenJson = JSON.parseObject(tokenResponse.body());
        String accessToken = tokenJson != null ? tokenJson.getString("access_token") : null;
        if (accessToken == null || accessToken.isEmpty())
        {
            throw new Exception("OAuth2 令牌响应缺少 access_token");
        }
        long expiresIn = tokenJson.getLongValue("expires_in");
        if (expiresIn <= 30) { expiresIn = 3600; }
        OAUTH2_TOKEN_CACHE.put(systemId, new OAuth2Token(accessToken, System.currentTimeMillis() + (expiresIn - 30) * 1000L));
        log.info("OAuth2 令牌获取成功（系统ID={}），有效期{}秒", systemId, expiresIn);
        return accessToken;
    }

    /**
     * D3：通用 ${变量} 占位符替换。urlValue=true 时对替换值做 URL 编码，
     * 未知占位符保持原样以便排查配置错误。
     */
    private String resolveTemplate(String text, Map<String, String> vars, boolean urlValue)
    {
        if (text == null || text.isEmpty())
        {
            return text;
        }
        String out = text;
        for (Map.Entry<String, String> e : vars.entrySet())
        {
            String placeholder = "${" + e.getKey() + "}";
            if (out.contains(placeholder))
            {
                String value = urlValue
                        ? URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8)
                        : e.getValue();
                out = out.replace(placeholder, value);
            }
        }
        return out;
    }

    /** D3：请求路径为相对路径时拼接外部系统 baseUrl；已是绝对 URL 或无 baseUrl 则原样返回 */
    private String buildRequestUrl(String baseUrl, String requestPath)
    {
        if (requestPath == null)
        {
            return null;
        }
        String lower = requestPath.toLowerCase();
        if (lower.startsWith("http://") || lower.startsWith("https://")
                || baseUrl == null || baseUrl.trim().isEmpty())
        {
            return requestPath;
        }
        String base = baseUrl.trim();
        String path = requestPath.startsWith("/") ? requestPath.substring(1) : requestPath;
        return base.endsWith("/") ? base + path : base + "/" + path;
    }

    /** 依据 Cron 表达式计算下次执行时间（Spring CronExpression，解析失败返回 null） */
    private java.util.Date computeNextExecuteTime(String cron, java.util.Date from)
    {
        if (cron == null || cron.trim().isEmpty())
        {
            return null;
        }
        try
        {
            org.springframework.scheduling.support.CronExpression ce =
                    org.springframework.scheduling.support.CronExpression.parse(cron.trim());
            java.time.LocalDateTime base = java.time.LocalDateTime.ofInstant(from.toInstant(), java.time.ZoneId.systemDefault());
            java.time.LocalDateTime next = ce.next(base);
            return next == null ? null : java.util.Date.from(next.atZone(java.time.ZoneId.systemDefault()).toInstant());
        }
        catch (Exception e)
        {
            log.warn("Cron表达式解析失败[{}]：{}", cron, e.getMessage());
            return null;
        }
    }

    /** 同步失败推送站内告警（D2：异常告警） */
    private void sendSyncAlert(DisSyncTask task, String batchNo, String error, boolean isRepush)
    {
        try
        {
            SysMessage msg = new SysMessage();
            msg.setReceiverId(alertReceiverId);
            msg.setMsgType("0");
            msg.setTitle(((isRepush ? "[重推失败] " : "[同步任务失败] ") + task.getTaskName()));
            String content = String.format("同步任务[%s]（批次%s）执行失败：%s。请核查数据交换日志，必要时人工重推。",
                    task.getTaskName(), batchNo, error == null || error.isEmpty() ? "未知错误" : error);
            if (content.length() > 490) { content = content.substring(0, 490); }
            msg.setContent(content);
            msg.setBusinessType("dis_sync");
            msg.setBusinessId(task.getTaskId());
            sysMessageService.sendMessage(msg);
        }
        catch (Exception e)
        {
            log.error("发送同步告警消息失败：{}", e.getMessage());
        }
    }

    /** 写入数据交换日志（含批次号与人工重推标记） */
    private void writeExchangeLog(DisSyncTask task, String method, int statusCode,
                                  boolean success, String responseBody, String error, long elapsed,
                                  String requestUrl, String requestBody, String batchNo, boolean isRepush)
    {
        try
        {
            DisDataExchangeLog exchangeLog = new DisDataExchangeLog();
            exchangeLog.setSystemId(task.getSystemId());
            exchangeLog.setInterfaceId(task.getInterfaceId());
            String url = requestUrl;
            if (url != null && url.length() > 500) { url = url.substring(0, 500); }
            exchangeLog.setRequestUrl(url);
            exchangeLog.setRequestMethod(method);
            exchangeLog.setRequestData(requestBody);
            String resp = responseBody;
            if (resp != null && resp.length() > 20000) { resp = resp.substring(0, 20000); }
            exchangeLog.setResponseData(resp);
            exchangeLog.setResponseCode(statusCode);
            exchangeLog.setStatus(success ? "0" : "1");
            if (error != null && error.length() > 2000) { error = error.substring(0, 2000); }
            exchangeLog.setErrorMsg(error);
            exchangeLog.setExecuteTime(new java.util.Date());
            exchangeLog.setCostTime(elapsed);
            String op;
            try { op = SecurityUtils.getUsername(); } catch (Exception e) { op = "system"; }
            exchangeLog.setOperator(op);
            exchangeLog.setSyncBatchNo(batchNo);
            exchangeLog.setRetryFlag(isRepush ? "1" : "0");
            exchangeLog.setCreateTime(new java.util.Date());
            disDataExchangeLogMapper.insertDisDataExchangeLog(exchangeLog);
        }
        catch (Exception e)
        {
            log.error("写入数据交换日志失败：{}", e.getMessage(), e);
        }
    }

    /** 组装人类可读的执行结果消息 */
    private String buildMessage(boolean callSuccess, int attempt, long elapsed, int persistedRows, String persistError, String lastError)
    {
        if (!callSuccess)
        {
            return String.format("同步任务调用失败，重试%d次，最后错误：%s", attempt - 1, lastError);
        }
        if (persistError != null)
        {
            return String.format("同步任务调用成功但落库异常：%s（重试%d次，耗时%dms）", persistError, attempt - 1, elapsed);
        }
        return String.format("同步任务执行成功，落库%d条，重试%d次，耗时%dms", persistedRows, attempt - 1, elapsed);
    }
}
