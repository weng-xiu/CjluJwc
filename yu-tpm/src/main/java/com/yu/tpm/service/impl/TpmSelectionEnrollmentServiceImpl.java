package com.yu.tpm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.tpm.mapper.TpmSelectionEnrollmentMapper;
import com.yu.tpm.domain.TpmSelectionEnrollment;
import com.yu.tpm.service.ITpmSelectionEnrollmentService;

/**
 * 选课名单Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmSelectionEnrollmentServiceImpl implements ITpmSelectionEnrollmentService 
{
    @Autowired
    private TpmSelectionEnrollmentMapper tpmSelectionEnrollmentMapper;

    @Override
    public TpmSelectionEnrollment selectTpmSelectionEnrollmentByEnrollId(Long enrollId)
    {
        return tpmSelectionEnrollmentMapper.selectTpmSelectionEnrollmentByEnrollId(enrollId);
    }

    @Override
    public List<TpmSelectionEnrollment> selectTpmSelectionEnrollmentList(TpmSelectionEnrollment tpmSelectionEnrollment)
    {
        return tpmSelectionEnrollmentMapper.selectTpmSelectionEnrollmentList(tpmSelectionEnrollment);
    }

    @Override
    public int insertTpmSelectionEnrollment(TpmSelectionEnrollment tpmSelectionEnrollment)
    {
        tpmSelectionEnrollment.setCreateTime(DateUtils.getNowDate());
        return tpmSelectionEnrollmentMapper.insertTpmSelectionEnrollment(tpmSelectionEnrollment);
    }

    @Override
    public int updateTpmSelectionEnrollment(TpmSelectionEnrollment tpmSelectionEnrollment)
    {
        tpmSelectionEnrollment.setUpdateTime(DateUtils.getNowDate());
        return tpmSelectionEnrollmentMapper.updateTpmSelectionEnrollment(tpmSelectionEnrollment);
    }

    @Override
    public int deleteTpmSelectionEnrollmentByEnrollId(Long enrollId)
    {
        return tpmSelectionEnrollmentMapper.deleteTpmSelectionEnrollmentByEnrollId(enrollId);
    }

    @Override
    public int deleteTpmSelectionEnrollmentByEnrollIds(Long[] enrollIds)
    {
        return tpmSelectionEnrollmentMapper.deleteTpmSelectionEnrollmentByEnrollIds(enrollIds);
    }
}
