package com.yu.tpm.service;

import java.util.List;
import com.yu.tpm.domain.TpmCreditStructure;

/**
 * 学分结构Service接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface ITpmCreditStructureService 
{
    public TpmCreditStructure selectTpmCreditStructureByStructId(Long structId);
    public List<TpmCreditStructure> selectTpmCreditStructureList(TpmCreditStructure tpmCreditStructure);
    public int insertTpmCreditStructure(TpmCreditStructure tpmCreditStructure);
    public int updateTpmCreditStructure(TpmCreditStructure tpmCreditStructure);
    public int deleteTpmCreditStructureByStructIds(Long[] structIds);
    public int deleteTpmCreditStructureByStructId(Long structId);
}
