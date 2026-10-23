package com.yu.dis.arbitration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import com.yu.dis.service.IDisSourcePriorityService;

/**
 * 主数据裁决优先级启动加载器（F5 主数据裁决前端化）。
 *
 * <p>应用启动后从 {@code dis_source_priority} 配置表加载权威优先级并覆盖
 * {@link MasterDataPriorityPolicy} 内存基线，使配置界面的调整真正驱动裁决。
 * 表尚未建立或读取异常时保留内存基线，不影响启动。
 *
 * @author F5
 */
@Component
public class MasterDataPolicyInitializer implements ApplicationRunner
{
    private static final Logger log = LoggerFactory.getLogger(MasterDataPolicyInitializer.class);

    @Autowired
    private IDisSourcePriorityService disSourcePriorityService;

    @Override
    public void run(ApplicationArguments args)
    {
        try
        {
            disSourcePriorityService.reloadPolicy();
        }
        catch (Exception e)
        {
            log.warn("主数据裁决优先级配置加载失败，回落内存基线（dis_source_priority 表可能尚未迁移）：{}", e.getMessage());
        }
    }
}
