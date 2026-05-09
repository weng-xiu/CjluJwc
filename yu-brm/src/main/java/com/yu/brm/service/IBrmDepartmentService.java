package com.yu.brm.service;

import java.util.List;
import com.yu.brm.domain.BrmDepartment;

/**
 * 院系Service接口
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
public interface IBrmDepartmentService 
{
    public BrmDepartment selectBrmDepartmentByDeptId(Long deptId);
    public List<BrmDepartment> selectBrmDepartmentList(BrmDepartment brmDepartment);
    public List<BrmDepartment> selectChildrenDeptById(Long deptId);
    public String checkDeptNameUnique(BrmDepartment brmDepartment);
    public boolean checkDeptExistUser(Long deptId);
    public int insertBrmDepartment(BrmDepartment brmDepartment);
    public int updateBrmDepartment(BrmDepartment brmDepartment);
    public int deleteBrmDepartmentByDeptId(Long deptId);
    public int deleteBrmDepartmentByDeptIds(Long[] deptIds);
}
