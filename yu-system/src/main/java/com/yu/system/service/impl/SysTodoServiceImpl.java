package com.yu.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.common.utils.DateUtils;
import com.yu.system.domain.SysTodo;
import com.yu.system.mapper.SysTodoMapper;
import com.yu.system.service.ISysTodoService;

@Service
public class SysTodoServiceImpl implements ISysTodoService
{
    @Autowired
    private SysTodoMapper sysTodoMapper;

    @Override
    public List<SysTodo> selectTodoList(SysTodo query)
    {
        return sysTodoMapper.selectList(query);
    }

    @Override
    public int createTodo(SysTodo todo)
    {
        todo.setStatus("0");
        todo.setCreateTime(DateUtils.getNowDate());
        return sysTodoMapper.insert(todo);
    }

    @Override
    public int completeTodo(Long todoId, Long receiverId)
    {
        return sysTodoMapper.complete(todoId, receiverId);
    }

    @Override
    public int countPending(Long receiverId)
    {
        return sysTodoMapper.countPending(receiverId);
    }
}
