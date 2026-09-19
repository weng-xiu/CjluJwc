package com.yu.tpm.service.impl;

import java.util.List;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.tpm.mapper.TpmCourseLibraryMapper;
import com.yu.tpm.mapper.TpmCourseOfferingMapper;
import com.yu.tpm.domain.TpmCourseLibrary;
import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.service.ITpmCourseLibraryService;

/**
 * 课程库Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmCourseLibraryServiceImpl implements ITpmCourseLibraryService 
{
    @Autowired
    private TpmCourseLibraryMapper tpmCourseLibraryMapper;

    @Autowired
    private TpmCourseOfferingMapper tpmCourseOfferingMapper;

    @Override
    public TpmCourseLibrary selectTpmCourseLibraryByCourseId(Long courseId)
    {
        return tpmCourseLibraryMapper.selectTpmCourseLibraryByCourseId(courseId);
    }

    @Override
    public List<TpmCourseLibrary> selectTpmCourseLibraryList(TpmCourseLibrary tpmCourseLibrary)
    {
        return tpmCourseLibraryMapper.selectTpmCourseLibraryList(tpmCourseLibrary);
    }

    @Transactional
    @Override
    public int insertTpmCourseLibrary(TpmCourseLibrary tpmCourseLibrary)
    {
        // 唯一性校验：课程编码不能重复
        TpmCourseLibrary existing = tpmCourseLibraryMapper.selectTpmCourseLibraryByCourseCode(tpmCourseLibrary.getCourseCode());
        if (existing != null)
        {
            throw new ServiceException("课程编码'" + tpmCourseLibrary.getCourseCode() + "'已存在");
        }
        tpmCourseLibrary.setCreateTime(DateUtils.getNowDate());
        return tpmCourseLibraryMapper.insertTpmCourseLibrary(tpmCourseLibrary);
    }

    @Transactional
    @Override
    public int updateTpmCourseLibrary(TpmCourseLibrary tpmCourseLibrary)
    {
        // 唯一性校验：课程编码不能重复（排除自身）
        TpmCourseLibrary existing = tpmCourseLibraryMapper.selectTpmCourseLibraryByCourseCode(tpmCourseLibrary.getCourseCode());
        if (existing != null && !existing.getCourseId().equals(tpmCourseLibrary.getCourseId()))
        {
            throw new ServiceException("课程编码'" + tpmCourseLibrary.getCourseCode() + "'已存在");
        }
        tpmCourseLibrary.setUpdateTime(DateUtils.getNowDate());
        return tpmCourseLibraryMapper.updateTpmCourseLibrary(tpmCourseLibrary);
    }

    @Transactional
    @Override
    public int deleteTpmCourseLibraryByCourseId(Long courseId)
    {
        TpmCourseOffering query = new TpmCourseOffering();
        query.setCourseId(courseId);
        List<TpmCourseOffering> offerings = tpmCourseOfferingMapper.selectTpmCourseOfferingList(query);
        if (offerings != null && !offerings.isEmpty())
        {
            throw new ServiceException("该课程下存在开课计划，不允许删除");
        }
        return tpmCourseLibraryMapper.deleteTpmCourseLibraryByCourseId(courseId);
    }

    @Transactional
    @Override
    public int deleteTpmCourseLibraryByCourseIds(Long[] courseIds)
    {
        for (Long courseId : courseIds)
        {
            TpmCourseOffering query = new TpmCourseOffering();
            query.setCourseId(courseId);
            List<TpmCourseOffering> offerings = tpmCourseOfferingMapper.selectTpmCourseOfferingList(query);
            if (offerings != null && !offerings.isEmpty())
            {
                throw new ServiceException("该课程下存在开课计划，不允许删除");
            }
        }
        return tpmCourseLibraryMapper.deleteTpmCourseLibraryByCourseIds(courseIds);
    }

    /**
     * P7：批量导入课程，逐行校验（必填、编码唯一）并生成校验报告。
     */
    @Transactional
    @Override
    public String importCourse(List<TpmCourseLibrary> courseList, String operName, boolean updateSupport)
    {
        if (courseList == null || courseList.isEmpty())
        {
            throw new ServiceException("导入课程数据不能为空！");
        }
        int successNum = 0;
        int updateNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        int row = 0;
        for (TpmCourseLibrary course : courseList)
        {
            row++;
            // 1. 必填字段校验
            if (StringUtils.isBlank(course.getCourseCode()))
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：课程编码不能为空");
                continue;
            }
            if (StringUtils.isBlank(course.getCourseName()))
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：课程名称不能为空");
                continue;
            }
            if (course.getCredit() != null && course.getCredit() < 0)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：学分不能为负数");
                continue;
            }
            // 2. 编码唯一性校验
            TpmCourseLibrary existing = tpmCourseLibraryMapper.selectTpmCourseLibraryByCourseCode(course.getCourseCode());
            try
            {
                if (existing == null)
                {
                    course.setCreateBy(operName);
                    course.setCreateTime(DateUtils.getNowDate());
                    tpmCourseLibraryMapper.insertTpmCourseLibrary(course);
                    successNum++;
                    successMsg.append("<br/>").append(successNum).append("、课程 ").append(course.getCourseCode()).append(" 导入成功");
                }
                else if (updateSupport)
                {
                    course.setCourseId(existing.getCourseId());
                    course.setUpdateBy(operName);
                    course.setUpdateTime(DateUtils.getNowDate());
                    tpmCourseLibraryMapper.updateTpmCourseLibrary(course);
                    updateNum++;
                    successMsg.append("<br/>").append(updateNum).append("、课程 ").append(course.getCourseCode()).append(" 更新成功");
                }
                else
                {
                    failureNum++;
                    failureMsg.append("<br/>第 ").append(row).append(" 行：课程编码 ").append(course.getCourseCode()).append(" 已存在");
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
