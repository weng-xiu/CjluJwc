package com.yu.system.service.impl;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.common.constant.CacheConstants;
import com.yu.common.constant.UserConstants;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.core.redis.RedisCache;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.StringUtils;
import com.yu.system.domain.SysTeacherLink;
import com.yu.system.mapper.SysTeacherLinkMapper;
import com.yu.system.mapper.SysUserMapper;
import com.yu.system.service.ISysTeacherLinkService;

/**
 * 教师账号关联 业务层处理
 *
 * @author yu
 */
@Service
public class SysTeacherLinkServiceImpl implements ISysTeacherLinkService
{
    private static final Logger log = LoggerFactory.getLogger(SysTeacherLinkServiceImpl.class);

    @Autowired
    private SysTeacherLinkMapper teacherLinkMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Autowired
    private RedisCache redisCache;

    /**
     * 查询教师关联列表
     *
     * @param link 教师关联信息
     * @return 教师关联信息集合
     */
    @Override
    public List<SysTeacherLink> selectTeacherLinkList(SysTeacherLink link)
    {
        return teacherLinkMapper.selectTeacherLinkList(link);
    }

    /**
     * 通过关联ID查询教师关联
     *
     * @param linkId 关联ID
     * @return 教师关联对象信息
     */
    @Override
    public SysTeacherLink selectTeacherLinkById(Long linkId)
    {
        return teacherLinkMapper.selectTeacherLinkById(linkId);
    }

    /**
     * 通过用户ID查询教师关联
     *
     * @param userId 用户ID
     * @return 教师关联对象信息
     */
    @Override
    public SysTeacherLink selectTeacherLinkByUserId(Long userId)
    {
        return teacherLinkMapper.selectTeacherLinkByUserId(userId);
    }

    /**
     * 新增教师关联信息
     *
     * @param link 教师关联信息
     * @return 结果
     */
    @Override
    @Transactional
    public int insertTeacherLink(SysTeacherLink link)
    {
        // 校验用户是否已关联教师
        if (!checkUserTeacherUnique(link))
        {
            throw new ServiceException("该用户已关联教师，不可重复关联");
        }
        int rows = teacherLinkMapper.insertTeacherLink(link);
        if (rows > 0)
        {
            // 同步更新 sys_user 的 userCategory='teacher' 和 identityId
            SysUser user = new SysUser();
            user.setUserId(link.getUserId());
            user.setUserCategory("teacher");
            user.setIdentityId(link.getTeacherId());
            user.setAccountStatus("active");
            userMapper.updateUser(user);
            clearUserCache(link.getUserId());
            log.info("[教师关联] 新增关联 - userId={}, teacherId={}", link.getUserId(), link.getTeacherId());
        }
        return rows;
    }

    /**
     * 修改教师关联信息
     *
     * @param link 教师关联信息
     * @return 结果
     */
    @Override
    @Transactional
    public int updateTeacherLink(SysTeacherLink link)
    {
        int rows = teacherLinkMapper.updateTeacherLink(link);
        if (rows > 0)
        {
            // 同步更新 sys_user 的 identityId
            SysUser user = new SysUser();
            user.setUserId(link.getUserId());
            user.setIdentityId(link.getTeacherId());
            userMapper.updateUser(user);
            clearUserCache(link.getUserId());
            log.info("[教师关联] 修改关联 - linkId={}", link.getLinkId());
        }
        return rows;
    }

    /**
     * 删除教师关联信息
     *
     * @param linkId 关联ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteTeacherLinkById(Long linkId)
    {
        SysTeacherLink link = teacherLinkMapper.selectTeacherLinkById(linkId);
        if (StringUtils.isNull(link))
        {
            throw new ServiceException("教师关联不存在");
        }
        int rows = teacherLinkMapper.deleteTeacherLinkById(linkId);
        if (rows > 0)
        {
            // 清除 sys_user 的关联字段
            SysUser user = new SysUser();
            user.setUserId(link.getUserId());
            user.setIdentityId(null);
            user.setUserCategory(null);
            userMapper.updateUser(user);
            clearUserCache(link.getUserId());
            log.info("[教师关联] 删除关联 - linkId={}", linkId);
        }
        return rows;
    }

    /**
     * 校验用户是否已关联教师
     *
     * @param link 教师关联信息
     * @return 结果
     */
    @Override
    public boolean checkUserTeacherUnique(SysTeacherLink link)
    {
        Long userId = StringUtils.isNull(link.getUserId()) ? -1L : link.getUserId();
        SysTeacherLink info = teacherLinkMapper.checkUserTeacherUnique(link.getUserId());
        if (StringUtils.isNotNull(info) && info.getUserId().longValue() != userId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    /**
     * 清除用户缓存
     */
    private void clearUserCache(Long userId)
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
