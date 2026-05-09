package com.yu.tpm.service;

import java.util.List;
import com.yu.tpm.domain.TpmSelectionEnrollment;

/**
 * 选课名单Service接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface ITpmSelectionEnrollmentService 
{
    public TpmSelectionEnrollment selectTpmSelectionEnrollmentByEnrollId(Long enrollId);
    public List<TpmSelectionEnrollment> selectTpmSelectionEnrollmentList(TpmSelectionEnrollment tpmSelectionEnrollment);
    public int insertTpmSelectionEnrollment(TpmSelectionEnrollment tpmSelectionEnrollment);
    public int updateTpmSelectionEnrollment(TpmSelectionEnrollment tpmSelectionEnrollment);
    public int deleteTpmSelectionEnrollmentByEnrollIds(Long[] enrollIds);
    public int deleteTpmSelectionEnrollmentByEnrollId(Long enrollId);
}
