package com.yu.portal.service;

import java.util.Map;

/**
 * 门户 AI 分析服务（Phase34 AI应用试点）
 *
 * 选课推荐与学业画像全部基于业务表实数计算，不引入任何模拟数据；
 * 当某项信号缺少数据时按「不参与评分」处理并在结果中如实说明，避免用 0 分误伤或凭空加分。
 *
 * @author yu
 * @date 2026-09-26
 */
public interface IPortalAiService
{
    /**
     * 个性化选课推荐
     *
     * @param studentId 学生ID（门户口径等于 user_id）
     * @return 推荐结果（含轮次、已选、推荐项与未推荐原因）
     */
    public Map<String, Object> recommendCourses(Long studentId);

    /**
     * 学生学业画像
     *
     * @param studentId 学生ID（门户口径等于 user_id）
     * @return 画像结果（六维评分、模块达成、成绩明细、预警与建议）
     */
    public Map<String, Object> buildPortrait(Long studentId);
}
