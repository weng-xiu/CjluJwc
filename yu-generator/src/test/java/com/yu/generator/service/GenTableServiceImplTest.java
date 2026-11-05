package com.yu.generator.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.yu.common.constant.GenConstants;
import com.yu.common.exception.ServiceException;
import com.yu.common.exception.UtilException;
import com.yu.common.utils.sql.SqlUtil;
import com.yu.generator.config.GenConfig;
import com.yu.generator.domain.GenTable;
import com.yu.generator.domain.GenTableColumn;
import com.yu.generator.mapper.GenTableColumnMapper;
import com.yu.generator.mapper.GenTableMapper;

/**
 * 代码生成服务测试（G5 覆盖率爬坡 · yu-generator）。
 *
 * <p>本类是**真模板渲染**：{@code previewCode}/{@code downloadCode} 走
 * {@code VelocityInitializer + VelocityUtils + 22 个 .vm}，而不是 mock 掉渲染。
 * 这样做的理由是这个模块的唯一产出就是文档字符串，mock 掉渲染等于什么都没测；
 * 同时它也顺手验证了一个容易被忽略的事实——{@code Velocity.init()} 是进程级单例，
 * 第二次预览不能因为「引擎已初始化」而失败。</p>
 *
 * <p>不连库、不连 Redis：两个 Mapper 全部 mock。审计字段、参数校验、异常转译这些
 * 「服务层自己该负责的事」在这里逐条钉住。</p>
 *
 * @author G5
 */
@ExtendWith(MockitoExtension.class)
class GenTableServiceImplTest
{
    @Mock
    private GenTableMapper genTableMapper;

    @Mock
    private GenTableColumnMapper genTableColumnMapper;

    @InjectMocks
    private GenTableServiceImpl genTableService;

    @BeforeEach
    void setUpGenConfig()
    {
        // importGenTable/initTable 走 GenConfig 静态配置，缺了会 NPE。
        // 字段是 public static、setter 是 @Value 实例方法，直接赋静态字段即可
        GenConfig.author = "yu";
        GenConfig.packageName = "com.yu.system";
        GenConfig.autoRemovePre = true;
        GenConfig.tablePrefix = "sys_";
    }

    @AfterEach
    void resetGenConfig()
    {
        GenConfig.author = null;
        GenConfig.packageName = null;
        GenConfig.autoRemovePre = false;
        GenConfig.tablePrefix = null;
    }

    @Test
    @DisplayName("previewCode：11 个模板全部渲染出实体内容（真 Velocity）")
    void previewCode_rendersAllTemplates()
    {
        GenTable table = table(GenConstants.TPL_CRUD);
        table.setTplWebType("element-plus-typescript");
        when(genTableMapper.selectGenTableById(1L)).thenReturn(table);

        Map<String, String> dataMap = genTableService.previewCode(1L);

        // 5 个 Java + mapper.xml + sql + api.ts + type.ts + index.ts + v3ts/index.vue
        assertEquals(11, dataMap.size());
        assertTrue(dataMap.containsKey("vm/java/domain.java.vm"));

        String domain = dataMap.get("vm/java/domain.java.vm");
        assertTrue(domain.contains("package com.yu.system.domain;"), "domain 必须带 package 行：" + head(domain));
        assertTrue(domain.contains("public class Student extends BaseEntity"), head(domain));
        assertTrue(domain.contains("private String studentName"), head(domain));
        assertTrue(domain.contains("private BigDecimal score"), head(domain));

        assertTrue(dataMap.get("vm/java/controller.java.vm").contains("class StudentController"));
        assertTrue(dataMap.get("vm/java/service.java.vm").contains("interface IStudentService"));
        assertTrue(dataMap.get("vm/java/serviceImpl.java.vm").contains("class StudentServiceImpl"));
        assertTrue(dataMap.get("vm/java/mapper.java.vm").contains("interface StudentMapper"));
        assertTrue(dataMap.get("vm/xml/mapper.xml.vm").contains("namespace"));

        String sql = dataMap.get("vm/sql/sql.vm");
        assertTrue(sql.contains("sys_menu"));
        assertTrue(sql.contains("system:student:list"), "菜单 SQL 的权限前缀必须来自 module/business");
    }

    @Test
    @DisplayName("previewCode：连续两次预览都成功（Velocity 单例不得二次 init 失败）")
    void previewCode_repeatedCalls()
    {
        GenTable table = table(GenConstants.TPL_CRUD);
        when(genTableMapper.selectGenTableById(1L)).thenReturn(table);

        Map<String, String> first = genTableService.previewCode(1L);
        Map<String, String> second = genTableService.previewCode(1L);

        assertEquals(first.keySet(), second.keySet());
        assertEquals(first.get("vm/java/domain.java.vm"), second.get("vm/java/domain.java.vm"));
    }

    @Test
    @DisplayName("previewCode：树表模板渲染出树控件上下文")
    void previewCode_treeTemplate()
    {
        GenTable table = table(GenConstants.TPL_TREE);
        table.setOptions("{\"treeCode\":\"student_id\",\"treeParentCode\":\"parent_id\",\"treeName\":\"student_name\"}");
        when(genTableMapper.selectGenTableById(1L)).thenReturn(table);

        Map<String, String> dataMap = genTableService.previewCode(1L);

        assertTrue(dataMap.containsKey("vm/vue/index-tree.vue.vm"));
        assertFalse(dataMap.containsKey("vm/vue/index.vue.vm"));
    }

    @Test
    @DisplayName("downloadCode：按模板清单写出 ZIP 条目（含菜单 SQL 与前端文件）")
    void downloadCode_writesZipEntries() throws Exception
    {
        GenTable table = table(GenConstants.TPL_CRUD);
        table.setTplWebType("element-plus-typescript");
        when(genTableMapper.selectGenTableByName("sys_student")).thenReturn(table);

        byte[] zipBytes = genTableService.downloadCode(new String[] { "sys_student" });
        Set<String> entries = readZipEntries(zipBytes);

        assertEquals(11, entries.size());
        assertTrue(entries.contains("main/java/com/yu/system/domain/Student.java"));
        assertTrue(entries.contains("main/java/com/yu/system/controller/StudentController.java"));
        assertTrue(entries.contains("main/resources/mapper/system/StudentMapper.xml"));
        assertTrue(entries.contains("studentMenu.sql"));
        assertTrue(entries.contains("vue/views/system/student/index.vue"));
        // element-plus-typescript 才有 index-bak.ts 汇总文件
        assertTrue(entries.contains("vue/types/api/index-bak.ts"));
    }

    @Test
    @DisplayName("validateEdit：树表三个树字段逐个必填")
    void validateEdit_tree()
    {
        GenTable tree = table(GenConstants.TPL_TREE);
        tree.setParams(null);
        ServiceException noCode = assertThrows(ServiceException.class, () -> genTableService.validateEdit(tree));
        assertEquals("树编码字段不能为空", noCode.getMessage());

        tree.getParams().put(GenConstants.TREE_CODE, "dept_id");
        ServiceException noParent = assertThrows(ServiceException.class, () -> genTableService.validateEdit(tree));
        assertEquals("树父编码字段不能为空", noParent.getMessage());

        tree.getParams().put(GenConstants.TREE_PARENT_CODE, "parent_id");
        ServiceException noName = assertThrows(ServiceException.class, () -> genTableService.validateEdit(tree));
        assertEquals("树名称字段不能为空", noName.getMessage());

        tree.getParams().put(GenConstants.TREE_NAME, "dept_name");
        genTableService.validateEdit(tree);
    }

    @Test
    @DisplayName("validateEdit：主子表两个外键字段必填，单表直接放行")
    void validateEdit_subAndCrud()
    {
        GenTable sub = table(GenConstants.TPL_SUB);
        ServiceException noSubTable = assertThrows(ServiceException.class, () -> genTableService.validateEdit(sub));
        assertEquals("关联子表的表名不能为空", noSubTable.getMessage());

        sub.setSubTableName("sys_student_item");
        ServiceException noFk = assertThrows(ServiceException.class, () -> genTableService.validateEdit(sub));
        assertEquals("子表关联的外键名不能为空", noFk.getMessage());

        sub.setSubTableFkName("student_id");
        genTableService.validateEdit(sub);

        genTableService.validateEdit(table(GenConstants.TPL_CRUD));
    }

    @Test
    @DisplayName("setPkColumn：优先 isPk 列，无主键时退回首列（子表同口径）")
    void setPkColumn_fallback()
    {
        GenTable withPk = table(GenConstants.TPL_CRUD);
        genTableService.setPkColumn(withPk);
        assertEquals("student_id", withPk.getPkColumn().getColumnName());

        GenTable withoutPk = table(GenConstants.TPL_CRUD);
        withoutPk.getColumns().forEach(column -> column.setIsPk(null));
        genTableService.setPkColumn(withoutPk);
        // 无 isPk 时回落 columns.get(0)，不做「跳过主键列」之类的二次猜测
        assertEquals("student_id", withoutPk.getPkColumn().getColumnName());

        GenTable sub = table(GenConstants.TPL_SUB);
        GenTable subTable = new GenTable();
        subTable.setColumns(new ArrayList<>(Arrays.asList(
                column("item_name", GenConstants.TYPE_STRING),
                column("item_id", GenConstants.TYPE_LONG))));
        sub.setSubTable(subTable);
        genTableService.setPkColumn(sub);
        assertSame(subTable.getColumns().get(0), subTable.getPkColumn());
    }

    @Test
    @DisplayName("setTableFromOptions：选项回填六个字段；options 为 null 时保持原值")
    void setTableFromOptions_bothBranches()
    {
        GenTable table = table(GenConstants.TPL_TREE);
        table.setOptions("{\"treeCode\":\"dept_id\",\"treeParentCode\":\"parent_id\",\"treeName\":\"dept_name\","
                + "\"parentMenuId\":2000,\"parentMenuName\":\"系统工具\",\"genView\":true}");

        genTableService.setTableFromOptions(table);

        assertEquals("dept_id", table.getTreeCode());
        assertEquals("parent_id", table.getTreeParentCode());
        assertEquals("dept_name", table.getTreeName());
        assertEquals(2000L, table.getParentMenuId());
        assertEquals("系统工具", table.getParentMenuName());
        assertTrue(table.isView());

        GenTable noOptions = table(GenConstants.TPL_CRUD);
        noOptions.setOptions(null);
        genTableService.setTableFromOptions(noOptions);
        assertNull(noOptions.getTreeCode());
        assertNull(noOptions.getParentMenuId());
    }

    @Test
    @DisplayName("selectGenTableById：查回的表必须带上 options 里解析出的树/视图信息")
    void selectGenTableById_parsesOptions()
    {
        GenTable table = table(GenConstants.TPL_TREE);
        table.setOptions("{\"treeCode\":\"dept_id\",\"parentMenuId\":2000}");
        when(genTableMapper.selectGenTableById(1L)).thenReturn(table);

        GenTable result = genTableService.selectGenTableById(1L);

        assertEquals("dept_id", result.getTreeCode());
        assertEquals(2000L, result.getParentMenuId());
    }

    @Test
    @DisplayName("getGenPath：genPath 为 / 时落到 ${user.dir}/src，否则拼到自定义目录")
    void getGenPath_bothBranches()
    {
        GenTable table = table(GenConstants.TPL_CRUD);
        table.setGenPath("/");
        String projectPath = GenTableServiceImpl.getGenPath(table, "vm/java/domain.java.vm");
        assertEquals(System.getProperty("user.dir") + File.separator + "src" + File.separator
                + "main/java/com/yu/system/domain/Student.java", projectPath);

        table.setGenPath("/tmp/gen-out");
        assertEquals("/tmp/gen-out" + File.separator + "main/java/com/yu/system/domain/Student.java",
                GenTableServiceImpl.getGenPath(table, "vm/java/domain.java.vm"));
    }

    @Test
    @DisplayName("createTable：非 CREATE TABLE 语句在服务层就被挡下（S-7 第二道闸）")
    void createTable_rejectsNonCreateStatement()
    {
        UtilException exception = assertThrows(UtilException.class,
                () -> genTableService.createTable("drop table sys_student"));
        assertEquals("仅允许执行 CREATE TABLE 语句", exception.getMessage());

        assertThrows(UtilException.class, () -> genTableService.createTable(null));
    }

    @Test
    @DisplayName("createTable：合法语句透传 Mapper，返回「影响行数为 0」的建表语义")
    void createTable_passesThrough()
    {
        String sql = "CREATE TABLE demo_probe (id int primary key)";
        when(genTableMapper.createTable(sql)).thenReturn(0);

        assertTrue(genTableService.createTable(sql));
        verify(genTableMapper).createTable(sql);
    }

    @Test
    @DisplayName("importGenTable：逐列初始化后落库（含 javaField / 表 id 归属）")
    void importGenTable_initializesColumns()
    {
        GenTable table = new GenTable();
        table.setTableName("sys_student");
        table.setTableComment("学生信息表");
        when(genTableMapper.insertGenTable(any(GenTable.class))).thenReturn(1);
        when(genTableColumnMapper.selectDbTableColumnsByName("sys_student"))
                .thenReturn(new ArrayList<>(Arrays.asList(column("student_name", "varchar(64)"),
                        column("score", "decimal(10,2)"))));

        genTableService.importGenTable(new ArrayList<>(Arrays.asList(table)), "element-plus", "admin");

        assertEquals("Student", table.getClassName());
        assertEquals("element-plus", table.getTplWebType());
        ArgumentCaptor<GenTableColumn> captor = ArgumentCaptor.forClass(GenTableColumn.class);
        verify(genTableColumnMapper, times(2)).insertGenTableColumn(captor.capture());
        List<GenTableColumn> saved = captor.getAllValues();
        assertEquals("studentName", saved.get(0).getJavaField());
        assertEquals(GenConstants.TYPE_BIGDECIMAL, saved.get(1).getJavaType());
        assertEquals(table.getTableId(), saved.get(0).getTableId());
    }

    @Test
    @DisplayName("importGenTable：底层异常统一转 ServiceException（带原始原因）")
    void importGenTable_wrapsException()
    {
        GenTable table = new GenTable();
        table.setTableName("sys_student");
        table.setTableComment("学生信息表");
        when(genTableMapper.insertGenTable(any(GenTable.class))).thenReturn(1);
        when(genTableColumnMapper.selectDbTableColumnsByName(anyString()))
                .thenThrow(new IllegalStateException("Table 'sys_student' doesn't exist"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> genTableService.importGenTable(new ArrayList<>(Arrays.asList(table)), "element-plus", "admin"));
        assertTrue(exception.getMessage().startsWith("导入失败："));
        assertTrue(exception.getMessage().contains("doesn't exist"));
    }

    @Test
    @DisplayName("synchDb：表结构不存在时报错；库表列按名匹配则保留字典与查询方式")
    void synchDb_matchesByColumnName()
    {
        GenTable table = table(GenConstants.TPL_CRUD);
        when(genTableMapper.selectGenTableByName("sys_student")).thenReturn(table);
        when(genTableColumnMapper.selectDbTableColumnsByName("sys_student")).thenReturn(new ArrayList<>());

        ServiceException empty = assertThrows(ServiceException.class, () -> genTableService.synchDb("sys_student"));
        assertEquals("同步数据失败，原表结构不存在", empty.getMessage());

        // 第二次：库里的列与已保存列同名 → 走 update 分支并保留 dictType
        GenTableColumn dbColumn = column("status", "char(1)");
        when(genTableColumnMapper.selectDbTableColumnsByName("sys_student"))
                .thenReturn(new ArrayList<>(Arrays.asList(dbColumn)));
        genTableService.synchDb("sys_student");
        verify(genTableColumnMapper).updateGenTableColumn(dbColumn);
        assertEquals("sys_normal_disable", dbColumn.getDictType());
    }

    @Test
    @DisplayName("SqlUtil 断言可直接复用：空串与非建表语句都被拒")
    void sqlUtilAssertion()
    {
        assertThrows(UtilException.class, () -> SqlUtil.assertCreateTableStatement(""));
        assertThrows(UtilException.class, () -> SqlUtil.assertCreateTableStatement("select 1"));
        SqlUtil.assertCreateTableStatement("create table t (id int)");
    }

    /**
     * 单表 fixture：与 VelocityUtilsTest 同形，但这里要能走完整渲染链路
     */
    private static GenTable table(String tplCategory)
    {
        GenTable table = new GenTable();
        table.setTableId(1L);
        table.setTableName("sys_student");
        table.setTableComment("学生信息表");
        table.setClassName("Student");
        table.setPackageName("com.yu.system");
        table.setModuleName("system");
        table.setBusinessName("student");
        table.setFunctionName("学生信息");
        table.setFunctionAuthor("yu");
        table.setTplCategory(tplCategory);
        table.setFormColNum(2);
        table.setCreateBy("admin");

        GenTableColumn pk = column("student_id", "bigint(20)");
        pk.setIsPk("1");
        GenTableColumn name = column("student_name", "varchar(64)");
        name.setIsList(GenConstants.REQUIRE);
        name.setHtmlType(GenConstants.HTML_INPUT);
        name.setQueryType(GenConstants.QUERY_LIKE);
        GenTableColumn score = column("score", "decimal(10,2)");
        // 库里真实的小数列 javaType 是 BigDecimal（initColumnField 算的），
        // fixture 得同形，否则渲染断言测的是 String 而不是真实类型
        score.setJavaType(GenConstants.TYPE_BIGDECIMAL);
        GenTableColumn status = column("status", "char(1)");
        status.setIsList(GenConstants.REQUIRE);
        status.setHtmlType(GenConstants.HTML_RADIO);
        status.setDictType("sys_normal_disable");
        table.setColumns(new ArrayList<>(Arrays.asList(pk, name, score, status)));
        table.setPkColumn(pk);
        return table;
    }

    private static GenTableColumn column(String columnName, String columnType)
    {
        GenTableColumn column = new GenTableColumn();
        column.setTableId(1L);
        column.setColumnName(columnName);
        column.setColumnType(columnType);
        column.setJavaField(com.yu.common.utils.StringUtils.toCamelCase(columnName));
        column.setJavaType(GenConstants.TYPE_STRING);
        column.setIsInsert(GenConstants.REQUIRE);
        return column;
    }

    private static Set<String> readZipEntries(byte[] zipBytes) throws Exception
    {
        Set<String> entries = new HashSet<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipBytes)))
        {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null)
            {
                entries.add(entry.getName());
            }
        }
        return entries;
    }

    private static String head(String content)
    {
        int end = Math.min(content.length(), 400);
        return content.substring(0, end);
    }
}
