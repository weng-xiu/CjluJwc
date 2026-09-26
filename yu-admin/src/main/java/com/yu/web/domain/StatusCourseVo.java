package com.yu.web.domain;

import com.yu.common.annotation.Excel;

/**
 * 状态数据上报——课程基本信息VO（对应 sys_status_report_field，report_type=02）。
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public class StatusCourseVo
{
    @Excel(name = "课程号(KCH)")
    private String kch;

    @Excel(name = "课程名(KCM)")
    private String kcm;

    @Excel(name = "课程英文名(KCMCYW)")
    private String kcmcyw;

    @Excel(name = "学分(XF)")
    private String xf;

    @Excel(name = "总学时(ZXS)")
    private Integer zxs;

    @Excel(name = "理论学时(LLXS)")
    private Integer llxs;

    @Excel(name = "实践学时(SJXS)")
    private Integer sjxs;

    @Excel(name = "课程性质代码(KCXZDM)")
    private String kcxzdm;

    @Excel(name = "课程类别(KCLB)")
    private String kclb;

    @Excel(name = "考核方式(KSKFMS)")
    private String kskfms;

    @Excel(name = "开设院系(SSYX)")
    private String ssyx;

    public String getKch() { return kch; }
    public void setKch(String kch) { this.kch = kch; }
    public String getKcm() { return kcm; }
    public void setKcm(String kcm) { this.kcm = kcm; }
    public String getKcmcyw() { return kcmcyw; }
    public void setKcmcyw(String kcmcyw) { this.kcmcyw = kcmcyw; }
    public String getXf() { return xf; }
    public void setXf(String xf) { this.xf = xf; }
    public Integer getZxs() { return zxs; }
    public void setZxs(Integer zxs) { this.zxs = zxs; }
    public Integer getLlxs() { return llxs; }
    public void setLlxs(Integer llxs) { this.llxs = llxs; }
    public Integer getSjxs() { return sjxs; }
    public void setSjxs(Integer sjxs) { this.sjxs = sjxs; }
    public String getKcxzdm() { return kcxzdm; }
    public void setKcxzdm(String kcxzdm) { this.kcxzdm = kcxzdm; }
    public String getKclb() { return kclb; }
    public void setKclb(String kclb) { this.kclb = kclb; }
    public String getKskfms() { return kskfms; }
    public void setKskfms(String kskfms) { this.kskfms = kskfms; }
    public String getSsyx() { return ssyx; }
    public void setSsyx(String ssyx) { this.ssyx = ssyx; }
}
