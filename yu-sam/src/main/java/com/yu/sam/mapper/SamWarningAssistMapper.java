package com.yu.sam.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.sam.domain.SamWarningAssist;

/**
 * 学业预警帮扶任务 Mapper（S6）
 */
public interface SamWarningAssistMapper
{
    public SamWarningAssist selectSamWarningAssistByAssistId(Long assistId);

    public List<SamWarningAssist> selectSamWarningAssistList(SamWarningAssist query);

    public int insertSamWarningAssist(SamWarningAssist assist);

    public int updateSamWarningAssist(SamWarningAssist assist);

    public int deleteSamWarningAssistByAssistId(Long assistId);

    public int deleteSamWarningAssistByAssistIds(Long[] assistIds);

    /** 幂等：查询某预警是否已有帮扶任务 */
    public SamWarningAssist selectByWarningId(@Param("warningId") Long warningId);

    /** 统计某帮扶人待处理（待认领+帮扶中）任务数 */
    public int countPendingByHelper(@Param("helperUserId") Long helperUserId);
}
