package com.yu.web.domain;

import com.yu.common.annotation.Excel;

/**
 * 状态数据上报——学生成绩信息VO（对应 sys_status_report_field，report_type=03）。
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public class StatusGradeVo
{
    @Excel(name = "学号(XH)")
    private String xh;

    @Excel(name = "姓名(XM)")
    private String xm;

    @Excel(name = "课程号(KCH)")
    private String kch;

    @Excel(name = "课程名(KCM)")
    private String kcm;

    @Excel(name = "学分(XF)")
    private String xf;

    @Excel(name = "成绩(CJ)")
    private String cj;

    @Excel(name = "绩点(JD)")
    private String jd;

    @Excel(name = "成绩等级(CJDJ)")
    private String cjdj;

    @Excel(name = "结果代码(JGDM)")
    private String jgdm;

    @Excel(name = "考核类型(KCLX)")
    private String kclx;

    @Excel(name = "学期(XQMC)")
    private String xqmc;

    @Excel(name = "所在院系(SZYX)")
    private String szyx;

    public String getXh() { return xh; }
    public void setXh(String xh) { this.xh = xh; }
    public String getXm() { return xm; }
    public void setXm(String xm) { this.xm = xm; }
    public String getKch() { return kch; }
    public void setKch(String kch) { this.kch = kch; }
    public String getKcm() { return kcm; }
    public void setKcm(String kcm) { this.kcm = kcm; }
    public String getXf() { return xf; }
    public void setXf(String xf) { this.xf = xf; }
    public String getCj() { return cj; }
    public void setCj(String cj) { this.cj = cj; }
    public String getJd() { return jd; }
    public void setJd(String jd) { this.jd = jd; }
    public String getCjdj() { return cjdj; }
    public void setCjdj(String cjdj) { this.cjdj = cjdj; }
    public String getJgdm() { return jgdm; }
    public void setJgdm(String jgdm) { this.jgdm = jgdm; }
    public String getKclx() { return kclx; }
    public void setKclx(String kclx) { this.kclx = kclx; }
    public String getXqmc() { return xqmc; }
    public void setXqmc(String xqmc) { this.xqmc = xqmc; }
    public String getSzyx() { return szyx; }
    public void setSzyx(String szyx) { this.szyx = szyx; }
}
