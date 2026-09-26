package com.yu.web.domain;

import com.yu.common.annotation.Excel;

/**
 * 状态数据上报——教师基本信息VO（对应 sys_status_report_field，report_type=04）。
 *
 * @author ruoyi
 * @date 2026-09-26
 */
public class StatusTeacherVo
{
    @Excel(name = "教工号(JSGH)")
    private String jsgh;

    @Excel(name = "姓名(XM)")
    private String xm;

    @Excel(name = "性别代码(XBDM)")
    private String xbdm;

    @Excel(name = "所属院系(SSYX)")
    private String ssyx;

    @Excel(name = "职称(ZCDM)")
    private String zcdm;

    @Excel(name = "学历(XLDM)")
    private String xldm;

    @Excel(name = "联系电话(LXDH)")
    private String lxdh;

    @Excel(name = "电子邮箱(DZYX)")
    private String dzyx;

    @Excel(name = "是否在职(SFJS)")
    private String sfjs;

    public String getJsgh() { return jsgh; }
    public void setJsgh(String jsgh) { this.jsgh = jsgh; }
    public String getXm() { return xm; }
    public void setXm(String xm) { this.xm = xm; }
    public String getXbdm() { return xbdm; }
    public void setXbdm(String xbdm) { this.xbdm = xbdm; }
    public String getSsyx() { return ssyx; }
    public void setSsyx(String ssyx) { this.ssyx = ssyx; }
    public String getZcdm() { return zcdm; }
    public void setZcdm(String zcdm) { this.zcdm = zcdm; }
    public String getXldm() { return xldm; }
    public void setXldm(String xldm) { this.xldm = xldm; }
    public String getLxdh() { return lxdh; }
    public void setLxdh(String lxdh) { this.lxdh = lxdh; }
    public String getDzyx() { return dzyx; }
    public void setDzyx(String dzyx) { this.dzyx = dzyx; }
    public String getSfjs() { return sfjs; }
    public void setSfjs(String sfjs) { this.sfjs = sfjs; }
}
