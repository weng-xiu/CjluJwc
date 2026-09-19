package com.yu.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.common.utils.DateUtils;
import com.yu.system.domain.SysMessage;
import com.yu.system.mapper.SysMessageMapper;
import com.yu.system.service.ISysMessageService;

@Service
public class SysMessageServiceImpl implements ISysMessageService
{
    @Autowired
    private SysMessageMapper sysMessageMapper;

    @Override
    public List<SysMessage> selectMessageList(SysMessage query)
    {
        return sysMessageMapper.selectList(query);
    }

    @Override
    public int sendMessage(SysMessage message)
    {
        message.setReadStatus("0");
        message.setCreateTime(DateUtils.getNowDate());
        return sysMessageMapper.insert(message);
    }

    @Override
    public int batchSendMessages(List<SysMessage> messages)
    {
        for (SysMessage msg : messages)
        {
            msg.setReadStatus("0");
            msg.setCreateTime(DateUtils.getNowDate());
        }
        return sysMessageMapper.batchInsert(messages);
    }

    @Override
    public int markRead(Long messageId, Long receiverId)
    {
        return sysMessageMapper.markRead(messageId, receiverId);
    }

    @Override
    public int markAllRead(Long receiverId)
    {
        return sysMessageMapper.markAllRead(receiverId);
    }

    @Override
    public int countUnread(Long receiverId)
    {
        return sysMessageMapper.countUnread(receiverId);
    }
}
