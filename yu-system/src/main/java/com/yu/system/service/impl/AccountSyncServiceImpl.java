package com.yu.system.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.brm.domain.BrmClass;
import com.yu.brm.domain.BrmTeacher;
import com.yu.brm.mapper.BrmClassMapper;
import com.yu.brm.mapper.BrmTeacherMapper;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.SecurityUtils;
import com.yu.common.utils.StringUtils;
import com.yu.system.domain.SysStudent;
import com.yu.system.domain.SysTeacherLink;
import com.yu.system.domain.SysUserRole;
import com.yu.system.mapper.SysStudentMapper;
import com.yu.system.mapper.SysTeacherLinkMapper;
import com.yu.system.mapper.SysUserMapper;
import com.yu.system.mapper.SysUserRoleMapper;
import com.yu.system.service.IAccountSyncService;
import com.yu.system.service.ISysConfigService;

/**
 * 账号同步 业务层处理
 *
 * @author yu
 */
@Service
public class AccountSyncServiceImpl implements IAccountSyncService
{
    private static final Logger log = LoggerFactory.getLogger(AccountSyncServiceImpl.class);

    /** 教师角色ID */
    private static final Long TEACHER_ROLE_ID = 6L;

    /** 学生角色ID */
    private static final Long STUDENT_ROLE_ID = 7L;

    @Autowired
    private BrmTeacherMapper brmTeacherMapper;

    @Autowired
    private BrmClassMapper brmClassMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysTeacherLinkMapper teacherLinkMapper;

    @Autowired
    private SysStudentMapper studentMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private ISysConfigService configService;

    /**
     * 从BRM同步教师账号
     *
     * @param deptId 院系ID，null则同步全部
     * @return 结果统计 {created: x, skipped: x, failed: x}
     */
    @Override
    @Transactional
    public Map<String, Integer> syncTeacherAccounts(Long deptId)
    {
        int created = 0;
        int skipped = 0;
        int failed = 0;

        // 查询 brm_teacher 列表
        BrmTeacher query = new BrmTeacher();
        if (deptId != null)
        {
            query.setDeptId(deptId);
        }
        List<BrmTeacher> teacherList = brmTeacherMapper.selectBrmTeacherList(query);

        // 获取默认密码
        String initPassword = configService.selectConfigByKey("sys.user.initPassword");
        String encryptedPassword = SecurityUtils.encryptPassword(initPassword);

        for (BrmTeacher teacher : teacherList)
        {
            try
            {
                // 检查是否已存在 sys_teacher_link（按 teacher_id），跳过已同步的
                SysTeacherLink existingLink = teacherLinkMapper.selectTeacherLinkByTeacherId(teacher.getTeacherId());
                if (StringUtils.isNotNull(existingLink))
                {
                    skipped++;
                    continue;
                }

                // 检查 sys_user 中是否已有同 userName = teacherCode 的账号
                SysUser existingUser = userMapper.selectUserByUserName(teacher.getTeacherCode());
                Long userId;

                if (StringUtils.isNull(existingUser))
                {
                    // 创建 sys_user
                    SysUser newUser = new SysUser();
                    newUser.setUserName(teacher.getTeacherCode());
                    newUser.setNickName(teacher.getTeacherName());
                    newUser.setUserCategory("teacher");
                    newUser.setAccountStatus("active");
                    newUser.setPassword(encryptedPassword);
                    newUser.setDeptId(teacher.getDeptId());
                    newUser.setStatus("0"); // 正常状态
                    newUser.setSex(teacher.getGender());
                    newUser.setPhonenumber(teacher.getPhone());
                    newUser.setEmail(teacher.getEmail());
                    userMapper.insertUser(newUser);
                    userId = newUser.getUserId();
                    log.info("[账号同步] 创建教师账号 - userName={}, teacherName={}", teacher.getTeacherCode(), teacher.getTeacherName());
                }
                else
                {
                    userId = existingUser.getUserId();
                    log.info("[账号同步] 教师账号已存在 - userName={}", teacher.getTeacherCode());
                }

                // 创建 sys_teacher_link 关联
                SysTeacherLink link = new SysTeacherLink();
                link.setUserId(userId);
                link.setTeacherId(teacher.getTeacherId());
                link.setTeacherCode(teacher.getTeacherCode());
                teacherLinkMapper.insertTeacherLink(link);

                // 更新 sys_user 的 identityId 和 userCategory
                SysUser updateUser = new SysUser();
                updateUser.setUserId(userId);
                updateUser.setUserCategory("teacher");
                updateUser.setIdentityId(teacher.getTeacherId());
                userMapper.updateUser(updateUser);

                // 分配 teacher 角色（插入 sys_user_role）
                List<SysUserRole> roleList = new ArrayList<>();
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(TEACHER_ROLE_ID);
                roleList.add(ur);
                userRoleMapper.batchUserRole(roleList);

                created++;
            }
            catch (Exception e)
            {
                failed++;
                log.error("[账号同步] 同步教师账号失败 - teacherCode={}, error={}", teacher.getTeacherCode(), e.getMessage(), e);
            }
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("failed", failed);
        log.info("[账号同步] 教师账号同步完成 - created={}, skipped={}, failed={}", created, skipped, failed);
        return result;
    }

    /**
     * 从BRM同步学生账号（基于班级）
     * 此方法作为框架预留，实际批量导入学生更多通过 Excel
     *
     * @param classId 班级ID，null则同步全部
     * @return 结果统计
     */
    @Override
    public Map<String, Integer> syncStudentAccounts(Long classId)
    {
        int created = 0;
        int skipped = 0;
        int failed = 0;

        // 如果传入 classId，读取 brm_class 获取班级信息
        if (classId != null)
        {
            BrmClass brmClass = brmClassMapper.selectBrmClassByClassId(classId);
            if (StringUtils.isNull(brmClass))
            {
                throw new ServiceException("班级不存在，classId=" + classId);
            }
            log.info("[账号同步] 基于班级同步学生账号 - className={}, classId={}", brmClass.getClassName(), classId);
            // 框架预留：基于班级信息创建学生账号框架
            // 实际批量导入学生更多通过 Excel 导入功能
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("created", created);
        result.put("skipped", skipped);
        result.put("failed", failed);
        log.info("[账号同步] 学生账号同步完成（框架预留） - created={}, skipped={}, failed={}", created, skipped, failed);
        return result;
    }
}
