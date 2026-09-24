package com.yu.sam.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.yu.sam.domain.SamRegistration;

/**
 * 学期注册记录Mapper接口（S8）
 *
 * @author ruoyi
 * @date 2026-09-23
 */
public interface SamRegistrationMapper
{
    public SamRegistration selectSamRegistrationByRegistrationId(Long registrationId);

    public List<SamRegistration> selectSamRegistrationList(SamRegistration samRegistration);

    public int insertSamRegistration(SamRegistration samRegistration);

    public int updateSamRegistration(SamRegistration samRegistration);

    public int deleteSamRegistrationByRegistrationIds(Long[] registrationIds);

    /** 报到初始化：为指定学期的全部在读学生批量生成"未注册"记录（幂等，已存在则跳过），返回新增条数 */
    public int initRegistrationForSemester(@Param("semesterId") Long semesterId, @Param("createBy") String createBy);

    /** 统计某学期在读且已有注册记录的学生总数（用于注册率分母） */
    public int countUnregistered(@Param("semesterId") Long semesterId);

    /** 注册情况总览：total/registered/deferred/unregistered */
    public Map<String, Object> statOverview(@Param("semesterId") Long semesterId);

    /** 按院系统计注册率 */
    public List<Map<String, Object>> statByDept(@Param("semesterId") Long semesterId);

    /** 当前学期ID（优先覆盖今天，无则最新启用学期） */
    public Long selectCurrentSemesterId();
}
