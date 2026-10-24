package com.yu.dis.service;

import java.util.List;
import com.yu.dis.domain.DisQualityTicket;

/**
 * 数据对接质量异常工单 Service 接口（F5 质量看板异常工单化）。
 */
public interface IDisQualityTicketService
{
    public DisQualityTicket selectDisQualityTicketByTicketId(Long ticketId);

    public List<DisQualityTicket> selectDisQualityTicketList(DisQualityTicket disQualityTicket);

    public int insertDisQualityTicket(DisQualityTicket disQualityTicket);

    public int updateDisQualityTicket(DisQualityTicket disQualityTicket);

    public int deleteDisQualityTicketByTicketIds(Long[] ticketIds);

    /**
     * 异常检测：扫描全部同步任务，对失败率超过阈值的任务自动生成待处理工单（幂等，不重复生成）。
     *
     * @param threshold 失败率阈值（百分比，如 20 表示失败率 ≥20% 视为异常）
     * @param operator  操作人
     * @return 本次新生成的工单数量
     */
    public int detectAndGenerate(double threshold, String operator);
}
