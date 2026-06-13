package com.yu.system.service;

import java.util.List;
import com.yu.system.domain.SysStudent;

/**
 * 学生信息 业务层
 *
 * @author yu
 */
public interface ISysStudentService
{
    /**
     * 查询学生列表
     *
     * @param student 学生信息
     * @return 学生信息集合
     */
    public List<SysStudent> selectStudentList(SysStudent student);

    /**
     * 通过学生ID查询学生
     *
     * @param studentId 学生ID
     * @return 学生对象信息
     */
    public SysStudent selectStudentById(Long studentId);

    /**
     * 通过用户ID查询学生
     *
     * @param userId 用户ID
     * @return 学生对象信息
     */
    public SysStudent selectStudentByUserId(Long userId);

    /**
     * 通过学号查询学生
     *
     * @param studentCode 学号
     * @return 学生对象信息
     */
    public SysStudent selectStudentByCode(String studentCode);

    /**
     * 新增学生信息
     *
     * @param student 学生信息
     * @return 结果
     */
    public int insertStudent(SysStudent student);

    /**
     * 修改学生信息
     *
     * @param student 学生信息
     * @return 结果
     */
    public int updateStudent(SysStudent student);

    /**
     * 批量删除学生信息
     *
     * @param studentIds 需要删除的学生ID
     * @return 结果
     */
    public int deleteStudentByIds(Long[] studentIds);

    /**
     * 校验学号是否唯一
     *
     * @param student 学生信息
     * @return 结果
     */
    public boolean checkStudentCodeUnique(SysStudent student);

    /**
     * 变更学籍状态
     *
     * @param studentId 学生ID
     * @param newStatus 新状态
     * @param remark 备注
     * @return 结果
     */
    public int changeStudentStatus(Long studentId, String newStatus, String remark);
}
