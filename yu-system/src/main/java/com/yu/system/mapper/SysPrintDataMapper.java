package com.yu.system.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;

/**
 * 打印凭证数据装配Mapper接口（P4，只读跨模块查询）
 *
 * @author yu
 * @date 2026-09-25
 */
public interface SysPrintDataMapper
{
    /**
     * 查询学生基本信息（含班级/专业/院系名称）
     */
    public Map<String, Object> selectStudentInfo(@Param("studentId") Long studentId);

    /**
     * 查询成绩单明细行（按学生，可选学期）
     */
    public List<Map<String, Object>> selectTranscriptRows(@Param("studentId") Long studentId,
                                                          @Param("semesterId") Long semesterId);

    /**
     * 查询学期名称
     */
    public String selectSemesterName(@Param("semesterId") Long semesterId);

    /**
     * 查询成绩单覆盖的学期名（多学期以"、"连接）
     */
    public List<String> selectTranscriptSemesterNames(@Param("studentId") Long studentId,
                                                      @Param("semesterId") Long semesterId);

    /**
     * 查询成绩单学生范围的学生ID列表（批量发放用）
     */
    public List<Long> selectTranscriptStudentIds(@Param("semesterId") Long semesterId);

    /**
     * 查询证书打印数据（含学生与专业）
     */
    public Map<String, Object> selectCertificateData(@Param("certId") Long certId);

    /**
     * 查询准考证打印数据（按考场座位，含学生与考试安排）
     */
    public Map<String, Object> selectExamTicketData(@Param("seatId") Long seatId);

    /**
     * 查询考试座位行（批量生成准考证用）
     */
    public List<Long> selectExamSeatIds(@Param("examId") Long examId);

    /**
     * 查询学生课表行（按人培养方案口径：选课→开课→排课）
     */
    public List<Map<String, Object>> selectScheduleRows(@Param("studentId") Long studentId,
                                                        @Param("semesterId") Long semesterId);

    /**
     * 查询教师课表行
     */
    public List<Map<String, Object>> selectTeacherScheduleRows(@Param("teacherId") Long teacherId,
                                                               @Param("semesterId") Long semesterId);

    /**
     * 查询监考通知单打印数据
     */
    public Map<String, Object> selectInvigilationData(@Param("invigilationId") Long invigilationId);

    /**
     * 学生ID（=sys_user.user_id）对应的用户ID，校验存在性
     */
    public Long selectUserIdByStudentId(@Param("studentId") Long studentId);

    /**
     * 证书归属学生对应的用户ID
     */
    public Long selectUserIdByCertId(@Param("certId") Long certId);

    /**
     * 用户ID → 学籍档案存在性确认（项目约定 student_id == user_id）
     */
    public Long selectStudentIdByUserId(@Param("userId") Long userId);

    /**
     * 本人考场座位列表（门户准考证选择源）
     */
    public List<Map<String, Object>> selectStudentSeatList(@Param("userId") Long userId);

    /**
     * 本人监考安排列表（门户监考通知单选择源）
     */
    public List<Map<String, Object>> selectTeacherInvigilationList(@Param("userId") Long userId);

    /**
     * 考场座位归属学生对应的用户ID
     */
    public Long selectUserIdBySeatId(@Param("seatId") Long seatId);

    /**
     * 监考安排归属教师对应的用户ID
     */
    public Long selectUserIdByInvigilationId(@Param("invigilationId") Long invigilationId);
}
