package com.yu.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.system.domain.SysTodo;

public interface SysTodoMapper
{
    public SysTodo selectById(Long todoId);
    public List<SysTodo> selectList(SysTodo query);
    public int insert(SysTodo todo);
    public int complete(@Param("todoId") Long todoId, @Param("receiverId") Long receiverId);
    public int countPending(@Param("receiverId") Long receiverId);
}
