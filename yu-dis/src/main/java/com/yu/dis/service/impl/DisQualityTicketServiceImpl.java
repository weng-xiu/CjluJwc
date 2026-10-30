package com.yu.dis.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.common.utils.DateUtils;
import com.yu.dis.domain.DisQualityTicket;
import com.yu.dis.domain.DisSyncTask;
import com.yu.dis.mapper.DisQualityTicketMapper;
import com.yu.dis.service.IDisQualityTicketService;
import com.yu.dis.service.IDisSyncTaskService;

/**
 * 数据对接质量异常工单 Service 实现（F5 质量看板异常工单化）。
 *
 * <p>异常检测复用质量看板（N2）的口径：按同步任务的执行量与失败量现算失败率，
 * 达到阈值即生成一条待处理工单；对同一任务同类未关闭工单做幂等去重，避免重复堆积。
 *
 * @author F5
 */
@Service
public class DisQualityTicketServiceImpl implements IDisQualityTicketService
{
    private static final Logger log = LoggerFactory.getLogger(DisQualityTicketServiceImpl.class);

    /** 异常类型：失败率超标 */
    private static final String TYPE_FAIL_RATE = "0";

    @Autowired
    private DisQualityTicketMapper disQualityTicketMapper;

    @Autowired
    private IDisSyncTaskService disSyncTaskService;

    @Override
    public DisQualityTicket selectDisQualityTicketByTicketId(Long ticketId)
    {
        return disQualityTicketMapper.selectDisQualityTicketByTicketId(ticketId);
    }

    @Override
    public List<DisQualityTicket> selectDisQualityTicketList(DisQualityTicket disQualityTicket)
    {
        return disQualityTicketMapper.selectDisQualityTicketList(disQualityTicket);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int insertDisQualityTicket(DisQualityTicket disQualityTicket)
    {
        disQualityTicket.setCreateTime(DateUtils.getNowDate());
        if (disQualityTicket.getStatus() == null || disQualityTicket.getStatus().isEmpty())
        {
            disQualityTicket.setStatus("0");
        }
        if (disQualityTicket.getDelFlag() == null)
        {
            disQualityTicket.setDelFlag("0");
        }
        return disQualityTicketMapper.insertDisQualityTicket(disQualityTicket);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int updateDisQualityTicket(DisQualityTicket disQualityTicket)
    {
        disQualityTicket.setUpdateTime(DateUtils.getNowDate());
        // 状态推进为已解决（2）时自动回填解决时间
        if ("2".equals(disQualityTicket.getStatus()) && disQualityTicket.getResolvedTime() == null)
        {
            disQualityTicket.setResolvedTime(DateUtils.getNowDate());
        }
        return disQualityTicketMapper.updateDisQualityTicket(disQualityTicket);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int deleteDisQualityTicketByTicketIds(Long[] ticketIds)
    {
        return disQualityTicketMapper.deleteDisQualityTicketByTicketIds(ticketIds);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int detectAndGenerate(double threshold, String operator)
    {
        List<DisSyncTask> tasks = disSyncTaskService.selectDisSyncTaskList(new DisSyncTask());
        if (tasks == null || tasks.isEmpty())
        {
            return 0;
        }
        int generated = 0;
        for (DisSyncTask t : tasks)
        {
            long exec = nz(t.getExecuteCount());
            long fail = nz(t.getFailCount());
            if (exec <= 0)
            {
                continue;
            }
            double failRate = round(fail * 100.0 / exec);
            if (failRate < threshold)
            {
                continue;
            }
            // 幂等：同任务同类未关闭工单已存在则跳过
            if (disQualityTicketMapper.countOpenByTaskAndType(t.getTaskId(), TYPE_FAIL_RATE) > 0)
            {
                continue;
            }
            DisQualityTicket ticket = new DisQualityTicket();
            ticket.setTaskId(t.getTaskId());
            ticket.setTaskName(t.getTaskName());
            ticket.setAnomalyType(TYPE_FAIL_RATE);
            ticket.setSeverity(severityOf(failRate));
            ticket.setFailRate(failRate);
            ticket.setAnomalyDetail(String.format("同步任务[%s]失败率 %.2f%%（执行 %d 次，失败 %d 次），已达到告警阈值 %.2f%%。",
                    t.getTaskName(), failRate, exec, fail, threshold));
            ticket.setStatus("0");
            ticket.setDelFlag("0");
            ticket.setCreateBy(operator);
            ticket.setCreateTime(DateUtils.getNowDate());
            ticket.setRemark("异常检测自动生成");
            generated += disQualityTicketMapper.insertDisQualityTicket(ticket);
        }
        log.info("[质量工单] 异常检测完成，阈值 {}%，新增工单 {} 条", threshold, generated);
        return generated;
    }

    /** 依据失败率分档严重程度：≥50 高，≥30 中，其余低 */
    private String severityOf(double failRate)
    {
        if (failRate >= 50) return "2";
        if (failRate >= 30) return "1";
        return "0";
    }

    private static long nz(Integer v)
    {
        return v == null ? 0L : v;
    }

    private static double round(double v)
    {
        return BigDecimal.valueOf(v).setScale(2, RoundingMode.HALF_UP).doubleValue();
    }
}
