package com.yu.system.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.SecurityUtils;
import com.yu.common.utils.StringUtils;
import com.yu.system.domain.SysPrintRecord;
import com.yu.system.domain.SysPrintTemplate;
import com.yu.system.mapper.SysPrintDataMapper;
import com.yu.system.mapper.SysPrintRecordMapper;
import com.yu.system.mapper.SysPrintTemplateMapper;
import com.yu.system.service.ISysPrintService;
import com.yu.system.service.ISysConfigService;

/**
 * 打印与电子凭证Service实现（P4）
 *
 * 渲染链路：SysPrintDataMapper 装配数据模型 → 全量 HTML 转义（防模板注入）
 * → Velocity 字符串模板渲染 → 可选生成电子凭证（快照 + SHA256 + 验证码落库）。
 *
 * @author yu
 * @date 2026-09-25
 */
@Service
public class SysPrintServiceImpl implements ISysPrintService
{
    private static final Logger log = LoggerFactory.getLogger(SysPrintServiceImpl.class);

    /** 凭证业务类型 */
    public static final String BIZ_GRADE = "GRADE";
    public static final String BIZ_SCHEDULE = "SCHEDULE";
    public static final String BIZ_CERTIFICATE = "CERTIFICATE";
    public static final String BIZ_EXAM_TICKET = "EXAM_TICKET";
    public static final String BIZ_INVIGILATION = "INVIGILATION";

    /** 凭证类型中文名 */
    private static final Map<String, String> BIZ_LABELS = new HashMap<>();
    static
    {
        BIZ_LABELS.put(BIZ_GRADE, "成绩证明单");
        BIZ_LABELS.put(BIZ_SCHEDULE, "课表");
        BIZ_LABELS.put(BIZ_CERTIFICATE, "证书");
        BIZ_LABELS.put(BIZ_EXAM_TICKET, "准考证");
        BIZ_LABELS.put(BIZ_INVIGILATION, "监考通知单");
    }

    private static final String PREVIEW_KEY = "PRINT_PREVIEW";
    private static final String DEFAULT_SCHOOL = "长江大学";

    /** Velocity 引擎（字符串资源模式，无磁盘模板） */
    private static final VelocityEngine VELOCITY;
    static
    {
        VELOCITY = new VelocityEngine();
        VELOCITY.setProperty(RuntimeConstants.RESOURCE_LOADERS, "string");
        VELOCITY.setProperty("resource.loader.string.class", StringResourceLoader.class.getName());
        VELOCITY.setProperty("resource.loader.string.repository.init", StringResourceRepository.class.getName());
        VELOCITY.setProperty("resource.loader.string.repository.instance.name", "printTemplateRepo");
        VELOCITY.setProperty("resource.loader.string.repository.static", "false");
        VELOCITY.setProperty(RuntimeConstants.INPUT_ENCODING, "UTF-8");
        VELOCITY.setProperty("runtime.string.literal", "true");
        VELOCITY.init();
    }

    /** 快照序列化器：Map 按 key 排序保证哈希可复现 */
    private static final ObjectMapper SNAPSHOT_MAPPER = new ObjectMapper()
            .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);

    @Autowired
    private SysPrintTemplateMapper sysPrintTemplateMapper;

    @Autowired
    private SysPrintRecordMapper sysPrintRecordMapper;

    @Autowired
    private SysPrintDataMapper sysPrintDataMapper;

    @Autowired
    private ISysConfigService sysConfigService;

    // ================= 模板管理 =================

    @Override
    public SysPrintTemplate selectTemplateById(Long templateId)
    {
        return sysPrintTemplateMapper.selectSysPrintTemplateByTemplateId(templateId);
    }

    @Override
    public List<SysPrintTemplate> selectTemplateList(SysPrintTemplate sysPrintTemplate)
    {
        return sysPrintTemplateMapper.selectSysPrintTemplateList(sysPrintTemplate);
    }

    @Override
    public int insertTemplate(SysPrintTemplate sysPrintTemplate)
    {
        sysPrintTemplate.setCreateTime(new Date());
        return sysPrintTemplateMapper.insertSysPrintTemplate(sysPrintTemplate);
    }

    @Override
    public int updateTemplate(SysPrintTemplate sysPrintTemplate)
    {
        sysPrintTemplate.setUpdateTime(new Date());
        return sysPrintTemplateMapper.updateSysPrintTemplate(sysPrintTemplate);
    }

    @Override
    public int deleteTemplateByIds(Long[] templateIds)
    {
        return sysPrintTemplateMapper.deleteSysPrintTemplateByTemplateIds(templateIds);
    }

    @Override
    public String previewTemplate(Long templateId)
    {
        SysPrintTemplate template = sysPrintTemplateMapper.selectSysPrintTemplateByTemplateId(templateId);
        if (template == null || StringUtils.isEmpty(template.getContent()))
        {
            throw new ServiceException("模板不存在或内容为空");
        }
        return doRender(template, buildPreviewModel(template.getBizType()));
    }

    // ================= 凭证渲染与发放 =================

    @Override
    public String render(String bizType, Long bizId, Long semesterId)
    {
        return render(bizType, bizId, semesterId, false);
    }

    @Override
    public String render(String bizType, Long bizId, Long semesterId, boolean enforceOwner)
    {
        checkOwner(enforceOwner, bizType, bizId);
        SysPrintTemplate template = requireTemplate(bizType);
        Map<String, Object> model = buildModel(bizType, bizId, semesterId);
        return doRender(template, model);
    }

    @Override
    public String issue(String bizType, Long bizId, Long semesterId, String channel)
    {
        return issue(bizType, bizId, semesterId, channel, false);
    }

    @Override
    public String issue(String bizType, Long bizId, Long semesterId, String channel, boolean enforceOwner)
    {
        checkOwner(enforceOwner, bizType, bizId);
        SysPrintTemplate template = requireTemplate(bizType);
        Map<String, Object> model = buildModel(bizType, bizId, semesterId);
        SysPrintRecord record = newRecord(bizType, bizId, semesterId, template, model, channel);
        // 验证码/编号写入模型后再渲染，凭证版与预览版的差异在页脚验真信息
        Map<String, Object> ctx = escapeModel(model);
        ctx.put("serialNo", record.getSerialNo());
        ctx.put("verifyCode", record.getVerifyCode());
        ctx.put("verifyTip", "验真：" + record.getSerialNo() + " / " + record.getVerifyCode());
        String html = renderHtml(template.getContent(), ctx);
        record.setSnapshot(toJson(canonical(model)));
        record.setDataHash(sha256(record.getSnapshot()));
        sysPrintRecordMapper.insertSysPrintRecord(record);
        return html;
    }

    @Override
    public Map<String, Object> batchIssue(String bizType, Long scopeId)
    {
        if (!BIZ_GRADE.equals(bizType) && !BIZ_EXAM_TICKET.equals(bizType))
        {
            throw new ServiceException("仅成绩证明单与准考证支持批量发放");
        }
        SysPrintTemplate template = requireTemplate(bizType);
        List<Long> bizIds = BIZ_GRADE.equals(bizType)
                ? sysPrintDataMapper.selectTranscriptStudentIds(scopeId)
                : sysPrintDataMapper.selectExamSeatIds(scopeId);
        int issued = 0;
        List<String> failed = new ArrayList<>();
        for (Long bizId : bizIds)
        {
            try
            {
                issue(bizType, bizId, BIZ_GRADE.equals(bizType) ? scopeId : null, "0");
                issued++;
            }
            catch (Exception e)
            {
                log.warn("批量发放凭证失败 bizType={} bizId={}: {}", bizType, bizId, e.getMessage());
                failed.add(BIZ_GRADE.equals(bizType) ? String.valueOf(bizId) + "：" + e.getMessage() : "座位" + bizId + "：" + e.getMessage());
            }
        }
        Map<String, Object> result = new HashMap<>();
        result.put("total", bizIds.size());
        result.put("issued", issued);
        result.put("failed", failed.size());
        result.put("failDetails", failed);
        return result;
    }

    @Override
    public List<SysPrintRecord> selectRecordList(SysPrintRecord sysPrintRecord)
    {
        return sysPrintRecordMapper.selectSysPrintRecordList(sysPrintRecord);
    }

    @Override
    public SysPrintRecord selectRecordById(Long recordId)
    {
        return sysPrintRecordMapper.selectSysPrintRecordByRecordId(recordId);
    }

    @Override
    public String renderByRecord(Long recordId)
    {
        SysPrintRecord record = sysPrintRecordMapper.selectSysPrintRecordByRecordId(recordId);
        if (record == null)
        {
            throw new ServiceException("凭证不存在");
        }
        SysPrintTemplate template = null;
        if (StringUtils.isNotEmpty(record.getTemplateCode()))
        {
            SysPrintTemplate q = new SysPrintTemplate();
            q.setTemplateCode(record.getTemplateCode());
            List<SysPrintTemplate> list = sysPrintTemplateMapper.selectSysPrintTemplateList(q);
            template = list.isEmpty() ? null : list.get(0);
        }
        if (template == null)
        {
            template = requireTemplate(record.getBizType());
        }
        Map<String, Object> model = buildModel(record.getBizType(), record.getBizId(), semesterIdOf(record));
        Map<String, Object> ctx = escapeModel(model);
        ctx.put("serialNo", record.getSerialNo());
        ctx.put("verifyCode", record.getVerifyCode());
        ctx.put("verifyTip", "验真：" + record.getSerialNo() + " / " + record.getVerifyCode());
        return renderHtml(template.getContent(), ctx);
    }

    /** 从凭证标题反查发放时学期（标题格式含学期名不可靠，改从快照取） */
    private Long semesterIdOf(SysPrintRecord record)
    {
        try
        {
            if (StringUtils.isNotEmpty(record.getSnapshot()))
            {
                Map<?, ?> snap = SNAPSHOT_MAPPER.readValue(record.getSnapshot(), Map.class);
                Object sem = snap.get("semesterId");
                if (sem != null)
                {
                    return Long.valueOf(String.valueOf(sem));
                }
            }
        }
        catch (Exception e)
        {
            log.warn("解析凭证快照学期失败 recordId={}: {}", record.getRecordId(), e.getMessage());
        }
        return null;
    }

    @Override
    public int revokeRecord(Long recordId)
    {
        return sysPrintRecordMapper.revokeByRecordId(recordId);
    }

    @Override
    public Map<String, Object> verify(String serialNo, String verifyCode)
    {
        Map<String, Object> result = new HashMap<>();
        if (StringUtils.isEmpty(serialNo) || StringUtils.isEmpty(verifyCode))
        {
            throw new ServiceException("凭证编号与验证码不能为空");
        }
        SysPrintRecord record = sysPrintRecordMapper.selectBySerialNo(serialNo.trim());
        result.put("serialNo", serialNo.trim());
        if (record == null || !record.getVerifyCode().equalsIgnoreCase(verifyCode.trim()))
        {
            result.put("valid", false);
            result.put("message", "凭证编号或验证码错误，未查询到发放记录");
            return result;
        }
        result.put("bizType", record.getBizType());
        result.put("bizTypeName", BIZ_LABELS.getOrDefault(record.getBizType(), record.getBizType()));
        result.put("title", record.getTitle());
        result.put("issueTime", record.getIssueTime());
        result.put("issueBy", record.getIssueBy());
        // 脱敏展示：姓名 + 编号，不返回完整快照
        result.put("receiveName", maskName(record.getReceiveName()));
        if ("1".equals(record.getStatus()))
        {
            result.put("valid", false);
            result.put("message", "该凭证已作废");
            return result;
        }
        boolean intact = record.getDataHash() != null && record.getDataHash().equals(sha256(record.getSnapshot()));
        result.put("valid", intact);
        result.put("message", intact ? "凭证真实有效，数据未被篡改" : "凭证数据与发放时快照不一致，请联系教务处核实");
        return result;
    }

    @Override
    public Long selectStudentIdByUserId(Long userId)
    {
        return sysPrintDataMapper.selectStudentIdByUserId(userId);
    }

    @Override
    public List<Map<String, Object>> selectMySeats(Long userId)
    {
        return sysPrintDataMapper.selectStudentSeatList(userId);
    }

    @Override
    public List<Map<String, Object>> selectMyInvigilations(Long userId)
    {
        return sysPrintDataMapper.selectTeacherInvigilationList(userId);
    }

    private String maskName(String name)
    {
        if (StringUtils.isEmpty(name))
        {
            return "";
        }
        if (name.length() == 1)
        {
            return name;
        }
        if (name.length() == 2)
        {
            return name.charAt(0) + "*";
        }
        return name.charAt(0) + "*".repeat(name.length() - 2) + name.charAt(name.length() - 1);
    }

    // ================= 数据装配 =================

    private SysPrintTemplate requireTemplate(String bizType)
    {
        if (!BIZ_LABELS.containsKey(bizType))
        {
            throw new ServiceException("不支持的凭证类型：" + bizType);
        }
        SysPrintTemplate template = sysPrintTemplateMapper.selectEnabledByBizType(bizType);
        if (template == null || StringUtils.isEmpty(template.getContent()))
        {
            throw new ServiceException("未配置启用的" + BIZ_LABELS.get(bizType) + "打印模板，请先到打印模板管理页维护");
        }
        return template;
    }

    /**
     * 门户越权防护：非管理员（无凭证管理权限）时，凭证业务主键必须归属当前登录用户
     */
    private void checkOwner(boolean enforceOwner, String bizType, Long bizId)
    {
        if (!enforceOwner)
        {
            return;
        }
        Long currentUserId = SecurityUtils.getUserId();
        if (SecurityUtils.isAdmin(currentUserId))
        {
            return;
        }
        Long ownerId;
        switch (bizType)
        {
            case BIZ_GRADE:
            case BIZ_SCHEDULE:
                ownerId = bizId;
                break;
            case BIZ_CERTIFICATE:
                ownerId = sysPrintDataMapper.selectUserIdByCertId(bizId);
                break;
            case BIZ_EXAM_TICKET:
                ownerId = sysPrintDataMapper.selectUserIdBySeatId(bizId);
                break;
            case BIZ_INVIGILATION:
                ownerId = sysPrintDataMapper.selectUserIdByInvigilationId(bizId);
                break;
            default:
                ownerId = null;
        }
        if (ownerId == null || !ownerId.equals(currentUserId))
        {
            throw new ServiceException("无权访问该凭证数据", 403);
        }
    }

    /**
     * 按业务类型装配渲染数据模型（未转义原值）
     */
    private Map<String, Object> buildModel(String bizType, Long bizId, Long semesterId)
    {
        if (bizId == null)
        {
            throw new ServiceException("业务主键不能为空");
        }
        Map<String, Object> model = new HashMap<>();
        model.put("schoolName", schoolName());
        model.put("issueDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        model.put("semesterId", semesterId);
        model.put("title", BIZ_LABELS.get(bizType));
        switch (bizType)
        {
            case BIZ_GRADE:
            {
                Map<String, Object> info = sysPrintDataMapper.selectStudentInfo(bizId);
                if (info == null || info.isEmpty())
                {
                    throw new ServiceException("学生不存在：" + bizId);
                }
                List<Map<String, Object>> rows = sysPrintDataMapper.selectTranscriptRows(bizId, semesterId);
                if (rows.isEmpty())
                {
                    throw new ServiceException("该学生暂无成绩记录");
                }
                model.putAll(info);
                model.put("rows", rows);
                model.put("personName", str(info.get("studentName")));
                model.put("semesterText", semesterText(sysPrintDataMapper.selectTranscriptSemesterNames(bizId, semesterId), semesterId));
                // 汇总：修读门次/总学分/通过学分/GPA/加权平均分
                double totalCredit = 0, passedCredit = 0, gpaWeightSum = 0, creditForGpa = 0, scoreWeightSum = 0, scoreTotal = 0;
                int passedCount = 0;
                for (Map<String, Object> r : rows)
                {
                    double credit = toDouble(r.get("credit"));
                    double gradePoint = toDouble(r.get("gradePoint"));
                    double totalScore = toDouble(r.get("totalScore"));
                    totalCredit += credit;
                    if ("是".equals(str(r.get("isPassText"))))
                    {
                        passedCredit += credit;
                        passedCount++;
                    }
                    if (gradePoint > 0 && credit > 0)
                    {
                        gpaWeightSum += gradePoint * credit;
                        creditForGpa += credit;
                    }
                    scoreWeightSum += totalScore * credit;
                    scoreTotal += credit;
                }
                model.put("courseCount", rows.size());
                model.put("passedCount", passedCount);
                model.put("totalCredit", round2(totalCredit));
                model.put("passedCredit", round2(passedCredit));
                model.put("gpa", creditForGpa > 0 ? round2(gpaWeightSum / creditForGpa) : 0);
                model.put("avgScore", scoreTotal > 0 ? round2(scoreWeightSum / scoreTotal) : 0);
                model.put("title", "学生成绩证明单");
                break;
            }
            case BIZ_CERTIFICATE:
            {
                Map<String, Object> data = sysPrintDataMapper.selectCertificateData(bizId);
                if (data == null || data.isEmpty())
                {
                    throw new ServiceException("证书记录不存在：" + bizId);
                }
                model.putAll(data);
                model.put("personName", str(data.get("studentName")));
                model.put("title", str(data.get("certTypeName")));
                break;
            }
            case BIZ_EXAM_TICKET:
            {
                Map<String, Object> data = sysPrintDataMapper.selectExamTicketData(bizId);
                if (data == null || data.isEmpty())
                {
                    throw new ServiceException("考场座位记录不存在：" + bizId);
                }
                model.putAll(data);
                model.put("personName", str(data.get("studentName")));
                model.put("title", "准考证");
                model.put("semesterText", str(data.get("semesterName")));
                break;
            }
            case BIZ_SCHEDULE:
            {
                List<Map<String, Object>> rows;
                String personName;
                // 优先按教师解析（教师用户ID），否则按学生选课口径
                rows = sysPrintDataMapper.selectTeacherScheduleRows(bizId, semesterId);
                personName = teacherScheduleName(bizId, rows);
                Map<String, Object> info = sysPrintDataMapper.selectStudentInfo(bizId);
                if (info != null)
                {
                    model.putAll(info);
                }
                if (personName == null)
                {
                    rows = sysPrintDataMapper.selectScheduleRows(bizId, semesterId);
                    personName = info == null ? "" : str(info.get("studentName"));
                }
                if (rows.isEmpty())
                {
                    throw new ServiceException("该人员本学期暂无课表数据");
                }
                fillScheduleText(rows);
                model.put("rows", rows);
                model.put("personName", personName);
                model.put("semesterText", semesterText(
                        java.util.Collections.singletonList(sysPrintDataMapper.selectSemesterName(semesterId)), semesterId));
                model.put("title", "学生课表");
                model.put("courseCount", distinctCourse(rows));
                break;
            }
            case BIZ_INVIGILATION:
            {
                Map<String, Object> data = sysPrintDataMapper.selectInvigilationData(bizId);
                if (data == null || data.isEmpty())
                {
                    throw new ServiceException("监考安排不存在：" + bizId);
                }
                model.putAll(data);
                model.put("personName", str(data.get("teacherName")));
                model.put("title", "监考工作通知单");
                model.put("semesterText", str(data.get("semesterName")));
                break;
            }
            default:
                throw new ServiceException("不支持的凭证类型：" + bizType);
        }
        model.putIfAbsent("serialNo", "");
        model.putIfAbsent("verifyCode", "");
        model.putIfAbsent("verifyTip", "预览版 · 未发放");
        return model;
    }

    /** 教师课表解析：能查到课表行则返回教师姓名，否则返回 null 走学生口径 */
    private String teacherScheduleName(Long userId, List<Map<String, Object>> teacherRows)
    {
        if (teacherRows != null && !teacherRows.isEmpty())
        {
            String name = str(teacherRows.get(0).get("teacherName"));
            return StringUtils.isNotEmpty(name) ? name : "教师";
        }
        return null;
    }

    private void fillScheduleText(List<Map<String, Object>> rows)
    {
        String[] weekNames = {"", "星期一", "星期二", "星期三", "星期四", "星期五", "星期六", "星期日"};
        for (Map<String, Object> r : rows)
        {
            int wd = (int) toDouble(r.get("weekDay"));
            r.put("weekDayText", wd >= 1 && wd <= 7 ? weekNames[wd] : "");
            r.put("periodText", toDouble(r.get("startPeriod")) == toDouble(r.get("endPeriod"))
                    ? (int) toDouble(r.get("startPeriod")) + "节"
                    : (int) toDouble(r.get("startPeriod")) + "-" + (int) toDouble(r.get("endPeriod")) + "节");
            r.put("weekText", (int) toDouble(r.get("startWeek")) + "-" + (int) toDouble(r.get("endWeek")) + "周");
        }
    }

    private int distinctCourse(List<Map<String, Object>> rows)
    {
        return (int) rows.stream().map(r -> str(r.get("courseName"))).distinct().count();
    }

    private String semesterText(List<String> names, Long semesterId)
    {
        List<String> valid = new ArrayList<>();
        for (String n : names)
        {
            if (StringUtils.isNotEmpty(n) && !valid.contains(n))
            {
                valid.add(n);
            }
        }
        if (valid.isEmpty())
        {
            return semesterId == null ? "全部学期" : "";
        }
        return valid.size() == 1 ? valid.get(0) : "共" + valid.size() + "个学期";
    }

    // ================= 渲染与安全 =================

    private String schoolName()
    {
        String name = sysConfigService.selectConfigByKey("sys.school.name");
        return StringUtils.isNotEmpty(name) ? name : DEFAULT_SCHOOL;
    }

    private String doRender(SysPrintTemplate template, Map<String, Object> model)
    {
        Map<String, Object> ctx = escapeModel(model);
        ctx.putIfAbsent("serialNo", "");
        ctx.putIfAbsent("verifyCode", "");
        ctx.putIfAbsent("verifyTip", "预览版 · 未发放");
        return renderHtml(template.getContent(), ctx);
    }

    private String renderHtml(String content, Map<String, Object> ctx)
    {
        try
        {
            VelocityContext velocityContext = new VelocityContext();
            ctx.forEach(velocityContext::put);
            java.io.StringWriter writer = new java.io.StringWriter();
            // 字符串字面量模式：第一个参数即模板内容
            VELOCITY.evaluate(velocityContext, writer, "printTemplate", content);
            return writer.toString();
        }
        catch (Exception e)
        {
            log.error("打印模板渲染失败", e);
            throw new ServiceException("模板渲染失败：" + e.getMessage());
        }
    }

    /** 递归对模型字符串做 HTML 转义，防止业务数据注入模板 */
    @SuppressWarnings("unchecked")
    private Map<String, Object> escapeModel(Map<String, Object> model)
    {
        Map<String, Object> out = new HashMap<>();
        model.forEach((k, v) -> out.put(k, escapeValue(v)));
        return out;
    }

    @SuppressWarnings("unchecked")
    private Object escapeValue(Object v)
    {
        if (v == null)
        {
            return "";
        }
        if (v instanceof Map)
        {
            Map<String, Object> m = new HashMap<>();
            ((Map<String, Object>) v).forEach((k, val) -> m.put(k, escapeValue(val)));
            return m;
        }
        if (v instanceof List)
        {
            List<Object> list = new ArrayList<>();
            for (Object item : (List<Object>) v)
            {
                list.add(escapeValue(item));
            }
            return list;
        }
        return escapeHtml(String.valueOf(v));
    }

    private String escapeHtml(String s)
    {
        if (s.isEmpty())
        {
            return s;
        }
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    /** 规范化模型（null→空串、递归 TreeMap），用于快照与哈希 */
    @SuppressWarnings("unchecked")
    private Object canonical(Object v)
    {
        if (v == null)
        {
            return "";
        }
        if (v instanceof Map)
        {
            TreeMap<String, Object> tm = new TreeMap<>();
            ((Map<String, Object>) v).forEach((k, val) -> tm.put(k, canonical(val)));
            return tm;
        }
        if (v instanceof List)
        {
            List<Object> list = new ArrayList<>();
            for (Object item : (List<Object>) v)
            {
                list.add(canonical(item));
            }
            return list;
        }
        return String.valueOf(v);
    }

    private String toJson(Object obj)
    {
        try
        {
            return SNAPSHOT_MAPPER.writeValueAsString(obj);
        }
        catch (Exception e)
        {
            throw new ServiceException("凭证快照序列化失败");
        }
    }

    private String sha256(String text)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash)
            {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        }
        catch (Exception e)
        {
            throw new ServiceException("计算凭证摘要失败");
        }
    }

    // ================= 凭证记录 =================

    private SysPrintRecord newRecord(String bizType, Long bizId, Long semesterId,
                                     SysPrintTemplate template, Map<String, Object> model, String channel)
    {
        SysPrintRecord record = new SysPrintRecord();
        record.setBizType(bizType);
        record.setBizId(bizId);
        record.setTitle(str(model.get("title")));
        record.setSerialNo(generateSerialNo(bizType));
        record.setVerifyCode(generateVerifyCode());
        record.setTemplateCode(template.getTemplateCode());
        Object person = model.get("personName");
        record.setReceiveName(person == null ? null : String.valueOf(person));
        record.setReceiveId(resolveReceiveId(bizType, bizId));
        record.setIssueChannel(channel);
        record.setIssueBy(SecurityUtils.getUsername());
        record.setIssueTime(new Date());
        record.setStatus("0");
        record.setCreateBy(SecurityUtils.getUsername());
        record.setCreateTime(new Date());
        return record;
    }

    private Long resolveReceiveId(String bizType, Long bizId)
    {
        switch (bizType)
        {
            case BIZ_GRADE:
            case BIZ_SCHEDULE:
                return bizId;
            case BIZ_CERTIFICATE:
                return sysPrintDataMapper.selectUserIdByCertId(bizId);
            case BIZ_EXAM_TICKET:
                return sysPrintDataMapper.selectUserIdBySeatId(bizId);
            case BIZ_INVIGILATION:
                return sysPrintDataMapper.selectUserIdByInvigilationId(bizId);
            default:
                return null;
        }
    }

    private String generateSerialNo(String bizType)
    {
        String prefix = bizType.substring(0, 1);
        return "P" + prefix + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())
                + String.format("%04d", java.util.concurrent.ThreadLocalRandom.current().nextInt(10000));
    }

    private String generateVerifyCode()
    {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 16).toUpperCase();
    }

    // ================= 预览示例数据 =================

    private Map<String, Object> buildPreviewModel(String bizType)
    {
        Map<String, Object> model = new HashMap<>();
        model.put("schoolName", schoolName());
        model.put("issueDate", new SimpleDateFormat("yyyy-MM-dd").format(new Date()));
        model.put("title", BIZ_LABELS.getOrDefault(bizType, bizType) + "（预览）");
        model.put("personName", "张某某");
        model.put("semesterText", "2025-2026学年第一学期");
        model.put("studentNo", "20231234567");
        model.put("studentName", "张某某");
        model.put("gender", "男");
        model.put("birthDate", "2005-01-01");
        model.put("enrollmentYear", "2023级");
        model.put("educationLevel", "本科");
        model.put("majorName", "计算机科学与技术");
        model.put("className", "计算机23001");
        model.put("deptName", "计算机与科学技术学院");
        model.put("courseCount", 2);
        model.put("passedCount", 2);
        model.put("totalCredit", 6.0);
        model.put("passedCredit", 6.0);
        model.put("gpa", 3.8);
        model.put("avgScore", 88.5);
        model.put("certNumber", "10489202600001");
        model.put("certDate", "2026-06-30");
        model.put("certTypeName", "毕业证书");
        model.put("examName", "2025-2026学年第一学期期末考试");
        model.put("courseName", "数据结构");
        model.put("courseCode", "CS2001");
        model.put("examDate", "2026-01-08");
        model.put("startTime", "14:30");
        model.put("endTime", "16:30");
        model.put("duration", 120);
        model.put("classroomName", "一教101");
        model.put("seatNumber", 12);
        model.put("rowNumber", 3);
        model.put("colNumber", 6);
        model.put("teacherName", "李某某");
        model.put("teacherCode", "T0001");
        model.put("dutyTypeName", "主监考");
        model.put("partners", "王某某（副）");
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<String, Object> r1 = new HashMap<>();
        r1.put("courseCode", "CS2001");
        r1.put("courseName", "数据结构");
        r1.put("credit", 4.0);
        r1.put("totalHours", 64);
        r1.put("courseType", "必修");
        r1.put("semesterName", "2025-2026学年第一学期");
        r1.put("examTypeText", "正考");
        r1.put("regularScore", 85.0);
        r1.put("examScore", 91.0);
        r1.put("totalScore", 89.2);
        r1.put("gradePoint", 3.9);
        r1.put("gradeLevel", "A");
        r1.put("isPassText", "是");
        r1.put("teacherName", "李某某");
        r1.put("classroomName", "一教101");
        r1.put("weekDayText", "星期一");
        r1.put("periodText", "1-2节");
        r1.put("weekText", "1-16周");
        Map<String, Object> r2 = new HashMap<>();
        r2.put("courseCode", "CS2002");
        r2.put("courseName", "操作系统");
        r2.put("credit", 2.0);
        r2.put("totalHours", 32);
        r2.put("courseType", "选修");
        r2.put("semesterName", "2025-2026学年第一学期");
        r2.put("examTypeText", "正考");
        r2.put("regularScore", 78.0);
        r2.put("examScore", 82.0);
        r2.put("totalScore", 80.8);
        r2.put("gradePoint", 3.0);
        r2.put("gradeLevel", "B");
        r2.put("isPassText", "是");
        r2.put("teacherName", "王某某");
        r2.put("classroomName", "二教202");
        r2.put("weekDayText", "星期三");
        r2.put("periodText", "3-4节");
        r2.put("weekText", "1-16周");
        rows.add(r1);
        rows.add(r2);
        model.put("rows", rows);
        return model;
    }

    private String str(Object o)
    {
        return o == null ? "" : String.valueOf(o);
    }

    private double toDouble(Object o)
    {
        if (o == null)
        {
            return 0;
        }
        if (o instanceof Number)
        {
            return ((Number) o).doubleValue();
        }
        try
        {
            return Double.parseDouble(String.valueOf(o));
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }

    private double round2(double d)
    {
        return Math.round(d * 100) / 100.0;
    }
}
