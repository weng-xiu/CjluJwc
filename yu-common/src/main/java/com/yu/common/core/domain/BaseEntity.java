package com.yu.common.core.domain;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Entity基类
 * 
 * @author ruoyi
 */
public class BaseEntity implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 搜索值 */
    @JsonIgnore
    private String searchValue;

    /**
     * 创建者
     *
     * S-6：审计字段只读（Jackson READ_ONLY = 只序列化出去，不从请求体反序列化进来）。
     * 原先控制器直接以领域实体接 @RequestBody，客户端可自带 createBy/updateBy 一并落库：
     * AemGradeRecordMapper.xml 的 insert 写 create_by、97 处 update 写 update_by = #{updateBy}，
     * 于是「伪造 createBy」能让 S-5 里按 createBy 判归属的过滤失效。
     * 服务端一律用 setCreateBy(getUsername()) 赋值，不依赖请求体，故全局封闭无副作用。
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String createBy;

    /** 创建时间（同 createBy，审计字段不接受请求体写入） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date createTime;

    /** 更新者（同 createBy，审计字段不接受请求体写入） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private String updateBy;

    /** 更新时间（同 createBy，审计字段不接受请求体写入） */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date updateTime;

    /** 备注 */
    private String remark;

    /** 请求参数 */
    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    private Map<String, Object> params;

    public String getSearchValue()
    {
        return searchValue;
    }

    public void setSearchValue(String searchValue)
    {
        this.searchValue = searchValue;
    }

    public String getCreateBy()
    {
        return createBy;
    }

    public void setCreateBy(String createBy)
    {
        this.createBy = createBy;
    }

    public Date getCreateTime()
    {
        return createTime;
    }

    public void setCreateTime(Date createTime)
    {
        this.createTime = createTime;
    }

    public String getUpdateBy()
    {
        return updateBy;
    }

    public void setUpdateBy(String updateBy)
    {
        this.updateBy = updateBy;
    }

    public Date getUpdateTime()
    {
        return updateTime;
    }

    public void setUpdateTime(Date updateTime)
    {
        this.updateTime = updateTime;
    }

    public String getRemark()
    {
        return remark;
    }

    public void setRemark(String remark)
    {
        this.remark = remark;
    }

    public Map<String, Object> getParams()
    {
        if (params == null)
        {
            params = new HashMap<>();
        }
        return params;
    }

    public void setParams(Map<String, Object> params)
    {
        this.params = params;
    }
}
