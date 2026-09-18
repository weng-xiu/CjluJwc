package com.yu.tpm.mapper;

import java.util.List;
import com.yu.tpm.domain.TpmCourseOffering;

/**
 * 开课计划Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-09
 */
public interface TpmCourseOfferingMapper 
{
    public TpmCourseOffering selectTpmCourseOfferingByOfferingId(Long offeringId);
    public List<TpmCourseOffering> selectTpmCourseOfferingList(TpmCourseOffering tpmCourseOffering);

    /** 门户端：可选课程列表（附带学分/已选人数，无数据范围过滤） */
    public List<TpmCourseOffering> selectTpmCourseOfferingListForPortal(TpmCourseOffering tpmCourseOffering);
    public int insertTpmCourseOffering(TpmCourseOffering tpmCourseOffering);
    public int updateTpmCourseOffering(TpmCourseOffering tpmCourseOffering);
    public int deleteTpmCourseOfferingByOfferingId(Long offeringId);
    public int deleteTpmCourseOfferingByOfferingIds(Long[] offeringIds);
}
