package com.yu.sam.domain.dto;

import com.yu.common.annotation.Excel;

/**
 * 学生数字档案导出扁平行（F4 档案导出）。
 *
 * 全息档案为多板块嵌套结构，Excel 单表导出按「板块 / 项目 / 内容」三列摊平，
 * 保证一份文件可完整留档且无需多 Sheet。
 *
 * @author F4
 */
public class StudentProfileExportVo
{
    /** 板块（学籍基础/学业汇总/学分结构/OBE达成度/学业预警/近期成绩） */
    @Excel(name = "板块", width = 20)
    private String section;

    /** 项目 */
    @Excel(name = "项目", width = 28)
    private String item;

    /** 内容 */
    @Excel(name = "内容", width = 60)
    private String value;

    public StudentProfileExportVo()
    {
    }

    public StudentProfileExportVo(String section, String item, String value)
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
