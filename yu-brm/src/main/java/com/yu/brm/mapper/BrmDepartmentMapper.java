package com.yu.brm.mapper;

import java.util.List;
import com.yu.brm.domain.BrmDepartment;

public interface BrmDepartmentMapper 
{
    public BrmDepartment selectBrmDepartmentByDeptId(Long deptId);
    public List<BrmDepartment> selectBrmDepartmentList(BrmDepartment brmDepartment);
    public int insertBrmDepartment(BrmDepartment brmDepartment);
    public int updateBrmDepartment(BrmDepartment brmDepartment);
    public int deleteBrmDepartmentByDeptId(Long deptId);
    public int deleteBrmDepartmentByDeptIds(Long[] deptIds);
    public List<BrmDepartment> selectChildrenDeptById(Long deptId);
    public int updateBrmDepartmentChildren(List<BrmDepartment> children);
    public int checkDeptExistUser(Long deptId);
    public BrmDepartment checkDeptNameUnique(String deptName, Long parentId);
}
