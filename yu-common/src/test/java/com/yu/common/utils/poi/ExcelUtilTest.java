package com.yu.common.utils.poi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockHttpServletResponse;

import com.yu.common.config.RuoYiConfig;
import com.yu.common.core.domain.AjaxResult;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.poi.ExcelUtilFixtures.Detail;
import com.yu.common.utils.poi.ExcelUtilFixtures.ExportVo;
import com.yu.common.utils.poi.ExcelUtilFixtures.Nested;

/**
 * G5：{@link ExcelUtil} 导出链路的契约测试（此前该类整块零覆盖）。
 *
 * <p>导出是「注解驱动 + 反射取值 + POI 写盘」的长链路，出错时往往不报错而是**静默少一列 / 空一列**，
 * 所以这里全部走真实 POI：在内存里生成 xlsx，再把字节流回读，逐格断言。
 * 只 mock 了 {@code HttpServletResponse}（拿字节），没有 mock POI。</p>
 *
 * <p>钉住的四件事：① 注解分支（日期/翻译/精度/默认值/后缀/统计/处理器）各写各的格；
 * ② 列序由 {@code sort} 决定、{@code type} 与 {@code isExport} 是两种不同的过滤（一个是列不存在，
 * 一个是列存在但不填）；③ 子列表是「一行变多行 + 纵向合并」；④ 下拉超过 15 项必须落到隐藏 sheet，
 * 否则 Excel 打开模板时下拉为空。</p>
 */
class ExcelUtilTest
{
    @TempDir
    Path tempDir;

    private String previousProfile;

    @BeforeEach
    void setUp()
    {
        previousProfile = RuoYiConfig.getProfile();
        new RuoYiConfig().setProfile(tempDir.toString());
    }

    @AfterEach
    void tearDown()
    {
        new RuoYiConfig().setProfile(previousProfile);
    }

    // ---------------------------------------------------------------- 夹具数据

    private static ExportVo sample()
    {
        ExportVo vo = new ExportVo();
        vo.setId(1L);
        vo.setName("张三");
        vo.setBirthday(DateUtils.parseDate("2020-01-02"));
        vo.setSex("0");
        vo.setScore(new BigDecimal("88.456"));
        vo.setAmount(10);
        vo.setRemark(null);
        vo.setNested(new Nested("一班", "东校区"));
        vo.setExportOnly("E");
        vo.setImportOnly("I");
        vo.setSkipExport("S");
        vo.setShortCombo("A");
        vo.setLongCombo("1");
        vo.setHandled("v");
        vo.setBoom("b");
        vo.setFormula("=1+1");
        vo.setNote("n");
        vo.setMerged("M");
        vo.setTenant("T");
        vo.setDetails(Arrays.asList(new Detail("数学", 4), new Detail("语文", 3)));
        vo.setMulti(new Nested("二班", "西校区"));
        return vo;
    }

    private static Sheet readSheet(MockHttpServletResponse response) throws Exception
    {
        byte[] bytes = response.getContentAsByteArray();
        assertTrue(bytes.length > 0, "导出必须写出字节流");
        return new XSSFWorkbook(new ByteArrayInputStream(bytes)).getSheetAt(0);
    }

    private static String text(Sheet sheet, int rowIndex, int column)
    {
        Row row = sheet.getRow(rowIndex);
        assertNotNull(row, "第 " + rowIndex + " 行不存在");
        return row.getCell(column) == null ? null : row.getCell(column).toString();
    }

    private static boolean hasMerged(Sheet sheet, int firstRow, int lastRow, int firstCol, int lastCol)
    {
        return sheet.getMergedRegions().stream()
                .anyMatch(r -> r.getFirstRow() == firstRow && r.getLastRow() == lastRow
                        && r.getFirstColumn() == firstCol && r.getLastColumn() == lastCol);
    }

    // ---------------------------------------------------------------- 导出主链路

    @Test
    @DisplayName("exportExcel：标题→子表头→列头→数据→明细→统计行，逐格对齐")
    void exportSingleSheet() throws Exception
    {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ExcelUtil<>(ExportVo.class).exportExcel(response, List.of(sample()), "成绩表", "成绩汇总");
        Sheet sheet = readSheet(response);

        assertEquals("成绩表", sheet.getSheetName());
        assertEquals("成绩汇总", text(sheet, 0, 0));
        assertTrue(hasMerged(sheet, 0, 0, 0, 21), "标题要横跨整张表（21 列 + 明细展开 1 列）");

        // 第 1 行是「注解大类名」，第 2 行才是真实列头（子列表在此展开）
        assertEquals("编号", text(sheet, 1, 0));
        assertEquals("明细", text(sheet, 1, 16));
        assertEquals("课程", text(sheet, 2, 16));
        assertEquals("学分", text(sheet, 2, 17));
        assertEquals("多注解A", text(sheet, 2, 18));
        assertEquals("多注解B", text(sheet, 2, 19));
        assertEquals("合并列", text(sheet, 2, 20));
        assertEquals("租户", text(sheet, 2, 21));

        // 数据首行
        assertEquals(1.0d, sheet.getRow(3).getCell(0).getNumericCellValue(), 0.0001);
        assertEquals("张三", text(sheet, 3, 1));
        assertEquals("2020-01-02", text(sheet, 3, 2), "dateFormat 生效");
        assertEquals("男", text(sheet, 3, 3), "readConverterExp 把 0 翻成男");
        assertEquals(88.46d, sheet.getRow(3).getCell(4).getNumericCellValue(), 0.0001, "scale=2");
        assertEquals("10", text(sheet, 3, 5), "金额列未声明 cellType，文本形态落盘");
        assertEquals("无", text(sheet, 3, 6), "值为 null 时填 defaultValue");
        assertEquals("一班", text(sheet, 3, 7), "targetAttr 取的是嵌套对象的属性");
        assertEquals("E", text(sheet, 3, 8));
        assertNull(sheet.getRow(3).getCell(9), "isExport=false 的列存在列头但不能有数据格");
        assertEquals("A", text(sheet, 3, 10));
        assertEquals("1", text(sheet, 3, 11));
        assertEquals("P-v", text(sheet, 3, 12), "handler 需要拿到 args");
        assertEquals("b", text(sheet, 3, 13), "处理器内部异常回退为原值，不能让整车导出失败");
        assertEquals("\t=1+1", text(sheet, 3, 14), "以 = 开头的文本要加制表符前缀，防 CSV 注入");
        assertEquals("n", text(sheet, 3, 15));
        assertEquals("数学", text(sheet, 3, 16));
        assertEquals(4.0d, sheet.getRow(3).getCell(17).getNumericCellValue(), 0.0001);
        assertEquals("二班", text(sheet, 3, 18), "@Excels 的第一条注解");
        assertEquals("西校区", text(sheet, 3, 19), "@Excels 的第二条注解");
        assertEquals("M", text(sheet, 3, 20));
        assertEquals("T", text(sheet, 3, 21), "父类字段也要导出，且 sort 大的排后面");

        // 子列表第二行 + 纵向合并
        assertEquals("语文", text(sheet, 4, 16));
        assertEquals(3.0d, sheet.getRow(4).getCell(17).getNumericCellValue(), 0.0001);
        assertTrue(hasMerged(sheet, 3, 4, 20, 20), "needMerge 列要在明细行之间纵向合并");

        // 统计行
        assertEquals("合计", text(sheet, 5, 0));
        assertEquals(10.0d, sheet.getRow(5).getCell(5).getNumericCellValue(), 0.0001);
    }

    @Test
    @DisplayName("exportExcel：下拉 ≤15 项走显式列表，>15 项落到隐藏 sheet 并由名称引用")
    void exportTemplateCombo() throws Exception
    {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ExcelUtil<>(ExportVo.class).exportExcel(response, new ArrayList<ExportVo>(), "下拉表", "");

        byte[] bytes = response.getContentAsByteArray();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes)))
        {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals(3, sheet.getDataValidations().size(), "两条下拉 + 一条纯提示各算一条验证规则");

            assertEquals(2, wb.getNumberOfSheets(), "超过 15 项必须额外建一个隐藏 sheet");
            assertEquals("combo_11_11", wb.getSheetName(1));
            assertTrue(wb.isSheetHidden(1), "存下拉值的 sheet 必须隐藏");
            assertEquals("16", wb.getSheetAt(1).getRow(15).getCell(0).getStringCellValue());
            assertNotNull(wb.getName("combo_11_11_data"), "隐藏 sheet 的值要靠定义名称被引用");
        }
    }

    @Test
    @DisplayName("exportExcel：注释列（注：开头）固定列宽 6000，其余按 width 换算")
    void exportColumnWidth() throws Exception
    {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ExcelUtil<>(ExportVo.class).exportExcel(response, new ArrayList<ExportVo>(), "列宽表", "");

        byte[] bytes = response.getContentAsByteArray();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes)))
        {
            Sheet sheet = wb.getSheetAt(0);
            int noteWidth = sheet.getColumnWidth(15);
            assertTrue(noteWidth >= 5500 && noteWidth <= 6500, "注：列固定 6000，实际 " + noteWidth);
            int normal = sheet.getColumnWidth(0);
            assertTrue(normal > 4000 && normal < 4400, "默认 width=16 换算后约 4280，实际 " + normal);
        }
    }

    @Test
    @DisplayName("exportExcel：空集合导出只有表头，不产生数据行与统计行")
    void exportEmptyList() throws Exception
    {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ExcelUtil<>(ExportVo.class).exportExcel(response, new ArrayList<ExportVo>(), "空表", "");

        byte[] bytes = response.getContentAsByteArray();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes)))
        {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals(1, sheet.getLastRowNum(), "无标题时只有子表头(0)与列头(1)两行，数据行与统计行都不能出现");
            assertEquals("编号", text(sheet, 1, 0));
        }
    }

    @Test
    @DisplayName("exportExcel：落盘导出的文件名是「UUID_sheetName.xlsx」且文件真实存在")
    void exportToFile() throws Exception
    {
        AjaxResult result = new ExcelUtil<>(ExportVo.class).exportExcel(List.of(sample()), "成绩表", "标题");
        // AjaxResult.success(String) 命中的是「消息」重载，文件名落在 msg 而不是 data
        Object filename = result.get(AjaxResult.MSG_TAG);
        assertNotNull(filename);

        File file = tempDir.resolve("download").resolve(filename.toString()).toFile();
        assertTrue(file.exists(), "导出件必须落在 ruoyi.profile/download 下：" + file);
        assertTrue(file.length() > 0);
        assertTrue(filename.toString().endsWith("_成绩表.xlsx"));
    }

    @Test
    @DisplayName("importTemplateExcel：IMPORT 模式只写表头，不写数据也不写统计行")
    void importTemplate() throws Exception
    {
        MockHttpServletResponse response = new MockHttpServletResponse();
        new ExcelUtil<>(ExportVo.class).importTemplateExcel(response, "成绩模板", "模板标题");

        byte[] bytes = response.getContentAsByteArray();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes)))
        {
            Sheet sheet = wb.getSheetAt(0);
            assertEquals("模板标题", text(sheet, 0, 0));
            assertEquals("编号", text(sheet, 1, 0), "第 1 行仍是子表头（集合字段的存在与 type 无关）");
            assertEquals("课程", text(sheet, 2, 16), "IMPORT 模式一样会展开子列头");
            assertEquals(2, sheet.getLastRowNum(), "模板不能带数据行，最后一行就是列头");
        }
    }

    // ---------------------------------------------------------------- 多 Sheet

    @Test
    @DisplayName("exportMultiSheet：多个 sheet 写进同一工作簿，各自带自己的标题")
    void exportMultiSheetToResponse() throws Exception
    {
        List<ExcelSheet<?>> sheets = new ArrayList<>();
        sheets.add(new ExcelSheet<>("第一页", List.of(new Detail("数学", 4)), Detail.class, "第一标题"));
        sheets.add(new ExcelSheet<>("第二页", List.of(new Detail("语文", 3), new Detail("英语", 2)), Detail.class));

        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelUtil.exportMultiSheet(response, sheets);

        byte[] bytes = response.getContentAsByteArray();
        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(bytes)))
        {
            assertEquals(2, wb.getNumberOfSheets());
            assertEquals("第一页", wb.getSheetName(0));
            assertEquals("第二页", wb.getSheetName(1));
            assertEquals("第一标题", text(wb.getSheetAt(0), 0, 0));
            assertEquals("数学", text(wb.getSheetAt(0), 2, 0));
            // Detail 没有集合字段，不会生成子表头行：无标题时第 0 行就是列头
            assertEquals("课程", text(wb.getSheetAt(1), 0, 0), "第二页没有标题，第 0 行直接是列头");
            assertEquals("语文", text(wb.getSheetAt(1), 1, 0), "无标题时数据紧跟列头");
        }
    }

    @Test
    @DisplayName("exportMultiSheet：空列表直接拒绝，不生成空文件")
    void exportMultiSheetEmpty()
    {
        AjaxResult result = ExcelUtil.exportMultiSheet(new ArrayList<>());
        assertEquals(Integer.valueOf(com.yu.common.constant.HttpStatus.ERROR), result.get(AjaxResult.CODE_TAG));

        MockHttpServletResponse response = new MockHttpServletResponse();
        ExcelUtil.exportMultiSheet(response, new ArrayList<>());
        assertEquals(0, response.getContentAsByteArray().length, "空 sheets 不应写出任何字节");
    }

    @Test
    @DisplayName("exportMultiSheet：落盘模式返回文件名，文件可被重新打开")
    void exportMultiSheetToFile() throws Exception
    {
        List<ExcelSheet<?>> sheets = List.of(new ExcelSheet<>("成绩卷", List.of(new Detail("数学", 4)), Detail.class));
        AjaxResult result = ExcelUtil.exportMultiSheet(sheets);
        String filename = String.valueOf(result.get(AjaxResult.MSG_TAG));

        File file = tempDir.resolve("download").resolve(filename).toFile();
        assertTrue(file.exists(), "多 sheet 落盘件：" + file);
        try (XSSFWorkbook wb = new XSSFWorkbook(file))
        {
            assertEquals("成绩卷", wb.getSheetName(0));
        }
    }

    // ---------------------------------------------------------------- 字段扫描

    @Test
    @DisplayName("getFields：type 为 null 时只放行 Type.ALL，仅导出/仅导入字段被滤掉")
    void fieldsRespectExcelType()
    {
        List<Object[]> fields = new ExcelUtil<>(ExportVo.class).getFields();
        List<String> names = fields.stream()
                .map(o -> ((java.lang.reflect.Field) o[0]).getName())
                .collect(Collectors.toList());

        assertEquals(20, fields.size(), "21 条注解里「仅导出」「仅导入」不属于 ALL，被过滤后剩 20");
        assertFalse(names.contains("exportOnly"), "未指定 type 时 EXPORT 专属字段不能出现");
        assertFalse(names.contains("importOnly"));
        assertTrue(names.contains("tenant"), "父类字段必须纳入");
    }

    @Test
    @DisplayName("showColumn / hideColumn：白名单与黑名单互斥，且白名单不挡 @Excels")
    void showAndHideColumn()
    {
        ExcelUtil<ExportVo> show = new ExcelUtil<>(ExportVo.class);
        show.showColumn("name", "id");
        List<String> shown = show.getFields().stream()
                .map(o -> ((java.lang.reflect.Field) o[0]).getName() + "#" + ((com.yu.common.annotation.Excel) o[1]).sort())
                .collect(Collectors.toList());
        assertEquals(2, shown.size(), "只保留 name/id 两个字段，@Excels 的 multi 不在白名单内");
        assertTrue(shown.contains("name#2") && shown.contains("id#1"), shown.toString());

        ExcelUtil<ExportVo> hide = new ExcelUtil<>(ExportVo.class);
        hide.hideColumn("name");
        assertTrue(hide.getFields().stream()
                .noneMatch(o -> "name".equals(((java.lang.reflect.Field) o[0]).getName())));
    }

    @Test
    @DisplayName("getRowHeight：取所有列 height 的最大值再乘 20")
    void rowHeight()
    {
        ExcelUtil<ExportVo> util = new ExcelUtil<>(ExportVo.class);
        util.init(new ArrayList<ExportVo>(), "行高", "", com.yu.common.annotation.Excel.Type.EXPORT);
        assertEquals(280, util.getRowHeight(), "默认 height=14 → 14*20");
    }

    @Test
    @DisplayName("isSubList：有集合字段才有子表头；getSubMethod：getter 缺失时返回 null 不抛")
    void subListDetection()
    {
        ExcelUtil<ExportVo> util = new ExcelUtil<>(ExportVo.class);
        util.getFields();
        assertTrue(util.isSubList());

        ExcelUtil<ExcelUtilFixtures.BrokenSubListVo> broken = new ExcelUtil<>(ExcelUtilFixtures.BrokenSubListVo.class);
        assertNull(broken.getSubMethod("details", ExcelUtilFixtures.BrokenSubListVo.class),
                "getter 命名不符时返回 null，由调用方按 0 行处理");
    }

    @Test
    @DisplayName("getListCellValue：没有 getter 的集合字段按 0 处理，不能让整表导出抛异常")
    void listCellValueWithoutGetter()
    {
        ExcelUtil<ExcelUtilFixtures.BrokenSubListVo> util = new ExcelUtil<>(ExcelUtilFixtures.BrokenSubListVo.class);
        util.getFields();
        assertEquals(0, util.getListCellValue(new ExcelUtilFixtures.BrokenSubListVo()));
        assertFalse(util.isSubListValue(new ExcelUtilFixtures.BrokenSubListVo()));
    }

    // ---------------------------------------------------------------- 下载目录

    @Test
    @DisplayName("getAbsoluteFile：下载目录不存在时自动创建，并登记归属")
    void absoluteFileCreatesDirectory()
    {
        File target = tempDir.resolve("download").toFile();
        assertFalse(target.exists());

        String path = new ExcelUtil<>(ExportVo.class).getAbsoluteFile("x.xlsx");
        assertTrue(path.startsWith(tempDir.toString()));

        ExcelUtil<ExportVo> util = new ExcelUtil<>(ExportVo.class);
        String absolute = util.getAbsoluteFile("y.xlsx");
        assertTrue(new File(absolute).getParentFile().exists());
    }

    @Test
    @DisplayName("exportExcel：底层写盘失败时要转成 UtilException，不能静默返回成功")
    void exportFailureIsTranslated() throws Exception
    {
        // 把 download 建成一个「文件」而不是目录，mkdirs 会失败，FileOutputStream 必然打不开
        Path blocked = tempDir.resolve("blocked");
        Files.createDirectories(blocked);
        Files.createFile(blocked.resolve("download"));
        new RuoYiConfig().setProfile(blocked.toString());
        try
        {
            ExcelUtil<ExportVo> util = new ExcelUtil<>(ExportVo.class);
            assertThrows(com.yu.common.exception.UtilException.class,
                    () -> util.exportExcel(List.of(sample()), "坏路径", ""));
        }
        finally
        {
            new RuoYiConfig().setProfile(tempDir.toString());
        }
    }

    @Test
    @DisplayName("parseDateToStr：同一实例反复调用不带状态（导出多行不会串日期）")
    void parseDateToStrStateless()
    {
        ExcelUtil<ExportVo> util = new ExcelUtil<>(ExportVo.class);
        Date one = DateUtils.parseDate("2020-01-02");
        Date two = DateUtils.parseDate("2021-12-31");
        assertEquals("2020-01-02", util.parseDateToStr("yyyy-MM-dd", one));
        assertEquals("2021-12-31", util.parseDateToStr("yyyy-MM-dd", two));
        assertEquals("2020-01-02", util.parseDateToStr("yyyy-MM-dd", one));
    }

    @Test
    @DisplayName("单元格样式缓存：多行导出不串格式，日期各自渲染")
    void cellStyleCachePerFormat()
    {
        ExportVo a = sample();
        a.setDetails(new ArrayList<Detail>());
        ExportVo b = sample();
        b.setDetails(new ArrayList<Detail>());
        b.setBirthday(DateUtils.parseDate("1999-09-09"));

        try
        {
            MockHttpServletResponse response = new MockHttpServletResponse();
            new ExcelUtil<>(ExportVo.class).exportExcel(response, Arrays.asList(a, b), "两行", "");
            Sheet sheet = readSheet(response);
            assertEquals("2020-01-02", text(sheet, 2, 2));
            assertEquals("1999-09-09", text(sheet, 3, 2));
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }
}
