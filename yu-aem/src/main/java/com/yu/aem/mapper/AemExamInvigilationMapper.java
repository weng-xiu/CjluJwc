package com.yu.aem.mapper;

import java.util.Date;
import java.util.List;
import java.util.Map;
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

    /** 批量插入监考记录 */
    public int batchInsert(List<AemExamInvigilation> list);

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

    /**
     * 批量查询指定日期、时段内已有监考任务（冲突）的教师ID集合，
     * 用于自动派监考时一次性过滤，避免逐教师查询的N+1。
     */
    List<Long> selectBusyTeacherIds(@Param("examDate") Date examDate,
                                    @Param("startTime") String startTime,
                                    @Param("endTime") String endTime,
                                    @Param("excludeExamId") Long excludeExamId);

    /**
     * A3：查询某课程的任课教师ID集合（来自 tpm_course_offering，用于回避派发）。
     *
     * @param courseId   课程ID
     * @param semesterId 学期ID（可为null，为空时匹配该课程全部任课教师）
     */
    List<Long> selectCourseTeacherIds(@Param("courseId") Long courseId,
                                      @Param("semesterId") Long semesterId);

    /**
     * A3：查询某课程任课教师所属院系ID集合（用于院系回避，软约束）。
     */
    List<Long> selectCourseTeacherDeptIds(@Param("courseId") Long courseId,
                                          @Param("semesterId") Long semesterId);

    /**
     * A3：按学期统计各教师已有监考次数（用于次数均衡与工作量上限）。
     * 返回每项含 teacherId 与 cnt。excludeExamId 用于排除当前考试（重排前旧记录已删除，一般传 null）。
     */
    List<Map<String, Object>> selectInvigilationCountBySemester(@Param("semesterId") Long semesterId,
                                                                @Param("excludeExamId") Long excludeExamId);
}
