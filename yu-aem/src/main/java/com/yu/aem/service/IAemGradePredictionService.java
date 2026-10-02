package com.yu.aem.service;

import java.util.List;
import java.util.Map;

/**
 * 成绩与学业预测Service接口（F2-3 智能算法深化）。
 *
 * <p>基于历史成绩的线性趋势拟合预测学生下一学期表现并分级学业风险，
 * 同时构建课程难度画像，二者可用于反哺选课推荐与学业干预。</p>
 *
 * @author ruoyi
 */
public interface IAemGradePredictionService
{
    /**
     * 学生学业风险预测：逐学期绩点序列 + 最小二乘趋势斜率 + 预测下一学期表现，输出可解释的风险分级与建议。
     *
     * @param studentId 学生ID
     * @return 含 base（学生信息）、series（逐学期）、trendSlope、predictedNextScore、
     *         predictedNextGpa、riskLevel、riskLabel、factors（判据明细）、suggestions（建议）
     */
    Map<String, Object> predictStudentRisk(Long studentId);

    /**
     * 课程难度画像：按课程聚合平均分/通过率/优秀率/不及格人次，计算可解释的难度指数并分级。
     *
     * @param semesterId 学期ID（可选，不传为全量口径）
     * @return 课程难度列表（含 difficultyIndex 0-100、difficultyLevel 高/中/低）
     */
    List<Map<String, Object>> courseDifficultyProfile(Long semesterId);

    /**
     * 班级/学期学业风险看板：批量按学期成绩做风险分级与分布统计。
     *
     * @param semesterId 学期ID
     * @param classId    班级ID（可选）
     * @return 含 list（学生风险明细）、distribution（各风险等级人数）、total
     */
    Map<String, Object> riskBoard(Long semesterId, Long classId);
}
