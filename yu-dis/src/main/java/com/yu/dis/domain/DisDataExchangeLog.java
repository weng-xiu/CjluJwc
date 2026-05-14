package com.yu.dis.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.BaseEntity;

/**
 * 数据交换日志对象 dis_data_exchange_log
 * 
 * @author ruoyi
 * @date 2026-05-14
 */
public class DisDataExchangeLog extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 日志ID */
    private Long logId;

    /** 外部系统ID */
    @Excel(name = "外部系统ID")
    private Long systemId;

    /** 接口ID */
    @Excel(name = "接口ID")
    private Long interfaceId;

    /** 请求完整URL */
    @Excel(name = "请求URL")
    private String requestUrl;

    /** 请求方式 */
    @Excel(name = "请求方式")
    private String requestMethod;

    /** 请求数据 */
    private String requestData;

    /** 响应数据 */
    private String responseData;

    /** HTTP响应码 */
    @Excel(name = "响应码")
    private Integer responseCode;

    /** 执行状态 */
    @Excel(name = "执行状态", readConverterExp = "0=成功,1=失败")
    private String status;

    /** 错误消息 */
    @Excel(name = "错误消息")
    private String errorMsg;

    /** 执行时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Excel(name = "执行时间", width = 30, dateFormat = "yyyy-MM-dd HH:mm:ss")
    private Date executeTime;

    /** 消耗时间（毫秒） */
    @Excel(name = "消耗时间(ms)")
    private Long costTime;

    /** 操作人员 */
    @Excel(name = "操作人员")
    private String operator;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    public Long getSystemId() { return systemId; }
    public void setSystemId(Long systemId) { this.systemId = systemId; }

    public Long getInterfaceId() { return interfaceId; }
    public void setInterfaceId(Long interfaceId) { this.interfaceId = interfaceId; }

    public String getRequestUrl() { return requestUrl; }
    public void setRequestUrl(String requestUrl) { this.requestUrl = requestUrl; }

    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }

    public String getRequestData() { return requestData; }
    public void setRequestData(String requestData) { this.requestData = requestData; }

    public String getResponseData() { return responseData; }
    public void setResponseData(String responseData) { this.responseData = responseData; }

    public Integer getResponseCode() { return responseCode; }
    public void setResponseCode(Integer responseCode) { this.responseCode = responseCode; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

    public Date getExecuteTime() { return executeTime; }
    public void setExecuteTime(Date executeTime) { this.executeTime = executeTime; }

    public Long getCostTime() { return costTime; }
    public void setCostTime(Long costTime) { this.costTime = costTime; }

    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("logId", getLogId())
            .append("systemId", getSystemId())
            .append("interfaceId", getInterfaceId())
            .append("requestUrl", getRequestUrl())
            .append("requestMethod", getRequestMethod())
            .append("responseCode", getResponseCode())
            .append("status", getStatus())
            .append("errorMsg", getErrorMsg())
            .append("executeTime", getExecuteTime())
            .append("costTime", getCostTime())
            .append("operator", getOperator())
            .toString();
    }
}
