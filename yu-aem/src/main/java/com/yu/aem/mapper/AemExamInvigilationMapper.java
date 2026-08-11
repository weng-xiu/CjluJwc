package com.yu.aem.mapper;

import java.util.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.aem.domain.AemExamInvigilation;

/**
 * 监考教师分配Mapper接口
 *
 * @author ruoyi
 * @date 2026-05-11
 */
public interface AemExamInvigilationMapper
{
    public AemExamInvigilation selectAemExamInvigilationByInvigilationId(Long invigilationId);
    public List<AemExamInvigilation> selectAemExamInvigilationList(AemExamInvigilation aemExamInvigilation);
    public int insertAemExamInvigilation(AemExamInvigilation aemExamInvigilation);
    public int updateAemExamInvigilation(AemExamInvigilation aemExamInvigilation);
    public int deleteAemExamInvigilationByInvigilationId(Long invigilationId);
    public int deleteAemExamInvigilationByInvigilationIds(Long[] invigilationIds);

    /** 根据考试ID删除监考记录 */
    public int deleteByExamId(Long examId);

    /**
     * 统计某教师在指定日期、时段内已有的监考任务数（用于跨考试时间冲突检测）。
     *
     * @param teacherId     教师ID
     * @param examDate      考试日期
     * @param startTime     开始时间（HH:mm）
     * @param endTime       结束时间（HH:mm）
     * @param excludeExamId 排除的考试ID（重排当前考试时传入）
     * @return 冲突任务数
     */
    int countTeacherTimeConflict(@Param("teacherId") Long teacherId,
                                 @Param("examDate") Date examDate,
                                 @Param("startTime") String startTime,
                                 @Param("endTime") String endTime,
                                 @Param("excludeExamId") Long excludeExamId);
}
