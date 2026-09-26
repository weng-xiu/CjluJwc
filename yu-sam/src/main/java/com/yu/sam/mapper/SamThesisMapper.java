package com.yu.sam.mapper;

import java.util.List;
import java.util.Map;
import org.apache.ibatis.annotations.Param;
import com.yu.sam.domain.SamThesis;

/**
 * 毕业论文（设计）Mapper接口
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public interface SamThesisMapper
{
    public SamThesis selectSamThesisByThesisId(Long thesisId);

    public List<SamThesis> selectSamThesisList(SamThesis samThesis);

    public int insertSamThesis(SamThesis samThesis);

    public int updateSamThesis(SamThesis samThesis);

    public int deleteSamThesisByThesisId(Long thesisId);

    public int deleteSamThesisByThesisIds(Long[] thesisIds);

    /** 按学生与届别查询论文档案（用于重复建档校验） */
    public SamThesis selectByStudentAndYear(@Param("studentId") Long studentId, @Param("planYear") String planYear);

    /** 查询学生最近一条论文档案（学位审核取用，不限届别） */
    public SamThesis selectLatestByStudentId(@Param("studentId") Long studentId);

    /** 统计某题目被选题人数（题目删除前校验） */
    public int countByTopicId(@Param("topicId") Long topicId);

    /** 按环节分组统计（过程进度看板） */
    public List<Map<String, Object>> statByStage(SamThesis samThesis);

    /** 汇总统计（总数、合格数、归档数、平均成绩、抽检异常数） */
    public Map<String, Object> statSummary(SamThesis samThesis);
}
