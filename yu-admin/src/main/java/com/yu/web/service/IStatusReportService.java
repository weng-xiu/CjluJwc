package com.yu.web.service;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import com.yu.web.domain.StatusReportBatch;
import com.yu.web.domain.StatusReportField;

/**
 * 教育部状态数据上报Service接口
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public interface IStatusReportService
{
    /** 上报类型：学生基本信息 */
    String TYPE_STUDENT = "01";
    /** 上报类型：课程基本信息 */
    String TYPE_COURSE = "02";
    /** 上报类型：成绩信息 */
    String TYPE_GRADE = "03";
    /** 上报类型：教师基本信息 */
    String TYPE_TEACHER = "04";

    /** 查询字段映射定义 */
    public List<StatusReportField> selectFieldList(String reportType);

    /** 按上报类型查询报盘数据（原始值，仅用于导出/生成） */
    public List<?> selectReportData(String reportType, String reportYear, Long deptId);

    /** 页面预览数据（身份证、手机号等敏感字段脱敏，不落盘） */
    public List<?> selectReportDataForPreview(String reportType, String reportYear, Long deptId);

    /** 上报范围院系下拉选项 */
    public List<Map<String, Object>> selectDeptOptions();

    /** 类型名称 */
    public String reportTypeName(String reportType);

    /** 批次列表 */
    public List<StatusReportBatch> selectBatchList(StatusReportBatch query);

    /** 批次详情 */
    public StatusReportBatch selectBatchById(Long batchId);

    /** 生成批次：按类型/年度/范围抽取数据并统计行数留痕 */
    public StatusReportBatch generateBatch(StatusReportBatch batch, String operator);

    /** 标记批次已上报 */
    public int markSubmitted(Long batchId, String operator);

    /** 作废批次 */
    public int cancelBatch(Long batchId, String operator);

    /** 删除批次留痕 */
    public int deleteBatchById(Long batchId);

    /** 导出报盘文件，并回填批次导出状态 */
    public void exportExcel(HttpServletResponse response, String reportType, String reportYear, Long deptId, Long batchId, String operator);
}
