package com.yu.aem.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
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
    public List<AemGradeRecord> selectAemGradeRecordList(AemGradeRecord aemGradeRecord)
    {
        return aemGradeRecordMapper.selectAemGradeRecordList(aemGradeRecord);
    }

    @Override
    public int insertAemGradeRecord(AemGradeRecord aemGradeRecord)
    {
        aemGradeRecord.setCreateTime(DateUtils.getNowDate());
        return aemGradeRecordMapper.insertAemGradeRecord(aemGradeRecord);
    }

    @Override
    public int updateAemGradeRecord(AemGradeRecord aemGradeRecord)
    {
        aemGradeRecord.setUpdateTime(DateUtils.getNowDate());
        return aemGradeRecordMapper.updateAemGradeRecord(aemGradeRecord);
    }

    @Override
    public int deleteAemGradeRecordByGradeId(Long gradeId)
    {
        return aemGradeRecordMapper.deleteAemGradeRecordByGradeId(gradeId);
    }

    @Override
    public int deleteAemGradeRecordByGradeIds(Long[] gradeIds)
    {
        return aemGradeRecordMapper.deleteAemGradeRecordByGradeIds(gradeIds);
    }
}
