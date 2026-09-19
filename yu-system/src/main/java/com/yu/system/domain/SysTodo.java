package com.yu.system.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.yu.common.core.domain.BaseEntity;

/**
 * 统一待办对象 sys_todo
 */
public class SysTodo extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 待办ID */
    private Long todoId;

    /** 接收人用户ID */
    private Long receiverId;

    /** 待办类型（0预警 1审批 2变更 3通知） */
    private String todoType;

    /** 标题 */
    private String title;

    /** 关联业务类型 */
    private String businessType;

    /** 关联业务ID */
    private Long businessId;

    /** 状态（0待办 1已办） */
    private String status;

    /** 完成时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date completeTime;

    public Long getTodoId() { return todoId; }
    public void setTodoId(Long todoId) { this.todoId = todoId; }
    public Long getReceiverId() { return receiverId; }
    public void setReceiverId(Long receiverId) { this.receiverId = receiverId; }
    public String getTodoType() { return todoType; }
    public void setTodoType(String todoType) { this.todoType = todoType; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getBusinessType() { return businessType; }
    public void setBusinessType(String businessType) { this.businessType = businessType; }
    public Long getBusinessId() { return businessId; }
    public void setBusinessId(Long businessId) { this.businessId = businessId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getCompleteTime() { return completeTime; }
    public void setCompleteTime(Date completeTime) { this.completeTime = completeTime; }
}
