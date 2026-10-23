package com.yu.dis.service.impl;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.common.utils.DateUtils;
import com.yu.dis.arbitration.ArbitrationPreviewRequest;
import com.yu.dis.arbitration.MasterDataArbiter;
import com.yu.dis.arbitration.MasterDataArbiter.MasterCandidate;
import com.yu.dis.arbitration.MasterDataPriorityPolicy;
import com.yu.dis.domain.DisSourcePriority;
import com.yu.dis.mapper.DisSourcePriorityMapper;
import com.yu.dis.service.IDisSourcePriorityService;

/**
 * 主数据裁决数据源优先级 Service 实现（F5）。
 *
 * <p>任何增删改后立即 {@link #reloadPolicy()} 用配置表覆盖内存裁决基线，
 * 使"以谁为准"实时生效、无需发版。
 *
 * @author F5
 */
@Service
public class DisSourcePriorityServiceImpl implements IDisSourcePriorityService
{
    @Autowired
    private DisSourcePriorityMapper disSourcePriorityMapper;

    @Autowired
    private MasterDataPriorityPolicy priorityPolicy;

    @Autowired
    private MasterDataArbiter masterDataArbiter;

    @Override
    public DisSourcePriority selectDisSourcePriorityByPriorityId(Long priorityId)
    {
        return disSourcePriorityMapper.selectDisSourcePriorityByPriorityId(priorityId);
    }

    @Override
    public List<DisSourcePriority> selectDisSourcePriorityList(DisSourcePriority disSourcePriority)
    {
        return disSourcePriorityMapper.selectDisSourcePriorityList(disSourcePriority);
    }

    @Override
    @Transactional
    public int insertDisSourcePriority(DisSourcePriority disSourcePriority)
    {
        disSourcePriority.setCreateTime(DateUtils.getNowDate());
        int rows = disSourcePriorityMapper.insertDisSourcePriority(disSourcePriority);
        reloadPolicy();
        return rows;
    }

    @Override
    @Transactional
    public int updateDisSourcePriority(DisSourcePriority disSourcePriority)
    {
        disSourcePriority.setUpdateTime(DateUtils.getNowDate());
        int rows = disSourcePriorityMapper.updateDisSourcePriority(disSourcePriority);
        reloadPolicy();
        return rows;
    }

    @Override
    @Transactional
    public int deleteDisSourcePriorityByPriorityIds(Long[] priorityIds)
    {
        int rows = disSourcePriorityMapper.deleteDisSourcePriorityByPriorityIds(priorityIds);
        reloadPolicy();
        return rows;
    }

    @Override
    public void reloadPolicy()
    {
        List<DisSourcePriority> enabled = disSourcePriorityMapper.selectEnabledList();
        // 表未建立/无启用配置时保留内存基线，避免裁决口径被清空
        if (enabled == null || enabled.isEmpty())
        {
            return;
        }
        Map<String, Integer> map = new LinkedHashMap<>();
        for (DisSourcePriority p : enabled)
        {
            if (p.getSourceCode() != null && p.getPriority() != null)
            {
                map.put(p.getSourceCode(), p.getPriority());
            }
        }
        priorityPolicy.replaceAll(map);
    }

    @Override
    public Map<String, Object> preview(ArbitrationPreviewRequest request)
    {
        Map<String, Object> result = new LinkedHashMap<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        result.put("businessKey", request == null ? null : request.getBusinessKey());
        result.put("matrix", priorityPolicy.snapshot());

        if (request == null || request.getCandidates() == null || request.getCandidates().isEmpty())
        {
            result.put("candidates", rows);
            result.put("winner", null);
            result.put("note", "未提供候选源，无法裁决。");
            return result;
        }

        List<MasterCandidate> candidates = new ArrayList<>();
        for (ArbitrationPreviewRequest.Candidate c : request.getCandidates())
        {
            Date ut = DateUtils.parseDate(c.getUpdateTime());
            candidates.add(new MasterCandidate(result.get("businessKey") == null ? "" : result.get("businessKey").toString(),
                    c.getSourceCode(), ut, c.getValue()));
        }
        MasterCandidate winner = masterDataArbiter.pickWinner(candidates);

        for (MasterCandidate c : candidates)
        {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("sourceCode", c.sourceCode());
            row.put("priority", priorityPolicy.resolvePriority(c.sourceCode()));
            row.put("updateTime", c.updateTime());
            row.put("value", c.value());
            row.put("winner", winner != null && c == winner);
            rows.add(row);
        }
        result.put("candidates", rows);
        if (winner != null)
        {
            Map<String, Object> w = new LinkedHashMap<>();
            w.put("sourceCode", winner.sourceCode());
            w.put("priority", priorityPolicy.resolvePriority(winner.sourceCode()));
            w.put("updateTime", winner.updateTime());
            w.put("value", winner.value());
            result.put("winner", w);
            result.put("note", "按源优先级最高（并列则更新时间最新）裁决为权威版本。");
        }
        else
        {
            result.put("winner", null);
        }
        return result;
    }
}
