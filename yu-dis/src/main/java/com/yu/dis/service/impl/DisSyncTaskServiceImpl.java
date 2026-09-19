package com.yu.dis.service.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
     * 执行同步任务（D1 完整链路：调用—解析—落库—留痕）
     * 1. 加载任务/接口/外部系统配置
     * 2. 携带鉴权头调用外部接口（失败按配置重试）
     * 3. 依字段映射解析响应并 upsert 落库
     * 4. 更新任务执行记录并写入数据交换日志（无论成功失败均留痕）
     */
    @Override
    public Map<String, Object> executeSyncTask(Long taskId)
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
        // 3. 执行HTTP调用（含重试）
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
                        .uri(URI.create(interfaceConfig.getRequestPath()))
                        .timeout(Duration.ofSeconds(timeoutSeconds));
                applyAuthHeaders(requestBuilder, externalSystem);
                if ("GET".equals(requestMethod))
                {
                    requestBuilder.GET();
                }
                else if ("POST".equals(requestMethod))
                {
                    String body = interfaceConfig.getRequestTemplate() != null
                            ? interfaceConfig.getRequestTemplate() : "";
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
                lastError = e.getMessage();
                log.warn("同步任务[{}]第{}次调用异常：{}", task.getTaskName(), attempt, e.getMessage());
            }
            if (!callSuccess && attempt <= maxRetry)
            {
                try { Thread.sleep(1000L * attempt); } catch (InterruptedException ie) { break; }
            }
        }
        long elapsed = System.currentTimeMillis() - startTime;

        // 4. 解析—落库
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

        // 5. 更新任务执行记录
        java.util.Date now = new java.util.Date();
        task.setLastExecuteTime(now);
        task.setNextExecuteTime(computeNextExecuteTime(task.getCronExpression(), now));
        task.setExecuteCount((task.getExecuteCount() != null ? task.getExecuteCount() : 0) + 1);
        if (!callSuccess)
        {
            task.setFailCount((task.getFailCount() != null ? task.getFailCount() : 0) + 1);
        }
        task.setUpdateTime(now);
        disSyncTaskMapper.updateDisSyncTask(task);

        // 6. 写入数据交换日志（留痕，无论成功失败）
        String error = !lastError.isEmpty() ? lastError : persistError;
        writeExchangeLog(task, interfaceConfig, requestMethod, statusCode, callSuccess, responseBody, error, elapsed);

        // 7. 返回结果
        boolean overallSuccess = callSuccess && persistError == null;
        result.put("success", overallSuccess);
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

    /** 依据外部系统认证配置注入鉴权头（TOKEN/BASIC） */
    private void applyAuthHeaders(HttpRequest.Builder builder, DisExternalSystem system)
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
        // OAUTH2 需先做令牌交换，视具体对接补充，此处不发起额外请求
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

    /** 写入数据交换日志 */
    private void writeExchangeLog(DisSyncTask task, DisInterfaceConfig iface, String method, int statusCode,
                                  boolean success, String responseBody, String error, long elapsed)
    {
        try
        {
            DisDataExchangeLog exchangeLog = new DisDataExchangeLog();
            exchangeLog.setSystemId(task.getSystemId());
            exchangeLog.setInterfaceId(task.getInterfaceId());
            String url = iface.getRequestPath();
            if (url != null && url.length() > 500) { url = url.substring(0, 500); }
            exchangeLog.setRequestUrl(url);
            exchangeLog.setRequestMethod(method);
            exchangeLog.setRequestData(iface.getRequestTemplate());
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
