package com.yu.web.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.StringUtils;
import com.yu.common.utils.poi.ExcelUtil;
import com.yu.web.domain.StatusCourseVo;
import com.yu.web.domain.StatusGradeVo;
import com.yu.web.domain.StatusReportBatch;
import com.yu.web.domain.StatusReportField;
import com.yu.web.domain.StatusStudentVo;
import com.yu.web.domain.StatusTeacherVo;
import com.yu.web.mapper.StatusReportMapper;
import com.yu.web.service.IStatusReportService;

/**
 * 教育部状态数据上报Service业务层处理
 *
 * <p>四类上报数据的字段口径与值域转换全部在 Mapper 的 SQL 中完成，Service 负责
 * 类型分发、批次留痕（生成/导出/上报）、预览脱敏与 Excel 报盘导出，保证上报过程
 * 可追溯且敏感信息不在页面明文展示。</p>
 *
 * @author ruoyi
 * @date 2026-09-26
 */
@Service
public class StatusReportServiceImpl implements IStatusReportService
{
    @Autowired
    private StatusReportMapper statusReportMapper;

    @Override
    public List<StatusReportField> selectFieldList(String reportType)
    {
        return statusReportMapper.selectFieldList(reportType);
    }

    @Override
    public List<?> selectReportData(String reportType, String reportYear, Long deptId)
    {
        String year = StringUtils.trimToEmpty(reportYear);
        Long scopeDeptId = deptId == null ? 0L : deptId;
        if (TYPE_STUDENT.equals(reportType))
        {
            return statusReportMapper.selectStudentReport(year, scopeDeptId);
        }
        if (TYPE_COURSE.equals(reportType))
        {
            return statusReportMapper.selectCourseReport(year, scopeDeptId);
        }
        if (TYPE_GRADE.equals(reportType))
        {
            return statusReportMapper.selectGradeReport(year, scopeDeptId);
        }
        if (TYPE_TEACHER.equals(reportType))
        {
            return statusReportMapper.selectTeacherReport(year, scopeDeptId);
        }
        throw new ServiceException("不支持的上报类型：" + reportType);
    }

    /** 预览口径：身份证保留前6后4，手机号保留前3后4，其余字段原样 */
    @Override
    public List<?> selectReportDataForPreview(String reportType, String reportYear, Long deptId)
    {
        List<?> rows = selectReportData(reportType, reportYear, deptId);
        if (TYPE_STUDENT.equals(reportType))
        {
            for (Object row : rows)
            {
                ((StatusStudentVo) row).setGmsfhm(mask(((StatusStudentVo) row).getGmsfhm(), 6, 4));
            }
        }
        else if (TYPE_TEACHER.equals(reportType))
        {
            for (Object row : rows)
            {
                ((StatusTeacherVo) row).setLxdh(mask(((StatusTeacherVo) row).getLxdh(), 3, 4));
            }
        }
        return rows;
    }

    @Override
    public List<Map<String, Object>> selectDeptOptions()
    {
        return statusReportMapper.selectDeptOptions();
    }

    @Override
    public String reportTypeName(String reportType)
    {
        if (TYPE_STUDENT.equals(reportType)) return "学生基本信息";
        if (TYPE_COURSE.equals(reportType)) return "课程基本信息";
        if (TYPE_GRADE.equals(reportType)) return "成绩信息";
        if (TYPE_TEACHER.equals(reportType)) return "教师基本信息";
        return "未知类型";
    }

    @Override
    public List<StatusReportBatch> selectBatchList(StatusReportBatch query)
    {
        return statusReportMapper.selectBatchList(query);
    }

    @Override
    public StatusReportBatch selectBatchById(Long batchId)
    {
        return batchId == null ? null : statusReportMapper.selectBatchById(batchId);
    }

    @Override
    public StatusReportBatch generateBatch(StatusReportBatch batch, String operator)
    {
        if (StringUtils.isEmpty(batch.getReportType()))
        {
            throw new ServiceException("请选择上报类型");
        }
        if (StringUtils.isEmpty(batch.getReportYear()))
        {
            throw new ServiceException("请填写上报年度");
        }
        Long deptId = batch.getScopeDeptId() == null ? 0L : batch.getScopeDeptId();
        batch.setScopeDeptId(deptId);
        batch.setScopeDeptName(deptId > 0 ? statusReportMapper.selectDeptNameById(deptId) : "全校");
        // 抽取一次数据用于行数留痕，导出时按同口径重查，避免批次与文件不一致
        List<?> rows = selectReportData(batch.getReportType(), batch.getReportYear(), deptId);
        batch.setRowCount(rows.size());
        batch.setReportName(String.format("%s-%s-%s", batch.getReportYear(), reportTypeName(batch.getReportType()),
                StringUtils.isEmpty(batch.getScopeDeptName()) ? "全校" : batch.getScopeDeptName()));
        batch.setBatchStatus("0");
        batch.setGenTime(DateUtils.getNowDate());
        batch.setCreateBy(operator);
        batch.setCreateTime(DateUtils.getNowDate());
        statusReportMapper.insertBatch(batch);
        return batch;
    }

    @Override
    public int markSubmitted(Long batchId, String operator)
    {
        StatusReportBatch batch = requireBatch(batchId);
        if ("3".equals(batch.getBatchStatus()))
        {
            throw new ServiceException("批次已作废，不能标记上报");
        }
        StatusReportBatch update = new StatusReportBatch();
        update.setBatchId(batch.getBatchId());
        update.setBatchStatus("2");
        update.setSubmitTime(DateUtils.getNowDate());
        update.setUpdateBy(operator);
        update.setUpdateTime(DateUtils.getNowDate());
        return statusReportMapper.updateBatch(update);
    }

    @Override
    public int cancelBatch(Long batchId, String operator)
    {
        StatusReportBatch batch = requireBatch(batchId);
        if ("2".equals(batch.getBatchStatus()))
        {
            throw new ServiceException("批次已上报，请先与上级平台确认后再处理");
        }
        StatusReportBatch update = new StatusReportBatch();
        update.setBatchId(batch.getBatchId());
        update.setBatchStatus("3");
        update.setUpdateBy(operator);
        update.setUpdateTime(DateUtils.getNowDate());
        return statusReportMapper.updateBatch(update);
    }

    @Override
    public int deleteBatchById(Long batchId)
    {
        StatusReportBatch batch = requireBatch(batchId);
        if ("2".equals(batch.getBatchStatus()))
        {
            throw new ServiceException("已上报批次需保留留痕，不允许删除");
        }
        return statusReportMapper.deleteBatchById(batch.getBatchId());
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    public void exportExcel(HttpServletResponse response, String reportType, String reportYear, Long deptId,
            Long batchId, String operator)
    {
        StatusReportBatch batch = batchId == null ? null : requireBatch(batchId);
        String type = batch != null ? batch.getReportType() : reportType;
        String year = batch != null ? batch.getReportYear() : reportYear;
        Long scopeDeptId = batch != null ? batch.getScopeDeptId() : (deptId == null ? 0L : deptId);
        if (StringUtils.isEmpty(type))
        {
            throw new ServiceException("请选择上报类型");
        }
        List<?> rows = selectReportData(type, year, scopeDeptId);
        Class<?> voClass = voClassOf(type);
        ExcelUtil util = new ExcelUtil(voClass);
        util.exportExcel(response, (List) rows, reportTypeName(type) + "报盘-" + StringUtils.trimToEmpty(year));
        if (batch != null && !"2".equals(batch.getBatchStatus()))
        {
            StatusReportBatch update = new StatusReportBatch();
            update.setBatchId(batch.getBatchId());
            update.setBatchStatus("1");
            update.setExportTime(new Date());
            update.setUpdateBy(operator);
            update.setUpdateTime(DateUtils.getNowDate());
            statusReportMapper.updateBatch(update);
        }
    }

    /** 上报类型对应的报盘对象，决定导出列与表头 */
    private Class<?> voClassOf(String reportType)
    {
        if (TYPE_STUDENT.equals(reportType)) return StatusStudentVo.class;
        if (TYPE_COURSE.equals(reportType)) return StatusCourseVo.class;
        if (TYPE_GRADE.equals(reportType)) return StatusGradeVo.class;
        if (TYPE_TEACHER.equals(reportType)) return StatusTeacherVo.class;
        throw new ServiceException("不支持的上报类型：" + reportType);
    }

    private StatusReportBatch requireBatch(Long batchId)
    {
        StatusReportBatch batch = batchId == null ? null : statusReportMapper.selectBatchById(batchId);
        if (batch == null)
        {
            throw new ServiceException("上报批次不存在");
        }
        return batch;
    }

    /** 保留头尾、中间以 **** 遮蔽（空值与过短值原样返回） */
    private String mask(String value, int head, int tail)
    {
        if (StringUtils.isEmpty(value) || value.length() <= head + tail)
        {
            return value;
        }
        return value.substring(0, head) + "****" + value.substring(value.length() - tail);
    }
}
