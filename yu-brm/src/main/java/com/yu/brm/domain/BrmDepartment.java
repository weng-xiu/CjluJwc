package com.yu.brm.domain;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.ToStringStyle;
import com.yu.common.annotation.Excel;
import com.yu.common.core.domain.TreeEntity;

/**
 * 院系对象 brm_department
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public class BrmDepartment extends TreeEntity
{
    private static final long serialVersionUID = 1L;

    /** 院系ID */
    private Long deptId;

    /** 院系编码 */
    @Excel(name = "院系编码")
    private String deptCode;

    /** 院系名称 */
    @Excel(name = "院系名称")
    private String deptName;

    /** 负责人 */
    @Excel(name = "负责人")
    private String leader;

    /** 联系电话 */
    @Excel(name = "联系电话")
    private String phone;

    /** 邮箱 */
    @Excel(name = "邮箱")
    private String email;

    /** 状态（0正常 1停用） */
    @Excel(name = "状态", readConverterExp = "0=正常,1=停用")
    private String status;

    public Long getDeptId() { return deptId; }
    public void setDeptId(Long deptId) { this.deptId = deptId; }

    @NotBlank(message = "院系编码不能为空")
    @Size(min = 0, max = 50, message = "院系编码长度不能超过50个字符")
    public String getDeptCode() { return deptCode; }
    public void setDeptCode(String deptCode) { this.deptCode = deptCode; }

    @NotBlank(message = "院系名称不能为空")
    @Size(min = 0, max = 100, message = "院系名称长度不能超过100个字符")
    public String getDeptName() { return deptName; }
    public void setDeptName(String deptName) { this.deptName = deptName; }

    public String getLeader() { return leader; }
    public void setLeader(String leader) { this.leader = leader; }

    @Size(min = 0, max = 20, message = "联系电话长度不能超过20个字符")
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    @Size(min = 0, max = 100, message = "邮箱长度不能超过100个字符")
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return new ToStringBuilder(this,ToStringStyle.MULTI_LINE_STYLE)
            .append("deptId", getDeptId())
            .append("parentId", getParentId())
            .append("ancestors", getAncestors())
            .append("deptCode", getDeptCode())
            .append("deptName", getDeptName())
            .append("leader", getLeader())
            .append("phone", getPhone())
            .append("email", getEmail())
            .append("orderNum", getOrderNum())
            .append("status", getStatus())
            .append("createBy", getCreateBy())
            .append("createTime", getCreateTime())
            .append("updateBy", getUpdateBy())
            .append("updateTime", getUpdateTime())
            .append("remark", getRemark())
            .toString();
    }
}
