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
import com.yu.common.core.domain.entity.SysDept;
import com.yu.common.utils.StringUtils;

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

    /**
     * 生成院系编码：与同步脚本一致，格式为 D + 4 位零填充的 dept_id
     */
    private String buildDeptCode(Long deptId)
    {
        return "D" + String.format("%04d", deptId);
    }

    /**
     * 全量同步：清空 brm_department 后，将 sys_dept（部门管理）整树灌入 brm_department（院系管理），
     * 共享同一 dept_id，零风险。dept_code 由 dept_id 生成。
     */
    @Override
    @Transactional
    public int syncFromSysDept()
    {
        brmDepartmentMapper.deleteAllBrmDepartment();
        List<SysDept> sysDepts = brmDepartmentMapper.selectAllSysDept();
        int count = 0;
        for (SysDept s : sysDepts)
        {
            BrmDepartment dept = new BrmDepartment();
            dept.setDeptId(s.getDeptId());
            dept.setParentId(s.getParentId());
            dept.setAncestors(s.getAncestors());
            dept.setDeptCode(buildDeptCode(s.getDeptId()));
            dept.setDeptName(s.getDeptName());
            dept.setLeader(s.getLeader());
            dept.setPhone(s.getPhone());
            dept.setEmail(s.getEmail());
            dept.setOrderNum(s.getOrderNum());
            dept.setStatus(s.getStatus());
            dept.setDelFlag("0");
            dept.setCreateBy(StringUtils.isNotEmpty(s.getCreateBy()) ? s.getCreateBy() : "admin");
            dept.setCreateTime(s.getCreateTime());
            dept.setUpdateBy(s.getUpdateBy());
            dept.setUpdateTime(s.getUpdateTime());
            dept.setRemark("由 sys_dept 同步导入");
            brmDepartmentMapper.insertBrmDepartment(dept);
            count++;
        }
        return count;
    }

    /**
     * 单条 upsert：部门增改时，将对应部门同步到 brm_department
     */
    @Override
    @Transactional
    public int upsertFromSysDept(SysDept s)
    {
        BrmDepartment existing = brmDepartmentMapper.selectBrmDepartmentByDeptId(s.getDeptId());
        if (existing == null)
        {
            BrmDepartment dept = new BrmDepartment();
            dept.setDeptId(s.getDeptId());
            dept.setParentId(s.getParentId());
            dept.setAncestors(s.getAncestors());
            dept.setDeptCode(buildDeptCode(s.getDeptId()));
            dept.setDeptName(s.getDeptName());
            dept.setLeader(s.getLeader());
            dept.setPhone(s.getPhone());
            dept.setEmail(s.getEmail());
            dept.setOrderNum(s.getOrderNum());
            dept.setStatus(s.getStatus());
            dept.setDelFlag("0");
            dept.setCreateBy(StringUtils.isNotEmpty(s.getCreateBy()) ? s.getCreateBy() : "admin");
            dept.setCreateTime(s.getCreateTime());
            dept.setUpdateBy(s.getUpdateBy());
            dept.setUpdateTime(s.getUpdateTime());
            dept.setRemark("由 sys_dept 同步导入");
            return brmDepartmentMapper.insertBrmDepartment(dept);
        }
        else
        {
            existing.setParentId(s.getParentId());
            existing.setAncestors(s.getAncestors());
            existing.setDeptName(s.getDeptName());
            existing.setLeader(s.getLeader());
            existing.setPhone(s.getPhone());
            existing.setEmail(s.getEmail());
            existing.setOrderNum(s.getOrderNum());
            existing.setStatus(s.getStatus());
            existing.setUpdateTime(s.getUpdateTime());
            return brmDepartmentMapper.updateBrmDepartment(existing);
        }
    }

    /**
     * 单条删除：部门删除时，同步逻辑删除 brm_department 对应记录
     */
    @Override
    @Transactional
    public int deleteByDeptId(Long deptId)
    {
        return brmDepartmentMapper.deleteBrmDepartmentByDeptId(deptId);
    }
}
