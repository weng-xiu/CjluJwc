package com.yu.sam.service;

import java.util.List;
import java.util.Map;

import com.yu.sam.domain.dto.ProgramQualityExportVo;

/**
 * F3 专业质量档案一页纸 Service 接口（跨模块只读聚合）。
 *
 * @author F3
 */
public interface ISamProgramQualityService
{
    /** 可选专业清单（前端下拉）。 */
    List<Map<String, Object>> listMajors();

    /** 生成指定专业的质量档案一页纸。 */
    Map<String, Object> buildProfile(Long majorId);

    /** 专业质量档案导出扁平行。 */
    List<ProgramQualityExportVo> buildProfileExport(Long majorId);
}
