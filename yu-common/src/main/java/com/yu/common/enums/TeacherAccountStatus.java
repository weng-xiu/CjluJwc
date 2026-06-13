package com.yu.common.enums;

/**
 * 教师账号状态枚举
 *
 * @author yu
 */
public enum TeacherAccountStatus
{
    PENDING_ENTRY("pending_entry", "待入职"),
    ACTIVE("active", "在职"),
    ON_LEAVE("on_leave", "请假"),
    TRANSFERRED("transferred", "调岗"),
    RESIGNED("resigned", "离职"),
    RETIRED("retired", "退休");

    private final String code;
    private final String info;

    TeacherAccountStatus(String code, String info)
    {
        this.code = code;
        this.info = info;
    }

    public String getCode()
    {
        return code;
    }

    public String getInfo()
    {
        return info;
    }

    /**
     * 校验状态流转是否合法
     */
    public boolean canTransitionTo(TeacherAccountStatus target)
    {
        switch (this)
        {
            case PENDING_ENTRY:
                return target == ACTIVE;
            case ACTIVE:
                return target == ON_LEAVE || target == TRANSFERRED || target == RESIGNED || target == RETIRED;
            case ON_LEAVE:
                return target == ACTIVE || target == RESIGNED;
            case TRANSFERRED:
                return target == ACTIVE || target == RESIGNED;
            default:
                return false;
        }
    }
}
