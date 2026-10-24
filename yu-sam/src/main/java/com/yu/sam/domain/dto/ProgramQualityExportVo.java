package com.yu.sam.domain.dto;

import com.yu.common.annotation.Excel;

/**
 * 专业质量档案一页纸导出扁平行（F3 档案导出）。
 *
 * 一页纸为多板块嵌套结构，Excel 单表导出按「板块 / 项目 / 内容」三列摊平，
 * 保证一份文件可完整留档且无需多 Sheet。
 *
 * @author F3
 */
public class ProgramQualityExportVo
{
    /** 板块（专业基础/培养规模/师资队伍/课程教学/OBE达成度/评教/督导） */
    @Excel(name = "板块", width = 20)
    private String section;

    /** 项目 */
    @Excel(name = "项目", width = 28)
    private String item;

    /** 内容 */
    @Excel(name = "内容", width = 60)
    private String value;

    public ProgramQualityExportVo()
    {
    }

    public ProgramQualityExportVo(String section, String item, String value)
    {
        this.section = section;
        this.item = item;
        this.value = value;
    }

    public String getSection() { return section; }
    public void setSection(String section) { this.section = section; }

    public String getItem() { return item; }
    public void setItem(String item) { this.item = item; }

    public String getValue() { return value; }
    public void setValue(String value) { this.value = value; }
}
