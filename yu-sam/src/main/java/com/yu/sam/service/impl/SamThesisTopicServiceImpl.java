package com.yu.sam.service.impl;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.common.core.domain.entity.SysUser;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import com.yu.sam.domain.SamThesisTopic;
import com.yu.sam.mapper.SamThesisMapper;
import com.yu.sam.mapper.SamThesisTopicMapper;
import com.yu.sam.service.ISamThesisTopicService;
import com.yu.system.service.ISysUserService;

/**
 * 毕业论文选题库Service实现
 *
 * @author ruoyi
 * @date 2026-09-26
 */
@Service
public class SamThesisTopicServiceImpl implements ISamThesisTopicService
{
    private static final Logger log = LoggerFactory.getLogger(SamThesisTopicServiceImpl.class);

    @Autowired
    private SamThesisTopicMapper samThesisTopicMapper;

    @Autowired
    private SamThesisMapper samThesisMapper;

    @Autowired
    private ISysUserService sysUserService;

    @Override
    public SamThesisTopic selectSamThesisTopicByTopicId(Long topicId)
    {
        return samThesisTopicMapper.selectSamThesisTopicByTopicId(topicId);
    }

    @Override
    public List<SamThesisTopic> selectSamThesisTopicList(SamThesisTopic samThesisTopic)
    {
        return samThesisTopicMapper.selectSamThesisTopicList(samThesisTopic);
    }

    /**
     * 新增题目：教师自助申报进入待审核，管理员/教务新增直接可用。
     */
    @Override
    @Transactional
    public int insertSamThesisTopic(SamThesisTopic samThesisTopic)
    {
        samThesisTopic.setTopicId(null);
        if (samThesisTopic.getCapacity() == null || samThesisTopic.getCapacity() <= 0)
        {
            samThesisTopic.setCapacity(1);
        }
        samThesisTopic.setElectedCount(0);
        if (StringUtils.isEmpty(samThesisTopic.getStatus()))
        {
            samThesisTopic.setStatus("0");
        }
        if (StringUtils.isEmpty(samThesisTopic.getAdvisorName()) && StringUtils.isNotEmpty(samThesisTopic.getAdvisor()))
        {
            samThesisTopic.setAdvisorName(resolveNickName(samThesisTopic.getAdvisor()));
        }
        samThesisTopic.setCreateTime(DateUtils.getNowDate());
        return samThesisTopicMapper.insertSamThesisTopic(samThesisTopic);
    }

    /**
     * 修改题目：已被选用的题目不允许改小容量到已选人数以下。
     */
    @Override
    @Transactional
    public int updateSamThesisTopic(SamThesisTopic samThesisTopic)
    {
        SamThesisTopic exist = samThesisTopicMapper.selectSamThesisTopicByTopicId(samThesisTopic.getTopicId());
        if (exist == null)
        {
            throw new ServiceException("选题不存在");
        }
        int elected = exist.getElectedCount() == null ? 0 : exist.getElectedCount();
        if (samThesisTopic.getCapacity() != null && samThesisTopic.getCapacity() < elected)
        {
            throw new ServiceException("该题已有 " + elected + " 人选定，可选题人数不得小于该值");
        }
        samThesisTopic.setUpdateTime(DateUtils.getNowDate());
        return samThesisTopicMapper.updateSamThesisTopic(samThesisTopic);
    }

    /**
     * 删除题目：已被选用的题目禁止删除，避免论文档案失去来源。
     */
    @Override
    @Transactional
    public int deleteSamThesisTopicByTopicIds(Long[] topicIds)
    {
        if (topicIds == null || topicIds.length == 0)
        {
            return 0;
        }
        for (Long topicId : topicIds)
        {
            if (samThesisMapper.countByTopicId(topicId) > 0)
            {
                SamThesisTopic topic = samThesisTopicMapper.selectSamThesisTopicByTopicId(topicId);
                String name = topic == null ? String.valueOf(topicId) : topic.getTopicName();
                throw new ServiceException("题目《" + name + "》已被学生选定，不能删除，可改为下架");
            }
        }
        return samThesisTopicMapper.deleteSamThesisTopicByTopicIds(topicIds);
    }

    @Override
    @Transactional
    public int auditTopic(Long topicId, boolean pass, String opinion, String operator)
    {
        SamThesisTopic exist = samThesisTopicMapper.selectSamThesisTopicByTopicId(topicId);
        if (exist == null)
        {
            throw new ServiceException("选题不存在");
        }
        if (!"0".equals(exist.getStatus()))
        {
            throw new ServiceException("该题目已审核，当前状态为" + statusName(exist.getStatus()));
        }
        SamThesisTopic update = new SamThesisTopic();
        update.setTopicId(topicId);
        update.setStatus(pass ? "1" : "3");
        update.setAuditOpinion(opinion);
        update.setUpdateBy(operator);
        update.setUpdateTime(DateUtils.getNowDate());
        int rows = samThesisTopicMapper.updateSamThesisTopic(update);
        log.info("选题[{}]审核{}，操作人{}", topicId, pass ? "通过" : "不通过", operator);
        return rows;
    }

    @Override
    @Transactional
    public int changeStatus(Long topicId, String status, String operator)
    {
        SamThesisTopic exist = samThesisTopicMapper.selectSamThesisTopicByTopicId(topicId);
        if (exist == null)
        {
            throw new ServiceException("选题不存在");
        }
        if (!"0".equals(status) && !"1".equals(status) && !"3".equals(status))
        {
            throw new ServiceException("只能手动设置待审核、可选题或已下架状态");
        }
        if ("1".equals(status) && exist.getElectedCount() != null && exist.getCapacity() != null
                && exist.getElectedCount() >= exist.getCapacity())
        {
            throw new ServiceException("该题目名额已满，无法上架");
        }
        SamThesisTopic update = new SamThesisTopic();
        update.setTopicId(topicId);
        update.setStatus(status);
        update.setUpdateBy(operator);
        update.setUpdateTime(DateUtils.getNowDate());
        return samThesisTopicMapper.updateSamThesisTopic(update);
    }

    private String statusName(String status)
    {
        if ("0".equals(status)) return "待审核";
        if ("1".equals(status)) return "可选题";
        if ("2".equals(status)) return "已选满";
        return "已下架";
    }

    private String resolveNickName(String userName)
    {
        try
        {
            SysUser user = sysUserService.selectUserByUserName(userName);
            return user == null ? userName : user.getNickName();
        }
        catch (Exception e)
        {
            log.warn("解析教师[{}]姓名失败：{}", userName, e.getMessage());
            return userName;
        }
    }
}
