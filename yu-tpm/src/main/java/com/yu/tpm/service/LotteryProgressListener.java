package com.yu.tpm.service;

/**
 * 抽签进度回调（T-5/P-3）。
 *
 * <p>抽签在单事务内分块落库（每块 {@code LOTTERY_UPDATE_CHUNK} 行），每落完一块回调一次，
 * 由调用方把进度写进任务表。回调刻意做成接口而非返回结果：进度必须在**执行过程中**可见，
 * 攒到方法返回再给就失去了“进度查询”的意义。</p>
 *
 * <p>实现方注意：回调发生在抽签事务内部，写进度必须走独立事务
 * （{@code @Transactional(propagation = REQUIRES_NEW)}），否则外部查询看不到未提交的行。</p>
 *
 * @author ruoyi
 */
@FunctionalInterface
public interface LotteryProgressListener
{
    /**
     * @param processed 已落库记录数（累计）
     * @param total     待抽签总记录数（抽签开始时的快照）
     * @param admitted  中签人数（抽签阶段已定，不是累计值）
     * @param rejected  落选人数（抽签阶段已定，不是累计值）
     */
    void onProgress(int processed, int total, int admitted, int rejected);

    /** 空实现，供同步调用方与单测使用 */
    LotteryProgressListener NOOP = (processed, total, admitted, rejected) -> {
    };
}
