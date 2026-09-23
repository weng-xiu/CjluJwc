package com.yu.tpm.service.impl;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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
import com.yu.tpm.domain.dto.PlanImportRow;
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

    /**
     * P7：培养方案 Excel 导入。
     * 逐行校验（必填 → 专业编码存在 → 课程编码存在），业务键=专业+年份+学历层次；
     * 新行建为草稿(V1)，已存在且 updateSupport 时仅允许覆盖未发布方案；课程清单按编码挂接 plan_id。
     * 部分失败不回滚已成功行，返回逐行报告。
     */
    @Override
    public String importPlan(List<PlanImportRow> rows, String operName, boolean updateSupport)
    {
        if (rows == null || rows.isEmpty())
        {
            throw new ServiceException("导入培养方案数据不能为空！");
        }
        int successNum = 0;
        int updateNum = 0;
        int failureNum = 0;
        StringBuilder successMsg = new StringBuilder();
        StringBuilder failureMsg = new StringBuilder();
        int rowNo = 0;
        // 同文件内业务键去重（防止一个 Excel 中多行指向同一方案）
        Set<String> bizKeys = new HashSet<>();
        for (PlanImportRow row : rows)
        {
            rowNo++;
            String planName = StringUtils.trimToEmpty(row.getPlanName());
            String majorCode = StringUtils.trimToEmpty(row.getMajorCode());
            String planYear = StringUtils.trimToEmpty(row.getPlanYear());
            // 1. 必填校验
            if (planName.isEmpty() || majorCode.isEmpty() || planYear.isEmpty())
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(rowNo).append(" 行：方案名称、专业编码、方案年份均不能为空");
                continue;
            }
            // 2. 专业编码解析
            Long majorId = tpmTrainingPlanMapper.selectMajorIdByCode(majorCode);
            if (majorId == null)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(rowNo).append(" 行：专业编码 ").append(majorCode).append(" 不存在");
                continue;
            }
            String educationLevel = normalizeEducationLevel(row.getEducationLevel());
            String bizKey = majorId + "|" + planYear + "|" + educationLevel;
            if (!bizKeys.add(bizKey))
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(rowNo).append(" 行：文件内存在相同业务键（专业+年份+学历层次）的方案行，已跳过");
                continue;
            }
            // 3. 课程编码清单预检（全部存在才挂接，避免半挂接）
            List<TpmCourseLibrary> courses = new ArrayList<>();
            String courseErr = resolveImportCourses(row.getCourseCodes(), courses);
            if (courseErr != null)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(rowNo).append(" 行：").append(courseErr);
                continue;
            }
            try
            {
                TpmTrainingPlan existing = tpmTrainingPlanMapper.selectPlanByBizKey(majorId, planYear, educationLevel);
                if (existing == null)
                {
                    TpmTrainingPlan plan = new TpmTrainingPlan();
                    plan.setPlanName(planName);
                    plan.setMajorId(majorId);
                    plan.setPlanYear(planYear);
                    plan.setEducationLevel(educationLevel);
                    plan.setTotalCredits(row.getTotalCredits());
                    plan.setVersion(StringUtils.isNotBlank(row.getVersion()) ? row.getVersion().trim() : "V1");
                    plan.setPublishStatus("0");
                    plan.setStatus("0");
                    plan.setCreateBy(operName);
                    plan.setCreateTime(DateUtils.getNowDate());
                    tpmTrainingPlanMapper.insertTpmTrainingPlan(plan);
                    bindImportCourses(plan.getPlanId(), courses, operName);
                    successNum++;
                    successMsg.append("<br/>").append(successNum).append("、方案 ").append(planName)
                            .append("（专业 ").append(majorCode).append(" / ").append(planYear).append(" 级）导入成功，已建为草稿");
                }
                else if (updateSupport)
                {
                    if ("1".equals(existing.getPublishStatus()))
                    {
                        failureNum++;
                        failureMsg.append("<br/>第 ").append(rowNo).append(" 行：方案【").append(existing.getPlanName())
                                .append("】已发布，不允许覆盖导入");
                        continue;
                    }
                    TpmTrainingPlan upd = new TpmTrainingPlan();
                    upd.setPlanId(existing.getPlanId());
                    upd.setPlanName(planName);
                    upd.setTotalCredits(row.getTotalCredits());
                    if (StringUtils.isNotBlank(row.getVersion()))
                    {
                        upd.setVersion(row.getVersion().trim());
                    }
                    upd.setUpdateBy(operName);
                    upd.setUpdateTime(DateUtils.getNowDate());
                    tpmTrainingPlanMapper.updateTpmTrainingPlan(upd);
                    bindImportCourses(existing.getPlanId(), courses, operName);
                    updateNum++;
                    successMsg.append("<br/>").append(updateNum).append("、方案 ").append(planName).append(" 覆盖更新成功");
                }
                else
                {
                    failureNum++;
                    failureMsg.append("<br/>第 ").append(rowNo).append(" 行：方案已存在（专业 ").append(majorCode)
                            .append(" / ").append(planYear).append(" 级 / 学历 ").append(educationLevel)
                            .append("），如需覆盖请勾选更新支持");
                }
            }
            catch (Exception e)
            {
                failureNum++;
                failureMsg.append("<br/>第 ").append(rowNo).append(" 行：").append(e.getMessage());
            }
        }
        if (failureNum > 0)
        {
            failureMsg.insert(0, "很抱歉，导入失败！共 " + failureNum + " 条数据格式不正确，错误如下：");
            throw new ServiceException(failureMsg.toString());
        }
        successMsg.insert(0, "恭喜您，数据已全部导入成功！共 " + successNum + " 条新增");
        if (updateNum > 0)
        {
            successMsg.append("，").append(updateNum).append(" 条覆盖更新");
        }
        return successMsg.toString();
    }

    /**
     * P7：学历层次宽容归一——历史数据存在编码（'1'）与文本（'本科'）两种口径，
     * 导入时按常见标签归一为编码，其余原样保留。
     */
    private String normalizeEducationLevel(String raw)
    {
        String v = StringUtils.trimToEmpty(raw);
        switch (v)
        {
            case "本科": return "1";
            case "专科": return "2";
            case "硕士研究生":
            case "硕士": return "3";
            case "博士研究生":
            case "博士": return "4";
            default: return v.isEmpty() ? null : v;
        }
    }

    /**
     * P7：解析并预检课程编码清单（逗号/分号/顿号/空白分隔）。
     * 全部编码存在于课程库则填充 courses 并返回 null；否则返回错误信息。
     */
    private String resolveImportCourses(String courseCodes, List<TpmCourseLibrary> courses)
    {
        if (StringUtils.isBlank(courseCodes))
        {
            return null;
        }
        Set<String> codes = new LinkedHashSet<>();
        for (String code : courseCodes.split("[,，;；、\\s]+"))
        {
            if (StringUtils.isNotBlank(code))
            {
                codes.add(code.trim());
            }
        }
        for (String code : codes)
        {
            TpmCourseLibrary course = tpmCourseLibraryMapper.selectTpmCourseLibraryByCourseCode(code);
            if (course == null)
            {
                return "课程编码 " + code + " 在课程库中不存在";
            }
            courses.add(course);
        }
        return null;
    }

    /**
     * P7：将课程挂接到方案（plan_id）。仅更新 plan_id 与审计字段，不覆盖课程其他属性。
     */
    private void bindImportCourses(Long planId, List<TpmCourseLibrary> courses, String operName)
    {
        for (TpmCourseLibrary course : courses)
        {
            if (planId.equals(course.getPlanId()))
            {
                continue;
            }
            TpmCourseLibrary upd = new TpmCourseLibrary();
            upd.setCourseId(course.getCourseId());
            upd.setPlanId(planId);
            upd.setUpdateBy(operName);
            upd.setUpdateTime(DateUtils.getNowDate());
            tpmCourseLibraryMapper.updateTpmCourseLibrary(upd);
        }
    }
}
