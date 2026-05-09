package com.yu.tpm.service.impl;

import java.util.List;
import com.yu.common.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.yu.tpm.mapper.TpmCreditStructureMapper;
import com.yu.tpm.domain.TpmCreditStructure;
import com.yu.tpm.service.ITpmCreditStructureService;

/**
 * 学分结构Service业务层处理
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
@Service
public class TpmCreditStructureServiceImpl implements ITpmCreditStructureService 
{
    @Autowired
    private TpmCreditStructureMapper tpmCreditStructureMapper;

    @Override
    public TpmCreditStructure selectTpmCreditStructureByStructId(Long structId)
    {
        return tpmCreditStructureMapper.selectTpmCreditStructureByStructId(structId);
    }

    @Override
    public List<TpmCreditStructure> selectTpmCreditStructureList(TpmCreditStructure tpmCreditStructure)
    {
        return tpmCreditStructureMapper.selectTpmCreditStructureList(tpmCreditStructure);
    }

    @Override
    public int insertTpmCreditStructure(TpmCreditStructure tpmCreditStructure)
    {
        tpmCreditStructure.setCreateTime(DateUtils.getNowDate());
        return tpmCreditStructureMapper.insertTpmCreditStructure(tpmCreditStructure);
    }

    @Override
    public int updateTpmCreditStructure(TpmCreditStructure tpmCreditStructure)
    {
        tpmCreditStructure.setUpdateTime(DateUtils.getNowDate());
        return tpmCreditStructureMapper.updateTpmCreditStructure(tpmCreditStructure);
    }

    @Override
    public int deleteTpmCreditStructureByStructId(Long structId)
    {
        return tpmCreditStructureMapper.deleteTpmCreditStructureByStructId(structId);
    }

    @Override
    public int deleteTpmCreditStructureByStructIds(Long[] structIds)
    {
        return tpmCreditStructureMapper.deleteTpmCreditStructureByStructIds(structIds);
    }
}
