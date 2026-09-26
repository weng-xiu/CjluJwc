package com.yu.sam.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.yu.sam.domain.SamThesisTopic;

/**
 * 毕业论文选题库Mapper接口
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public interface SamThesisTopicMapper
{
    public SamThesisTopic selectSamThesisTopicByTopicId(Long topicId);

    public List<SamThesisTopic> selectSamThesisTopicList(SamThesisTopic samThesisTopic);

    public int insertSamThesisTopic(SamThesisTopic samThesisTopic);

    public int updateSamThesisTopic(SamThesisTopic samThesisTopic);

    public int deleteSamThesisTopicByTopicId(Long topicId);

    public int deleteSamThesisTopicByTopicIds(Long[] topicIds);

    /**
     * 占用一个选题名额：仅在 elected_count &lt; capacity 时自增，返回 0 表示已被选满。
     */
    public int occupyTopic(@Param("topicId") Long topicId);

    /**
     * 释放一个选题名额（论文档案删除时回退），不低于 0。
     */
    public int releaseTopic(@Param("topicId") Long topicId);
}
