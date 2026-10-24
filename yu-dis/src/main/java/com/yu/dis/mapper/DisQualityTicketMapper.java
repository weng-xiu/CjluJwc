package com.yu.dis.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.dis.domain.DisQualityTicket;

/**
 * 数据对接质量异常工单 Mapper（F5）。
 */
public interface DisQualityTicketMapper
{
    public DisQualityTicket selectDisQualityTicketByTicketId(Long ticketId);

    public List<DisQualityTicket> selectDisQualityTicketList(DisQualityTicket disQualityTicket);

    public int insertDisQualityTicket(DisQualityTicket disQualityTicket);

    public int updateDisQualityTicket(DisQualityTicket disQualityTicket);

    public int deleteDisQualityTicketByTicketIds(Long[] ticketIds);

    /** 判断某同步任务是否已有未关闭（状态 0/1）的同类型工单，避免异常检测重复生成 */
    public int countOpenByTaskAndType(@Param("taskId") Long taskId, @Param("anomalyType") String anomalyType);
}
