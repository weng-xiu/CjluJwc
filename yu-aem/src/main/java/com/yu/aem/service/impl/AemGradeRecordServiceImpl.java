package com.yu.aem.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import com.yu.common.annotation.DataScope;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.aem.mapper.AemGradeRecordMapper;
import com.yu.aem.mapper.AemGradeReviewMapper;
import com.yu.aem.mapper.AemGpaAlgorithmConfigMapper;
import com.yu.aem.domain.AemGradeRecord;
import com.yu.aem.domain.AemGradeReview;
import com.yu.aem.domain.AemGpaAlgorithmConfig;
import com.yu.aem.service.IAemGradeRecordService;
import com.yu.aem.service.IAemGradeWeightService;
import com.yu.aem.strategy.GpaStrategyFactory;
import com.yu.aem.strategy.IGpaCalculationStrategy;

/**
 * 成绩记录Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@Service
public class AemGradeRecordServiceImpl implements IAemGradeRecordService 
{
    @Autowired
    private AemGradeRecordMapper aemGradeRecordMapper;

    @Autowired
    private AemGradeReviewMapper aemGradeReviewMapper;

    @Autowired
    private AemGpaAlgorithmConfigMapper aemGpaAlgorithmConfigMapper;

    @Autowired
    private GpaStrategyFactory gpaStrategyFactory;

    /** A4：成绩权重配置（无配置时回退默认 30/70） */
    @Autowired
    private IAemGradeWeightService aemGradeWeightService;

    @Override
    public AemGradeRecord selectAemGradeRecordByGradeId(Long gradeId)
    {
        return aemGradeRecordMapper.selectAemGradeRecordByGradeId(gradeId);
    }

    @Override
    public AemGradeRecord selectAemGradeRecordDetail(Long gradeId)
    {
        AemGradeRecord record = aemGradeRecordMapper.selectAemGradeRecordByGradeId(gradeId);
        if (record == null)
        {
            return null;
        }
        AemGradeReview reviewQuery = new AemGradeReview();
        reviewQuery.setGradeId(gradeId);
        record.setReviews(aemGradeReviewMapper.selectAemGradeReviewList(reviewQuery));
        return record;
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "ss")
    public List<AemGradeRecord> selectAemGradeRecordList(AemGradeRecord aemGradeRecord)
    {
        return aemGradeRecordMapper.selectAemGradeRecordList(aemGradeRecord);
    }

    @Override
    public List<AemGradeRecord> selectAemGradeRecordListForPortal(AemGradeRecord aemGradeRecord)
    {
        // 门户端专供：sam_student 无 user_id 列，不能走 @DataScope(userAlias="ss")，否则学生查询直接 SQL 报错
        return aemGradeRecordMapper.selectAemGradeRecordListForPortal(aemGradeRecord);
    }

    @Override
    @Transactional
    public int insertAemGradeRecord(AemGradeRecord aemGradeRecord)
    {
        // 数据范围校验：成绩分数必须在0-100范围内
        if (aemGradeRecord.getRegularScore() != null && (aemGradeRecord.getRegularScore() < 0 || aemGradeRecord.getRegularScore() > 100))
        {
            throw new ServiceException("平时成绩必须在0-100范围内");
        }
        if (aemGradeRecord.getExamScore() != null && (aemGradeRecord.getExamScore() < 0 || aemGradeRecord.getExamScore() > 100))
        {
            throw new ServiceException("考试成绩必须在0-100范围内");
        }
        if (aemGradeRecord.getTotalScore() != null && (aemGradeRecord.getTotalScore() < 0 || aemGradeRecord.getTotalScore() > 100))
        {
            throw new ServiceException("总成绩必须在0-100范围内");
        }
        aemGradeRecord.setCreateTime(DateUtils.getNowDate());
        return aemGradeRecordMapper.insertAemGradeRecord(aemGradeRecord);
    }

    @Override
    @Transactional
    public int updateAemGradeRecord(AemGradeRecord aemGradeRecord)
    {
        // 状态校验：已复核的成绩记录不允许修改
        AemGradeRecord existing = aemGradeRecordMapper.selectAemGradeRecordByGradeId(aemGradeRecord.getGradeId());
        if (existing != null && "1".equals(existing.getIsReviewed()))
        {
            throw new ServiceException("已复核的成绩记录不允许修改");
        }
        // 数据范围校验：成绩分数必须在0-100范围内
        if (aemGradeRecord.getRegularScore() != null && (aemGradeRecord.getRegularScore() < 0 || aemGradeRecord.getRegularScore() > 100))
        {
            throw new ServiceException("平时成绩必须在0-100范围内");
        }
        if (aemGradeRecord.getExamScore() != null && (aemGradeRecord.getExamScore() < 0 || aemGradeRecord.getExamScore() > 100))
        {
            throw new ServiceException("考试成绩必须在0-100范围内");
        }
        if (aemGradeRecord.getTotalScore() != null && (aemGradeRecord.getTotalScore() < 0 || aemGradeRecord.getTotalScore() > 100))
        {
            throw new ServiceException("总成绩必须在0-100范围内");
        }
        aemGradeRecord.setUpdateTime(DateUtils.getNowDate());
        return aemGradeRecordMapper.updateAemGradeRecord(aemGradeRecord);
    }

    @Override
    @Transactional
    public int deleteAemGradeRecordByGradeId(Long gradeId)
    {
        // 级联删除复核子表，保证主子表数据一致性
        aemGradeReviewMapper.deleteByGradeId(gradeId);
        return aemGradeRecordMapper.deleteAemGradeRecordByGradeId(gradeId);
    }

    @Override
    @Transactional
    public int deleteAemGradeRecordByGradeIds(Long[] gradeIds)
    {
        for (Long gradeId : gradeIds)
        {
            aemGradeReviewMapper.deleteByGradeId(gradeId);
        }
        return aemGradeRecordMapper.deleteAemGradeRecordByGradeIds(gradeIds);
    }

    @Override
    public Double calculateStudentGpa(Long studentId, Long semesterId, String algorithmCode)
    {
        IGpaCalculationStrategy strategy = getStrategy(algorithmCode);
        // 一次查询取出成绩及课程学分（JOIN tpm_course_library），避免N+1
        List<AemGradeRecord> records = aemGradeRecordMapper.selectByStudentAndSemester(studentId, semesterId);
        BigDecimal totalWeightedGpa = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        for (AemGradeRecord record : records)
        {
            if (record.getTotalScore() == null)
            {
                continue;
            }
            Double credit = record.getCredit();
            if (credit == null || credit <= 0)
            {
                continue;
            }
            double gpa = strategy.calculate(record.getTotalScore());
            BigDecimal gpaDecimal = new BigDecimal(Double.toString(gpa));
            BigDecimal creditDecimal = new BigDecimal(Double.toString(credit));
            totalWeightedGpa = totalWeightedGpa.add(gpaDecimal.multiply(creditDecimal));
            totalCredits = totalCredits.add(creditDecimal);
        }
        if (totalCredits.compareTo(BigDecimal.ZERO) == 0)
        {
            return 0.00;
        }
        return totalWeightedGpa.divide(totalCredits, 2, RoundingMode.HALF_UP).doubleValue();
    }

    @Override
    @Transactional
    public void batchRecalculateGpa(Long semesterId, String algorithmCode)
    {
        IGpaCalculationStrategy strategy = getStrategy(algorithmCode);
        List<AemGradeRecord> records = aemGradeRecordMapper.selectBySemester(semesterId);
        if (records == null || records.isEmpty())
        {
            return;
        }
        for (AemGradeRecord record : records)
        {
            if (record.getTotalScore() != null)
            {
                record.setGradePoint(strategy.calculate(record.getTotalScore()));
                record.setGradeLevel(strategy.getGradeLevel(record.getTotalScore()));
            }
        }
        aemGradeRecordMapper.updateGradePointBatch(records);
    }

    @Override
    @Transactional
    public int importGrade(List<AemGradeRecord> list, String operator, String algorithmCode)
    {
        if (list == null || list.isEmpty())
        {
            throw new ServiceException("导入数据不能为空");
        }
        IGpaCalculationStrategy strategy = getStrategy(algorithmCode);
        Date now = DateUtils.getNowDate();
        for (AemGradeRecord record : list)
        {
            // 必填校验
            if (record.getStudentId() == null || record.getCourseId() == null || record.getSemesterId() == null)
            {
                throw new ServiceException("存在学生ID/课程ID/学期ID为空的记录");
            }
            validateScore(record.getRegularScore(), "平时成绩");
            validateScore(record.getExamScore(), "考试成绩");
            // A4：总成绩为空时按配置权重计算（课程级 > 类别级 > 全局 > 默认 30/70）
            if (record.getTotalScore() == null)
            {
                double regular = record.getRegularScore() == null ? 0 : record.getRegularScore();
                double exam = record.getExamScore() == null ? 0 : record.getExamScore();
                double[] ratios = aemGradeWeightService.resolveRatios(record.getCourseId());
                double total = Math.round((regular * ratios[0] + exam * ratios[1]) * 100.0) / 100.0;
                record.setTotalScore(total);
            }
            validateScore(record.getTotalScore(), "总成绩");
            // 绩点/等级
            record.setGradePoint(strategy.calculate(record.getTotalScore()));
            record.setGradeLevel(strategy.getGradeLevel(record.getTotalScore()));
            record.setIsPass(record.getTotalScore() >= 60 ? "1" : "0");
            if (record.getExamType() == null || record.getExamType().isEmpty())
            {
                record.setExamType("0");
            }
            if (record.getIsReviewed() == null)
            {
                record.setIsReviewed("0");
            }
            if (record.getStatus() == null)
            {
                record.setStatus("0");
            }
            record.setCreateBy(operator);
            record.setCreateTime(now);
        }
        return aemGradeRecordMapper.batchInsert(list);
    }

    /**
     * 分数范围校验
     */
    private void validateScore(Double score, String fieldName)
    {
        if (score != null && (score < 0 || score > 100))
        {
            throw new ServiceException(fieldName + "必须在0-100范围内，当前值：" + score);
        }
    }

    /**
     * 获取算法策略，空值时使用默认算法
     */
    private IGpaCalculationStrategy getStrategy(String algorithmCode)
    {
        if (algorithmCode == null || algorithmCode.isEmpty())
        {
            algorithmCode = getDefaultAlgorithmCode();
        }
        IGpaCalculationStrategy strategy = gpaStrategyFactory.getStrategy(algorithmCode);
        if (strategy == null)
        {
            throw new ServiceException("GPA算法不存在：" + algorithmCode);
        }
        return strategy;
    }

    /**
     * 获取默认算法代码
     */
    private String getDefaultAlgorithmCode()
    {
        AemGpaAlgorithmConfig config = aemGpaAlgorithmConfigMapper.selectDefaultAlgorithm();
        if (config != null && config.getAlgorithmCode() != null && !config.getAlgorithmCode().isEmpty())
        {
            return config.getAlgorithmCode();
        }
        return "CN_STANDARD";
    }
}
