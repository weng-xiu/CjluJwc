package com.yu.sam.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Param;

/**
 * F3 专业质量档案一页纸只读聚合Mapper（跨模块只读，JOIN brm/tpm/aem/sam）。
 * 仅做查询聚合，不写库。
 *
 * @author F3
 */
public interface SamProgramQualityMapper
{
    /** 专业基础信息（含所属院系名称）。 */
    Map<String, Object> selectMajorBase(@Param("majorId") Long majorId);

    /** 可选专业清单（前端下拉，仅正常状态）。 */
    List<Map<String, Object>> selectMajorOptions();

    /** 该专业全部已发布培养方案（按方案年份倒序）。 */
    List<Map<String, Object>> selectPublishedPlans(@Param("majorId") Long majorId);

    /** 在校生规模统计：总数/在读/毕业。 */
    Map<String, Object> selectStudentStats(@Param("majorId") Long majorId);

    /** 师资汇总：总数（专业所属院系在岗教师）。 */
    Map<String, Object> selectFacultySummary(@Param("deptId") Long deptId);

    /** 师资职称分布。 */
    List<Map<String, Object>> selectTitleDist(@Param("deptId") Long deptId);

    /** 师资学历分布。 */
    List<Map<String, Object>> selectEducationDist(@Param("deptId") Long deptId);

    /** 方案课程统计：门数/总学分/实践环节门数与学分。 */
    Map<String, Object> selectCourseStats(@Param("planId") Long planId);

    /** 方案学分结构分项。 */
    List<Map<String, Object>> selectCreditSections(@Param("planId") Long planId);

    /** 开课统计：方案课程累计开课批次数与教学班数。 */
    Map<String, Object> selectOfferingStats(@Param("planId") Long planId);

    /** 评教汇总：份数/平均分/优秀数(≥90)。按专业所属院系教师口径。 */
    Map<String, Object> selectEvalStats(@Param("deptId") Long deptId);

    /** 督导汇总：记录数/平均分/不合格数。按专业所属院系教师口径。 */
    Map<String, Object> selectSupervisionStats(@Param("deptId") Long deptId);

    /** 督导评价等级分布（0优秀 1良好 2合格 3不合格）。 */
    List<Map<String, Object>> selectSupervisionLevelDist(@Param("deptId") Long deptId);
}
