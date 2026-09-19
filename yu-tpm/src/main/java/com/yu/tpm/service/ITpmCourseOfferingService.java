package com.yu.tpm.service;

import java.util.List;
import java.util.Map;
import com.yu.tpm.domain.TpmCourseOffering;
import com.yu.tpm.domain.dto.BatchOfferingRequest;

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

    /** 门户端：可选课程列表（不套用部门数据范围，附带学分/已选人数） */
    public List<TpmCourseOffering> selectTpmCourseOfferingListForPortal(TpmCourseOffering tpmCourseOffering);
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

    /**
     * T4 按培养方案批量生成开课计划（含容量与教师预分配）。
     * 生成结果为待确认状态，可编辑后再确认；对已存在开课的课程默认跳过以保幂等。
     *
     * @param request 生成参数
     * @return 生成结果摘要
     */
    public Map<String, Object> batchGenerateOfferings(BatchOfferingRequest request);
}
