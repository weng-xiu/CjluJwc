package com.yu.oa.mapper;

import java.util.List;
import com.yu.oa.domain.OaTaskRecord;

/**
 * 审批任务记录Mapper接口
 * 
 * @author ruoyi
 * @date 2026-07-13
 */
public interface OaTaskRecordMapper 
{
    public OaTaskRecord selectOaTaskRecordByRecordId(Long recordId);
    public List<OaTaskRecord> selectOaTaskRecordByInstanceId(Long instanceId);
    public List<OaTaskRecord> selectOaTaskRecordList(OaTaskRecord oaTaskRecord);
    public int insertOaTaskRecord(OaTaskRecord oaTaskRecord);
    public int updateOaTaskRecord(OaTaskRecord oaTaskRecord);
    public int deleteOaTaskRecordByRecordId(Long recordId);
    public int deleteOaTaskRecordByRecordIds(Long[] recordIds);
}
