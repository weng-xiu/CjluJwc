package com.yu.portal.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.system.domain.SysMessage;
import com.yu.system.domain.SysTodo;
import com.yu.system.service.ISysMessageService;
import com.yu.system.service.ISysTodoService;

/**
 * 门户消息与待办Controller（P5：移动端办事的消息/待办入口）
 *
 * 说明：管理端 /system/msgCenter 的列表接口带 system:msg:list 等权限字符，
 * 门户学生/教师角色无该权限会 403，故门户侧提供本人专用端点，
 * 一律强制 receiverId = 当前登录用户，不加 @PreAuthorize 权限字符（登录即可用）。
 *
 * @author ruoyi
 * @date 2026-09-25
 */
@RestController
@RequestMapping("/portal/msg")
public class PortalMsgController extends BaseController
{
    @Autowired
    private ISysMessageService sysMessageService;

    @Autowired
    private ISysTodoService sysTodoService;

    /** 我的消息列表（可按已读状态过滤） */
    @GetMapping("/list")
    public TableDataInfo list(SysMessage query)
    {
        query.setReceiverId(getUserId());
        startPage();
        List<SysMessage> list = sysMessageService.selectMessageList(query);
        return getDataTable(list);
    }

    /** 标记单条消息已读（service 内按 receiverId 校验归属） */
    @PutMapping("/read/{messageId}")
    public AjaxResult markRead(@PathVariable("messageId") Long messageId)
    {
        return toAjax(sysMessageService.markRead(messageId, getUserId()));
    }

    /** 全部标记已读 */
    @PutMapping("/readAll")
    public AjaxResult markAllRead()
    {
        return toAjax(sysMessageService.markAllRead(getUserId()));
    }

    /** 未读消息数 */
    @GetMapping("/unreadCount")
    public AjaxResult unreadCount()
    {
        return success(sysMessageService.countUnread(getUserId()));
    }

    /** 我的待办列表（默认仅未完成，可传 status 查已完成） */
    @GetMapping("/todoList")
    public TableDataInfo todoList(SysTodo query)
    {
        query.setReceiverId(getUserId());
        if (query.getStatus() == null || query.getStatus().trim().isEmpty())
        {
            query.setStatus("0");
        }
        startPage();
        List<SysTodo> list = sysTodoService.selectTodoList(query);
        return getDataTable(list);
    }

    /** 手动办结我的待办 */
    @PostMapping("/todo/complete/{todoId}")
    public AjaxResult completeTodo(@PathVariable("todoId") Long todoId)
    {
        return toAjax(sysTodoService.completeTodo(todoId, getUserId()));
    }

    /** 未完成待办数 */
    @GetMapping("/todo/pendingCount")
    public AjaxResult pendingCount()
    {
        return success(sysTodoService.countPending(getUserId()));
    }
}
