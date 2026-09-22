package com.yu.brm.service.impl;

import java.util.ArrayList;
import java.util.List;
import com.yu.common.annotation.DataScope;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.brm.mapper.BrmDepartmentMapper;
import com.yu.brm.mapper.BrmTeacherMapper;
import com.yu.brm.domain.BrmTeacher;
import com.yu.brm.domain.BrmTeacherPosition;
import com.yu.brm.domain.BrmTeacherQualification;
import com.yu.brm.service.IBrmTeacherService;

/**
 * 教师Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
@Service
public class BrmTeacherServiceImpl implements IBrmTeacherService 
{
    @Autowired
    private BrmTeacherMapper brmTeacherMapper;

    @Autowired
    private BrmDepartmentMapper brmDepartmentMapper;

    @Override
    public BrmTeacher selectBrmTeacherByTeacherId(Long teacherId)
    {
        return brmTeacherMapper.selectBrmTeacherByTeacherId(teacherId);
    }

    @Override
    public BrmTeacher selectBrmTeacherByUserId(Long userId)
    {
        return brmTeacherMapper.selectBrmTeacherByUserId(userId);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "t")
    public List<BrmTeacher> selectBrmTeacherList(BrmTeacher brmTeacher)
    {
        return brmTeacherMapper.selectBrmTeacherList(brmTeacher);
    }

    @Override
    @Transactional
    public int insertBrmTeacher(BrmTeacher brmTeacher)
    {
        brmTeacher.setCreateTime(DateUtils.getNowDate());
        int rows = brmTeacherMapper.insertBrmTeacher(brmTeacher);
        insertBrmTeacherPosition(brmTeacher);
        insertBrmTeacherQualification(brmTeacher);
        return rows;
    }

    @Override
    @Transactional
    public int updateBrmTeacher(BrmTeacher brmTeacher)
    {
        brmTeacher.setUpdateTime(DateUtils.getNowDate());
        brmTeacherMapper.deleteBrmTeacherPositionByTeacherId(brmTeacher.getTeacherId());
        brmTeacherMapper.deleteBrmTeacherQualificationByTeacherId(brmTeacher.getTeacherId());
        insertBrmTeacherPosition(brmTeacher);
        insertBrmTeacherQualification(brmTeacher);
        return brmTeacherMapper.updateBrmTeacher(brmTeacher);
    }

    @Override
    @Transactional
    public int updateBrmTeacherByUserId(BrmTeacher brmTeacher)
    {
        brmTeacher.setUpdateTime(DateUtils.getNowDate());
        return brmTeacherMapper.updateBrmTeacherByUserId(brmTeacher);
    }

    @Override
    @Transactional
    public int deleteBrmTeacherByTeacherId(Long teacherId)
    {
        brmTeacherMapper.deleteBrmTeacherPositionByTeacherId(teacherId);
        brmTeacherMapper.deleteBrmTeacherQualificationByTeacherId(teacherId);
        return brmTeacherMapper.deleteBrmTeacherByTeacherId(teacherId);
    }

    @Override
    @Transactional
    public int deleteBrmTeacherByTeacherIds(Long[] teacherIds)
    {
        brmTeacherMapper.deleteBrmTeacherPositionByTeacherIds(teacherIds);
        brmTeacherMapper.deleteBrmTeacherQualificationByTeacherIds(teacherIds);
        return brmTeacherMapper.deleteBrmTeacherByTeacherIds(teacherIds);
    }

    @Override
    @Transactional
    public int deleteBrmTeacherByUserId(Long userId)
    {
        BrmTeacher teacher = brmTeacherMapper.selectBrmTeacherByUserId(userId);
        if (teacher != null)
        {
            Long teacherId = teacher.getTeacherId();
            brmTeacherMapper.deleteBrmTeacherPositionByTeacherId(teacherId);
            brmTeacherMapper.deleteBrmTeacherQualificationByTeacherId(teacherId);
            return brmTeacherMapper.deleteBrmTeacherByUserId(userId);
        }
        return 0;
    }

    public void insertBrmTeacherPosition(BrmTeacher brmTeacher)
    {
        List<BrmTeacherPosition> positionList = brmTeacher.getPositionList();
        Long teacherId = brmTeacher.getTeacherId();
        if (StringUtils.isNotNull(positionList))
        {
            List<BrmTeacherPosition> list = new ArrayList<BrmTeacherPosition>();
            for (BrmTeacherPosition position : positionList)
            {
                position.setTeacherId(teacherId);
                list.add(position);
            }
            if (list.size() > 0)
            {
                brmTeacherMapper.batchBrmTeacherPosition(list);
            }
        }
    }

    public void insertBrmTeacherQualification(BrmTeacher brmTeacher)
    {
        List<BrmTeacherQualification> qualificationList = brmTeacher.getQualificationList();
        Long teacherId = brmTeacher.getTeacherId();
        if (StringUtils.isNotNull(qualificationList))
        {
            List<BrmTeacherQualification> list = new ArrayList<BrmTeacherQualification>();
            for (BrmTeacherQualification qualification : qualificationList)
            {
                qualification.setTeacherId(teacherId);
                list.add(qualification);
            }
            if (list.size() > 0)
            {
                brmTeacherMapper.batchBrmTeacherQualification(list);
            }
        }
    }

    /**
     * P7：批量导入教师，逐行校验（工号/姓名必填、院系存在、工号唯一）并生成校验报告。
     */
    @Transactional
    @Override
    public String importTeacher(List<BrmTeacher> teacherList, String operName, boolean updateSupport)
    {
        if (teacherList == null || teacherList.isEmpty())
        {
            throw new ServiceException("导入教师数据不能为空！");
        }
        int successNum = 0;
        int updateNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        int row = 0;
        for (BrmTeacher teacher : teacherList)
        {
            row++;
            // 1. 必填字段校验
            if (StringUtils.isBlank(teacher.getTeacherCode()))
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：教师工号不能为空");
                continue;
            }
            if (StringUtils.isBlank(teacher.getTeacherName()))
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：教师姓名不能为空");
                continue;
            }
            if (teacher.getDeptId() == null)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：所属院系ID不能为空");
                continue;
            }
            // 2. 院系存在性校验
            if (brmDepartmentMapper.selectBrmDepartmentByDeptId(teacher.getDeptId()) == null)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：院系ID ").append(teacher.getDeptId()).append(" 不存在");
                continue;
            }
            // 3. 工号唯一性校验
            BrmTeacher existing = brmTeacherMapper.selectBrmTeacherByTeacherCode(teacher.getTeacherCode());
            try
            {
                if (StringUtils.isEmpty(teacher.getStatus()))
                {
                    teacher.setStatus("0");
                }
                if (existing == null)
                {
                    teacher.setCreateBy(operName);
                    teacher.setCreateTime(DateUtils.getNowDate());
                    brmTeacherMapper.insertBrmTeacher(teacher);
                    successNum++;
                    successMsg.append("<br/>").append(successNum).append("、教师 ").append(teacher.getTeacherCode()).append(" 导入成功");
                }
                else if (updateSupport)
                {
                    teacher.setTeacherId(existing.getTeacherId());
                    teacher.setUpdateBy(operName);
                    teacher.setUpdateTime(DateUtils.getNowDate());
                    brmTeacherMapper.updateBrmTeacher(teacher);
                    updateNum++;
                    successMsg.append("<br/>").append(updateNum).append("、教师 ").append(teacher.getTeacherCode()).append(" 更新成功");
                }
                else
                {
                    failureNum++;
                    failureMsg.append("<br/>第 ").append(row).append(" 行：教师工号 ").append(teacher.getTeacherCode()).append(" 已存在");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：").append(e.getMessage());
            }
        }
        if (failureNum > 0)
        {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            if (successNum > 0 || updateNum > 0)
            {
                // 存在成功行时不回滚，仅提示部分失败
                return "导入完成：成功 " + successNum + " 条，更新 " + updateNum + " 条，失败 " + failureNum + " 条。" + failureMsg;
            }
            throw new ServiceException(failureMsg.toString());
        }
        successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条"
                + (updateNum > 0 ? "，更新 " + updateNum + " 条" : "") + "，数据如下：");
        return successMsg.toString();
    }
}
