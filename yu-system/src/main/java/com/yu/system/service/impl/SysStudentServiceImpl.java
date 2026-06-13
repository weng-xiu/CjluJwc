package com.yu.system.service.impl;

import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.common.annotation.DataScope;
import com.yu.common.constant.CacheConstants;
import com.yu.common.constant.UserConstants;
import com.yu.common.core.redis.RedisCache;
import com.yu.common.enums.StudentStatus;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.StringUtils;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.system.domain.SysStudent;
import com.yu.system.mapper.SysStudentMapper;
import com.yu.system.mapper.SysUserMapper;
import com.yu.system.service.ISysStudentService;

/**
 * 学生信息 业务层处理
 *
 * @author yu
 */
@Service
public class SysStudentServiceImpl implements ISysStudentService
{
    private static final Logger log = LoggerFactory.getLogger(SysStudentServiceImpl.class);

    @Autowired
    private SysStudentMapper studentMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private RedisCache redisCache;

    /**
     * 查询学生列表
     *
     * @param student 学生信息
     * @return 学生信息集合
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysStudent> selectStudentList(SysStudent student)
    {
        return studentMapper.selectStudentList(student);
    }

    /**
     * 通过学生ID查询学生
     *
     * @param studentId 学生ID
     * @return 学生对象信息
     */
    @Override
    public SysStudent selectStudentById(Long studentId)
    {
        return studentMapper.selectStudentById(studentId);
    }

    /**
     * 通过用户ID查询学生
     *
     * @param userId 用户ID
     * @return 学生对象信息
     */
    @Override
    public SysStudent selectStudentByUserId(Long userId)
    {
        return studentMapper.selectStudentByUserId(userId);
    }

    /**
     * 通过学号查询学生
     *
     * @param studentCode 学号
     * @return 学生对象信息
     */
    @Override
    public SysStudent selectStudentByCode(String studentCode)
    {
        return studentMapper.selectStudentByCode(studentCode);
    }

    /**
     * 新增学生信息
     *
     * @param student 学生信息
     * @return 结果
     */
    @Override
    @Transactional
    public int insertStudent(SysStudent student)
    {
        // 校验学号唯一性
        if (!checkStudentCodeUnique(student))
        {
            throw new ServiceException("新增学生'" + student.getStudentCode() + "'失败，学号已存在");
        }
        int rows = studentMapper.insertStudent(student);
        log.info("[学生管理] 新增学生 - studentId={}, studentCode={}", student.getStudentId(), student.getStudentCode());
        return rows;
    }

    /**
     * 修改学生信息
     *
     * @param student 学生信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updateStudent(SysStudent student)
    {
        // 校验学号唯一性
        if (!checkStudentCodeUnique(student))
        {
            throw new ServiceException("修改学生'" + student.getStudentCode() + "'失败，学号已存在");
        }
        int rows = studentMapper.updateStudent(student);
        if (rows > 0)
        {
            clearStudentCache(student.getUserId());
            log.info("[学生管理] 修改学生 - studentId={}", student.getStudentId());
        }
        return rows;
    }

    /**
     * 批量删除学生信息
     *
     * @param studentIds 需要删除的学生ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteStudentByIds(Long[] studentIds)
    {
        int rows = studentMapper.deleteStudentByIds(studentIds);
        if (rows > 0)
        {
            // 同步清除 sys_user 的关联字段（清空 identity_id）
            for (Long studentId : studentIds)
            {
                SysStudent student = studentMapper.selectStudentById(studentId);
                if (student != null && student.getUserId() != null)
                {
                    SysUser user = new SysUser();
                    user.setUserId(student.getUserId());
                    user.setIdentityId(null);
                    user.setUserCategory(null);
                    userMapper.updateUser(user);
                    clearStudentCache(student.getUserId());
                }
            }
            log.info("[学生管理] 删除学生 - studentIds={}", Arrays.toString(studentIds));
        }
        return rows;
    }

    /**
     * 校验学号是否唯一
     *
     * @param student 学生信息
     * @return 结果
     */
    @Override
    public boolean checkStudentCodeUnique(SysStudent student)
    {
        Long studentId = StringUtils.isNull(student.getStudentId()) ? -1L : student.getStudentId();
        SysStudent info = studentMapper.checkStudentCodeUnique(student.getStudentCode());
        if (StringUtils.isNotNull(info) && info.getStudentId().longValue() != studentId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 变更学籍状态
     *
     * @param studentId 学生ID
     * @param newStatus 新状态
     * @param remark 备注
     * @return 结果
     */
    @Override
    @Transactional
    public int changeStudentStatus(Long studentId, String newStatus, String remark)
    {
        SysStudent student = studentMapper.selectStudentById(studentId);
        if (StringUtils.isNull(student))
        {
            throw new ServiceException("学生不存在");
        }
        // 校验状态流转合法性
        StudentStatus currentStatus = StudentStatus.valueOf(student.getStudentStatus());
        StudentStatus targetStatus = StudentStatus.valueOf(newStatus);
        if (!currentStatus.canTransitionTo(targetStatus))
        {
            throw new ServiceException(String.format("学籍状态不允许从[%s]变更为[%s]", currentStatus.getInfo(), targetStatus.getInfo()));
        }
        // 更新学生状态
        SysStudent updateStudent = new SysStudent();
        updateStudent.setStudentId(studentId);
        updateStudent.setStudentStatus(newStatus);
        updateStudent.setRemark(remark);
        int rows = studentMapper.updateStudent(updateStudent);
        if (rows > 0)
        {
            // 同步更新 sys_user.account_status
            if (student.getUserId() != null)
            {
                SysUser user = new SysUser();
                user.setUserId(student.getUserId());
                user.setAccountStatus(newStatus);
                userMapper.updateUser(user);
                clearStudentCache(student.getUserId());
            }
            log.info("[学生管理] 变更学籍状态 - studentId={}, {}->{}", studentId, currentStatus.getInfo(), targetStatus.getInfo());
        }
        return rows;
    }

    /**
     * 清除学生关联的用户缓存
     */
    private void clearStudentCache(Long userId)
    {
        if (userId != null)
        {
            redisCache.deleteObject(CacheConstants.SYS_USER_ID_KEY + userId);
            SysUser user = userMapper.selectUserById(userId);
            if (StringUtils.isNotNull(user) && StringUtils.isNotEmpty(user.getUserName()))
            {
                redisCache.deleteObject(CacheConstants.SYS_USER_NAME_KEY + user.getUserName());
            }
        }
    }
}
