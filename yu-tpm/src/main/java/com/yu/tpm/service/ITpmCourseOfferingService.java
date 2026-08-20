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

    /**
     * 确认开课：将开课状态置为已确认(1)
     *
     * @param offeringId 开课ID
     * @return 结果
     */
    public int confirmOffering(Long offeringId);

    /**
     * 取消开课：将开课状态置为已取消(2)
     *
     * @param offeringId 开课ID
     * @return 结果
     */
    public int cancelOffering(Long offeringId);
}
