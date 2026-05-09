package com.yu.brm.service.impl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.brm.mapper.BrmDepartmentMapper;
import com.yu.brm.domain.BrmDepartment;
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
    public int insertBrmDepartment(BrmDepartment dept)
    {
        BrmDepartment info = brmDepartmentMapper.selectBrmDepartmentByDeptId(dept.getParentId());
        if (StringUtils.isNotNull(info))
        {
            dept.setAncestors(info.getAncestors() + "," + dept.getParentId());
        }
        else
        {
            dept.setAncestors("0");
        }
        dept.setCreateTime(DateUtils.getNowDate());
        return brmDepartmentMapper.insertBrmDepartment(dept);
    }

    @Override
    public int updateBrmDepartment(BrmDepartment dept)
    {
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
    public int deleteBrmDepartmentByDeptId(Long deptId)
    {
        return brmDepartmentMapper.deleteBrmDepartmentByDeptId(deptId);
    }

    @Override
    public int deleteBrmDepartmentByDeptIds(Long[] deptIds)
    {
        return brmDepartmentMapper.deleteBrmDepartmentByDeptIds(deptIds);
    }
}
