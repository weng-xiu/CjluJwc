package com.yu.web.controller.system;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.yu.common.core.controller.BaseController;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.core.page.TableDataInfo;
import com.yu.common.utils.SecurityUtils;
import com.yu.system.domain.SysMessage;
import com.yu.system.domain.SysTodo;
import com.yu.system.service.ISysMessageService;
import com.yu.system.service.ISysTodoService;

/**
 * 统一消息与待办中心 Controller
 */
@RestController
@RequestMapping("/system/msgCenter")
public class SysMsgCenterController extends BaseController
{
    @Autowired
    private ISysMessageService sysMessageService;

    @Autowired
    private ISysTodoService sysTodoService;

    /** 查询当前用户消息列表 */
    @PreAuthorize("@ss.hasPermi('system:msg:list')")
    @GetMapping("/message/list")
    public TableDataInfo messageList(SysMessage query)
    {
        query.setReceiverId(SecurityUtils.getLoginUser().getUserId());
        startPage();
        List<SysMessage> list = sysMessageService.selectMessageList(query);
        return getDataTable(list);
    }

    /** 标记消息已读 */
    @PreAuthorize("@ss.hasPermi('system:msg:edit')")
    @PutMapping("/message/read/{messageId}")
    public AjaxResult markRead(@PathVariable("messageId") Long messageId)
    {
        return toAjax(sysMessageService.markRead(messageId, SecurityUtils.getLoginUser().getUserId()));
    }

    /** 全部标记已读 */
    @PreAuthorize("@ss.hasPermi('system:msg:edit')")
    @PutMapping("/message/readAll")
    public AjaxResult markAllRead()
    {
        return toAjax(sysMessageService.markAllRead(SecurityUtils.getLoginUser().getUserId()));
    }

    /** 未读消息数 */
    @GetMapping("/message/unreadCount")
    public AjaxResult unreadCount()
    {
        int count = sysMessageService.countUnread(SecurityUtils.getLoginUser().getUserId());
        return success(count);
    }

    /** 查询当前用户待办列表 */
    @PreAuthorize("@ss.hasPermi('system:todo:list')")
    @GetMapping("/todo/list")
    public TableDataInfo todoList(SysTodo query)
    {
        query.setReceiverId(SecurityUtils.getLoginUser().getUserId());
        startPage();
        List<SysTodo> list = sysTodoService.selectTodoList(query);
        return getDataTable(list);
    }

    /** 完成待办 */
    @PreAuthorize("@ss.hasPermi('system:todo:edit')")
    @PostMapping("/todo/complete/{todoId}")
    public AjaxResult completeTodo(@PathVariable("todoId") Long todoId)
    {
        return toAjax(sysTodoService.completeTodo(todoId, SecurityUtils.getLoginUser().getUserId()));
    }

    /** 待办数量 */
    @GetMapping("/todo/pendingCount")
    public AjaxResult pendingCount()
    {
        int count = sysTodoService.countPending(SecurityUtils.getLoginUser().getUserId());
        return success(count);
    }
}
