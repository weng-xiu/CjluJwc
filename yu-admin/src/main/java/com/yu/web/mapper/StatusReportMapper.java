package com.yu.web.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.yu.web.domain.StatusCourseVo;
import com.yu.web.domain.StatusGradeVo;
import com.yu.web.domain.StatusReportBatch;
import com.yu.web.domain.StatusReportField;
import com.yu.web.domain.StatusStudentVo;
import com.yu.web.domain.StatusTeacherVo;

/**
 * 教育部状态数据上报Mapper接口
 *
 * <p>四类上报数据（学生/课程/成绩/教师）的口径化查询在 SQL 中完成值域转换，
 * Java 侧只做批次留痕与导出，保证「转换规则集中、可审计」。</p>
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public interface StatusReportMapper
{
    /** 查询某类上报的字段映射定义 */
    public List<StatusReportField> selectFieldList(@Param("reportType") String reportType);

    /** 学生基本信息上报数据 */
    public List<StatusStudentVo> selectStudentReport(@Param("reportYear") String reportYear, @Param("deptId") Long deptId);

    /** 课程基本信息上报数据 */
    public List<StatusCourseVo> selectCourseReport(@Param("reportYear") String reportYear, @Param("deptId") Long deptId);

    /** 学生成绩信息上报数据 */
    public List<StatusGradeVo> selectGradeReport(@Param("reportYear") String reportYear, @Param("deptId") Long deptId);

    /** 教师基本信息上报数据 */
    public List<StatusTeacherVo> selectTeacherReport(@Param("reportYear") String reportYear, @Param("deptId") Long deptId);

    /** 上报范围院系名称（用于批次留痕） */
    public String selectDeptNameById(Long deptId);

    /** 上报范围下拉选项 */
    public List<Map<String, Object>> selectDeptOptions();

    /** 上报批次列表 */
    public List<StatusReportBatch> selectBatchList(StatusReportBatch query);

    /** 上报批次详情 */
    public StatusReportBatch selectBatchById(Long batchId);

    /** 新增批次 */
    public int insertBatch(StatusReportBatch batch);

    /** 修改批次（状态流转与导出时间回填） */
    public int updateBatch(StatusReportBatch batch);

    /** 删除批次 */
    public int deleteBatchById(Long batchId);
}
