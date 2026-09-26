package com.yu.sam.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.sam.domain.SamThesisProcess;

/**
 * 毕业论文环节留痕Mapper接口
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public interface SamThesisProcessMapper
{
    public SamThesisProcess selectSamThesisProcessByProcessId(Long processId);

    public List<SamThesisProcess> selectSamThesisProcessList(SamThesisProcess samThesisProcess);

    /** 按论文ID查询环节留痕（时间正序，详情页过程轴） */
    public List<SamThesisProcess> selectByThesisId(@Param("thesisId") Long thesisId);

    public int insertSamThesisProcess(SamThesisProcess samThesisProcess);

    public int updateSamThesisProcess(SamThesisProcess samThesisProcess);

    public int deleteByThesisIds(@Param("thesisIds") Long[] thesisIds);
}
