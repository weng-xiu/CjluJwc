package com.yu.system.service;

import java.util.Map;

/**
 * 账号同步 业务层
 *
 * @author yu
 */
public interface IAccountSyncService
{
    /**
     * 从BRM同步教师账号
     *
     * @param deptId 院系ID，null则同步全部
     * @return 结果统计 {created: x, skipped: x, failed: x}
     */
    public Map<String, Integer> syncTeacherAccounts(Long deptId);

    /**
     * 从BRM同步学生账号（基于班级）
     *
     * @param classId 班级ID，null则同步全部
     * @return 结果统计
     */
    public Map<String, Integer> syncStudentAccounts(Long classId);
}
