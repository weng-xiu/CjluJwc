package com.yu.brm.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.brm.mapper.BrmBuildingMapper;
import com.yu.brm.mapper.BrmClassroomMapper;
import com.yu.brm.mapper.BrmClassroomTypeMapper;
import com.yu.brm.domain.BrmClassroom;
import com.yu.brm.service.IBrmClassroomService;

/**
 * 教室Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-08
 */
@Service
public class BrmClassroomServiceImpl implements IBrmClassroomService 
{
    @Autowired
    private BrmClassroomMapper brmClassroomMapper;

    @Autowired
    private BrmBuildingMapper brmBuildingMapper;

    @Autowired
    private BrmClassroomTypeMapper brmClassroomTypeMapper;

    @Override
    public BrmClassroom selectBrmClassroomByClassroomId(Long classroomId)
    {
        return brmClassroomMapper.selectBrmClassroomByClassroomId(classroomId);
    }

    @Override
    public List<BrmClassroom> selectBrmClassroomList(BrmClassroom brmClassroom)
    {
        return brmClassroomMapper.selectBrmClassroomList(brmClassroom);
    }

    @Override
    @Transactional
    public int insertBrmClassroom(BrmClassroom brmClassroom)
    {
        // 数据范围校验：容量必须为正整数
        if (brmClassroom.getCapacity() != null && brmClassroom.getCapacity() <= 0)
        {
            throw new ServiceException("教室容量必须为正整数");
        }
        brmClassroom.setCreateTime(DateUtils.getNowDate());
        return brmClassroomMapper.insertBrmClassroom(brmClassroom);
    }

    @Override
    @Transactional
    public int updateBrmClassroom(BrmClassroom brmClassroom)
    {
        // 数据范围校验：容量必须为正整数
        if (brmClassroom.getCapacity() != null && brmClassroom.getCapacity() <= 0)
        {
            throw new ServiceException("教室容量必须为正整数");
        }
        brmClassroom.setUpdateTime(DateUtils.getNowDate());
        return brmClassroomMapper.updateBrmClassroom(brmClassroom);
    }

    @Override
    @Transactional
    public int deleteBrmClassroomByClassroomId(Long classroomId)
    {
        return brmClassroomMapper.deleteBrmClassroomByClassroomId(classroomId);
    }

    @Override
    @Transactional
    public int deleteBrmClassroomByClassroomIds(Long[] classroomIds)
    {
        return brmClassroomMapper.deleteBrmClassroomByClassroomIds(classroomIds);
    }

    /**
     * P7：批量导入教室，逐行校验（名称/教学楼/类型必填、容量为正、外键存在、教学楼+名称唯一）并生成校验报告。
     */
    @Transactional
    @Override
    public String importClassroom(List<BrmClassroom> classroomList, String operName, boolean updateSupport)
    {
        if (classroomList == null || classroomList.isEmpty())
        {
            throw new ServiceException("导入教室数据不能为空！");
        }
        int successNum = 0;
        int updateNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        int row = 0;
        for (BrmClassroom classroom : classroomList)
        {
            row++;
            // 1. 必填字段校验
            if (StringUtils.isBlank(classroom.getClassroomName()))
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：教室名称不能为空");
                continue;
            }
            if (classroom.getBuildingId() == null)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：所属教学楼ID不能为空");
                continue;
            }
            if (classroom.getTypeId() == null)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：教室类型ID不能为空");
                continue;
            }
            if (classroom.getCapacity() != null && classroom.getCapacity() <= 0)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：教室容量必须为正整数");
                continue;
            }
            // 2. 外键存在性校验
            if (brmBuildingMapper.selectBrmBuildingByBuildingId(classroom.getBuildingId()) == null)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：教学楼ID ").append(classroom.getBuildingId()).append(" 不存在");
                continue;
            }
            if (brmClassroomTypeMapper.selectBrmClassroomTypeByTypeId(classroom.getTypeId()) == null)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：教室类型ID ").append(classroom.getTypeId()).append(" 不存在");
                continue;
            }
            // 3. 教学楼+名称唯一性校验
            BrmClassroom existing = brmClassroomMapper.selectByBuildingAndName(classroom.getBuildingId(), classroom.getClassroomName());
            try
            {
                if (StringUtils.isEmpty(classroom.getStatus()))
                {
                    classroom.setStatus("0");
                }
                if (existing == null)
                {
                    classroom.setCreateBy(operName);
                    classroom.setCreateTime(DateUtils.getNowDate());
                    brmClassroomMapper.insertBrmClassroom(classroom);
                    successNum++;
                    successMsg.append("<br/>").append(successNum).append("、教室 ").append(classroom.getClassroomName()).append(" 导入成功");
                }
                else if (updateSupport)
                {
                    classroom.setClassroomId(existing.getClassroomId());
                    classroom.setUpdateBy(operName);
                    classroom.setUpdateTime(DateUtils.getNowDate());
                    brmClassroomMapper.updateBrmClassroom(classroom);
                    updateNum++;
                    successMsg.append("<br/>").append(updateNum).append("、教室 ").append(classroom.getClassroomName()).append(" 更新成功");
                }
                else
                {
                    failureNum++;
                    failureMsg.append("<br/>第 ").append(row).append(" 行：教室 ").append(classroom.getClassroomName()).append(" 在该教学楼已存在");
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
