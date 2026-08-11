package com.yu.sam.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.sam.domain.SamWarning;

/**
 * 学籍预警Mapper接口
 *
 * @author ruoyi
 * @date 2026-05-13
 */
public interface SamWarningMapper
{
    public SamWarning selectSamWarningByWarningId(Long warningId);
    public List<SamWarning> selectSamWarningList(SamWarning samWarning);
    public int insertSamWarning(SamWarning samWarning);
    public int updateSamWarning(SamWarning samWarning);
    public int deleteSamWarningByWarningId(Long warningId);
    public int deleteSamWarningByWarningIds(Long[] warningIds);

    /**
     * 删除某学生某学期"未解除"的预警（用于批量重新生成前去重）。
     *
     * @param studentId  学生ID
     * @param semesterId 学期ID（可为null，表示全部学期）
     * @return 删除条数
     */
    public int deleteActiveWarningsByStudentAndSemester(@Param("studentId") Long studentId,
                                                       @Param("semesterId") Long semesterId);
}
