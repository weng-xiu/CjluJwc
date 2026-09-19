package com.yu.system.service;

import java.util.List;
import com.yu.system.domain.SysTodo;

public interface ISysTodoService
{
    public List<SysTodo> selectTodoList(SysTodo query);
    public int createTodo(SysTodo todo);
    public int completeTodo(Long todoId, Long receiverId);
    public int countPending(Long receiverId);
}
