package com.yu.system.service.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import jakarta.validation.Validator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import com.yu.common.annotation.DataScope;
import com.yu.common.constant.CacheConstants;
import com.yu.common.constant.UserConstants;
import com.yu.common.core.domain.entity.SysRole;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.redis.RedisCache;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.SecurityUtils;
import com.yu.common.utils.StringUtils;
import com.yu.common.utils.bean.BeanValidators;
import com.yu.common.utils.spring.SpringUtils;
import com.yu.system.domain.SysPost;
import com.yu.system.domain.SysUserPost;
import com.yu.system.domain.SysUserRole;
import com.yu.system.mapper.SysPostMapper;
import com.yu.system.mapper.SysRoleMapper;
import com.yu.system.mapper.SysUserMapper;
import com.yu.system.mapper.SysUserPostMapper;
import com.yu.system.mapper.SysUserRoleMapper;
import com.yu.system.service.ISysConfigService;
import com.yu.system.service.ISysDeptService;
import com.yu.system.service.ISysUserService;

/**
 * 用户 业务层处理
 * 
 * @author ruoyi
 */
@Service
public class SysUserServiceImpl implements ISysUserService
{
    private static final Logger log = LoggerFactory.getLogger(SysUserServiceImpl.class);

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private SysRoleMapper roleMapper;

    @Autowired
    private SysPostMapper postMapper;

    @Autowired
    private SysUserRoleMapper userRoleMapper;

    @Autowired
    private SysUserPostMapper userPostMapper;

    @Autowired
    private ISysConfigService configService;

    @Autowired
    private ISysDeptService deptService;

    @Autowired
    protected Validator validator;

    @Autowired
    private RedisCache redisCache;

    /**
     * 根据条件分页查询用户列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysUser> selectUserList(SysUser user)
    {
        return userMapper.selectUserList(user);
    }

    /**
     * 根据条件分页查询已分配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysUser> selectAllocatedList(SysUser user)
    {
        return userMapper.selectAllocatedList(user);
    }

    /**
     * 根据条件分页查询未分配用户角色列表
     * 
     * @param user 用户信息
     * @return 用户信息集合信息
     */
    @Override
    @DataScope(deptAlias = "d", userAlias = "u")
    public List<SysUser> selectUnallocatedList(SysUser user)
    {
        return userMapper.selectUnallocatedList(user);
    }

    /**
     * 通过用户名查询用户
     * 
     * @param userName 用户名
     * @return 用户对象信息
     */
    @Override
    public SysUser selectUserByUserName(String userName)
    {
        String cacheKey = CacheConstants.SYS_USER_NAME_KEY + userName;
        SysUser user = redisCache.getCacheObject(cacheKey);
        if (user != null)
        {
            return user;
        }
        user = userMapper.selectUserByUserName(userName);
        if (user != null)
        {
            redisCache.setCacheObject(cacheKey, user, CacheConstants.SYS_USER_CACHE_EXPIRATION, TimeUnit.MINUTES);
        }
        return user;
    }

    /**
     * 通过用户ID查询用户
     * 
     * @param userId 用户ID
     * @return 用户对象信息
     */
    @Override
    public SysUser selectUserById(Long userId)
    {
        String cacheKey = CacheConstants.SYS_USER_ID_KEY + userId;
        SysUser user = redisCache.getCacheObject(cacheKey);
        if (user != null)
        {
            return user;
        }
        user = userMapper.selectUserById(userId);
        if (user != null)
        {
            redisCache.setCacheObject(cacheKey, user, CacheConstants.SYS_USER_CACHE_EXPIRATION, TimeUnit.MINUTES);
        }
        return user;
    }

    /**
     * 查询用户所属角色组
     * 
     * @param userName 用户名
     * @return 结果
     */
    @Override
    public String selectUserRoleGroup(String userName)
    {
        List<SysRole> list = roleMapper.selectRolesByUserName(userName);
        if (CollectionUtils.isEmpty(list))
        {
            return StringUtils.EMPTY;
        }
        return list.stream().map(SysRole::getRoleName).collect(Collectors.joining(","));
    }

    /**
     * 查询用户所属岗位组
     * 
     * @param userName 用户名
     * @return 结果
     */
    @Override
    public String selectUserPostGroup(String userName)
    {
        List<SysPost> list = postMapper.selectPostsByUserName(userName);
        if (CollectionUtils.isEmpty(list))
        {
            return StringUtils.EMPTY;
        }
        return list.stream().map(SysPost::getPostName).collect(Collectors.joining(","));
    }

    /**
     * 校验用户名称是否唯一
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean checkUserNameUnique(SysUser user)
    {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkUserNameUnique(user.getUserName());
        return isUnique(info, userId) ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
    }

    /**
     * 校验手机号码是否唯一
     *
     * @param user 用户信息
     * @return
     */
    @Override
    public boolean checkPhoneUnique(SysUser user)
    {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkPhoneUnique(user.getPhonenumber());
        return isUnique(info, userId) ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
    }

    /**
     * 校验email是否唯一
     *
     * @param user 用户信息
     * @return
     */
    @Override
    public boolean checkEmailUnique(SysUser user)
    {
        Long userId = StringUtils.isNull(user.getUserId()) ? -1L : user.getUserId();
        SysUser info = userMapper.checkEmailUnique(user.getEmail());
        return isUnique(info, userId) ? UserConstants.UNIQUE : UserConstants.NOT_UNIQUE;
    }

    /**
     * 通用唯一性校验
     *
     * @param existUser 数据库中已存在的用户
     * @param currentUserId 当前待校验的用户ID
     * @return true=唯一 false=不唯一
     */
    private boolean isUnique(SysUser existUser, Long currentUserId)
    {
        Long existUserId = StringUtils.isNull(existUser) ? -1L : existUser.getUserId();
        return StringUtils.isNull(existUser) || existUserId.equals(currentUserId);
    }

    /**
     * 校验用户是否允许操作
     * 
     * @param user 用户信息
     */
    @Override
    public void checkUserAllowed(SysUser user)
    {
        if (StringUtils.isNotNull(user.getUserId()) && user.isAdmin())
        {
            throw new ServiceException("不允许操作超级管理员用户");
        }
    }

    /**
     * 校验用户是否有数据权限
     * 
     * @param userId 用户id
     */
    @Override
    public void checkUserDataScope(Long userId)
    {
        if (!SecurityUtils.isAdmin())
        {
            SysUser user = new SysUser();
            user.setUserId(userId);
            List<SysUser> users = SpringUtils.getAopProxy(this).selectUserList(user);
            if (StringUtils.isEmpty(users))
            {
                throw new ServiceException("没有权限访问用户数据！");
            }
        }
    }

    /**
     * 新增保存用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @Transactional
    public int insertUser(SysUser user)
    {
        // 新增用户信息
        int rows = userMapper.insertUser(user);
        // 新增用户岗位关联
        insertUserPost(user);
        // 新增用户与角色管理
        insertUserRole(user);
        log.info("[用户管理] 新增用户 - userId={}, userName={}", user.getUserId(), user.getUserName());
        return rows;
    }

    /**
     * 注册用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public boolean registerUser(SysUser user)
    {
        return userMapper.insertUser(user) > 0;
    }

    /**
     * 修改保存用户信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updateUser(SysUser user)
    {
        Long userId = user.getUserId();
        // 删除用户与角色关联
        userRoleMapper.deleteUserRoleByUserId(userId);
        // 新增用户与角色管理
        insertUserRole(user);
        // 删除用户与岗位关联
        userPostMapper.deleteUserPostByUserId(userId);
        // 新增用户与岗位管理
        insertUserPost(user);
        int rows = userMapper.updateUser(user);
        if (rows > 0)
        {
            clearUserCache(userId, user.getUserName());
            log.info("[用户管理] 修改用户 - userId={}, userName={}", user.getUserId(), user.getUserName());
        }
        return rows;
    }

    /**
     * 用户授权角色
     * 
     * @param userId 用户ID
     * @param roleIds 角色组
     */
    @Override
    @Transactional
    public void insertUserAuth(Long userId, Long[] roleIds)
    {
        userRoleMapper.deleteUserRoleByUserId(userId);
        insertUserRole(userId, roleIds);
    }

    /**
     * 修改用户状态
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserStatus(SysUser user)
    {
        int rows = userMapper.updateUserStatus(user.getUserId(), user.getStatus());
        if (rows > 0)
        {
            clearUserCache(user.getUserId(), null);
        }
        return rows;
    }

    /**
     * 修改用户基本信息
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int updateUserProfile(SysUser user)
    {
        return userMapper.updateUser(user);
    }

    /**
     * 修改用户头像
     * 
     * @param userId 用户ID
     * @param avatar 头像地址
     * @return 结果
     */
    @Override
    public boolean updateUserAvatar(Long userId, String avatar)
    {
        return userMapper.updateUserAvatar(userId, avatar) > 0;
    }

    /**
     * 更新用户登录信息（IP和登录时间）
     * 
     * @param userId 用户ID
     * @param loginIp 登录IP地址
     * @param loginDate 登录时间
     * @return 结果
     */
    public void updateLoginInfo(Long userId, String loginIp, Date loginDate)
    {
        userMapper.updateLoginInfo(userId, loginIp, loginDate);
    }

    /**
     * 重置用户密码
     * 
     * @param user 用户信息
     * @return 结果
     */
    @Override
    public int resetPwd(SysUser user)
    {
        int rows = userMapper.resetUserPwd(user.getUserId(), user.getPassword());
        if (rows > 0)
        {
            clearUserCache(user.getUserId(), null);
        }
        return rows;
    }

    /**
     * 重置用户密码
     * 
     * @param userId 用户ID
     * @param password 密码
     * @return 结果
     */
    @Override
    public int resetUserPwd(Long userId, String password)
    {
        return userMapper.resetUserPwd(userId, password);
    }

    /**
     * 新增用户角色信息
     * 
     * @param user 用户对象
     */
    public void insertUserRole(SysUser user)
    {
        this.insertUserRole(user.getUserId(), user.getRoleIds());
    }

    /**
     * 新增用户岗位信息
     * 
     * @param user 用户对象
     */
    public void insertUserPost(SysUser user)
    {
        Long[] posts = user.getPostIds();
        if (StringUtils.isNotEmpty(posts))
        {
            // 新增用户与岗位管理
            List<SysUserPost> list = new ArrayList<SysUserPost>(posts.length);
            for (Long postId : posts)
            {
                SysUserPost up = new SysUserPost();
                up.setUserId(user.getUserId());
                up.setPostId(postId);
                list.add(up);
            }
            userPostMapper.batchUserPost(list);
        }
    }

    /**
     * 新增用户角色信息
     * 
     * @param userId 用户ID
     * @param roleIds 角色组
     */
    public void insertUserRole(Long userId, Long[] roleIds)
    {
        if (StringUtils.isNotEmpty(roleIds))
        {
            // 新增用户与角色管理
            List<SysUserRole> list = new ArrayList<SysUserRole>(roleIds.length);
            for (Long roleId : roleIds)
            {
                SysUserRole ur = new SysUserRole();
                ur.setUserId(userId);
                ur.setRoleId(roleId);
                list.add(ur);
            }
            userRoleMapper.batchUserRole(list);
        }
    }

    /**
     * 通过用户ID删除用户
     * 
     * @param userId 用户ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteUserById(Long userId)
    {
        // 删除用户与角色关联
        userRoleMapper.deleteUserRoleByUserId(userId);
        // 删除用户与岗位表
        userPostMapper.deleteUserPostByUserId(userId);
        return userMapper.deleteUserById(userId);
    }

    /**
     * 批量删除用户信息
     * 
     * @param userIds 需要删除的用户ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteUserByIds(Long[] userIds)
    {
        for (Long userId : userIds)
        {
            checkUserAllowed(new SysUser(userId));
            checkUserDataScope(userId);
        }
        // 删除用户与角色关联
        userRoleMapper.deleteUserRole(userIds);
        // 删除用户与岗位关联
        userPostMapper.deleteUserPost(userIds);
        int rows = userMapper.deleteUserByIds(userIds);
        if (rows > 0)
        {
            for (Long userId : userIds)
            {
                clearUserCache(userId, null);
            }
            log.info("[用户管理] 删除用户 - userIds={}", Arrays.toString(userIds));
        }
        return rows;
    }

    /**
     * 导入用户数据
     * 
     * @param userList 用户数据列表
     * @param isUpdateSupport 是否更新支持，如果已存在，则进行更新数据
     * @param operName 操作用户
     * @return 结果
     */
    @Override
    public String importUser(List<SysUser> userList, Boolean isUpdateSupport, String operName)
    {
        if (StringUtils.isNull(userList) || userList.size() == 0)
        {
            throw new ServiceException("导入用户数据不能为空！");
        }
        int successNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        for (SysUser user : userList)
        {
            try
            {
                boolean isExist = checkImportUserExists(user);
                if (!isExist)
                {
                    successNum += importNewUser(user, operName, successMsg);
                }
                else if (isUpdateSupport)
                {
                    successNum += importUpdateUser(user, operName, successMsg);
                }
                else
                {
                    failureNum++;
                    failureMsg.append("<br/>").append(failureNum).append("、账号 ").append(user.getUserName()).append(" 已存在");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                String msg = "<br/>" + failureNum + "、账号 " + user.getUserName() + " 导入失败：";
                failureMsg.append(msg).append(e.getMessage());
                log.error(msg, e);
            }
        }
        return buildImportResult(successNum, failureNum, successMsg, failureMsg);
    }

    /**
     * 校验导入用户是否已存在
     *
     * @param user 用户信息
     * @return true=已存在 false=不存在
     */
    private boolean checkImportUserExists(SysUser user)
    {
        return StringUtils.isNotNull(userMapper.selectUserByUserName(user.getUserName()));
    }

    /**
     * 导入新增用户
     *
     * @param user 用户信息
     * @param operName 操作人
     * @param successMsg 成功消息
     * @return 成功条数
     */
    private int importNewUser(SysUser user, String operName, StringBuilder successMsg)
    {
        BeanValidators.validateWithException(validator, user);
        deptService.checkDeptDataScope(user.getDeptId());
        String password = configService.selectConfigByKey("sys.user.initPassword");
        user.setPassword(SecurityUtils.encryptPassword(password));
        user.setCreateBy(operName);
        userMapper.insertUser(user);
        successMsg.append("<br/>").append("账号 ").append(user.getUserName()).append(" 导入成功");
        return 1;
    }

    /**
     * 导入更新用户
     *
     * @param user 用户信息
     * @param operName 操作人
     * @param successMsg 成功消息
     * @return 成功条数
     */
    private int importUpdateUser(SysUser user, String operName, StringBuilder successMsg)
    {
        SysUser u = userMapper.selectUserByUserName(user.getUserName());
        BeanValidators.validateWithException(validator, user);
        checkUserAllowed(u);
        checkUserDataScope(u.getUserId());
        deptService.checkDeptDataScope(user.getDeptId());
        user.setUserId(u.getUserId());
        user.setDeptId(u.getDeptId());
        user.setUpdateBy(operName);
        userMapper.updateUser(user);
        clearUserCache(user.getUserId(), user.getUserName());
        successMsg.append("<br/>").append("账号 ").append(user.getUserName()).append(" 更新成功");
        return 1;
    }

    /**
     * 构建导入结果消息
     *
     * @param successNum 成功数
     * @param failureNum 失败数
     * @param successMsg 成功消息
     * @param failureMsg 失败消息
     * @return 结果消息
     */
    private String buildImportResult(int successNum, int failureNum, StringBuilder successMsg, StringBuilder failureMsg)
    {
        if (failureNum > 0)
        {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        else
        {
            successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条，数据如下：");
        }
        return successMsg.toString();
    }

    /**
     * 清除用户缓存
     */
    private void clearUserCache(Long userId, String userName)
    {
        if (userId != null)
        {
            redisCache.deleteObject(CacheConstants.SYS_USER_ID_KEY + userId);
        }
        if (StringUtils.isNotEmpty(userName))
        {
            redisCache.deleteObject(CacheConstants.SYS_USER_NAME_KEY + userName);
        }
    }

    /**
     * 变更用户生命周期状态
     *
     * @param userId 用户ID
     * @param newStatus 新状态
     * @return 结果
     */
    @Override
    public int changeLifecycleStatus(Long userId, String newStatus)
    {
        SysUser user = new SysUser();
        user.setUserId(userId);
        user.setAccountStatus(newStatus);
        // 清除缓存
        clearUserCache(userId, selectUserById(userId).getUserName());
        log.info("[用户管理] 变更生命周期状态 - userId={}, newStatus={}", userId, newStatus);
        return userMapper.updateUser(user);
    }
}
