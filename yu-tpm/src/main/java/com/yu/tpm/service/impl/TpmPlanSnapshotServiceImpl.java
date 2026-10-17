++++package com.yu.tpm.service.impl;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.SecurityUtils;
import com.yu.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.tpm.domain.TpmCourseLibrary;
import com.yu.tpm.domain.TpmCreditStructure;
import com.yu.tpm.domain.TpmPlanSnapshot;
import com.yu.tpm.domain.TpmTrainingPlan;
import com.yu.tpm.mapper.TpmCourseLibraryMapper;
import com.yu.tpm.mapper.TpmCreditStructureMapper;
import com.yu.tpm.mapper.TpmPlanSnapshotMapper;
import com.yu.tpm.service.ITpmPlanSnapshotService;
import com.yu.tpm.snapshot.TrainingPlanSnapshotService;

/**
 * 人才培养方案版本快照Service实现（F2）。
 *
 * <p>发布时点定格：以 {@link TrainingPlanSnapshotService} 的 SHA-256 指纹锁定实质内容，
 * 并把当时的学分结构分项、课程清单摘要序列化为 JSON 一并落库，形成不可变历史版本，
 * 供毕业审核按版比对。快照只增不改，与实时 tpm_credit_structure/tpm_course_library 解耦。
 *
 * @author F2
 * @date 2026-10-17
 */
@Service
public class TpmPlanSnapshotServiceImpl implements ITpmPlanSnapshotService
{
    /** JSON 序列化（静态实例，无需 Spring Bean） */
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Autowired
    private TpmPlanSnapshotMapper tpmPlanSnapshotMapper;

    @Autowired
    private TpmCreditStructureMapper tpmCreditStructureMapper;

    @Autowired
    private TpmCourseLibraryMapper tpmCourseLibraryMapper;

    @Autowired
    private TrainingPlanSnapshotService trainingPlanSnapshotService;

    @Override
    public TpmPlanSnapshot createSnapshotOnPublish(TpmTrainingPlan plan)
    {
        if (plan == null || plan.getPlanId() == null)
        {
            return null;
        }
        Long planId = plan.getPlanId();

        // 1. 学分结构定格分项（仅保留影响毕业审核的实质字段）
        TpmCreditStructure structQuery = new TpmCreditStructure();
        structQuery.setPlanId(planId);
        List<TpmCreditStructure> structures = tpmCreditStructureMapper.selectTpmCreditStructureList(structQuery);
        List<Map<String, Object>> creditItems = new ArrayList<>();
        if (structures != null)
        {
            for (TpmCreditStructure s : structures)
            {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("creditType", s.getCreditType());
                item.put("creditTypeName", s.getCreditTypeName());
                item.put("requiredCredit", s.getRequiredCredit());
                item.put("minCredit", s.getMinCredit());
                creditItems.add(item);
            }
        }

        // 2. 课程清单定格摘要
        TpmCourseLibrary courseQuery = new TpmCourseLibrary();
        courseQuery.setPlanId(planId);
        List<TpmCourseLibrary> courses = tpmCourseLibraryMapper.selectTpmCourseLibraryList(courseQuery);
        List<Map<String, Object>> courseItems = new ArrayList<>();
        if (courses != null)
        {
            for (TpmCourseLibrary c : courses)
            {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("courseCode", c.getCourseCode());
                item.put("courseName", c.getCourseName());
                item.put("credit", c.getCredit());
                item.put("courseType", c.getCourseType());
                item.put("semesterOrder", c.getSemesterOrder());
                courseItems.add(item);
            }
        }

        // 3. 组装快照实体（指纹复用 N2 计算逻辑；版本号缺省补齐 V1，与发布口径一致）
        String effectiveVersion = StringUtils.isBlank(plan.getVersion()) ? "V1" : plan.getVersion();
        TpmPlanSnapshot snapshot = new TpmPlanSnapshot();
        snapshot.setPlanId(planId);
        snapshot.setVersion(effectiveVersion);
        snapshot.setContentHash(trainingPlanSnapshotService.fingerprint(plan));
        snapshot.setTotalCredit(plan.getTotalCredits());
        snapshot.setCreditStructureJson(writeJson(creditItems));
        snapshot.setCourseListJson(writeJson(courseItems));
        snapshot.setPublishDate(plan.getPublishDate() != null ? plan.getPublishDate() : DateUtils.getNowDate());
        snapshot.setSnapshotTime(DateUtils.getNowDate());
        String operator;
        try { operator = SecurityUtils.getUsername(); } catch (Exception e) { operator = "system"; }
        snapshot.setCreateBy(operator);
        snapshot.setCreateTime(DateUtils.getNowDate());

        tpmPlanSnapshotMapper.insertTpmPlanSnapshot(snapshot);
        return snapshot;
    }

    @Override
    public List<TpmPlanSnapshot> selectSnapshotsByPlanId(Long planId)
    {
        return tpmPlanSnapshotMapper.selectSnapshotsByPlanId(planId);
    }

    @Override
    public TpmPlanSnapshot selectSnapshotBySnapshotId(Long snapshotId)
    {
        return tpmPlanSnapshotMapper.selectTpmPlanSnapshotBySnapshotId(snapshotId);
    }

    private String writeJson(Object obj)
    {
        try
        {
            return OBJECT_MAPPER.writeValueAsString(obj);
        }
        catch (Exception e)
        {
            throw new IllegalStateException("方案版本快照 JSON 序列化失败", e);
        }
    }
}
