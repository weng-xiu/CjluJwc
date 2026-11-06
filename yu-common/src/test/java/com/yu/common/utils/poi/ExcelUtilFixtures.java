package com.yu.common.utils.poi;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Workbook;

import com.yu.common.annotation.Excel;
import com.yu.common.annotation.Excel.ColumnType;
import com.yu.common.annotation.Excel.Type;
import com.yu.common.annotation.Excels;

/**
 * G5：{@link ExcelUtil} 契约测试的实体与处理器夹具。
 *
 * <p>夹具只服务测试，故意把「注解的每条分支」都摆出来：日期格式、字典、翻译表达式、
 * 精度、统计、后缀、默认值、下拉（短/长）、仅导出/仅导入/不导出、自定义处理器、
 * 子列表合并、多级 targetAttr。任何一条注解语义被改动，对应用例必须能红。</p>
 */
final class ExcelUtilFixtures
{
    private ExcelUtilFixtures()
    {
    }

    /** 处理器：验证 {@code args} 是否原样透传。 */
    public static class PrefixHandler implements ExcelHandlerAdapter
    {
        @Override
        public Object format(Object value, String[] args, Cell cell, Workbook wb)
        {
            return (args.length > 0 ? args[0] : "H") + "-" + value;
        }
    }

    /** 处理器：一定会抛异常，用来锁住「处理器失败不能拖垮整张导出」。 */
    public static class BoomHandler implements ExcelHandlerAdapter
    {
        @Override
        public Object format(Object value, String[] args, Cell cell, Workbook wb)
        {
            throw new IllegalStateException("handler boom");
        }
    }

    /** 子列表明细行。 */
    public static class Detail
    {
        @Excel(name = "课程", sort = 1)
        private String course;

        @Excel(name = "学分", sort = 2, cellType = ColumnType.NUMERIC)
        private Integer credit;

        public Detail()
        {
        }

        public Detail(String course, Integer credit)
        {
            this.course = course;
            this.credit = credit;
        }

        public String getCourse()
        {
            return course;
        }

        public void setCourse(String course)
        {
            this.course = course;
        }

        public Integer getCredit()
        {
            return credit;
        }

        public void setCredit(Integer credit)
        {
            this.credit = credit;
        }
    }

    /** 多级 targetAttr 的载体。 */
    public static class Nested
    {
        private String className;

        private String campus;

        public Nested()
        {
        }

        public Nested(String className, String campus)
        {
            this.className = className;
            this.campus = campus;
        }

        public String getClassName()
        {
            return className;
        }

        public void setClassName(String className)
        {
            this.className = className;
        }

        public String getCampus()
        {
            return campus;
        }

        public void setCampus(String campus)
        {
            this.campus = campus;
        }
    }

    /** 父类字段也要被导出：验证 {@code getFields()} 会并上 superclass 的声明字段。 */
    public static class ExportBase
    {
        @Excel(name = "租户", sort = 99)
        private String tenant;

        public String getTenant()
        {
            return tenant;
        }

        public void setTenant(String tenant)
        {
            this.tenant = tenant;
        }
    }

    /**
     * 导出实体：把 {@code @Excel} 的主要分支一次铺满。
     */
    public static class ExportVo extends ExportBase
    {
        @Excel(name = "编号", sort = 1, cellType = ColumnType.NUMERIC)
        private Long id;

        @Excel(name = "姓名", sort = 2)
        private String name;

        @Excel(name = "生日", sort = 3, dateFormat = "yyyy-MM-dd")
        private Date birthday;

        @Excel(name = "性别", sort = 4, readConverterExp = "0=男,1=女")
        private String sex;

        @Excel(name = "成绩", sort = 5, scale = 2)
        private BigDecimal score;

        @Excel(name = "金额", sort = 6, isStatistics = true)
        private Integer amount;

        @Excel(name = "备注", sort = 7, defaultValue = "无")
        private String remark;

        @Excel(name = "班级", sort = 8, targetAttr = "className")
        private Nested nested;

        @Excel(name = "仅导出", sort = 10, type = Type.EXPORT)
        private String exportOnly;

        @Excel(name = "仅导入", sort = 11, type = Type.IMPORT)
        private String importOnly;

        @Excel(name = "不导出", sort = 12, isExport = false, prompt = "请手工填写")
        private String skipExport;

        @Excel(name = "短下拉", sort = 13, combo = { "A", "B" }, prompt = "只能选 A/B")
        private String shortCombo;

        @Excel(name = "长下拉", sort = 14, combo = { "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13",
                "14", "15", "16" })
        private String longCombo;

        @Excel(name = "处理器", sort = 15, handler = PrefixHandler.class, args = { "P" })
        private String handled;

        @Excel(name = "坏处理器", sort = 16, handler = BoomHandler.class)
        private String boom;

        @Excel(name = "公式", sort = 17)
        private String formula;

        @Excel(name = "注：说明行", sort = 18)
        private String note;

        @Excel(name = "明细", sort = 19)
        private List<Detail> details = new ArrayList<>();

        @Excel(name = "合并列", sort = 22, needMerge = true)
        private String merged;

        @Excels({ @Excel(name = "多注解A", sort = 20, targetAttr = "className"),
                @Excel(name = "多注解B", sort = 21, targetAttr = "campus") })
        private Nested multi;

        public Long getId()
        {
            return id;
        }

        public void setId(Long id)
        {
            this.id = id;
        }

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public Date getBirthday()
        {
            return birthday;
        }

        public void setBirthday(Date birthday)
        {
            this.birthday = birthday;
        }

        public String getSex()
        {
            return sex;
        }

        public void setSex(String sex)
        {
            this.sex = sex;
        }

        public BigDecimal getScore()
        {
            return score;
        }

        public void setScore(BigDecimal score)
        {
            this.score = score;
        }

        public Integer getAmount()
        {
            return amount;
        }

        public void setAmount(Integer amount)
        {
            this.amount = amount;
        }

        public String getRemark()
        {
            return remark;
        }

        public void setRemark(String remark)
        {
            this.remark = remark;
        }

        public Nested getNested()
        {
            return nested;
        }

        public void setNested(Nested nested)
        {
            this.nested = nested;
        }

        public String getExportOnly()
        {
            return exportOnly;
        }

        public void setExportOnly(String exportOnly)
        {
            this.exportOnly = exportOnly;
        }

        public String getImportOnly()
        {
            return importOnly;
        }

        public void setImportOnly(String importOnly)
        {
            this.importOnly = importOnly;
        }

        public String getSkipExport()
        {
            return skipExport;
        }

        public void setSkipExport(String skipExport)
        {
            this.skipExport = skipExport;
        }

        public String getShortCombo()
        {
            return shortCombo;
        }

        public void setShortCombo(String shortCombo)
        {
            this.shortCombo = shortCombo;
        }

        public String getLongCombo()
        {
            return longCombo;
        }

        public void setLongCombo(String longCombo)
        {
            this.longCombo = longCombo;
        }

        public String getHandled()
        {
            return handled;
        }

        public void setHandled(String handled)
        {
            this.handled = handled;
        }

        public String getBoom()
        {
            return boom;
        }

        public void setBoom(String boom)
        {
            this.boom = boom;
        }

        public String getFormula()
        {
            return formula;
        }

        public void setFormula(String formula)
        {
            this.formula = formula;
        }

        public String getNote()
        {
            return note;
        }

        public void setNote(String note)
        {
            this.note = note;
        }

        public String getMerged()
        {
            return merged;
        }

        public void setMerged(String merged)
        {
            this.merged = merged;
        }

        public List<Detail> getDetails()
        {
            return details;
        }

        public void setDetails(List<Detail> details)
        {
            this.details = details;
        }

        public Nested getMulti()
        {
            return multi;
        }

        public void setMulti(Nested multi)
        {
            this.multi = multi;
        }
    }

    /** 子列表 getter 缺失时 {@link ExcelUtil#getSubMethod} 应记日志返回 null 而不是崩。 */
    public static class BrokenSubListVo
    {
        @Excel(name = "明细")
        private List<Detail> details = new ArrayList<>();
    }

    /** 导入实体：覆盖 {@code importExcel} 的八种字段类型分支。 */
    public static class ImportVo
    {
        @Excel(name = "姓名")
        private String name;

        @Excel(name = "数量")
        private Integer count;

        @Excel(name = "总量")
        private Long total;

        @Excel(name = "比率")
        private Double rate;

        @Excel(name = "比例")
        private Float ratio;

        @Excel(name = "金额")
        private BigDecimal money;

        @Excel(name = "生日")
        private Date birthday;

        @Excel(name = "启用")
        private Boolean enabled;

        @Excel(name = "性别", readConverterExp = "0=男,1=女")
        private String sex;

        @Excel(name = "字典", dictType = "sys_status")
        private String dictLabel;

        @Excel(name = "日期文本", dateFormat = "yyyy年MM月dd日")
        private String dateText;

        @Excel(name = "文本整数")
        private String textInteger;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public Integer getCount()
        {
            return count;
        }

        public void setCount(Integer count)
        {
            this.count = count;
        }

        public Long getTotal()
        {
            return total;
        }

        public void setTotal(Long total)
        {
            this.total = total;
        }

        public Double getRate()
        {
            return rate;
        }

        public void setRate(Double rate)
        {
            this.rate = rate;
        }

        public Float getRatio()
        {
            return ratio;
        }

        public void setRatio(Float ratio)
        {
            this.ratio = ratio;
        }

        public BigDecimal getMoney()
        {
            return money;
        }

        public void setMoney(BigDecimal money)
        {
            this.money = money;
        }

        public Date getBirthday()
        {
            return birthday;
        }

        public void setBirthday(Date birthday)
        {
            this.birthday = birthday;
        }

        public Boolean getEnabled()
        {
            return enabled;
        }

        public void setEnabled(Boolean enabled)
        {
            this.enabled = enabled;
        }

        public String getSex()
        {
            return sex;
        }

        public void setSex(String sex)
        {
            this.sex = sex;
        }

        public String getDictLabel()
        {
            return dictLabel;
        }

        public void setDictLabel(String dictLabel)
        {
            this.dictLabel = dictLabel;
        }

        public String getDateText()
        {
            return dateText;
        }

        public void setDateText(String dateText)
        {
            this.dateText = dateText;
        }

        public String getTextInteger()
        {
            return textInteger;
        }

        public void setTextInteger(String textInteger)
        {
            this.textInteger = textInteger;
        }
    }

    /** 图片列导入实体。 */
    public static class ImageVo
    {
        @Excel(name = "姓名")
        private String name;

        @Excel(name = "照片", cellType = ColumnType.IMAGE)
        private String photo;

        public String getName()
        {
            return name;
        }

        public void setName(String name)
        {
            this.name = name;
        }

        public String getPhoto()
        {
            return photo;
        }

        public void setPhoto(String photo)
        {
            this.photo = photo;
        }
    }
}
