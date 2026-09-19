package com.yu.system.service;

import java.util.List;
import com.yu.system.domain.SysMessage;

public interface ISysMessageService
{
    public List<SysMessage> selectMessageList(SysMessage query);
    public int sendMessage(SysMessage message);
    public int batchSendMessages(List<SysMessage> messages);
    public int markRead(Long messageId, Long receiverId);
    public int markAllRead(Long receiverId);
    public int countUnread(Long receiverId);
}
