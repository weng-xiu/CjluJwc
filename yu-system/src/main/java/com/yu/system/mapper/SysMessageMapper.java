package com.yu.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.system.domain.SysMessage;

public interface SysMessageMapper
{
    public SysMessage selectById(Long messageId);
    public List<SysMessage> selectList(SysMessage query);
    public int insert(SysMessage message);
    public int batchInsert(@Param("list") List<SysMessage> list);
    public int markRead(@Param("messageId") Long messageId, @Param("receiverId") Long receiverId);
    public int markAllRead(@Param("receiverId") Long receiverId);
    public int countUnread(@Param("receiverId") Long receiverId);
}
