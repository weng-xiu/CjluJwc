package com.yu.aem.service.impl;

import java.util.List;
import com.yu.common.annotation.DataScope;
import com.yu.common.exception.ServiceException;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.aem.mapper.AemGradeRecordMapper;
import com.yu.aem.domain.AemGradeRecord;
import com.yu.aem.service.IAemGradeRecordService;

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

    @Override
    public AemGradeRecord selectAemGradeRecordByGradeId(Long gradeId)
    {
        return aemGradeRecordMapper.selectAemGradeRecordByGradeId(gradeId);
    }

    @Override
    @DataScope(deptAlias = "d", userAlias = "ss")
    public List<AemGradeRecord> selectAemGradeRecordList(AemGradeRecord aemGradeRecord)
    {
        return aemGradeRecordMapper.selectAemGradeRecordList(aemGradeRecord);
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
        return aemGradeRecordMapper.deleteAemGradeRecordByGradeId(gradeId);
    }

    @Override
    @Transactional
    public int deleteAemGradeRecordByGradeIds(Long[] gradeIds)
    {
        return aemGradeRecordMapper.deleteAemGradeRecordByGradeIds(gradeIds);
    }
}
