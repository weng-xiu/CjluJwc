package com.yu.system.mapper;

import java.util.List;
import com.yu.system.domain.SysStudent;

/**
 * 学生信息Mapper接口
 */
public interface SysStudentMapper
{
    public List<SysStudent> selectStudentList(SysStudent student);

    public SysStudent selectStudentById(Long studentId);

    public SysStudent selectStudentByUserId(Long userId);

    public SysStudent selectStudentByCode(String studentCode);

    public int insertStudent(SysStudent student);

    public int updateStudent(SysStudent student);

    public int deleteStudentById(Long studentId);

    public int deleteStudentByIds(Long[] studentIds);

    public int deleteStudentByUserId(Long userId);

    public int batchInsertStudent(List<SysStudent> list);

    public SysStudent checkStudentCodeUnique(String studentCode);
}
