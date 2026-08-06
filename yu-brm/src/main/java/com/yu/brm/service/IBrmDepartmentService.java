package com.yu.brm.service;

import java.util.List;
import com.yu.brm.domain.BrmDepartment;
import com.yu.common.core.domain.entity.SysDept;

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

    /**
     * 全量同步：将 sys_dept（部门管理）数据同步到 brm_department（院系管理）
     * 
     * @return 同步的记录数
     */
    public int syncFromSysDept();

    /**
     * 单条 upsert：部门增改时，将该部门同步到 brm_department
     * 
     * @param dept 部门信息
     * @return 影响行数
     */
    public int upsertFromSysDept(SysDept dept);

    /**
     * 单条删除：部门删除时，同步删除 brm_department 中的对应记录
     * 
     * @param deptId 部门ID
     * @return 影响行数
     */
    public int deleteByDeptId(Long deptId);
}
