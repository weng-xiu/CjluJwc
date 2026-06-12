package com.yu.sam.service.impl;

import java.util.List;
import com.yu.common.annotation.DataScope;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.sam.mapper.SamStatusChangeMapper;
import com.yu.sam.domain.SamStatusChange;
import com.yu.sam.service.ISamStatusChangeService;

@Service
public class SamStatusChangeServiceImpl implements ISamStatusChangeService 
{
    @Autowired
    private SamStatusChangeMapper samStatusChangeMapper;

    @Override
    public SamStatusChange selectSamStatusChangeByChangeId(Long changeId) { return samStatusChangeMapper.selectSamStatusChangeByChangeId(changeId); }
    @Override
    @DataScope(deptAlias = "d")
    public List<SamStatusChange> selectSamStatusChangeList(SamStatusChange samStatusChange) { return samStatusChangeMapper.selectSamStatusChangeList(samStatusChange); }
    @Override
    @Transactional
    public int insertSamStatusChange(SamStatusChange samStatusChange)
    {
        // 业务校验：同一学生不能有"处理中"状态的重复申请
        if (samStatusChange.getStudentId() != null)
        {
            SamStatusChange query = new SamStatusChange();
            query.setStudentId(samStatusChange.getStudentId());
            query.setApproveStatus("0");
            List<SamStatusChange> pendingList = samStatusChangeMapper.selectSamStatusChangeList(query);
            if (pendingList != null && !pendingList.isEmpty())
            {
                throw new ServiceException("该学生已有处理中的学籍异动申请，不允许重复提交");
            }
        }
        samStatusChange.setCreateTime(DateUtils.getNowDate());
        return samStatusChangeMapper.insertSamStatusChange(samStatusChange);
    }
    @Override
    @Transactional
    public int updateSamStatusChange(SamStatusChange samStatusChange)
    {
        // 状态校验：已审批的异动不允许修改
        SamStatusChange existing = samStatusChangeMapper.selectSamStatusChangeByChangeId(samStatusChange.getChangeId());
        if (existing != null && "1".equals(existing.getApproveStatus()))
        {
            throw new ServiceException("已审批的学籍异动不允许修改");
        }
        samStatusChange.setUpdateTime(DateUtils.getNowDate());
        return samStatusChangeMapper.updateSamStatusChange(samStatusChange);
    }
    @Override
    @Transactional
    public int deleteSamStatusChangeByChangeId(Long changeId) { return samStatusChangeMapper.deleteSamStatusChangeByChangeId(changeId); }
    @Override
    @Transactional
    public int deleteSamStatusChangeByChangeIds(Long[] changeIds) { return samStatusChangeMapper.deleteSamStatusChangeByChangeIds(changeIds); }
}
