package com.yu.common.utils.poi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.Date;
import java.util.List;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFClientAnchor;
import org.apache.poi.xssf.usermodel.XSSFDrawing;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.alibaba.fastjson2.JSONArray;
import com.yu.common.config.RuoYiConfig;
import com.yu.common.core.domain.entity.SysDictData;
import com.yu.common.core.redis.RedisCache;
import com.yu.common.exception.UtilException;
import com.yu.common.utils.DateUtils;
import com.yu.common.utils.poi.ExcelUtilFixtures.ImageVo;
import com.yu.common.utils.poi.ExcelUtilFixtures.ImportVo;

/**
 * G5：{@link ExcelUtil} 导入链路的契约测试（此前该类整块零覆盖）。
 *
 * <p>导入与导出方向相反但最容易出错的地方恰恰是「同名不同义」：<b>表头文字</b>才是列的主键，
 * 列的位置无关；日期既可能是 Excel 原生日期格，也可能是一段文本；数值既可能是 5.0 也可能是 "5"。
 * 因此这里全部用真实 POI 生成输入流（xlsx 与 xls 各一套），只断言读回来的实体字段。</p>
 *
 * <p>字典列需要 Spring 上下文（{@code DictUtils} 走 Redis 缓存），用 {@code beanFactory} 的
 * mock 顶住；完成后必须原样还原静态字段，否则会污染其他测试类。</p>
 */
class ExcelUtilImportTest
{
    private static Field beanFactoryField;

    private static Object previousBeanFactory;

    @TempDir
    Path tempDir;

    private String previousProfile;

    @BeforeAll
    static void installDictCache() throws Exception
    {
        beanFactoryField = com.yu.common.utils.spring.SpringUtils.class.getDeclaredField("beanFactory");
        beanFactoryField.setAccessible(true);
        previousBeanFactory = beanFactoryField.get(null);

        SysDictData enabled = new SysDictData();
        enabled.setDictType("sys_status");
        enabled.setDictLabel("启用");
        enabled.setDictValue("1");
        SysDictData disabled = new SysDictData();
        disabled.setDictType("sys_status");
        disabled.setDictLabel("停用");
        disabled.setDictValue("0");

        ConfigurableListableBeanFactory factory = mock(ConfigurableListableBeanFactory.class);
        RedisCache redisCache = mock(RedisCache.class);
        when(factory.getBean(RedisCache.class)).thenReturn(redisCache);
        JSONArray cached = JSONArray.from(List.of(enabled, disabled));
        doReturn(cached).when(redisCache).getCacheObject(anyString());

        beanFactoryField.set(null, factory);
    }

    @AfterAll
    static void restoreDictCache() throws Exception
    {
        beanFactoryField.set(null, previousBeanFactory);
    }

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

    // ---------------------------------------------------------------- 构造输入

    private static void header(Sheet sheet, int rowIndex, String... names)
    {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < names.length; i++)
        {
            row.createCell(i).setCellValue(names[i]);
        }
    }

    private static Cell dateCell(Row row, int column, String ymd)
    {
        Cell cell = row.createCell(column);
        cell.setCellValue(DateUtils.parseDate(ymd));
        Workbook wb = row.getSheet().getWorkbook();
        CellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.createDataFormat().getFormat("yyyy-MM-dd"));
        cell.setCellStyle(style);
        return cell;
    }

    private static InputStream toStream(Workbook wb) throws IOException
    {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        return new ByteArrayInputStream(out.toByteArray());
    }

    private static final String[] IMPORT_HEAD =
            { "姓名", "数量", "总量", "比率", "比例", "金额", "生日", "启用", "性别", "字典", "日期文本", "文本整数" };

    private static Row fullRow(Sheet sheet, int rowIndex)
    {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue("张三");
        row.createCell(1).setCellValue(5.0);
        row.createCell(2).setCellValue(9.0);
        row.createCell(3).setCellValue(1.25);
        row.createCell(4).setCellValue(0.5);
        row.createCell(5).setCellValue(12.34);
        dateCell(row, 6, "2020-01-02");
        row.createCell(7).setCellValue(true);
        row.createCell(8).setCellValue("男");
        row.createCell(9).setCellValue("启用");
        dateCell(row, 10, "2021-03-04");
        row.createCell(11).setCellValue("123.0");
        return row;
    }

    // ---------------------------------------------------------------- 主链路

    @Test
    @DisplayName("importExcel：xlsx 八种字段类型各走各的转换分支")
    void importXlsxMixedTypes() throws Exception
    {
        List<ImportVo> list;
        try (XSSFWorkbook wb = new XSSFWorkbook())
        {
            Sheet sheet = wb.createSheet("成绩");
            header(sheet, 0, IMPORT_HEAD);
            fullRow(sheet, 1);
            list = new ExcelUtil<>(ImportVo.class).importExcel(toStream(wb), 0);
        }

        assertEquals(1, list.size());
        ImportVo vo = list.get(0);
        assertEquals("张三", vo.getName());
        assertEquals(Integer.valueOf(5), vo.getCount());
        assertEquals(Long.valueOf(9L), vo.getTotal());
        assertEquals(1.25d, vo.getRate(), 0.0001);
        assertEquals(0.5f, vo.getRatio(), 0.0001f);
        assertEquals(new BigDecimal("12.34"), vo.getMoney());
        assertInstanceOf(Date.class, vo.getBirthday());
        assertEquals("2020-01-02", DateUtils.parseDateToStr("yyyy-MM-dd", vo.getBirthday()));
        assertEquals(Boolean.TRUE, vo.getEnabled());
        assertEquals("0", vo.getSex(), "readConverterExp 反向：标签「男」→ 码值 0");
        assertEquals("1", vo.getDictLabel(), "dictType 走字典缓存反向取值");
        assertEquals("2021年03月04日", vo.getDateText(), "String 字段 + dateFormat：Excel 日期格被格式化");
        assertEquals("123", vo.getTextInteger(), "文本形式的 123.0 要剥掉小数点，不能原样灌进字符串字段");
    }

    @Test
    @DisplayName("importExcel：xls（HSSF）与 xlsx 走同一条转换逻辑")
    void importXlsMixedTypes() throws Exception
    {
        List<ImportVo> list;
        try (HSSFWorkbook wb = new HSSFWorkbook())
        {
            Sheet sheet = wb.createSheet("成绩");
            header(sheet, 0, IMPORT_HEAD);
            fullRow(sheet, 1);
            list = new ExcelUtil<>(ImportVo.class).importExcel(toStream(wb), 0);
        }

        assertEquals(1, list.size());
        assertEquals("张三", list.get(0).getName());
        assertEquals(Integer.valueOf(5), list.get(0).getCount());
        assertEquals("1", list.get(0).getDictLabel());
    }

    @Test
    @DisplayName("importExcel：列顺序无关，按表头文字匹配；全空行跳过")
    void importMatchesByHeaderText() throws Exception
    {
        List<ImportVo> list;
        try (XSSFWorkbook wb = new XSSFWorkbook())
        {
            Sheet sheet = wb.createSheet("乱序");
            // 故意把「姓名」放到第 3 列，并插入一列实体里没有的「备注」
            Row head = sheet.createRow(0);
            head.createCell(0).setCellValue("备注");
            head.createCell(1).setCellValue("数量");
            head.createCell(2).setCellValue("姓名");

            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("忽略我");
            data.createCell(1).setCellValue(7.0);
            data.createCell(2).setCellValue("李四");

            sheet.createRow(2); // 全空行

            Row third = sheet.createRow(3);
            third.createCell(1).setCellValue(8.0);
            third.createCell(2).setCellValue("王五");

            list = new ExcelUtil<>(ImportVo.class).importExcel(toStream(wb), 0);
        }

        assertEquals(2, list.size(), "空行不能变成一条 null 记录");
        assertEquals("李四", list.get(0).getName());
        assertEquals(Integer.valueOf(7), list.get(0).getCount());
        assertEquals("王五", list.get(1).getName());
    }

    @Test
    @DisplayName("importExcel：titleNum 跳过顶部标题行，表头从指定行开始")
    void importWithTitleRows() throws Exception
    {
        List<ImportVo> list;
        try (XSSFWorkbook wb = new XSSFWorkbook())
        {
            Sheet sheet = wb.createSheet("带标题");
            sheet.createRow(0).createCell(0).setCellValue("成绩总表");
            header(sheet, 1, IMPORT_HEAD);
            fullRow(sheet, 2);
            list = new ExcelUtil<>(ImportVo.class).importExcel(toStream(wb), 1);
        }

        assertEquals(1, list.size());
        assertEquals("张三", list.get(0).getName());
    }

    @Test
    @DisplayName("importExcel：单参版本等价于 titleNum=0，且会关闭输入流")
    void importDefaultOverload() throws Exception
    {
        byte[] bytes;
        try (XSSFWorkbook wb = new XSSFWorkbook())
        {
            Sheet sheet = wb.createSheet("默认");
            header(sheet, 0, IMPORT_HEAD);
            fullRow(sheet, 1);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            bytes = out.toByteArray();
        }

        InputStream is = new ByteArrayInputStream(bytes);
        List<ImportVo> list = new ExcelUtil<>(ImportVo.class).importExcel(is);
        assertEquals(1, list.size());
    }

    // ---------------------------------------------------------------- 异常与空表

    @Test
    @DisplayName("importExcel：指定的 sheet 不存在时抛 IOException，而不是静默返回空表")
    void importMissingSheet() throws Exception
    {
        byte[] bytes;
        try (XSSFWorkbook wb = new XSSFWorkbook())
        {
            wb.createSheet("真名");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            bytes = out.toByteArray();
        }

        try (InputStream is = new ByteArrayInputStream(bytes))
        {
            ExcelUtil<ImportVo> util = new ExcelUtil<>(ImportVo.class);
            IOException error = assertThrows(IOException.class, () -> util.importExcel("假名", is, 0));
            assertEquals("文件sheet不存在", error.getMessage());
        }
    }

    @Test
    @DisplayName("importExcel：表头行缺失时明确报错，不能当成空数据")
    void importMissingHeader() throws Exception
    {
        byte[] bytes;
        try (XSSFWorkbook wb = new XSSFWorkbook())
        {
            Sheet sheet = wb.createSheet("无表头");
            sheet.createRow(1).createCell(0).setCellValue("只有数据没有表头");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            bytes = out.toByteArray();
        }

        try (InputStream is = new ByteArrayInputStream(bytes))
        {
            ExcelUtil<ImportVo> util = new ExcelUtil<>(ImportVo.class);
            assertThrows(UtilException.class, () -> util.importExcel(is, 0));
        }
    }

    @Test
    @DisplayName("importExcel：空 sheet 返回空集合（不抛异常）")
    void importEmptySheet() throws Exception
    {
        byte[] bytes;
        try (XSSFWorkbook wb = new XSSFWorkbook())
        {
            wb.createSheet("空表");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            bytes = out.toByteArray();
        }

        try (InputStream is = new ByteArrayInputStream(bytes))
        {
            List<ImportVo> list = new ExcelUtil<>(ImportVo.class).importExcel(is, 0);
            assertTrue(list.isEmpty());
        }
    }

    @Test
    @DisplayName("importExcel：表头与实体全不匹配时不产生实体，列表里只有 null 占位")
    void importHeaderWithoutMatch() throws Exception
    {
        byte[] bytes;
        try (XSSFWorkbook wb = new XSSFWorkbook())
        {
            Sheet sheet = wb.createSheet("无关列");
            header(sheet, 0, "甲", "乙");
            sheet.createRow(1).createCell(0).setCellValue("x");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            bytes = out.toByteArray();
        }

        try (InputStream is = new ByteArrayInputStream(bytes))
        {
            List<ImportVo> list = new ExcelUtil<>(ImportVo.class).importExcel(is, 0);
            assertEquals(1, list.size());
            assertNull(list.get(0), "没有任何列命中，实体不该被创建");
        }
    }

    // ---------------------------------------------------------------- 图片列

    @Test
    @DisplayName("importExcel：图片列的图片落盘为文件路径串，多张用逗号拼接")
    void importImageColumn() throws Exception
    {
        byte[] png = new byte[] { (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13, 0, 0, 0, 0 };

        byte[] bytes;
        try (XSSFWorkbook wb = new XSSFWorkbook())
        {
            Sheet sheet = wb.createSheet("照片");
            header(sheet, 0, "姓名", "照片");
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("张三");
            data.createCell(1);

            int pictureIndex = wb.addPicture(png, Workbook.PICTURE_TYPE_PNG);
            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            drawing.createPicture(new XSSFClientAnchor(0, 0, 0, 0, 1, 1, 2, 2), pictureIndex);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            bytes = out.toByteArray();
        }

        try (InputStream is = new ByteArrayInputStream(bytes))
        {
            List<ImageVo> list = new ExcelUtil<>(ImageVo.class).importExcel(is, 0);
            assertEquals(1, list.size());
            assertEquals("张三", list.get(0).getName());
            assertNotNull(list.get(0).getPhoto(), "图片列的图片必须落盘并回写路径");
            assertTrue(list.get(0).getPhoto().endsWith(".png"), list.get(0).getPhoto());
        }
    }

    @Test
    @DisplayName("importExcel：同一 sheet 里有的行有图、有的行没图，不能因缺图整批失败")
    void importImageColumnWithoutPicture() throws Exception
    {
        byte[] png = new byte[] { (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13, 0, 0, 0, 0 };

        byte[] bytes;
        try (XSSFWorkbook wb = new XSSFWorkbook())
        {
            Sheet sheet = wb.createSheet("照片");
            header(sheet, 0, "姓名", "照片");
            Row first = sheet.createRow(1);
            first.createCell(0).setCellValue("张三");
            first.createCell(1);
            Row second = sheet.createRow(2);
            second.createCell(0).setCellValue("李四");
            second.createCell(1).setCellValue("无图");

            // 只给第 1 行配图：pictures 非空，但第 2 行的 key 查不到
            int pictureIndex = wb.addPicture(png, Workbook.PICTURE_TYPE_PNG);
            XSSFDrawing drawing = (XSSFDrawing) sheet.createDrawingPatriarch();
            drawing.createPicture(new XSSFClientAnchor(0, 0, 0, 0, 1, 1, 2, 2), pictureIndex);

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            bytes = out.toByteArray();
        }

        try (InputStream is = new ByteArrayInputStream(bytes))
        {
            List<ImageVo> list = new ExcelUtil<>(ImageVo.class).importExcel(is, 0);
            assertEquals(2, list.size(), "缺图的那一行不能让整批导入抛 NullPointerException");
            assertEquals("张三", list.get(0).getName());
            assertNotNull(list.get(0).getPhoto());
            assertEquals("李四", list.get(1).getName());
            assertNull(list.get(1).getPhoto(), "没有图片时图片字段保持为空");
        }
    }
}
