package com.yu.system.mapper;

import java.util.List;
import com.yu.system.domain.SysTeacherLink;

/**
 * 教师账号关联Mapper接口
 */
public interface SysTeacherLinkMapper
{
    public List<SysTeacherLink> selectTeacherLinkList(SysTeacherLink link);

    public SysTeacherLink selectTeacherLinkById(Long linkId);

    public SysTeacherLink selectTeacherLinkByUserId(Long userId);

    public SysTeacherLink selectTeacherLinkByTeacherId(Long teacherId);

    public int insertTeacherLink(SysTeacherLink link);

    public int updateTeacherLink(SysTeacherLink link);

    public int deleteTeacherLinkById(Long linkId);

    public int deleteTeacherLinkByUserId(Long userId);

    public int batchInsertTeacherLink(List<SysTeacherLink> list);

    public SysTeacherLink checkUserTeacherUnique(Long userId);
}
