package com.yu.sam.service.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.yu.common.annotation.DataScope;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import com.yu.common.utils.bean.FieldChange;
import com.yu.common.utils.bean.FieldDiffUtils;
import com.yu.system.service.IDataChangeAuditService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.sam.mapper.SamStudentMapper;
import com.yu.sam.mapper.SamStatusChangeMapper;
import com.yu.sam.mapper.SamCertificateMapper;
import com.yu.sam.domain.SamStudent;
import com.yu.sam.domain.SamStatusChange;
import com.yu.sam.domain.SamCertificate;
import com.yu.sam.service.ISamStudentService;

/**
 * 学生学籍Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-13
 */
@Service
public class SamStudentServiceImpl implements ISamStudentService 
{
    @Autowired
    private SamStudentMapper samStudentMapper;

    @Autowired
    private SamStatusChangeMapper samStatusChangeMapper;

    @Autowired
    private SamCertificateMapper samCertificateMapper;

    /** K1 合规③：字段级数据变更留痕服务（学籍含身份证等个人敏感信息，修改须可追溯旧值/新值/操作人） */
    @Autowired
    private IDataChangeAuditService dataChangeAuditService;

    /** 学籍留痕跟踪字段：仅比对这些业务列，忽略关联展示字段与技术字段 */
    private static final Set<String> STUDENT_TRACKED_FIELDS = new HashSet<>(Arrays.asList(
            "studentNo", "studentName", "gender", "birthDate", "idCard", "majorId", "deptId",
            "classId", "enrollmentYear", "educationLevel", "studentStatus", "status"));

    /** 学籍敏感字段：命中者旧/新值掩码后再落库，防止明文身份证进入审计流水 */
    private static final Set<String> STUDENT_SENSITIVE_FIELDS = new HashSet<>(Arrays.asList("idCard"));

    /** 学籍变更流水的实体类型标识（与表名一致，供审计查询归类） */
    private static final String STUDENT_ENTITY_TYPE = "sam_student";

    @Override
    public SamStudent selectSamStudentByStudentId(Long studentId)
    {
        return samStudentMapper.selectSamStudentByStudentId(studentId);
    }

    @Override
    public SamStudent selectSamStudentByUserId(Long userId)
    {
        return samStudentMapper.selectSamStudentByUserId(userId);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "s")
    public List<SamStudent> selectSamStudentList(SamStudent samStudent)
    {
        return samStudentMapper.selectSamStudentList(samStudent);
    }

    @Override
    @Transactional
    public int insertSamStudent(SamStudent samStudent)
    {
        // 唯一性校验：学号不能重复
        SamStudent query = new SamStudent();
        query.setStudentNo(samStudent.getStudentNo());
        List<SamStudent> existing = samStudentMapper.selectSamStudentList(query);
        if (existing != null && !existing.isEmpty())
        {
            throw new ServiceException("学号'" + samStudent.getStudentNo() + "'已存在");
        }
        samStudent.setCreateTime(DateUtils.getNowDate());
        return samStudentMapper.insertSamStudent(samStudent);
    }

    @Override
    @Transactional
    public int updateSamStudent(SamStudent samStudent)
    {
        // 状态校验：已毕业的学生不允许修改学籍信息
        SamStudent existing = samStudentMapper.selectSamStudentByStudentId(samStudent.getStudentId());
        if (existing != null && "3".equals(existing.getStudentStatus()))
        {
            throw new ServiceException("已毕业的学生不允许修改学籍信息");
        }
        samStudent.setUpdateTime(DateUtils.getNowDate());
        int rows = samStudentMapper.updateSamStudent(samStudent);
        // K1 合规③：修改成功后，在同一事务内比对旧值并写字段级变更流水（无差异则不写，身份证等敏感字段脱敏）
        if (rows > 0 && existing != null)
        {
            List<FieldChange> changes = FieldDiffUtils.diff(existing, samStudent, STUDENT_TRACKED_FIELDS, STUDENT_SENSITIVE_FIELDS);
            dataChangeAuditService.recordDiff(STUDENT_ENTITY_TYPE, "学生学籍",
                    String.valueOf(samStudent.getStudentId()), changes);
        }
        return rows;
    }

    @Override
    @Transactional
    public int deleteSamStudentByStudentId(Long studentId)
    {
        if (samStudentMapper.checkStudentHasGradeRecord(studentId) > 0)
        {
            throw new ServiceException("该学生存在成绩记录，不允许删除");
        }
        SamStatusChange scQuery = new SamStatusChange();
        scQuery.setStudentId(studentId);
        List<SamStatusChange> statusChanges = samStatusChangeMapper.selectSamStatusChangeList(scQuery);
        if (statusChanges != null && !statusChanges.isEmpty())
        {
            throw new ServiceException("该学生存在学籍异动记录，不允许删除");
        }
        SamCertificate certQuery = new SamCertificate();
        certQuery.setStudentId(studentId);
        List<SamCertificate> certificates = samCertificateMapper.selectSamCertificateList(certQuery);
        if (certificates != null && !certificates.isEmpty())
        {
            throw new ServiceException("该学生存在证书记录，不允许删除");
        }
        return samStudentMapper.deleteSamStudentByStudentId(studentId);
    }

    @Override
    @Transactional
    public int deleteSamStudentByStudentIds(Long[] studentIds)
    {
        for (Long studentId : studentIds)
        {
            if (samStudentMapper.checkStudentHasGradeRecord(studentId) > 0)
            {
                throw new ServiceException("该学生存在成绩记录，不允许删除");
            }
            SamStatusChange scQuery = new SamStatusChange();
            scQuery.setStudentId(studentId);
            List<SamStatusChange> statusChanges = samStatusChangeMapper.selectSamStatusChangeList(scQuery);
            if (statusChanges != null && !statusChanges.isEmpty())
            {
                throw new ServiceException("该学生存在学籍异动记录，不允许删除");
            }
            SamCertificate certQuery = new SamCertificate();
            certQuery.setStudentId(studentId);
            List<SamCertificate> certificates = samCertificateMapper.selectSamCertificateList(certQuery);
            if (certificates != null && !certificates.isEmpty())
            {
                throw new ServiceException("该学生存在证书记录，不允许删除");
            }
        }
        return samStudentMapper.deleteSamStudentByStudentIds(studentIds);
    }

    /**
     * P7：批量导入学籍，逐行校验（学号/姓名必填、专业/院系/班级存在、身份证唯一、学号唯一）并生成校验报告。
     */
    @Transactional
    @Override
    public String importStudent(List<SamStudent> studentList, String operName, boolean updateSupport)
    {
        if (studentList == null || studentList.isEmpty())
        {
            throw new ServiceException("导入学籍数据不能为空！");
        }
        int successNum = 0;
        int updateNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        int row = 0;
        for (SamStudent student : studentList)
        {
            row++;
            // 1. 必填字段校验
            if (StringUtils.isBlank(student.getStudentNo()))
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：学号不能为空");
                continue;
            }
            if (StringUtils.isBlank(student.getStudentName()))
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：姓名不能为空");
                continue;
            }
            if (student.getMajorId() == null)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：专业ID不能为空");
                continue;
            }
            if (student.getDeptId() == null)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：院系ID不能为空");
                continue;
            }
            if (student.getClassId() == null)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：班级ID不能为空");
                continue;
            }
            // 2. 外键存在性校验
            if (samStudentMapper.countMajorExists(student.getMajorId()) == 0)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：专业ID ").append(student.getMajorId()).append(" 不存在");
                continue;
            }
            if (samStudentMapper.countDeptExists(student.getDeptId()) == 0)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：院系ID ").append(student.getDeptId()).append(" 不存在");
                continue;
            }
            if (samStudentMapper.countClassExists(student.getClassId()) == 0)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：班级ID ").append(student.getClassId()).append(" 不存在");
                continue;
            }
            // 3. 学号唯一性校验 + 身份证冲突校验
            SamStudent existing = samStudentMapper.selectSamStudentByStudentNo(student.getStudentNo());
            if (StringUtils.isNotEmpty(student.getIdCard())
                    && samStudentMapper.countIdCardConflict(student.getIdCard(), existing == null ? null : existing.getStudentId()) > 0)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(row).append(" 行：身份证号已存在于其他学籍记录");
                continue;
            }
            try
            {
                if (StringUtils.isEmpty(student.getStudentStatus()))
                {
                    student.setStudentStatus("0");
                }
                if (StringUtils.isEmpty(student.getStatus()))
                {
                    student.setStatus("0");
                }
                if (existing == null)
                {
                    student.setCreateBy(operName);
                    student.setCreateTime(DateUtils.getNowDate());
                    samStudentMapper.insertSamStudent(student);
                    successNum++;
                    successMsg.append("<br/>").append(successNum).append("、学生 ").append(student.getStudentNo()).append(" 导入成功");
                }
                else if ("3".equals(existing.getStudentStatus()))
                {
                    failureNum++;
                    failureMsg.append("<br/>第 ").append(row).append(" 行：学号 ").append(student.getStudentNo()).append(" 已毕业，不允许覆盖");
                }
                else if (updateSupport)
                {
                    student.setStudentId(existing.getStudentId());
                    student.setUpdateBy(operName);
                    student.setUpdateTime(DateUtils.getNowDate());
                    samStudentMapper.updateSamStudent(student);
                    updateNum++;
                    successMsg.append("<br/>").append(updateNum).append("、学生 ").append(student.getStudentNo()).append(" 更新成功");
                }
                else
                {
                    failureNum++;
                    failureMsg.append("<br/>第 ").append(row).append(" 行：学号 ").append(student.getStudentNo()).append(" 已存在");
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
