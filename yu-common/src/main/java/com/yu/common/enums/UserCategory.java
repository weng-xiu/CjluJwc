package com.yu.common.enums;

/**
 * 用户类别枚举
 *
 * @author yu
 */
public enum UserCategory
{
    STUDENT("student", "学生"),
    TEACHER("teacher", "教师"),
    ADMIN("admin", "管理员"),
    SECRETARY("secretary", "教学秘书");

    private final String code;
    private final String info;

    UserCategory(String code, String info)
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
}
