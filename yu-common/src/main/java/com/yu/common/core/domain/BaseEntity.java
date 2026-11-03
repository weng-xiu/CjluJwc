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

    /**
     * 数据范围过滤片段在 params 里的键（与 DataScopeAspect.DATA_SCOPE 一致）。
     * 该值会被 16 处 Mapper 用 ${params.dataScope} 原样拼进 WHERE，所以只能由切面写。
     */
    private static final String DATA_SCOPE_KEY = "dataScope";

    /**
     * S-7：行级过滤片段的唯一可信存储。
     *
     * 刻意不叫 dataScope、也没有 getter/setter：名字一旦对上 Bean 属性，Spring 的查询串绑定
     * 与 Jackson 就能写进来（“能写”本身就是漏洞）；而 Mapper 取的是 params.dataScope，
     * 不依赖该字段名。
     */
    @JsonIgnore
    private transient String trustedDataScope;

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
        // S-7：params 是 Map 属性，Spring 的 WebDataBinder 会“拿 getter 返回的 Map 直接 put”，
        // 因此 setParams 里的剥离对查询串（params[dataScope]=...）无效——实测确认。
        // 可靠拦截点在这里：每次取用都用切面写入的可信片段覆盖（没有则移除），
        // 外部塞进来的值到不了 Mapper 的 ${params.dataScope}。
        if (trustedDataScope != null)
        {
            params.put(DATA_SCOPE_KEY, trustedDataScope);
        }
        else
        {
            params.remove(DATA_SCOPE_KEY);
        }
        return params;
    }

    /**
     * S-7：写入行级过滤片段。只允许 DataScopeAspect 调用（刻意不用 set 前缀，避免成为可绑定属性）。
     *
     * @param dataScope 已由切面拼接好的 “ AND (...)” 片段；传 "" 表示不过滤
     */
    public void applyDataScope(String dataScope)
    {
        this.trustedDataScope = dataScope;
    }

    /**
     * S-7：外部绑定的 params 里不允许带 dataScope（第一层）。
     *
     * params 是 Map<String, Object> 属性，会被 JSON 请求体（{"params":{"dataScope":"..."}}）
     * 与查询串（params[dataScope]=...）绑定，而 Mapper 里的 ${params.dataScope} 是字面拼接。
     * 注意：查询串那一路由 WebDataBinder 直接对 getParams() 返回的 Map 做 put，
     * **不经过本方法**——所以真正的守点在 {@link #getParams()} 的取用覆盖（第二层）。
     * 这里先剥一层，只是让请求体绑定后的对象不残留该键。
     */
    public void setParams(Map<String, Object> params)
    {
        if (params != null && params.containsKey(DATA_SCOPE_KEY))
        {
            params = new HashMap<>(params);
            params.remove(DATA_SCOPE_KEY);
        }
        this.params = params;
    }
}
