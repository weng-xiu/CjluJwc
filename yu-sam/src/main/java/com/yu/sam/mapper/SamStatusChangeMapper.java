package com.yu.sam.mapper;

import java.util.List;
import com.yu.sam.domain.SamStatusChange;

/**
 * 学籍异动Mapper接口
 * 
 * @author ruoyi
 * @date 2026-05-13
 */
public interface SamStatusChangeMapper 
{
    public SamStatusChange selectSamStatusChangeByChangeId(Long changeId);
    public List<SamStatusChange> selectSamStatusChangeList(SamStatusChange samStatusChange);

    /** P6：门户端本人异动记录查询（不走数据权限过滤） */
    public List<SamStatusChange> selectMyStatusChangeList(SamStatusChange samStatusChange);

    public int insertSamStatusChange(SamStatusChange samStatusChange);
    public int updateSamStatusChange(SamStatusChange samStatusChange);
    public int deleteSamStatusChangeByChangeId(Long changeId);
    public int deleteSamStatusChangeByChangeIds(Long[] changeIds);
}
