package com.yu.sam.service;

import java.util.List;
import java.util.Map;
import com.yu.sam.domain.SamRegistration;

/**
 * 学期注册记录Service接口（S8 学期注册与报到管理）
 *
 * @author ruoyi
 * @date 2026-09-23
 */
public interface ISamRegistrationService
{
    public SamRegistration selectSamRegistrationByRegistrationId(Long registrationId);

    public List<SamRegistration> selectSamRegistrationList(SamRegistration samRegistration);

    public int insertSamRegistration(SamRegistration samRegistration);

    public int updateSamRegistration(SamRegistration samRegistration);

    public int deleteSamRegistrationByRegistrationIds(Long[] registrationIds);

    /** 报到初始化：为指定学期全部在读学生批量生成未注册记录（幂等），返回新增条数 */
    public int initRegistration(Long semesterId, String operator);

    /**
     * 批量注册办理：将选中记录置为目标状态（1已注册 / 2延迟注册 / 0撤销为未注册）
     *
     * @param registrationIds 记录ID数组
     * @param status          目标注册状态
     * @param deferReason     延迟/未注册原因（status=2 或 0 时可填）
     * @param operator        经办人
     * @return 更新条数
     */
    public int batchRegister(Long[] registrationIds, String status, String deferReason, String operator);

    /** 注册情况总览（total/registered/deferred/unregistered + registerRate） */
    public Map<String, Object> statOverview(Long semesterId);

    /** 按院系注册率统计 */
    public List<Map<String, Object>> statByDept(Long semesterId);

    /** 当前学期ID */
    public Long getCurrentSemesterId();
}
