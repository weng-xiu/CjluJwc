package com.yu.tpm.service.impl;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.yu.common.annotation.DataScope;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.tpm.mapper.TpmCourseOfferingMapper;
import com.yu.tpm.mapper.TpmCourseLibraryMapper;
import com.yu.tpm.mapper.TpmScheduleMapper;
import com.yu.tpm.mapper.TpmSelectionEnrollmentMapper;
import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.domain.TpmCourseLibrary;
import com.yu.tpm.domain.TpmSchedule;
import com.yu.tpm.domain.dto.BatchOfferingRequest;
import com.yu.tpm.plan.OfferingGenerationResult;
import com.yu.tpm.plan.OfferingPlanGenerator;
import com.yu.tpm.service.ITpmCourseOfferingService;

/**
 * 开课计划Service业务层处理
 *
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmCourseOfferingServiceImpl implements ITpmCourseOfferingService
{
    @Autowired
    private TpmCourseOfferingMapper tpmCourseOfferingMapper;

    @Autowired
    private TpmScheduleMapper tpmScheduleMapper;

    @Autowired
    private TpmSelectionEnrollmentMapper tpmSelectionEnrollmentMapper;

    @Autowired
    private TpmCourseLibraryMapper tpmCourseLibraryMapper;

    @Autowired
    private OfferingPlanGenerator offeringPlanGenerator;

    /** 默认开课容量（可配置，兼容排课默认） */
    @Value("${tpm.schedule.defaultCapacity:30}")
    private int defaultCapacity;

    @Override
    public TpmCourseOffering selectTpmCourseOfferingByOfferingId(Long offeringId)
    {
        return tpmCourseOfferingMapper.selectTpmCourseOfferingByOfferingId(offeringId);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "t")
    public List<TpmCourseOffering> selectTpmCourseOfferingList(TpmCourseOffering tpmCourseOffering)
    {
        return tpmCourseOfferingMapper.selectTpmCourseOfferingList(tpmCourseOffering);
    }

    @Override
    public List<TpmCourseOffering> selectTpmCourseOfferingListForPortal(TpmCourseOffering tpmCourseOffering)
    {
        // 门户端学生选课不受部门限制，且教师 userAlias 数据范围会让学生（仅本人）永远查空
        return tpmCourseOfferingMapper.selectTpmCourseOfferingListForPortal(tpmCourseOffering);
    }

    @Transactional
    @Override
    public int insertTpmCourseOffering(TpmCourseOffering tpmCourseOffering)
    {
        tpmCourseOffering.setCreateTime(DateUtils.getNowDate());
        return tpmCourseOfferingMapper.insertTpmCourseOffering(tpmCourseOffering);
    }

    @Transactional
    @Override
    public int updateTpmCourseOffering(TpmCourseOffering tpmCourseOffering)
    {
        tpmCourseOffering.setUpdateTime(DateUtils.getNowDate());
        return tpmCourseOfferingMapper.updateTpmCourseOffering(tpmCourseOffering);
    }

    /**
     * 删除前校验：存在排课记录或选课记录时不允许删除
     */
    private void checkBeforeDelete(Long offeringId)
    {
        List<TpmSchedule> schedules = tpmScheduleMapper.selectByOfferingIds(java.util.Collections.singletonList(offeringId));
        if (schedules != null && !schedules.isEmpty())
        {
            throw new ServiceException("该开课已存在排课记录，不允许删除");
        }
        int enrollCount = tpmSelectionEnrollmentMapper.selectCountByOffering(offeringId);
        if (enrollCount > 0)
        {
            throw new ServiceException("该开课已存在选课记录，不允许删除");
        }
    }

    @Transactional
    @Override
    public int deleteTpmCourseOfferingByOfferingId(Long offeringId)
    {
        checkBeforeDelete(offeringId);
        return tpmCourseOfferingMapper.deleteTpmCourseOfferingByOfferingId(offeringId);
    }

    @Transactional
    @Override
    public int deleteTpmCourseOfferingByOfferingIds(Long[] offeringIds)
    {
        if (offeringIds != null)
        {
            for (Long offeringId : offeringIds)
            {
                checkBeforeDelete(offeringId);
            }
        }
        return tpmCourseOfferingMapper.deleteTpmCourseOfferingByOfferingIds(offeringIds);
    }

    @Transactional
    @Override
    public int confirmOffering(Long offeringId)
    {
        TpmCourseOffering offering = new TpmCourseOffering();
        offering.setOfferingId(offeringId);
        offering.setOfferingStatus("1");
        offering.setUpdateTime(DateUtils.getNowDate());
        return tpmCourseOfferingMapper.updateTpmCourseOffering(offering);
    }

    @Transactional
    @Override
    public int cancelOffering(Long offeringId)
    {
        TpmCourseOffering offering = new TpmCourseOffering();
        offering.setOfferingId(offeringId);
        offering.setOfferingStatus("2");
        offering.setUpdateTime(DateUtils.getNowDate());
        return tpmCourseOfferingMapper.updateTpmCourseOffering(offering);
    }

    @Transactional
    @Override
    public Map<String, Object> batchGenerateOfferings(BatchOfferingRequest request)
    {
        Map<String, Object> result = new HashMap<>();
        if (request == null || request.getPlanId() == null)
        {
            throw new ServiceException("培养方案ID不能为空");
        }
        if (request.getSemesterId() == null)
        {
            throw new ServiceException("学期ID不能为空");
        }
        // 1. 方案课程
        List<TpmCourseLibrary> courses = tpmCourseLibraryMapper.selectPlanCourses(request.getPlanId(), request.getSemesterOrder());
        result.put("scanned", courses == null ? 0 : courses.size());
        if (courses == null || courses.isEmpty())
        {
            result.put("generated", 0);
            result.put("message", "该培养方案下未找到可生成课程（请检查方案课程与建议修读学期）");
            return result;
        }
        // 2. 幂等：当前学期已存在开课的课程
        boolean skipExisting = request.getSkipExisting() == null || request.getSkipExisting();
        Set<Long> existingCourseIds = skipExisting
                ? new HashSet<>(tpmCourseOfferingMapper.selectActiveCourseIdsBySemester(request.getSemesterId()))
                : new HashSet<>();
        // 3. 教师池
        List<Long> teacherIds = tpmCourseOfferingMapper.selectTeacherPool(request.getTeacherDeptId());
        // 4. 参数兼容
        int capacity = request.getDefaultCapacity() != null && request.getDefaultCapacity() > 0
                ? request.getDefaultCapacity() : defaultCapacity;
        int classCount = request.getClassCount() != null && request.getClassCount() > 0
                ? request.getClassCount() : 1;
        // 5. 生成
        OfferingGenerationResult gen = offeringPlanGenerator.generate(courses, existingCourseIds, teacherIds,
                request.getSemesterId(), request.getCampusId(), capacity, classCount, skipExisting);
        List<TpmCourseOffering> toInsert = gen.getOfferings();
        if (!toInsert.isEmpty())
        {
            Date now = DateUtils.getNowDate();
            String operator;
            try { operator = SecurityUtils.getUsername(); } catch (Exception e) { operator = "system"; }
            for (TpmCourseOffering o : toInsert)
            {
                o.setCreateBy(operator);
                o.setCreateTime(now);
            }
            tpmCourseOfferingMapper.batchInsertTpmCourseOffering(toInsert);
        }
        result.put("generated", gen.getGenerated());
        result.put("skippedExisting", gen.getSkippedExisting());
        result.put("teacherAssigned", gen.getTeacherAssigned());
        result.put("capacity", capacity);
        result.put("classCount", classCount);
        result.put("teacherPoolSize", teacherIds == null ? 0 : teacherIds.size());
        result.put("message", String.format("开课计划生成完成：新增%d门，跳过已存在%d门，预分配教师%d门（待确认，可编辑后确认）",
                gen.getGenerated(), gen.getSkippedExisting(), gen.getTeacherAssigned()));
        return result;
    }
}
