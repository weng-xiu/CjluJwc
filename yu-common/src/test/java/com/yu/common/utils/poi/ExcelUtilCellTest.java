package com.yu.common.utils.poi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FormulaError;
import org.apache.poi.ss.usermodel.PictureData;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.yu.common.utils.DateUtils;

/**
 * G5：{@link ExcelUtil} 里「不碰 IO 也能验证」的那部分契约。
 *
 * <p>这一层是导出/导入两条主链路的地基：值翻译（{@code convertByExp} 家族）、
 * 日期归一（{@code parseDateToStr}）、单元格读取（{@code getCellValue}）、
 * 图片类型判定。它们都是静态或纯函数，一旦语义漂移，导入导出的错误会以
 * 「某列莫名其妙空了」的形式出现，而不是抛异常——所以必须逐条钉住。</p>
 */
class ExcelUtilCellTest
{
    // ---------------------------------------------------------------- 值翻译

    @Test
    @DisplayName("convertByExp：单值命中直接返回，未命中返回空串")
    void convertByExpSingleValue()
    {
        String exp = "0=男,1=女,2=未知";
        assertEquals("男", ExcelUtil.convertByExp("0", exp, ","));
        assertEquals("女", ExcelUtil.convertByExp("1", exp, ","));
        assertEquals("", ExcelUtil.convertByExp("9", exp, ","), "未登记的码值不能原样吐出");
        assertEquals("", ExcelUtil.convertByExp("", exp, ","));
    }

    @Test
    @DisplayName("convertByExp：带分隔符的多值逐段翻译，尾部不留分隔符")
    void convertByExpMultiValue()
    {
        String exp = "0=男,1=女";
        assertEquals("男,女", ExcelUtil.convertByExp("0,1", exp, ","));
        assertEquals("男", ExcelUtil.convertByExp("0,9", exp, ","), "未命中的段被丢弃，不能残留空段");
        assertEquals("", ExcelUtil.convertByExp("8,9", exp, ","));
    }

    @Test
    @DisplayName("reverseByExp：按标签反查码值，与 convertByExp 互为逆运算")
    void reverseByExp()
    {
        String exp = "0=男,1=女,2=未知";
        assertEquals("0", ExcelUtil.reverseByExp("男", exp, ","));
        assertEquals("2", ExcelUtil.reverseByExp("未知", exp, ","));
        assertEquals("", ExcelUtil.reverseByExp("外星人", exp, ","));
        assertEquals("0,1", ExcelUtil.reverseByExp("男,女", exp, ","));
        assertEquals("1", ExcelUtil.reverseByExp("女,外星人", exp, ","));

        String raw = "0,1";
        assertEquals(raw, ExcelUtil.reverseByExp(ExcelUtil.convertByExp(raw, exp, ","), exp, ","),
                "码值 → 标签 → 码值应回到原值");
    }

    @Test
    @DisplayName("convertByExp：自定义分隔符（分隔符不复用逗号）")
    void convertByExpCustomSeparator()
    {
        assertEquals("男|女", ExcelUtil.convertByExp("0|1", "0=男,1=女", "|"));
    }

    // ---------------------------------------------------------------- 日期归一

    @Test
    @DisplayName("parseDateToStr：null 返回空串，其余按类型各自格式化")
    void parseDateToStr()
    {
        ExcelUtil<Object> util = new ExcelUtil<>(Object.class);
        assertEquals("", util.parseDateToStr("yyyy-MM-dd", null));

        Date date = DateUtils.parseDate("2020-01-02");
        assertEquals("2020-01-02", util.parseDateToStr("yyyy-MM-dd", date));

        LocalDateTime dateTime = LocalDateTime.of(2021, 3, 4, 5, 6, 7);
        assertEquals("2021-03-04 05:06:07", util.parseDateToStr("yyyy-MM-dd HH:mm:ss", dateTime));

        LocalDate localDate = LocalDate.of(2022, 7, 8);
        assertEquals("2022-07-08", util.parseDateToStr("yyyy-MM-dd", localDate));

        assertEquals("原样", util.parseDateToStr("yyyy-MM-dd", "原样"),
                "非日期对象不能走格式化，直接 toString");
    }

    // ---------------------------------------------------------------- 文件名

    @Test
    @DisplayName("encodingFilename：UUID 前缀 + 原名 + .xlsx")
    void encodingFilename()
    {
        String name = new ExcelUtil<>(ExcelUtilFixtures.ExportVo.class).encodingFilename("成绩表");
        assertTrue(name.endsWith("_成绩表.xlsx"), name);
        assertEquals(36 + 1 + 3 + 5, name.length(), "UUID(36) + 下划线 + 名字 + 扩展名");
    }

    // ---------------------------------------------------------------- 图片类型

    @Test
    @DisplayName("getImageType：按文件头识别 PNG/JPG/GIF/BMP，认不出按 JPEG 兜底")
    void getImageType()
    {
        ExcelUtil<?> util = new ExcelUtil<>(ExcelUtilFixtures.ExportVo.class);

        byte[] png = new byte[] { (byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10, 0, 0, 0, 13 };
        assertEquals(Workbook.PICTURE_TYPE_PNG, util.getImageType(png));

        byte[] jpg = new byte[] { (byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 16, 'J', 'F', 'I', 'F', 0, 1 };
        assertEquals(Workbook.PICTURE_TYPE_JPEG, util.getImageType(jpg));

        byte[] gif = new byte[] { 'G', 'I', 'F', '8', '9', 'a', 0, 0, 0, 0, 0, 0 };
        assertEquals(Workbook.PICTURE_TYPE_JPEG, util.getImageType(gif), "GIF 没有对应的 POI 常量，按 JPEG 兜底");

        byte[] bmp = new byte[] { 'B', 'M', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0 };
        assertEquals(Workbook.PICTURE_TYPE_JPEG, util.getImageType(bmp));

        byte[] unknown = new byte[12];
        assertEquals(Workbook.PICTURE_TYPE_JPEG, util.getImageType(unknown));
    }

    @Test
    @DisplayName("getImageType：null 或过短字节数组不能抛越界/空指针")
    void getImageTypeShortOrNull()
    {
        ExcelUtil<?> util = new ExcelUtil<>(ExcelUtilFixtures.ExportVo.class);
        assertEquals(Workbook.PICTURE_TYPE_JPEG, util.getImageType(null),
                "图片读取失败时 value 就是 null，此处若 NPE 会被 addCell 的 catch 吞成「整列空白」");
        assertEquals(Workbook.PICTURE_TYPE_JPEG, util.getImageType(new byte[] { 1, 2 }));
        assertEquals(Workbook.PICTURE_TYPE_JPEG, util.getImageType(new byte[] { 1 }),
                "长度为 1 时任何魔数分支都不能越界");
        assertEquals(Workbook.PICTURE_TYPE_JPEG, util.getImageType(new byte[] { 1, 2, 3 }),
                "PNG 分支要看第 4 个字节，长度不足时必须短路");
    }

    // ---------------------------------------------------------------- 单元格读取

    private static Row newRow(Workbook wb, String sheetName)
    {
        Sheet sheet = wb.createSheet(sheetName);
        return sheet.createRow(0);
    }

    @Test
    @DisplayName("getCellValue：null 行原样返回 null，空单元格返回空串")
    void getCellValueNullRow()
    {
        try (Workbook wb = new XSSFWorkbook())
        {
            ExcelUtil<Object> util = new ExcelUtil<>(Object.class);
            assertNull(util.getCellValue(null, 0));

            Row row = newRow(wb, "s");
            row.createCell(0);
            assertEquals("", util.getCellValue(row, 0));
            assertEquals("", util.getCellValue(row, 5), "不存在的列也返回空串");
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("getCellValue：整数去掉小数点、小数保留为 BigDecimal、日期还原为 Date")
    void getCellValueNumeric()
    {
        try (Workbook wb = new XSSFWorkbook())
        {
            ExcelUtil<Object> util = new ExcelUtil<>(Object.class);
            Row row = newRow(wb, "s");

            row.createCell(0).setCellValue(5.0);
            assertEquals("5", util.getCellValue(row, 0), "整数值不能导出成 5.0");

            row.createCell(1).setCellValue(1.5);
            Object decimal = util.getCellValue(row, 1);
            assertInstanceOf(BigDecimal.class, decimal);
            assertEquals(new BigDecimal("1.5"), decimal);

            Cell dateCell = row.createCell(2);
            dateCell.setCellValue(DateUtils.parseDate("2020-01-02"));
            CellStyle style = wb.createCellStyle();
            style.setDataFormat(wb.createDataFormat().getFormat("yyyy-MM-dd"));
            dateCell.setCellStyle(style);
            Object dateValue = util.getCellValue(row, 2);
            assertInstanceOf(Date.class, dateValue);
            assertEquals("2020-01-02", DateUtils.parseDateToStr("yyyy-MM-dd", (Date) dateValue));
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("getCellValue：字符串/布尔/错误值各走各的分支")
    void getCellValueOtherTypes()
    {
        try (Workbook wb = new XSSFWorkbook())
        {
            ExcelUtil<Object> util = new ExcelUtil<>(Object.class);
            Row row = newRow(wb, "s");

            row.createCell(0).setCellValue("abc");
            assertEquals("abc", util.getCellValue(row, 0));

            row.createCell(1).setCellValue(true);
            assertEquals(Boolean.TRUE, util.getCellValue(row, 1));

            row.createCell(2).setCellErrorValue(FormulaError.DIV0.getCode());
            assertEquals(FormulaError.DIV0.getCode(), util.getCellValue(row, 2));
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }

    // ---------------------------------------------------------------- 图片索引表

    @Test
    @DisplayName("getSheetPictures07 / 03：没有图片时返回空表而不是 null")
    void getSheetPicturesEmpty()
    {
        try (XSSFWorkbook xwb = new XSSFWorkbook())
        {
            Map<String, List<PictureData>> pictures = ExcelUtil.getSheetPictures07(
                    (org.apache.poi.xssf.usermodel.XSSFSheet) xwb.createSheet("s"), xwb);
            assertTrue(pictures.isEmpty());
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }

        try (HSSFWorkbook hwb = new HSSFWorkbook())
        {
            Map<String, List<PictureData>> pictures = ExcelUtil.getSheetPictures03(
                    (org.apache.poi.hssf.usermodel.HSSFSheet) hwb.createSheet("s"), hwb);
            assertTrue(pictures.isEmpty());
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }

    @Test
    @DisplayName("getDrawingPatriarch：首次调用创建画布，重复调用复用同一实例")
    void getDrawingPatriarch()
    {
        try (XSSFWorkbook wb = new XSSFWorkbook())
        {
            Sheet sheet = wb.createSheet("s");
            assertNull(sheet.getDrawingPatriarch());
            Object first = ExcelUtil.getDrawingPatriarch(sheet);
            Object second = ExcelUtil.getDrawingPatriarch(sheet);
            assertTrue(first != null);
            assertTrue(first == second, "第二次必须复用已建画布，否则图片会挂到互相不认识的 drawing 上");
            assertFalse(sheet.getDrawingPatriarch() == null);
        }
        catch (Exception e)
        {
            throw new IllegalStateException(e);
        }
    }
}
