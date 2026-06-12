package com.yu.brm.service.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.brm.mapper.BrmDepartmentMapper;
import com.yu.brm.mapper.BrmMajorMapper;
import com.yu.brm.domain.BrmDepartment;
import com.yu.brm.domain.BrmMajor;
import com.yu.brm.service.IBrmDepartmentService;

/**
 * 院系Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
@Service
public class BrmDepartmentServiceImpl implements IBrmDepartmentService 
{
    @Autowired
    private BrmDepartmentMapper brmDepartmentMapper;

    @Autowired
    private BrmMajorMapper brmMajorMapper;

    @Override
    public BrmDepartment selectBrmDepartmentByDeptId(Long deptId)
    {
        return brmDepartmentMapper.selectBrmDepartmentByDeptId(deptId);
    }

    @Override
    public List<BrmDepartment> selectBrmDepartmentList(BrmDepartment brmDepartment)
    {
        return brmDepartmentMapper.selectBrmDepartmentList(brmDepartment);
    }

    @Override
    public List<BrmDepartment> selectChildrenDeptById(Long deptId)
    {
        return brmDepartmentMapper.selectChildrenDeptById(deptId);
    }

    @Override
    public String checkDeptNameUnique(BrmDepartment dept)
    {
        Long deptId = StringUtils.isNull(dept.getDeptId()) ? -1L : dept.getDeptId();
        BrmDepartment info = brmDepartmentMapper.checkDeptNameUnique(dept.getDeptName(), dept.getParentId());
        if (StringUtils.isNotNull(info) && info.getDeptId().longValue() != deptId.longValue())
        {
            return "0";
        }
        return "1";
    }

    @Override
    public boolean checkDeptExistUser(Long deptId)
    {
        int result = brmDepartmentMapper.checkDeptExistUser(deptId);
        return result > 0;
    }

    @Override
    @Transactional
    public int insertBrmDepartment(BrmDepartment dept)
    {
        // 唯一性校验：院系名称不能重复
        BrmDepartment info = brmDepartmentMapper.checkDeptNameUnique(dept.getDeptName(), dept.getParentId());
        if (StringUtils.isNotNull(info))
        {
            throw new ServiceException("院系名称'" + dept.getDeptName() + "'已存在");
        }
        BrmDepartment parentDept = brmDepartmentMapper.selectBrmDepartmentByDeptId(dept.getParentId());
        if (StringUtils.isNotNull(parentDept))
        {
            dept.setAncestors(parentDept.getAncestors() + "," + dept.getParentId());
        }
        else
        {
            dept.setAncestors("0");
        }
        dept.setCreateTime(DateUtils.getNowDate());
        return brmDepartmentMapper.insertBrmDepartment(dept);
    }

    @Override
    @Transactional
    public int updateBrmDepartment(BrmDepartment dept)
    {
        // 唯一性校验：院系名称不能重复（排除自身）
        BrmDepartment info = brmDepartmentMapper.checkDeptNameUnique(dept.getDeptName(), dept.getParentId());
        if (StringUtils.isNotNull(info) && !info.getDeptId().equals(dept.getDeptId()))
        {
            throw new ServiceException("院系名称'" + dept.getDeptName() + "'已存在");
        }
        BrmDepartment newParentDept = brmDepartmentMapper.selectBrmDepartmentByDeptId(dept.getParentId());
        BrmDepartment oldDept = brmDepartmentMapper.selectBrmDepartmentByDeptId(dept.getDeptId());
        if (StringUtils.isNotNull(newParentDept) && StringUtils.isNotNull(oldDept))
        {
            String newAncestors = newParentDept.getAncestors() + "," + newParentDept.getDeptId();
            String oldAncestors = oldDept.getAncestors();
            dept.setAncestors(newAncestors);
            updateDeptChildren(dept.getDeptId(), newAncestors, oldAncestors);
        }
        dept.setUpdateTime(DateUtils.getNowDate());
        return brmDepartmentMapper.updateBrmDepartment(dept);
    }

    private void updateDeptChildren(Long deptId, String newAncestors, String oldAncestors)
    {
        List<BrmDepartment> children = brmDepartmentMapper.selectChildrenDeptById(deptId);
        for (BrmDepartment child : children)
        {
            child.setAncestors(child.getAncestors().replaceFirst(oldAncestors, newAncestors));
        }
        if (children.size() > 0)
        {
            brmDepartmentMapper.updateBrmDepartmentChildren(children);
        }
    }

    @Override
    @Transactional
    public int deleteBrmDepartmentByDeptId(Long deptId)
    {
        BrmMajor query = new BrmMajor();
        query.setDeptId(deptId);
        List<BrmMajor> majors = brmMajorMapper.selectBrmMajorList(query);
        if (majors != null && !majors.isEmpty())
        {
            throw new ServiceException("该院系下存在专业，不允许删除");
        }
        return brmDepartmentMapper.deleteBrmDepartmentByDeptId(deptId);
    }

    @Override
    @Transactional
    public int deleteBrmDepartmentByDeptIds(Long[] deptIds)
    {
        for (Long deptId : deptIds)
        {
            BrmMajor query = new BrmMajor();
            query.setDeptId(deptId);
            List<BrmMajor> majors = brmMajorMapper.selectBrmMajorList(query);
            if (majors != null && !majors.isEmpty())
            {
                throw new ServiceException("该院系下存在专业，不允许删除");
            }
        }
        return brmDepartmentMapper.deleteBrmDepartmentByDeptIds(deptIds);
    }
}
