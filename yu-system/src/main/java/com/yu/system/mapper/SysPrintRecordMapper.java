package com.yu.system.mapper;

import java.util.List;
import com.yu.system.domain.SysPrintRecord;

/**
 * 电子凭证发放记录Mapper接口
 *
 * @author yu
 * @date 2026-09-25
 */
public interface SysPrintRecordMapper
{
    /**
     * 查询电子凭证记录
     *
     * @param recordId 电子凭证记录主键
     * @return 电子凭证记录
     */
    public SysPrintRecord selectSysPrintRecordByRecordId(Long recordId);

    /**
     * 按凭证编号查询（含快照）
     *
     * @param serialNo 凭证编号
     * @return 电子凭证记录
     */
    public SysPrintRecord selectBySerialNo(String serialNo);

    /**
     * 查询电子凭证记录列表
     *
     * @param sysPrintRecord 电子凭证记录
     * @return 电子凭证记录集合
     */
    public List<SysPrintRecord> selectSysPrintRecordList(SysPrintRecord sysPrintRecord);

    /**
     * 新增电子凭证记录
     *
     * @param sysPrintRecord 电子凭证记录
     * @return 结果
     */
    public int insertSysPrintRecord(SysPrintRecord sysPrintRecord);

    /**
     * 作废电子凭证
     *
     * @param recordId 电子凭证记录主键
     * @return 结果
     */
    public int revokeByRecordId(Long recordId);
}
