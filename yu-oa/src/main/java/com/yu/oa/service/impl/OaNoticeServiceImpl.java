package com.yu.oa.service.impl;

import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.SecurityUtils;
import com.yu.oa.domain.OaNotice;
import com.yu.oa.domain.OaNoticeDept;
import com.yu.oa.domain.OaNoticeRead;
import com.yu.oa.mapper.OaNoticeMapper;
import com.yu.oa.mapper.OaNoticeDeptMapper;
import com.yu.oa.mapper.OaNoticeReadMapper;
import com.yu.oa.service.IOaNoticeService;

/**
 * 通知公告Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
@Service
public class OaNoticeServiceImpl implements IOaNoticeService
{
    @Autowired
    private OaNoticeMapper oaNoticeMapper;

    @Autowired
    private OaNoticeDeptMapper oaNoticeDeptMapper;

    @Autowired
    private OaNoticeReadMapper oaNoticeReadMapper;

    /**
     * 查询通知公告
     * 
     * @param noticeId 通知公告ID
     * @return 通知公告
     */
    @Override
    public OaNotice selectOaNoticeByNoticeId(Long noticeId)
    {
        OaNotice notice = oaNoticeMapper.selectOaNoticeByNoticeId(noticeId);
        if (notice != null)
        {
            notice.setDeptList(oaNoticeDeptMapper.selectOaNoticeDeptByNoticeId(noticeId));
        }
        return notice;
    }

    /**
     * 查询通知公告列表
     * 
     * @param oaNotice 通知公告
     * @return 通知公告集合
     */
    @Override
    public List<OaNotice> selectOaNoticeList(OaNotice oaNotice)
    {
        return oaNoticeMapper.selectOaNoticeList(oaNotice);
    }

    /**
     * 新增通知公告
     * 
     * @param oaNotice 通知公告
     * @return 结果
     */
    @Override
    @Transactional
    public int insertOaNotice(OaNotice oaNotice)
    {
        oaNotice.setCreateTime(DateUtils.getNowDate());
        oaNotice.setPublishStatus("0");
        oaNotice.setReadCount(0);
        oaNotice.setPublisherId(getLoginUserId());
        oaNotice.setPublisherName(getLoginUserName());
        int rows = oaNoticeMapper.insertOaNotice(oaNotice);
        insertOaNoticeDept(oaNotice);
        return rows;
    }

    /**
     * 修改通知公告
     * 
     * @param oaNotice 通知公告
     * @return 结果
     */
    @Override
    @Transactional
    public int updateOaNotice(OaNotice oaNotice)
    {
        oaNotice.setUpdateTime(DateUtils.getNowDate());
        oaNoticeDeptMapper.deleteOaNoticeDeptByNoticeId(oaNotice.getNoticeId());
        insertOaNoticeDept(oaNotice);
        return oaNoticeMapper.updateOaNotice(oaNotice);
    }

    /**
     * 批量删除通知公告
     * 
     * @param noticeIds 需要删除的通知公告ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteOaNoticeByNoticeIds(Long[] noticeIds)
    {
        for (Long noticeId : noticeIds)
        {
            oaNoticeDeptMapper.deleteOaNoticeDeptByNoticeId(noticeId);
            oaNoticeReadMapper.deleteOaNoticeReadByNoticeId(noticeId);
        }
        return oaNoticeMapper.deleteOaNoticeByNoticeIds(noticeIds);
    }

    /**
     * 删除通知公告信息
     * 
     * @param noticeId 通知公告ID
     * @return 结果
     */
    @Override
    @Transactional
    public int deleteOaNoticeByNoticeId(Long noticeId)
    {
        oaNoticeDeptMapper.deleteOaNoticeDeptByNoticeId(noticeId);
        oaNoticeReadMapper.deleteOaNoticeReadByNoticeId(noticeId);
        return oaNoticeMapper.deleteOaNoticeByNoticeId(noticeId);
    }

    /**
     * 发布公告
     * 
     * @param noticeId 通知公告ID
     * @return 结果
     */
    @Override
    @Transactional
    public int publishOaNotice(Long noticeId)
    {
        OaNotice notice = new OaNotice();
        notice.setNoticeId(noticeId);
        notice.setPublishStatus("1");
        notice.setPublishTime(DateUtils.getNowDate());
        notice.setUpdateTime(DateUtils.getNowDate());
        return oaNoticeMapper.updateOaNotice(notice);
    }

    /**
     * 撤回公告
     * 
     * @param noticeId 通知公告ID
     * @return 结果
     */
    @Override
    @Transactional
    public int revokeOaNotice(Long noticeId)
    {
        OaNotice notice = new OaNotice();
        notice.setNoticeId(noticeId);
        notice.setPublishStatus("2");
        notice.setUpdateTime(DateUtils.getNowDate());
        return oaNoticeMapper.updateOaNotice(notice);
    }

    /**
     * 记录已读
     * 
     * @param noticeId 通知公告ID
     * @return 结果
     */
    @Override
    @Transactional
    public int readOaNotice(Long noticeId)
    {
        Long userId = getLoginUserId();
        OaNoticeRead exist = oaNoticeReadMapper.selectOaNoticeReadByUser(noticeId, userId);
        if (exist != null)
        {
            return 1;
        }
        OaNoticeRead read = new OaNoticeRead();
        read.setNoticeId(noticeId);
        read.setUserId(userId);
        read.setUserName(getLoginUserName());
        read.setReadTime(new Date());
        read.setCreateTime(DateUtils.getNowDate());
        int rows = oaNoticeReadMapper.insertOaNoticeRead(read);

        OaNotice notice = oaNoticeMapper.selectOaNoticeByNoticeId(noticeId);
        if (notice != null)
        {
            Integer readCount = notice.getReadCount();
            if (readCount == null)
            {
                readCount = 0;
            }
            OaNotice update = new OaNotice();
            update.setNoticeId(noticeId);
            update.setReadCount(readCount + 1);
            oaNoticeMapper.updateOaNotice(update);
        }
        return rows;
    }

    /**
     * 新增指定部门范围
     * 
     * @param oaNotice 通知公告对象
     */
    private void insertOaNoticeDept(OaNotice oaNotice)
    {
        List<OaNoticeDept> deptList = oaNotice.getDeptList();
        if (deptList == null || deptList.isEmpty())
        {
            return;
        }
        for (OaNoticeDept dept : deptList)
        {
            dept.setNoticeId(oaNotice.getNoticeId());
            dept.setCreateTime(DateUtils.getNowDate());
            oaNoticeDeptMapper.insertOaNoticeDept(dept);
        }
    }

    private Long getLoginUserId()
    {
        return SecurityUtils.getLoginUser().getUserId();
    }

    private String getLoginUserName()
    {
        return SecurityUtils.getUsername();
    }
}
