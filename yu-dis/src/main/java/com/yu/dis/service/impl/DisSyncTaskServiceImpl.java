package com.yu.dis.service.impl;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.dis.mapper.DisSyncTaskMapper;
import com.yu.dis.mapper.DisInterfaceConfigMapper;
import com.yu.dis.domain.DisSyncTask;
import com.yu.dis.domain.DisInterfaceConfig;
import com.yu.dis.service.IDisSyncTaskService;

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
     * 执行同步任务
     * 1. 获取任务配置和接口配置
     * 2. 使用 HttpClient 调用外部接口
     * 3. 失败时按配置重试
     * 4. 记录执行结果
     */
    @Override
    public Map<String, Object> executeSyncTask(Long taskId)
    {
        Map<String, Object> result = new HashMap<>();
        // 1. 获取任务配置
        DisSyncTask task = disSyncTaskMapper.selectDisSyncTaskByTaskId(taskId);
        if (task == null)
        {
            result.put("message", "任务不存在");
            return result;
        }
        // 2. 获取接口配置
        DisInterfaceConfig interfaceConfig = disInterfaceConfigMapper.selectDisInterfaceConfigByInterfaceId(task.getInterfaceId());
        if (interfaceConfig == null)
        {
            result.put("message", "接口配置不存在");
            return result;
        }
        if (interfaceConfig.getRequestPath() == null || interfaceConfig.getRequestPath().trim().isEmpty())
        {
            result.put("message", "接口请求路径未配置");
            return result;
        }
        // 3. 执行HTTP调用（含重试）
        int maxRetry = interfaceConfig.getRetryCount() != null ? interfaceConfig.getRetryCount() : 3;
        int timeoutSeconds = interfaceConfig.getTimeoutSeconds() != null ? interfaceConfig.getTimeoutSeconds() : 30;
        int attempt = 0;
        boolean success = false;
        String lastError = "";
        String responseBody = "";
        long startTime = System.currentTimeMillis();
        while (attempt <= maxRetry && !success)
        {
            attempt++;
            try
            {
                HttpClient client = HttpClient.newBuilder()
                        .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                        .build();
                String requestMethod = interfaceConfig.getRequestMethod() != null
                        ? interfaceConfig.getRequestMethod().toUpperCase() : "GET";
                HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                        .uri(URI.create(interfaceConfig.getRequestPath()))
                        .timeout(Duration.ofSeconds(timeoutSeconds));
                if ("GET".equals(requestMethod))
                {
                    requestBuilder.GET();
                }
                else if ("POST".equals(requestMethod))
                {
                    String body = interfaceConfig.getRequestTemplate() != null
                            ? interfaceConfig.getRequestTemplate() : "";
                    requestBuilder.POST(HttpRequest.BodyPublishers.ofString(body));
                }
                else
                {
                    requestBuilder.method(requestMethod, HttpRequest.BodyPublishers.noBody());
                }
                HttpResponse<String> response = client.send(requestBuilder.build(),
                        HttpResponse.BodyHandlers.ofString());
                int statusCode = response.statusCode();
                responseBody = response.body();
                if (statusCode >= 200 && statusCode < 300)
                {
                    success = true;
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
            if (!success && attempt < maxRetry)
            {
                try { Thread.sleep(1000L * attempt); } catch (InterruptedException ie) { break; }
            }
        }
        long elapsed = System.currentTimeMillis() - startTime;
        // 4. 更新任务执行记录
        task.setLastExecuteTime(new java.util.Date());
        task.setExecuteCount((task.getExecuteCount() != null ? task.getExecuteCount() : 0) + 1);
        if (!success)
        {
            task.setFailCount((task.getFailCount() != null ? task.getFailCount() : 0) + 1);
        }
        task.setUpdateTime(new java.util.Date());
        disSyncTaskMapper.updateDisSyncTask(task);
        // 5. 返回结果
        result.put("success", success);
        result.put("attempts", attempt);
        result.put("elapsedMs", elapsed);
        result.put("statusCode", success ? 200 : 0);
        result.put("response", responseBody);
        result.put("lastError", lastError);
        result.put("message", success
                ? String.format("同步任务执行成功，重试%d次，耗时%dms", attempt - 1, elapsed)
                : String.format("同步任务执行失败，重试%d次，最后错误：%s", attempt - 1, lastError));
        log.info("同步任务[{}]执行完成：成功={}，耗时={}ms", task.getTaskName(), success, elapsed);
        return result;
    }
}
