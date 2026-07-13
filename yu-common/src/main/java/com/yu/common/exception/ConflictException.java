package com.yu.common.exception;

/**
 * 选课/排课冲突异常
 *
 * @author ruoyi
 */
public class ConflictException extends ServiceException
{
    private static final long serialVersionUID = 1L;

    /**
     * 冲突类型（如 TIME_CONFLICT 时间冲突、CAPACITY_FULL 容量已满等）
     */
    private String conflictType;

    /**
     * 空构造方法
     */
    public ConflictException()
    {
    }

    /**
     * 构造方法
     *
     * @param message 异常消息
     */
    public ConflictException(String message)
    {
        super(message);
    }

    /**
     * 构造方法
     *
     * @param message      异常消息
     * @param conflictType 冲突类型
     */
    public ConflictException(String message, String conflictType)
    {
        super(message);
        this.conflictType = conflictType;
    }

    /**
     * 构造方法
     *
     * @param message      异常消息
     * @param code         错误码
     * @param conflictType 冲突类型
     */
    public ConflictException(String message, Integer code, String conflictType)
    {
        super(message, code);
        this.conflictType = conflictType;
    }

    /**
     * 获取冲突类型
     *
     * @return 冲突类型
     */
    public String getConflictType()
    {
        return conflictType;
    }

    /**
     * 设置冲突类型
     *
     * @param conflictType 冲突类型
     * @return 当前对象
     */
    public ConflictException setConflictType(String conflictType)
    {
        this.conflictType = conflictType;
        return this;
    }
}
