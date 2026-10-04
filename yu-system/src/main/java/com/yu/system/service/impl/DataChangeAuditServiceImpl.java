package com.yu.system.service.impl;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import com.yu.common.utils.MdcUtils;
import com.yu.common.utils.SecurityUtils;
import com.yu.common.utils.bean.FieldChange;
import com.yu.common.utils.ip.IpUtils;
import com.yu.system.domain.DataChangeAudit;
import com.yu.system.mapper.DataChangeAuditMapper;
import com.yu.system.service.IDataChangeAuditService;

/**
 * 字段级数据变更流水 服务实现（K1 合规③）
 *
 * @author yu
 */
@Service
public class DataChangeAuditServiceImpl implements IDataChangeAuditService
{
    private static final Logger log = LoggerFactory.getLogger(DataChangeAuditServiceImpl.class);

    /** 无登录上下文（定时任务/系统触发）时的操作人占位 */
    private static final String SYSTEM_OPERATOR = "system";

    @Autowired
    private DataChangeAuditMapper dataChangeAuditMapper;

    @Override
    public void recordDiff(String entityType, String entityTypeLabel, String bizId, List<FieldChange> changes)
    {
        if (changes == null || changes.isEmpty())
        {
            return;
        }
        String operName = resolveOperator();
        String operIp = resolveIp();
        String traceId = MDC.get(MdcUtils.TRACE_ID);
        List<DataChangeAudit> rows = new ArrayList<>(changes.size());
        for (FieldChange c : changes)
        {
            DataChangeAudit row = new DataChangeAudit();
            row.setEntityType(entityType);
            row.setEntityTypeLabel(entityTypeLabel);
            row.setBizId(bizId);
            row.setFieldName(c.getFieldName());
            row.setFieldLabel(StringUtils.isNotBlank(c.getFieldLabel()) ? c.getFieldLabel() : c.getFieldName());
            row.setOldValue(c.getOldValue());
            row.setNewValue(c.getNewValue());
            row.setOperName(operName);
            row.setOperIp(operIp);
            row.setTraceId(traceId);
            row.setChangeTime(DateUtils.getNowDate());
            rows.add(row);
        }
        // 与业务变更同事务：任一流水写入失败将随外层事务回滚，保证「变更」与「留痕」原子
        dataChangeAuditMapper.batchInsertDataChangeAudit(rows);
    }

    @Override
    public List<DataChangeAudit> selectDataChangeAuditList(DataChangeAudit dataChangeAudit)
    {
        return dataChangeAuditMapper.selectDataChangeAuditList(dataChangeAudit);
    }

    @Override
    public List<DataChangeAudit> selectByBiz(String entityType, String bizId)
    {
        return dataChangeAuditMapper.selectByBiz(entityType, bizId);
    }

    /**
     * 取当前登录用户名；无安全上下文（异步任务/系统触发）时回退 system，绝不因取用户名异常影响主流程。
     */
    private String resolveOperator()
    {
        try
        {
            return SecurityUtils.getUsername();
        }
        catch (Exception e)
        {
            return SYSTEM_OPERATOR;
        }
    }

    /**
     * 取请求来源 IP；非 Web 线程取不到时返回 null。
     */
    private String resolveIp()
    {
        try
        {
            return IpUtils.getIpAddr();
        }
        catch (Exception e)
        {
            log.debug("获取操作 IP 失败（非 Web 上下文），忽略：{}", e.getMessage());
            return null;
        }
    }
}
