package com.yu.sam.service.impl;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import com.yu.sam.domain.SamWarning;
import com.yu.sam.domain.SamWarningAssist;
import com.yu.sam.domain.SamWarningAssistRecord;
import com.yu.sam.mapper.SamWarningAssistMapper;
import com.yu.sam.mapper.SamWarningAssistRecordMapper;
import com.yu.sam.service.ISamWarningAssistService;
import com.yu.sam.service.ISamWarningService;
import com.yu.system.domain.SysTodo;
import com.yu.system.service.ISysConfigService;
import com.yu.system.service.ISysNotifyService;
import com.yu.system.service.ISysTodoService;

/**
 * 学业预警帮扶任务服务实现（S6 帮扶闭环）。
 * 派发/自动派发时向帮扶人推送站内通知并生成待办；跟踪记录可追溯；完结可联动解除预警。
 */
@Service
public class SamWarningAssistServiceImpl implements ISamWarningAssistService
{
    private static final Logger log = LoggerFactory.getLogger(SamWarningAssistServiceImpl.class);

    /** 帮扶状态 */
    private static final String ST_PENDING = "0";
    private static final String ST_ACTIVE = "1";
    private static final String ST_DONE = "2";
    private static final String ST_CLOSED = "3";

    @Autowired
    private SamWarningAssistMapper assistMapper;

    @Autowired
    private SamWarningAssistRecordMapper recordMapper;

    @Autowired
    private ISamWarningService samWarningService;

    @Autowired
    private ISysNotifyService sysNotifyService;

    @Autowired
    private ISysTodoService sysTodoService;

    @Autowired
    private ISysConfigService configService;

    @Override
    public SamWarningAssist selectSamWarningAssistByAssistId(Long assistId)
    {
        SamWarningAssist assist = assistMapper.selectSamWarningAssistByAssistId(assistId);
        if (assist != null)
        {
            assist.setRecords(recordMapper.selectByAssistId(assistId));
        }
        return assist;
    }

    @Override
    public List<SamWarningAssist> selectSamWarningAssistList(SamWarningAssist query)
    {
        return assistMapper.selectSamWarningAssistList(query);
    }

    @Override
    @Transactional
    public int dispatch(SamWarningAssist assist)
    {
        if (assist.getAssistStatus() == null)
        {
            assist.setAssistStatus(ST_PENDING);
        }
        if (assist.getFollowCount() == null)
        {
            assist.setFollowCount(0);
        }
        assist.setCreateTime(DateUtils.getNowDate());
        int rows = assistMapper.insertSamWarningAssist(assist);
        notifyHelper(assist);
        return rows;
    }

    @Override
    @Transactional
    public SamWarningAssist autoDispatch(SamWarning warning, String studentName, String studentNo)
    {
        if (warning == null || warning.getWarningId() == null)
        {
            return null;
        }
        // 开关与最低级别门限
        if (!"true".equalsIgnoreCase(trimCfg("sam.warning.assist.auto")))
        {
            return null;
        }
        int minLevel = parseInt(trimCfg("sam.warning.assist.minLevel"), 1);
        int level = parseInt(warning.getWarningLevel(), 0);
        if (level < minLevel)
        {
            return null;
        }
        // 幂等：同一预警只派发一次
        SamWarningAssist exist = assistMapper.selectByWarningId(warning.getWarningId());
        if (exist != null)
        {
            return exist;
        }
        SamWarningAssist assist = new SamWarningAssist();
        assist.setWarningId(warning.getWarningId());
        assist.setStudentId(warning.getStudentId());
        assist.setSemesterId(warning.getSemesterId());
        assist.setStudentName(studentName);
        assist.setStudentNo(studentNo);
        assist.setWarningType(warning.getWarningType());
        assist.setWarningLevel(warning.getWarningLevel());
        Long helperId = parseLong(trimCfg("sam.warning.assist.helperUserId"));
        if (helperId == null)
        {
            helperId = 1L; // 默认超级管理员兜底，保证任务不丢失
        }
        assist.setHelperUserId(helperId);
        assist.setHelperName(trimCfg("sam.warning.assist.helperName"));
        assist.setAssistStatus(ST_PENDING);
        assist.setFollowCount(0);
        assist.setMeasure("系统按预警级别自动派发");
        assist.setCreateBy("system");
        assist.setCreateTime(DateUtils.getNowDate());
        assistMapper.insertSamWarningAssist(assist);
        notifyHelper(assist);
        return assist;
    }

    @Override
    @Transactional
    public int claim(Long assistId, Long helperUserId, String helperName)
    {
        SamWarningAssist db = assistMapper.selectSamWarningAssistByAssistId(assistId);
        if (db == null)
        {
            throw new com.yu.common.exception.ServiceException("帮扶任务不存在");
        }
        if (!ST_PENDING.equals(db.getAssistStatus()))
        {
            throw new com.yu.common.exception.ServiceException("仅待认领任务可认领");
        }
        SamWarningAssist upd = new SamWarningAssist();
        upd.setAssistId(assistId);
        upd.setAssistStatus(ST_ACTIVE);
        if (helperUserId != null)
        {
            upd.setHelperUserId(helperUserId);
            upd.setHelperName(helperName);
        }
        upd.setClaimTime(DateUtils.getNowDate());
        upd.setUpdateTime(DateUtils.getNowDate());
        return assistMapper.updateSamWarningAssist(upd);
    }

    @Override
    @Transactional
    public int follow(Long assistId, SamWarningAssistRecord record)
    {
        SamWarningAssist db = assistMapper.selectSamWarningAssistByAssistId(assistId);
        if (db == null)
        {
            throw new com.yu.common.exception.ServiceException("帮扶任务不存在");
        }
        if (ST_DONE.equals(db.getAssistStatus()) || ST_CLOSED.equals(db.getAssistStatus()))
        {
            throw new com.yu.common.exception.ServiceException("任务已完结/关闭，不能再登记跟踪");
        }
        record.setAssistId(assistId);
        if (record.getFollowTime() == null)
        {
            record.setFollowTime(DateUtils.getNowDate());
        }
        record.setCreateTime(DateUtils.getNowDate());
        int rows = recordMapper.insertSamWarningAssistRecord(record);

        SamWarningAssist upd = new SamWarningAssist();
        upd.setAssistId(assistId);
        upd.setFollowCount((db.getFollowCount() == null ? 0 : db.getFollowCount()) + 1);
        upd.setLastFollowTime(record.getFollowTime());
        // 待认领状态下首次跟踪自动进入帮扶中
        if (ST_PENDING.equals(db.getAssistStatus()))
        {
            upd.setAssistStatus(ST_ACTIVE);
        }
        upd.setUpdateTime(DateUtils.getNowDate());
        assistMapper.updateSamWarningAssist(upd);
        return rows;
    }

    @Override
    @Transactional
    public int finish(Long assistId, String finishRemark, boolean resolveWarning)
    {
        SamWarningAssist db = assistMapper.selectSamWarningAssistByAssistId(assistId);
        if (db == null)
        {
            throw new com.yu.common.exception.ServiceException("帮扶任务不存在");
        }
        SamWarningAssist upd = new SamWarningAssist();
        upd.setAssistId(assistId);
        upd.setAssistStatus(ST_DONE);
        upd.setFinishTime(DateUtils.getNowDate());
        upd.setFinishRemark(finishRemark);
        upd.setUpdateTime(DateUtils.getNowDate());
        int rows = assistMapper.updateSamWarningAssist(upd);

        if (resolveWarning && db.getWarningId() != null)
        {
            SamWarning w = new SamWarning();
            w.setWarningId(db.getWarningId());
            w.setIsResolved("1");
            w.setResolveDate(DateUtils.getNowDate());
            w.setResolveRemark(StringUtils.isNotEmpty(finishRemark) ? finishRemark : "帮扶任务已完成，自动解除预警");
            w.setUpdateTime(DateUtils.getNowDate());
            samWarningService.updateSamWarning(w);
        }
        // 完结后办结相关待办
        completeHelperTodo(db);
        return rows;
    }

    @Override
    @Transactional
    public int closeAssist(Long assistId, String finishRemark)
    {
        SamWarningAssist db = assistMapper.selectSamWarningAssistByAssistId(assistId);
        if (db == null)
        {
            throw new com.yu.common.exception.ServiceException("帮扶任务不存在");
        }
        SamWarningAssist upd = new SamWarningAssist();
        upd.setAssistId(assistId);
        upd.setAssistStatus(ST_CLOSED);
        upd.setFinishTime(DateUtils.getNowDate());
        upd.setFinishRemark(finishRemark);
        upd.setUpdateTime(DateUtils.getNowDate());
        int rows = assistMapper.updateSamWarningAssist(upd);
        completeHelperTodo(db);
        return rows;
    }

    @Override
    @Transactional
    public int deleteSamWarningAssistByAssistIds(Long[] assistIds)
    {
        recordMapper.deleteByAssistIds(assistIds);
        return assistMapper.deleteSamWarningAssistByAssistIds(assistIds);
    }

    // ================= 内部辅助 =================

    private void notifyHelper(SamWarningAssist assist)
    {
        if (assist.getHelperUserId() == null)
        {
            return;
        }
        String title = "【学业预警帮扶】" + StringUtils.nvl(assist.getStudentName(), "学号" + assist.getStudentNo()) + " 需帮扶跟进";
        String content = "学生：" + StringUtils.nvl(assist.getStudentName(), "-")
                + "（" + StringUtils.nvl(assist.getStudentNo(), "-") + "）\n"
                + "预警级别：" + levelName(assist.getWarningLevel()) + "\n"
                + "请及时认领并开展帮扶。";
        try
        {
            sysNotifyService.notifyUserSite(assist.getHelperUserId(), "0", title, content, "warningAssist", assist.getAssistId());
            SysTodo todo = new SysTodo();
            todo.setReceiverId(assist.getHelperUserId());
            todo.setTodoType("0");
            todo.setTitle(title);
            todo.setBusinessType("warningAssist");
            todo.setBusinessId(assist.getAssistId());
            todo.setCreateBy("system");
            sysTodoService.createTodo(todo);
        }
        catch (Exception e)
        {
            log.error("帮扶任务通知帮扶人失败 assistId={}", assist.getAssistId(), e);
        }
    }

    private void completeHelperTodo(SamWarningAssist assist)
    {
        if (assist.getHelperUserId() == null || assist.getAssistId() == null)
        {
            return;
        }
        try
        {
            SysTodo query = new SysTodo();
            query.setReceiverId(assist.getHelperUserId());
            query.setBusinessType("warningAssist");
            List<SysTodo> todos = sysTodoService.selectTodoList(query);
            for (SysTodo t : todos)
            {
                if ("0".equals(t.getStatus()) && assist.getAssistId().equals(t.getBusinessId()))
                {
                    sysTodoService.completeTodo(t.getTodoId(), assist.getHelperUserId());
                }
            }
        }
        catch (Exception e)
        {
            log.error("办结帮扶待办失败 assistId={}", assist.getAssistId(), e);
        }
    }

    private String levelName(String level)
    {
        if ("2".equals(level)) return "高危";
        if ("1".equals(level)) return "严重";
        return "一般";
    }

    private String trimCfg(String key)
    {
        String v = configService.selectConfigByKey(key);
        return v == null ? null : v.trim();
    }

    private int parseInt(String v, int def)
    {
        try { return StringUtils.isEmpty(v) ? def : Integer.parseInt(v); }
        catch (NumberFormatException e) { return def; }
    }

    private Long parseLong(String v)
    {
        try { return StringUtils.isEmpty(v) ? null : Long.parseLong(v); }
        catch (NumberFormatException e) { return null; }
    }
}
