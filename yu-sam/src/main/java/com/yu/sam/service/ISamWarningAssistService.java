package com.yu.sam.service;

import java.util.List;
import com.yu.sam.domain.SamWarning;
import com.yu.sam.domain.SamWarningAssist;
import com.yu.sam.domain.SamWarningAssistRecord;

/**
 * 学业预警帮扶任务服务（S6 帮扶闭环）
 */
public interface ISamWarningAssistService
{
    public SamWarningAssist selectSamWarningAssistByAssistId(Long assistId);

    public List<SamWarningAssist> selectSamWarningAssistList(SamWarningAssist query);

    /** 手动派发帮扶任务（写入并向帮扶人推送通知+待办） */
    public int dispatch(SamWarningAssist assist);

    /** 预警自动生成时按级别幂等派发（引擎调用，已存在则跳过） */
    public SamWarningAssist autoDispatch(SamWarning warning, String studentName, String studentNo);

    /** 认领：待认领(0)→帮扶中(1) */
    public int claim(Long assistId, Long helperUserId, String helperName);

    /** 跟踪：登记一条记录并推进状态 */
    public int follow(Long assistId, SamWarningAssistRecord record);

    /** 完结：→已完成(2)，可选同步解除关联预警 */
    public int finish(Long assistId, String finishRemark, boolean resolveWarning);

    /** 关闭：→已关闭(3)（预警已失效等场景） */
    public int closeAssist(Long assistId, String finishRemark);

    public int deleteSamWarningAssistByAssistIds(Long[] assistIds);
}
