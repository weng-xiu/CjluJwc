package com.yu.tpm.mapper;

import java.util.List;
import com.yu.tpm.domain.TpmSelectionEnrollment;

/**
 * 选课名单Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface TpmSelectionEnrollmentMapper 
{
    public TpmSelectionEnrollment selectTpmSelectionEnrollmentByEnrollId(Long enrollId);
    public List<TpmSelectionEnrollment> selectTpmSelectionEnrollmentList(TpmSelectionEnrollment tpmSelectionEnrollment);
    public int insertTpmSelectionEnrollment(TpmSelectionEnrollment tpmSelectionEnrollment);
    public int updateTpmSelectionEnrollment(TpmSelectionEnrollment tpmSelectionEnrollment);
    public int deleteTpmSelectionEnrollmentByEnrollId(Long enrollId);
    public int deleteTpmSelectionEnrollmentByEnrollIds(Long[] enrollIds);
}
