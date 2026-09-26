package com.yu.sam.service;

import java.util.List;
import com.yu.sam.domain.SamThesisTopic;

/**
 * 毕业论文选题库Service接口
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public interface ISamThesisTopicService
{
    public SamThesisTopic selectSamThesisTopicByTopicId(Long topicId);

    public List<SamThesisTopic> selectSamThesisTopicList(SamThesisTopic samThesisTopic);

    public int insertSamThesisTopic(SamThesisTopic samThesisTopic);

    public int updateSamThesisTopic(SamThesisTopic samThesisTopic);

    public int deleteSamThesisTopicByTopicIds(Long[] topicIds);

    /**
     * 题目审核：通过后置为可选题，不通过下架并留审核意见。
     */
    public int auditTopic(Long topicId, boolean pass, String opinion, String operator);

    /**
     * 上架/下架题目（已选满的题目在名额释放后重新上架）。
     */
    public int changeStatus(Long topicId, String status, String operator);
}
