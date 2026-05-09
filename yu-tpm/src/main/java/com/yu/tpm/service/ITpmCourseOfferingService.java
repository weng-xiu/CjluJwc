package com.yu.tpm.service;

import java.util.List;
import com.yu.tpm.domain.TpmCourseOffering;

/**
 * 开课计划Service接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface ITpmCourseOfferingService 
{
    public TpmCourseOffering selectTpmCourseOfferingByOfferingId(Long offeringId);
    public List<TpmCourseOffering> selectTpmCourseOfferingList(TpmCourseOffering tpmCourseOffering);
    public int insertTpmCourseOffering(TpmCourseOffering tpmCourseOffering);
    public int updateTpmCourseOffering(TpmCourseOffering tpmCourseOffering);
    public int deleteTpmCourseOfferingByOfferingIds(Long[] offeringIds);
    public int deleteTpmCourseOfferingByOfferingId(Long offeringId);
}
