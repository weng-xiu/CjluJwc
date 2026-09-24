package com.yu.sam.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.sam.mapper.SamGraduationProcedureMapper;
import com.yu.sam.mapper.SamProcedureItemMapper;
import com.yu.sam.mapper.SamProcedureStepMapper;
import com.yu.sam.domain.SamGraduationProcedure;
import com.yu.sam.domain.SamProcedureItem;
import com.yu.sam.domain.SamProcedureStep;
import com.yu.sam.service.ISamGraduationProcedureService;

/**
 * 毕业离校手续Service业务层处理
 * S7c：环节可配置（sam_procedure_step）+ 办理明细（sam_procedure_item），
 * 支持自动判定（按数据源同步办理状态）与手续完成状态自动重算。
 *
 * @author ruoyi
 * @date 2026-09-24
 */
@Service
public class SamGraduationProcedureServiceImpl implements ISamGraduationProcedureService
{
    /** 环节编码 -> 旧列名映射（LEGACY 自动判定的白名单列，杜绝 SQL 注入） */
    private static final Map<String, String> LEGACY_COLUMN = new HashMap<>();
    static {
        LEGACY_COLUMN.put("LIBRARY", "library_cleared");
        LEGACY_COLUMN.put("FINANCE", "finance_cleared");
        LEGACY_COLUMN.put("DORMITORY", "dormitory_cleared");
        LEGACY_COLUMN.put("CARD", "card_returned");
    }

    @Autowired
    private SamGraduationProcedureMapper samGraduationProcedureMapper;

    @Autowired
    private SamProcedureStepMapper samProcedureStepMapper;

    @Autowired
    private SamProcedureItemMapper samProcedureItemMapper;

    @Override
    public SamGraduationProcedure selectSamGraduationProcedureByProcedureId(Long procedureId) { return samGraduationProcedureMapper.selectSamGraduationProcedureByProcedureId(procedureId); }
    @Override
    public List<SamGraduationProcedure> selectSamGraduationProcedureList(SamGraduationProcedure samGraduationProcedure) { return samGraduationProcedureMapper.selectSamGraduationProcedureList(samGraduationProcedure); }

    @Override
    @Transactional
    public int insertSamGraduationProcedure(SamGraduationProcedure samGraduationProcedure) { samGraduationProcedure.setCreateTime(DateUtils.getNowDate()); return samGraduationProcedureMapper.insertSamGraduationProcedure(samGraduationProcedure); }
    @Override
    @Transactional
    public int updateSamGraduationProcedure(SamGraduationProcedure samGraduationProcedure) { samGraduationProcedure.setUpdateTime(DateUtils.getNowDate()); return samGraduationProcedureMapper.updateSamGraduationProcedure(samGraduationProcedure); }
    @Override
    @Transactional
    public int deleteSamGraduationProcedureByProcedureId(Long procedureId) { return samGraduationProcedureMapper.deleteSamGraduationProcedureByProcedureId(procedureId); }
    @Override
    @Transactional
    public int deleteSamGraduationProcedureByProcedureIds(Long[] procedureIds) { return samGraduationProcedureMapper.deleteSamGraduationProcedureByProcedureIds(procedureIds); }

    @Override
    @Transactional
    public int initProcedures(Long studentId, String operator)
    {
        int created = samGraduationProcedureMapper.initProceduresForGraduates(studentId, operator);
        // 为全部（或该生）手续记录按启用环节补齐办理明细
        samProcedureItemMapper.initItemsForProcedures(studentId);
        return created;
    }

    @Override
    public List<SamProcedureItem> listItems(Long procedureId)
    {
        return samProcedureItemMapper.selectItemsByProcedureId(procedureId);
    }

    @Override
    @Transactional
    public int toggleItem(Long procedureId, Long stepId, boolean done, String operator)
    {
        SamGraduationProcedure proc = samGraduationProcedureMapper.selectSamGraduationProcedureByProcedureId(procedureId);
        if (proc == null)
        {
            throw new ServiceException("离校手续记录不存在");
        }
        Date now = DateUtils.getNowDate();
        if (done)
        {
            samProcedureItemMapper.upsertItemDone(procedureId, proc.getStudentId(), stepId, "0", operator, now);
        }
        else
        {
            SamProcedureItem item = samProcedureItemMapper.selectItemByProcedureAndStep(procedureId, stepId);
            if (item != null)
            {
                SamProcedureItem upd = new SamProcedureItem();
                upd.setItemId(item.getItemId());
                upd.setItemStatus("0");
                upd.setCheckType("0");
                upd.setCheckBy(operator);
                upd.setUpdateTime(now);
                samProcedureItemMapper.updateSamProcedureItem(upd);
            }
        }
        recomputeProcedureStatus(procedureId, now);
        return 1;
    }

    @Override
    @Transactional
    public int autoCheck(Long studentId, String operator)
    {
        List<SamProcedureStep> steps = samProcedureStepMapper.selectActiveSteps();
        Date now = DateUtils.getNowDate();
        Set<Long> affected = new HashSet<>();
        for (SamProcedureStep step : steps)
        {
            String source = step.getAutoCheckType();
            if (StringUtils.isBlank(source) || "NONE".equalsIgnoreCase(source))
            {
                continue; // 手动登记环节不参与自动判定
            }
            String column = null;
            if ("LEGACY".equalsIgnoreCase(source))
            {
                column = LEGACY_COLUMN.get(step.getStepKey());
                if (column == null)
                {
                    continue; // 自定义环节无旧列，跳过
                }
            }
            List<Long> okStudents = samProcedureItemMapper.selectAutoCheckStudentIds(source.toUpperCase(), column);
            if (okStudents == null || okStudents.isEmpty())
            {
                continue;
            }
            for (Map<String, Object> pr : samProcedureItemMapper.selectProcedureIdsByStudents(okStudents))
            {
                Long pid = toLong(pr.get("procedureId"));
                Long sid = toLong(pr.get("studentId"));
                if (pid == null) continue;
                if (studentId != null && !studentId.equals(sid)) continue;
                samProcedureItemMapper.upsertItemDone(pid, sid, step.getStepId(), "1", "auto:" + source, now);
                affected.add(pid);
            }
        }
        for (Long pid : affected)
        {
            recomputeProcedureStatus(pid, now);
        }
        return affected.size();
    }

    @Override
    public Map<String, Object> statOverview()
    {
        Map<String, Object> result = new LinkedHashMap<>();
        Map<String, Object> proc = samGraduationProcedureMapper.statOverview();
        result.put("procedure", proc == null ? new LinkedHashMap<>() : proc);
        result.put("steps", samProcedureStepMapper.statProcedureSteps());
        return result;
    }

    /**
     * 重算手续完成状态并回写旧四列（保持向后兼容）：
     * 全部必办环节已办=2已完成，部分已办=1办理中，否则=0未办理。
     */
    private void recomputeProcedureStatus(Long procedureId, Date now)
    {
        List<SamProcedureItem> items = samProcedureItemMapper.selectItemsByProcedureId(procedureId);
        Set<String> doneKeys = new HashSet<>();
        boolean anyDone = false;
        for (SamProcedureItem it : items)
        {
            if ("1".equals(it.getItemStatus()))
            {
                anyDone = true;
                if (it.getStepKey() != null) doneKeys.add(it.getStepKey().toUpperCase());
            }
        }
        List<SamProcedureStep> steps = samProcedureStepMapper.selectActiveSteps();
        boolean allRequiredDone = true;
        for (SamProcedureStep st : steps)
        {
            if ("1".equals(st.getRequiredFlag()) && !doneKeys.contains(st.getStepKey().toUpperCase()))
            {
                allRequiredDone = false;
                break;
            }
        }
        if (steps.isEmpty())
        {
            allRequiredDone = false; // 无启用环节时不自动判完成
        }
        String status = allRequiredDone ? "2" : (anyDone ? "1" : "0");

        SamGraduationProcedure upd = new SamGraduationProcedure();
        upd.setProcedureId(procedureId);
        upd.setProcedureStatus(status);
        if ("2".equals(status))
        {
            upd.setCompleteDate(now);
        }
        // 回写旧四列，保证既有查询/导出与页面仍可用
        upd.setLibraryCleared(doneKeys.contains("LIBRARY") ? "1" : "0");
        upd.setFinanceCleared(doneKeys.contains("FINANCE") ? "1" : "0");
        upd.setDormitoryCleared(doneKeys.contains("DORMITORY") ? "1" : "0");
        upd.setCardReturned(doneKeys.contains("CARD") ? "1" : "0");
        upd.setUpdateTime(now);
        samGraduationProcedureMapper.updateSamGraduationProcedure(upd);
    }

    private Long toLong(Object v)
    {
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try { return Long.parseLong(String.valueOf(v)); } catch (NumberFormatException e) { return null; }
    }
}
