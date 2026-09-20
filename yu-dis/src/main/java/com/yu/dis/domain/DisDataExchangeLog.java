package com.yu.dis.domain;

import java.util.Date;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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

    /** 同步批次号（D2：一次执行的全链路留痕标识） */
    @Excel(name = "同步批次号")
    private String syncBatchNo;

    /** 是否人工重推（0否 1是） */
    @Excel(name = "人工重推", readConverterExp = "0=否,1=是")
    private String retryFlag;

    public Long getLogId() { return logId; }
    public void setLogId(Long logId) { this.logId = logId; }

    @NotNull(message = "外部系统ID不能为空")
    public Long getSystemId() { return systemId; }
    public void setSystemId(Long systemId) { this.systemId = systemId; }

    @NotNull(message = "接口ID不能为空")
    public Long getInterfaceId() { return interfaceId; }
    public void setInterfaceId(Long interfaceId) { this.interfaceId = interfaceId; }

    @NotBlank(message = "请求URL不能为空")
    @Size(min = 0, max = 500, message = "请求URL长度不能超过500个字符")
    public String getRequestUrl() { return requestUrl; }
    public void setRequestUrl(String requestUrl) { this.requestUrl = requestUrl; }

    @NotBlank(message = "请求方式不能为空")
    @Size(min = 0, max = 10, message = "请求方式长度不能超过10个字符")
    public String getRequestMethod() { return requestMethod; }
    public void setRequestMethod(String requestMethod) { this.requestMethod = requestMethod; }

    public String getRequestData() { return requestData; }
    public void setRequestData(String requestData) { this.requestData = requestData; }

    public String getResponseData() { return responseData; }
    public void setResponseData(String responseData) { this.responseData = responseData; }

    public Integer getResponseCode() { return responseCode; }
    public void setResponseCode(Integer responseCode) { this.responseCode = responseCode; }

    @NotBlank(message = "执行状态不能为空")
    @Size(min = 0, max = 1, message = "执行状态长度不能超过1个字符")
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Size(min = 0, max = 500, message = "错误消息长度不能超过500个字符")
    public String getErrorMsg() { return errorMsg; }
    public void setErrorMsg(String errorMsg) { this.errorMsg = errorMsg; }

    public Date getExecuteTime() { return executeTime; }
    public void setExecuteTime(Date executeTime) { this.executeTime = executeTime; }

    public Long getCostTime() { return costTime; }
    public void setCostTime(Long costTime) { this.costTime = costTime; }

    @Size(min = 0, max = 50, message = "操作人员长度不能超过50个字符")
    public String getOperator() { return operator; }
    public void setOperator(String operator) { this.operator = operator; }

    @Size(min = 0, max = 64, message = "同步批次号长度不能超过64个字符")
    public String getSyncBatchNo() { return syncBatchNo; }
    public void setSyncBatchNo(String syncBatchNo) { this.syncBatchNo = syncBatchNo; }

    public String getRetryFlag() { return retryFlag; }
    public void setRetryFlag(String retryFlag) { this.retryFlag = retryFlag; }

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
