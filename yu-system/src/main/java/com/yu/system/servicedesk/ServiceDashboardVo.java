package com.yu.system.servicedesk;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * 服务大厅时效看板视图（N3 一站式服务大厅）。
 *
 * <p>汇总全校待办按服务事项与时效档位（正常/临期/超时）的分布，供管理端驾驶舱展示。
 *
 * @author N3
 */
public class ServiceDashboardVo
{
    private Date generatedAt;
    private int totalPending;
    private int normalCount;
    private int nearDueCount;
    private int overdueCount;
    private List<ItemStat> items = new ArrayList<>();

    public Date getGeneratedAt() { return generatedAt; }
    public void setGeneratedAt(Date generatedAt) { this.generatedAt = generatedAt; }
    public int getTotalPending() { return totalPending; }
    public void setTotalPending(int totalPending) { this.totalPending = totalPending; }
    public int getNormalCount() { return normalCount; }
    public void setNormalCount(int normalCount) { this.normalCount = normalCount; }
    public int getNearDueCount() { return nearDueCount; }
    public void setNearDueCount(int nearDueCount) { this.nearDueCount = nearDueCount; }
    public int getOverdueCount() { return overdueCount; }
    public void setOverdueCount(int overdueCount) { this.overdueCount = overdueCount; }
    public List<ItemStat> getItems() { return items; }
    public void setItems(List<ItemStat> items) { this.items = items; }

    /** 单个服务事项的时效统计 */
    public static class ItemStat
    {
        private String code;
        private String name;
        private String category;
        private int slaHours;
        private int total;
        private int normal;
        private int nearDue;
        private int overdue;
        /** 最老待办的已挂起小时数，反映积压程度 */
        private long oldestHours;

        public String getCode() { return code; }
        public void setCode(String code) { this.code = code; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public int getSlaHours() { return slaHours; }
        public void setSlaHours(int slaHours) { this.slaHours = slaHours; }
        public int getTotal() { return total; }
        public void setTotal(int total) { this.total = total; }
        public int getNormal() { return normal; }
        public void setNormal(int normal) { this.normal = normal; }
        public int getNearDue() { return nearDue; }
        public void setNearDue(int nearDue) { this.nearDue = nearDue; }
        public int getOverdue() { return overdue; }
        public void setOverdue(int overdue) { this.overdue = overdue; }
        public long getOldestHours() { return oldestHours; }
        public void setOldestHours(long oldestHours) { this.oldestHours = oldestHours; }
    }
}
