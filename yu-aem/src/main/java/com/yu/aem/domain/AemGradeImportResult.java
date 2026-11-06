package com.yu.aem.domain;

import java.util.ArrayList;
import java.util.List;

/**
 * Excel 成绩导入结果（不落库，仅用于回显导入口径）。
 *
 * <p>重复导入时的口径：同一 (学生, 课程, 学期, 考试类型) 已有成绩时，
 * 「未提交(0)/已驳回(3)」被覆盖，「已提交待审(1)/已锁定(2)」保持原值并被跳过。
 * 本对象把「处理了几条、跳过了几条、跳过的是 Excel 哪几行」一并带回前端，
 * 避免用户以为全部导入成功而漏改。</p>
 *
 * <p>行号口径：Excel 第 1 行是表头，解析出的列表下标 i（从 0 开始）
 * 对应 Excel 第 i + 2 行。</p>
 *
 * @author 成绩导入幂等闭环
 */
public class AemGradeImportResult
{
    /** 实际提交给数据库的行数（剔除跳过后） */
    private int processed;

    /** 因已提交待审/已锁定而被跳过的行数 */
    private int skipped;

    /** 被跳过的行对应的 Excel 行号（升序，可能为空） */
    private List<Integer> skippedRows = new ArrayList<>();

    /** 数据库报告的影响行数（含 ODKU 命中的更新行） */
    private int affectedRows;

    /** 记录一条被跳过的 Excel 行号 */
    public void addSkippedRow(int excelRowNo)
    {
        this.skippedRows.add(excelRowNo);
        this.skipped++;
    }

    /**
     * 跳过行号的简报，形如「12, 15, 18 等 25 行」。
     *
     * @param limit 最多列出多少个行号
     */
    public String skippedRowSummary(int limit)
    {
        if (skippedRows.isEmpty())
        {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        int shown = Math.min(limit, skippedRows.size());
        for (int i = 0; i < shown; i++)
        {
            if (i > 0)
            {
                sb.append(", ");
            }
            sb.append(skippedRows.get(i));
        }
        if (skippedRows.size() > shown)
        {
            sb.append(" 等 ").append(skippedRows.size()).append(" 行");
        }
        return sb.toString();
    }

    public int getProcessed()
    {
        return processed;
    }

    public void setProcessed(int processed)
    {
        this.processed = processed;
    }

    public int getSkipped()
    {
        return skipped;
    }

    public void setSkipped(int skipped)
    {
        this.skipped = skipped;
    }

    public List<Integer> getSkippedRows()
    {
        return skippedRows;
    }

    public void setSkippedRows(List<Integer> skippedRows)
    {
        this.skippedRows = skippedRows;
    }

    public int getAffectedRows()
    {
        return affectedRows;
    }

    public void setAffectedRows(int affectedRows)
    {
        this.affectedRows = affectedRows;
    }
}
