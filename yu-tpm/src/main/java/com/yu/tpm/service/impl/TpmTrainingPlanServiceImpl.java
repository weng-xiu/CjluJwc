package com.yu.tpm.service.impl;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.yu.common.annotation.DataScope;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.SecurityUtils;
import com.yu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.tpm.mapper.TpmTrainingPlanMapper;
import com.yu.tpm.mapper.TpmCreditStructureMapper;
import com.yu.tpm.mapper.TpmCourseLibraryMapper;
import com.yu.tpm.domain.TpmTrainingPlan;
import com.yu.tpm.domain.TpmCreditStructure;
import com.yu.tpm.domain.TpmCourseLibrary;
import com.yu.tpm.service.ITpmTrainingPlanService;
import com.yu.tpm.service.ITpmCourseLibraryService;
import com.yu.tpm.service.ITpmCreditStructureService;

/**
 * 人才培养方案Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmTrainingPlanServiceImpl implements ITpmTrainingPlanService 
{
    @Autowired
    private TpmTrainingPlanMapper tpmTrainingPlanMapper;

    @Autowired
    private TpmCreditStructureMapper tpmCreditStructureMapper;

    @Autowired
    private TpmCourseLibraryMapper tpmCourseLibraryMapper;

    @Autowired
    private ITpmCourseLibraryService tpmCourseLibraryService;

    @Autowired
    private ITpmCreditStructureService tpmCreditStructureService;

    @Override
    public TpmTrainingPlan selectTpmTrainingPlanByPlanId(Long planId)
    {
        return tpmTrainingPlanMapper.selectTpmTrainingPlanByPlanId(planId);
    }

    @Override
    @DataScope(deptAlias = "d")
    public List<TpmTrainingPlan> selectTpmTrainingPlanList(TpmTrainingPlan tpmTrainingPlan)
    {
        return tpmTrainingPlanMapper.selectTpmTrainingPlanList(tpmTrainingPlan);
    }

    @Transactional
    @Override
    public int insertTpmTrainingPlan(TpmTrainingPlan tpmTrainingPlan)
    {
        // T3：新建方案未指定版本号时默认 V1
        if (StringUtils.isBlank(tpmTrainingPlan.getVersion()))
        {
            tpmTrainingPlan.setVersion("V1");
        }
        tpmTrainingPlan.setCreateTime(DateUtils.getNowDate());
        return tpmTrainingPlanMapper.insertTpmTrainingPlan(tpmTrainingPlan);
    }

    @Transactional
    @Override
    public int updateTpmTrainingPlan(TpmTrainingPlan tpmTrainingPlan)
    {
        // T3：已发布方案锁定，不允许编辑（含 savePlanWithChildren 的更新分支）
        if (tpmTrainingPlan.getPlanId() != null)
        {
            checkNotPublished(tpmTrainingPlan.getPlanId());
        }
        tpmTrainingPlan.setUpdateTime(DateUtils.getNowDate());
        return tpmTrainingPlanMapper.updateTpmTrainingPlan(tpmTrainingPlan);
    }

    /**
     * T3：校验方案未处于已发布状态，已发布则拒绝修改/删除
     */
    private void checkNotPublished(Long planId)
    {
        TpmTrainingPlan db = tpmTrainingPlanMapper.selectTpmTrainingPlanByPlanId(planId);
        if (db == null)
        {
            throw new ServiceException("培养方案不存在或已删除");
        }
        if ("1".equals(db.getPublishStatus()))
        {
            throw new ServiceException("培养方案【" + db.getPlanName() + "】已发布，不允许编辑或删除；如需修改请先复制新版本");
        }
    }

    @Transactional
    @Override
    public int deleteTpmTrainingPlanByPlanId(Long planId)
    {
        checkCanDelete(planId);
        return tpmTrainingPlanMapper.deleteTpmTrainingPlanByPlanId(planId);
    }

    @Transactional
    @Override
    public int deleteTpmTrainingPlanByPlanIds(Long[] planIds)
    {
        for (Long planId : planIds)
        {
            checkCanDelete(planId);
        }
        return tpmTrainingPlanMapper.deleteTpmTrainingPlanByPlanIds(planIds);
    }

    /**
     * 删除前级联校验：不允许存在学分结构或课程；T3：已发布方案不允许删除
     */
    private void checkCanDelete(Long planId)
    {
        checkNotPublished(planId);
        TpmCreditStructure structQuery = new TpmCreditStructure();
        structQuery.setPlanId(planId);
        List<TpmCreditStructure> structures = tpmCreditStructureMapper.selectTpmCreditStructureList(structQuery);
        if (structures != null && !structures.isEmpty())
        {
            throw new ServiceException("该培养方案下存在学分结构，不允许删除");
        }
        TpmCourseLibrary courseQuery = new TpmCourseLibrary();
        courseQuery.setPlanId(planId);
        List<TpmCourseLibrary> courses = tpmCourseLibraryMapper.selectTpmCourseLibraryList(courseQuery);
        if (courses != null && !courses.isEmpty())
        {
            throw new ServiceException("该培养方案下存在课程，不允许删除");
        }
    }

    @Transactional
    @Override
    public int publishTrainingPlan(Long planId)
    {
        // T3：发布前校验——存在性/重复发布/同专业同学年冲突
        TpmTrainingPlan db = tpmTrainingPlanMapper.selectTpmTrainingPlanByPlanId(planId);
        if (db == null)
        {
            throw new ServiceException("培养方案不存在或已删除");
        }
        if ("1".equals(db.getPublishStatus()))
        {
            throw new ServiceException("该方案已是发布状态，无需重复发布");
        }
        if (db.getMajorId() != null)
        {
            int conflict = tpmTrainingPlanMapper.countPublishedConflict(db.getMajorId(), db.getPlanYear(), planId);
            if (conflict > 0)
            {
                throw new ServiceException("同专业同学年（" + db.getPlanYear() + "）已存在其他已发布方案，不允许重复发布；请先废止旧方案或使用版本复制");
            }
        }
        TpmTrainingPlan plan = new TpmTrainingPlan();
        plan.setPlanId(planId);
        plan.setPublishStatus("1");
        plan.setPublishDate(DateUtils.getNowDate());
        plan.setUpdateTime(DateUtils.getNowDate());
        // T3：存量数据版本号补齐
        if (StringUtils.isBlank(db.getVersion()))
        {
            plan.setVersion("V1");
        }
        return tpmTrainingPlanMapper.updateTpmTrainingPlan(plan);
    }

    @Transactional
    @Override
    public int deprecateTrainingPlan(Long planId)
    {
        TpmTrainingPlan plan = new TpmTrainingPlan();
        plan.setPlanId(planId);
        plan.setPublishStatus("2");
        plan.setUpdateTime(DateUtils.getNowDate());
        return tpmTrainingPlanMapper.updateTpmTrainingPlan(plan);
    }

    /**
     * T3：复制培养方案（含课程与学分结构子表）为新草稿版本，版本号自动递增。
     */
    @Transactional
    @Override
    public Long copyTrainingPlan(Long planId)
    {
        TpmTrainingPlan src = tpmTrainingPlanMapper.selectTpmTrainingPlanByPlanId(planId);
        if (src == null)
        {
            throw new ServiceException("待复制的培养方案不存在或已删除");
        }
        String operator;
        try { operator = SecurityUtils.getUsername(); } catch (Exception e) { operator = "system"; }

        // 1. 复制主表：重置为草稿、版本号递增
        TpmTrainingPlan copy = new TpmTrainingPlan();
        copy.setPlanName(src.getPlanName());
        copy.setMajorId(src.getMajorId());
        copy.setDeptId(src.getDeptId());
        copy.setEducationLevel(src.getEducationLevel());
        copy.setPlanYear(src.getPlanYear());
        copy.setTotalCredits(src.getTotalCredits());
        copy.setPublishStatus("0");
        String newVersion = nextVersion(maxVersionOfSiblings(src));
        copy.setVersion(newVersion);
        copy.setStatus("0");
        copy.setRemark(src.getRemark());
        copy.setCreateBy(operator);
        tpmTrainingPlanMapper.insertTpmTrainingPlan(copy);
        Long newPlanId = copy.getPlanId();

        // 2. 复制课程子表（作为新记录挂到新方案）
        TpmCourseLibrary courseQuery = new TpmCourseLibrary();
        courseQuery.setPlanId(planId);
        List<TpmCourseLibrary> courses = tpmCourseLibraryMapper.selectTpmCourseLibraryList(courseQuery);
        if (courses != null)
        {
            for (TpmCourseLibrary course : courses)
            {
                course.setCourseId(null);
                course.setPlanId(newPlanId);
                // 复制课程编码加版本后缀（MATH101 -> MATH101-V2），避免撞课程库编码唯一校验
                if (StringUtils.isNotBlank(course.getCourseCode()))
                {
                    course.setCourseCode(course.getCourseCode().replaceAll("-V\\d+$", "") + "-" + newVersion);
                }
                course.setCreateBy(operator);
                tpmCourseLibraryService.insertTpmCourseLibrary(course);
            }
        }

        // 3. 复制学分结构子表
        TpmCreditStructure structQuery = new TpmCreditStructure();
        structQuery.setPlanId(planId);
        List<TpmCreditStructure> structures = tpmCreditStructureMapper.selectTpmCreditStructureList(structQuery);
        if (structures != null)
        {
            for (TpmCreditStructure struct : structures)
            {
                struct.setStructId(null);
                struct.setPlanId(newPlanId);
                struct.setCreateBy(operator);
                tpmCreditStructureService.insertTpmCreditStructure(struct);
            }
        }
        return newPlanId;
    }

    /**
     * T3：取同专业同学年全部方案的版本号（含已废止），用于递推不重复的新版本。
     */
    private String maxVersionOfSiblings(TpmTrainingPlan src)
    {
        String max = src.getVersion();
        if (src.getMajorId() == null)
        {
            return max;
        }
        TpmTrainingPlan query = new TpmTrainingPlan();
        query.setMajorId(src.getMajorId());
        query.setPlanYear(src.getPlanYear());
        List<TpmTrainingPlan> siblings = tpmTrainingPlanMapper.selectTpmTrainingPlanList(query);
        if (siblings != null)
        {
            for (TpmTrainingPlan sibling : siblings)
            {
                if (versionNumber(sibling.getVersion()) > versionNumber(max))
                {
                    max = sibling.getVersion();
                }
            }
        }
        return max;
    }

    /**
     * T3：解析 Vn 格式版本号的数字部分，非法/空返回 -1。
     */
    private int versionNumber(String version)
    {
        if (StringUtils.isBlank(version))
        {
            return -1;
        }
        Matcher matcher = Pattern.compile("^\\s*[Vv](\\d+)\\s*$").matcher(version);
        return matcher.matches() ? Integer.parseInt(matcher.group(1)) : -1;
    }

    /**
     * T3：版本号递增：V1 -> V2；无版本或非 V 格式时回退为 V1。
     */
    private String nextVersion(String version)
    {
        int num = versionNumber(version);
        if (num >= 0)
        {
            return "V" + (num + 1);
        }
        return "V1";
    }

    @Transactional
    @Override
    public int savePlanWithChildren(TpmTrainingPlan plan, List<TpmCourseLibrary> courseList, List<TpmCreditStructure> creditList)
    {
        int rows;
        if (plan.getPlanId() == null)
        {
            rows = insertTpmTrainingPlan(plan);
        }
        else
        {
            rows = updateTpmTrainingPlan(plan);
        }
        if (courseList != null)
        {
            for (TpmCourseLibrary course : courseList)
            {
                course.setPlanId(plan.getPlanId());
                if (course.getStatus() == null)
                {
                    course.setStatus("0");
                }
                if (course.getCourseId() == null)
                {
                    tpmCourseLibraryService.insertTpmCourseLibrary(course);
                }
                else
                {
                    tpmCourseLibraryService.updateTpmCourseLibrary(course);
                }
            }
        }
        if (creditList != null)
        {
            for (TpmCreditStructure credit : creditList)
            {
                credit.setPlanId(plan.getPlanId());
                if (credit.getStatus() == null)
                {
                    credit.setStatus("0");
                }
                if (credit.getStructId() == null)
                {
                    tpmCreditStructureService.insertTpmCreditStructure(credit);
                }
                else
                {
                    tpmCreditStructureService.updateTpmCreditStructure(credit);
                }
            }
        }
        return rows;
    }
}
