package com.yu.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.system.domain.SysNotifyLog;

/**
 * 消息渠道发送留痕 Mapper（S6）
 */
public interface SysNotifyLogMapper
{
    public int insertSysNotifyLog(SysNotifyLog log);

    public List<SysNotifyLog> selectSysNotifyLogList(SysNotifyLog query);

    /** 查询某业务对象的渠道发送记录（用于送达审计） */
    public List<SysNotifyLog> selectByBusiness(@Param("businessType") String businessType,
                                               @Param("businessId") Long businessId);
}
