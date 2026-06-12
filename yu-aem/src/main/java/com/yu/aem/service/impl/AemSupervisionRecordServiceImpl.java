package com.yu.aem.service.impl;

import java.util.List;
import com.yu.common.annotation.DataScope;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.yu.aem.mapper.AemSupervisionRecordMapper;
import com.yu.aem.domain.AemSupervisionRecord;
import com.yu.aem.service.IAemSupervisionRecordService;

/**
 * 督导听课记录Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-11
 */
@Service
public class AemSupervisionRecordServiceImpl implements IAemSupervisionRecordService 
{
    @Autowired
    private AemSupervisionRecordMapper aemSupervisionRecordMapper;

    @Override
    public AemSupervisionRecord selectAemSupervisionRecordByRecordId(Long recordId)
    {
        return aemSupervisionRecordMapper.selectAemSupervisionRecordByRecordId(recordId);
    }

    @Override
    @DataScope(deptAlias = "d")
    public List<AemSupervisionRecord> selectAemSupervisionRecordList(AemSupervisionRecord aemSupervisionRecord)
    {
        return aemSupervisionRecordMapper.selectAemSupervisionRecordList(aemSupervisionRecord);
    }

    @Override
    @Transactional
    public int insertAemSupervisionRecord(AemSupervisionRecord aemSupervisionRecord)
    {
        aemSupervisionRecord.setCreateTime(DateUtils.getNowDate());
        return aemSupervisionRecordMapper.insertAemSupervisionRecord(aemSupervisionRecord);
    }

    @Override
    @Transactional
    public int updateAemSupervisionRecord(AemSupervisionRecord aemSupervisionRecord)
    {
        aemSupervisionRecord.setUpdateTime(DateUtils.getNowDate());
        return aemSupervisionRecordMapper.updateAemSupervisionRecord(aemSupervisionRecord);
    }

    @Override
    @Transactional
    public int deleteAemSupervisionRecordByRecordId(Long recordId)
    {
        return aemSupervisionRecordMapper.deleteAemSupervisionRecordByRecordId(recordId);
    }

    @Override
    @Transactional
    public int deleteAemSupervisionRecordByRecordIds(Long[] recordIds)
    {
        return aemSupervisionRecordMapper.deleteAemSupervisionRecordByRecordIds(recordIds);
    }
}
