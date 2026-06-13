package com.yu.common.enums;

/**
 * 学籍状态枚举
 *
 * @author yu
 */
public enum StudentStatus
{
    PENDING_ENROLLMENT("pending_enrollment", "待入学"),
    ENROLLED("enrolled", "在读"),
    SUSPENDED("suspended", "休学"),
    TRANSFERRED("transferred", "转专业"),
    WITHDRAWN("withdrawn", "退学"),
    GRADUATED("graduated", "已毕业");

    private final String code;
    private final String info;

    StudentStatus(String code, String info)
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
    public boolean canTransitionTo(StudentStatus target)
    {
        switch (this)
        {
            case PENDING_ENROLLMENT:
                return target == ENROLLED;
            case ENROLLED:
                return target == SUSPENDED || target == TRANSFERRED || target == WITHDRAWN || target == GRADUATED;
            case SUSPENDED:
                return target == ENROLLED || target == WITHDRAWN;
            case TRANSFERRED:
                return target == ENROLLED;
            default:
                return false;
        }
    }
}
