package com.yu.tpm.schedule.dsl;

/**
 * N4 排课约束策略（DSL 解析产物 / 引擎入参）。
 *
 * <p>把原先硬编码在 {@code ScheduleOptimizationServiceImpl#chooseClassroom} 里的
 * 硬约束开关与软约束权重集中为可声明式配置的对象：硬约束决定是否可行（越界即不可排），
 * 软约束以加分/减分参与择优排序。{@link #defaults()} 精确复刻既有贪心口径，
 * 因此默认策略下增量重排与全量自动排课的选择行为保持一致，配置项仅用于按需调参。</p>
 *
 * @author N4
 */
public class ScheduleConstraintPolicy
{
    // ===== 硬约束开关 =====

    /** 教师禁排时间片是否作为硬约束屏蔽 */
    private boolean hardForbidden = true;

    /** 是否强制同校区：为 true 时带校区偏好的开课只能选同校区教室，否则判不可排 */
    private boolean hardSameCampus = false;

    /** 单个开课每天最多排入的会话数（0 表示不限制，保持既有铺天策略） */
    private int hardMaxSessionsPerDay = 0;

    // ===== 软约束权重 =====

    /** 需要实践/机房且命中实验类教室的加分 */
    private int softLabMatch = 100;

    /** 需要实践/机房但落到普通教室的减分（负值，通常足以排除） */
    private int softLabMismatch = -20;

    /** 理论课命中普通教室的加分 */
    private int softRegularRoom = 5;

    /** 理论课误占实验/机房的减分 */
    private int softRegularInLab = -10;

    /** 与开课期望同校区的加分 */
    private int softSameCampus = 50;

    /** 与该开课首个会话同楼宇的加分 */
    private int softSameBuilding = 30;

    /** 容量贴合惩罚除数：富余容量越大扣分越多（(cap-req)/divisor） */
    private int capacityFitDivisor = 10;

    /** 是否按教师当日课时负载均衡择日 */
    private boolean dailyBalance = true;

    /** 返回与既有贪心完全一致的默认策略 */
    public static ScheduleConstraintPolicy defaults()
    {
        return new ScheduleConstraintPolicy();
    }

    public boolean isHardForbidden() { return hardForbidden; }
    public ScheduleConstraintPolicy setHardForbidden(boolean hardForbidden) { this.hardForbidden = hardForbidden; return this; }

    public boolean isHardSameCampus() { return hardSameCampus; }
    public ScheduleConstraintPolicy setHardSameCampus(boolean hardSameCampus) { this.hardSameCampus = hardSameCampus; return this; }

    public int getHardMaxSessionsPerDay() { return hardMaxSessionsPerDay; }
    public ScheduleConstraintPolicy setHardMaxSessionsPerDay(int hardMaxSessionsPerDay) { this.hardMaxSessionsPerDay = hardMaxSessionsPerDay; return this; }

    public int getSoftLabMatch() { return softLabMatch; }
    public ScheduleConstraintPolicy setSoftLabMatch(int softLabMatch) { this.softLabMatch = softLabMatch; return this; }

    public int getSoftLabMismatch() { return softLabMismatch; }
    public ScheduleConstraintPolicy setSoftLabMismatch(int softLabMismatch) { this.softLabMismatch = softLabMismatch; return this; }

    public int getSoftRegularRoom() { return softRegularRoom; }
    public ScheduleConstraintPolicy setSoftRegularRoom(int softRegularRoom) { this.softRegularRoom = softRegularRoom; return this; }

    public int getSoftRegularInLab() { return softRegularInLab; }
    public ScheduleConstraintPolicy setSoftRegularInLab(int softRegularInLab) { this.softRegularInLab = softRegularInLab; return this; }

    public int getSoftSameCampus() { return softSameCampus; }
    public ScheduleConstraintPolicy setSoftSameCampus(int softSameCampus) { this.softSameCampus = softSameCampus; return this; }

    public int getSoftSameBuilding() { return softSameBuilding; }
    public ScheduleConstraintPolicy setSoftSameBuilding(int softSameBuilding) { this.softSameBuilding = softSameBuilding; return this; }

    public int getCapacityFitDivisor() { return capacityFitDivisor; }
    public ScheduleConstraintPolicy setCapacityFitDivisor(int capacityFitDivisor) { this.capacityFitDivisor = capacityFitDivisor; return this; }

    public boolean isDailyBalance() { return dailyBalance; }
    public ScheduleConstraintPolicy setDailyBalance(boolean dailyBalance) { this.dailyBalance = dailyBalance; return this; }

    /** 计算某候选教室对某次会话的软约束评分（纯函数，供贪心择优复用） */
    public int scoreRoom(boolean needsLab, boolean roomIsLab, boolean sameCampus, boolean sameBuilding,
                        int roomCapacity, int reqCapacity)
    {
        int score;
        if (needsLab)
        {
            score = roomIsLab ? softLabMatch : softLabMismatch;
        }
        else
        {
            score = roomIsLab ? softRegularInLab : softRegularRoom;
        }
        if (sameCampus) { score += softSameCampus; }
        if (sameBuilding) { score += softSameBuilding; }
        int divisor = capacityFitDivisor > 0 ? capacityFitDivisor : 1;
        score -= (roomCapacity - reqCapacity) / divisor;
        return score;
    }
}
