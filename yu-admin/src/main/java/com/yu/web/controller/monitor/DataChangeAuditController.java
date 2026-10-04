package com.yu.web.controller.monitor;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.yu.common.core.controller.BaseController;
import com.yu.common.core.page.TableDataInfo;
import com.yu.system.domain.DataChangeAudit;
import com.yu.system.service.IDataChangeAuditService;

/**
 * 字段级数据变更流水 查询（K1 合规③：只读审计视图）
 *
 * <p>仅提供查询能力，流水由业务服务在变更事务内自动写入，不开放增删改。
 * 导出/前端页面为后续交付，本接口先支撑审计核查与接口契约测试。</p>
 *
 * @author yu
 */
@RestController
@RequestMapping("/monitor/dataAudit")
public class DataChangeAuditController extends BaseController
{
    @Autowired
    private IDataChangeAuditService dataChangeAuditService;

    /**
     * 分页查询变更流水列表（按实体类型/业务主键/字段/操作人/时间范围过滤）。
     */
    @PreAuthorize("@ss.hasPermi('monitor:dataAudit:list')")
    @GetMapping("/list")
    public TableDataInfo list(DataChangeAudit dataChangeAudit)
    {
        startPage();
        List<DataChangeAudit> list = dataChangeAuditService.selectDataChangeAuditList(dataChangeAudit);
        return getDataTable(list);
    }

    /**
     * 查询某条业务记录（entityType + bizId）的字段变更历史。
     */
    @PreAuthorize("@ss.hasPermi('monitor:dataAudit:list')")
    @GetMapping("/biz/{entityType}/{bizId}")
    public TableDataInfo bizHistory(@PathVariable("entityType") String entityType, @PathVariable("bizId") String bizId)
    {
        List<DataChangeAudit> list = dataChangeAuditService.selectByBiz(entityType, bizId);
        return getDataTable(list);
    }
}
