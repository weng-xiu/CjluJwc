package com.yu.system.service;

import java.util.List;
import com.yu.system.domain.SysTeacherLink;

/**
 * 教师账号关联 业务层
 *
 * @author yu
 */
public interface ISysTeacherLinkService
{
    /**
     * 查询教师关联列表
     *
     * @param link 教师关联信息
     * @return 教师关联信息集合
     */
    public List<SysTeacherLink> selectTeacherLinkList(SysTeacherLink link);

    /**
     * 通过关联ID查询教师关联
     *
     * @param linkId 关联ID
     * @return 教师关联对象信息
     */
    public SysTeacherLink selectTeacherLinkById(Long linkId);

    /**
     * 通过用户ID查询教师关联
     *
     * @param userId 用户ID
     * @return 教师关联对象信息
     */
    public SysTeacherLink selectTeacherLinkByUserId(Long userId);

    /**
     * 新增教师关联信息
     *
     * @param link 教师关联信息
     * @return 结果
     */
    public int insertTeacherLink(SysTeacherLink link);

    /**
     * 修改教师关联信息
     *
     * @param link 教师关联信息
     * @return 结果
     */
    public int updateTeacherLink(SysTeacherLink link);

    /**
     * 删除教师关联信息
     *
     * @param linkId 关联ID
     * @return 结果
     */
    public int deleteTeacherLinkById(Long linkId);

    /**
     * 校验用户是否已关联教师
     *
     * @param link 教师关联信息
     * @return 结果
     */
    public boolean checkUserTeacherUnique(SysTeacherLink link);
}
