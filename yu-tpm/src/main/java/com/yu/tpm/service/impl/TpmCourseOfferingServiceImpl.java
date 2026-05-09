package com.yu.tpm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.tpm.mapper.TpmCourseOfferingMapper;
import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.service.ITpmCourseOfferingService;

/**
 * 开课计划Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmCourseOfferingServiceImpl implements ITpmCourseOfferingService 
{
    @Autowired
    private TpmCourseOfferingMapper tpmCourseOfferingMapper;

    @Override
    public TpmCourseOffering selectTpmCourseOfferingByOfferingId(Long offeringId)
    {
        return tpmCourseOfferingMapper.selectTpmCourseOfferingByOfferingId(offeringId);
    }

    @Override
    public List<TpmCourseOffering> selectTpmCourseOfferingList(TpmCourseOffering tpmCourseOffering)
    {
        return tpmCourseOfferingMapper.selectTpmCourseOfferingList(tpmCourseOffering);
    }

    @Override
    public int insertTpmCourseOffering(TpmCourseOffering tpmCourseOffering)
    {
        tpmCourseOffering.setCreateTime(DateUtils.getNowDate());
        return tpmCourseOfferingMapper.insertTpmCourseOffering(tpmCourseOffering);
    }

    @Override
    public int updateTpmCourseOffering(TpmCourseOffering tpmCourseOffering)
    {
        tpmCourseOffering.setUpdateTime(DateUtils.getNowDate());
        return tpmCourseOfferingMapper.updateTpmCourseOffering(tpmCourseOffering);
    }

    @Override
    public int deleteTpmCourseOfferingByOfferingId(Long offeringId)
    {
        return tpmCourseOfferingMapper.deleteTpmCourseOfferingByOfferingId(offeringId);
    }

    @Override
    public int deleteTpmCourseOfferingByOfferingIds(Long[] offeringIds)
    {
        return tpmCourseOfferingMapper.deleteTpmCourseOfferingByOfferingIds(offeringIds);
    }
}
