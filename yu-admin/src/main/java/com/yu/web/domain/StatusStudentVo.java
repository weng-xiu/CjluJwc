package com.yu.web.domain;

import com.yu.common.annotation.Excel;
import com.yu.common.annotation.Excel.ColumnType;

/**
 * 状态数据上报——学生基本信息VO（字段代码取自教育部状态数据常用口径）。
 *
 * <p>字段与 sys_status_report_field（report_type=01）中的 std_code 一一对应，
 * 页面预览列由映射表驱动，导出列由本对象的 @Excel 注解决定。</p>
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public class StatusStudentVo
{
    @Excel(name = "学号(XH)")
    private String xh;

    @Excel(name = "姓名(XM)")
    private String xm;

    @Excel(name = "性别代码(XBDM)")
    private String xbdm;

    @Excel(name = "出生日期(CSRQ)")
    private String csrq;

    @Excel(name = "公民身份号码(GMSFHM)")
    private String gmsfhm;

    @Excel(name = "所在院系(SZYX)")
    private String szyx;

    @Excel(name = "专业名称(ZYMC)")
    private String zymc;

    @Excel(name = "班级名称(BJMC)")
    private String bjmc;

    @Excel(name = "入学年份(RXNJ)")
    private String rxnj;

    @Excel(name = "学历层次代码(XLCCDM)")
    private String xlccdm;

    @Excel(name = "学籍状态代码(XJZTDM)")
    private String xjztdm;

    @Excel(name = "修业年限(ZXNX)", cellType = ColumnType.NUMERIC)
    private Integer zxnx;

    public String getXh() { return xh; }
    public void setXh(String xh) { this.xh = xh; }
    public String getXm() { return xm; }
    public void setXm(String xm) { this.xm = xm; }
    public String getXbdm() { return xbdm; }
    public void setXbdm(String xbdm) { this.xbdm = xbdm; }
    public String getCsrq() { return csrq; }
    public void setCsrq(String csrq) { this.csrq = csrq; }
    public String getGmsfhm() { return gmsfhm; }
    public void setGmsfhm(String gmsfhm) { this.gmsfhm = gmsfhm; }
    public String getSzyx() { return szyx; }
    public void setSzyx(String szyx) { this.szyx = szyx; }
    public String getZymc() { return zymc; }
    public void setZymc(String zymc) { this.zymc = zymc; }
    public String getBjmc() { return bjmc; }
    public void setBjmc(String bjmc) { this.bjmc = bjmc; }
    public String getRxnj() { return rxnj; }
    public void setRxnj(String rxnj) { this.rxnj = rxnj; }
    public String getXlccdm() { return xlccdm; }
    public void setXlccdm(String xlccdm) { this.xlccdm = xlccdm; }
    public String getXjztdm() { return xjztdm; }
    public void setXjztdm(String xjztdm) { this.xjztdm = xjztdm; }
    public Integer getZxnx() { return zxnx; }
    public void setZxnx(Integer zxnx) { this.zxnx = zxnx; }
}
