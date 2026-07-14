package com.yu.oa.mapper;

import java.util.List;
import com.yu.oa.domain.OaNoticeDept;

/**
 * 通知公告部门范围Mapper接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface OaNoticeDeptMapper 
{
    public OaNoticeDept selectOaNoticeDeptByNoticeDeptId(Long noticeDeptId);
    public List<OaNoticeDept> selectOaNoticeDeptByNoticeId(Long noticeId);
    public int insertOaNoticeDept(OaNoticeDept oaNoticeDept);
    public int updateOaNoticeDept(OaNoticeDept oaNoticeDept);
    public int deleteOaNoticeDeptByNoticeId(Long noticeId);
    public int deleteOaNoticeDeptByNoticeDeptIds(Long[] noticeDeptIds);
}
