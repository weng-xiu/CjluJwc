package com.yu.oa.config;

import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Flowable 工作流引擎配置
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@Configuration
public class FlowableConfig
{
    /**
     * 初始化时部署 resources/processes 目录下的流程定义
     */
    @Bean
    public String deployDefaultProcesses(RepositoryService repositoryService)
    {
        // Flowable Spring Boot Starter 默认会自动部署 classpath:processes/*.bpmn20.xml
        // 此处预留自定义部署逻辑入口
        return "flowable-initialized";
    }

    @Bean
    public RuntimeService runtimeService(org.flowable.engine.ProcessEngine processEngine)
    {
        return processEngine.getRuntimeService();
    }

    @Bean
    public TaskService taskService(org.flowable.engine.ProcessEngine processEngine)
    {
        return processEngine.getTaskService();
    }

    @Bean
    public RepositoryService repositoryService(org.flowable.engine.ProcessEngine processEngine)
    {
        return processEngine.getRepositoryService();
    }

    @Bean
    public HistoryService historyService(org.flowable.engine.ProcessEngine processEngine)
    {
        return processEngine.getHistoryService();
    }

    @Bean
    public ManagementService managementService(org.flowable.engine.ProcessEngine processEngine)
    {
        return processEngine.getManagementService();
    }
}
