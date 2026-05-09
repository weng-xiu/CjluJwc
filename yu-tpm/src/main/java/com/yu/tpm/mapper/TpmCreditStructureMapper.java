package com.yu.tpm.mapper;

import java.util.List;
import com.yu.tpm.domain.TpmCreditStructure;

/**
 * 学分结构Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface TpmCreditStructureMapper 
{
    public TpmCreditStructure selectTpmCreditStructureByStructId(Long structId);
    public List<TpmCreditStructure> selectTpmCreditStructureList(TpmCreditStructure tpmCreditStructure);
    public int insertTpmCreditStructure(TpmCreditStructure tpmCreditStructure);
    public int updateTpmCreditStructure(TpmCreditStructure tpmCreditStructure);
    public int deleteTpmCreditStructureByStructId(Long structId);
    public int deleteTpmCreditStructureByStructIds(Long[] structIds);
}
