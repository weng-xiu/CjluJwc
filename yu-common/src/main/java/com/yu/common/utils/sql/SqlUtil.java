package com.yu.common.utils.sql;

import com.yu.common.exception.UtilException;
import com.yu.common.utils.StringUtils;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * sql操作工具类
 * 
 * @author ruoyi
 */
public class SqlUtil
{
    /**
     * 定义常用的 sql关键字
     */
    public static String SQL_REGEX = "\u000B|%0A|and |extractvalue|updatexml|sleep|information_schema|exec |insert |select |delete |update |drop |count |chr |mid |master |truncate |char |declare |or |union |like |+|/*|user()";

    /**
     * 仅支持字母、数字、下划线、空格、逗号、小数点（支持多个字段排序）
     */
    public static String SQL_PATTERN = "[a-zA-Z0-9_\\ \\,\\.]+";

    /**
     * 限制orderBy最大长度
     */
    private static final int ORDER_BY_MAX_LENGTH = 500;

    /**
     * 合法 SQL 标识符（表名/列名）：字母或下划线开头，仅含字母数字下划线。
     * 故意比 MySQL 允许的范围窄：反引号、点号、空格、引号一律不接，因为它们的唯一用途
     * 就是把「一个名字」变成「一段 SQL」。
     */
    private static final Pattern IDENTIFIER_PATTERN = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*");

    /** 标识符最大长度（MySQL 表/列名上限 64） */
    private static final int IDENTIFIER_MAX_LENGTH = 64;

    /**
     * 检查字符，防止注入绕过
     */
    public static String escapeOrderBySql(String value)
    {
        if (StringUtils.isNotEmpty(value) && !isValidOrderBySql(value))
        {
            throw new UtilException("参数不符合规范，不能进行查询");
        }
        if (StringUtils.length(value) > ORDER_BY_MAX_LENGTH)
        {
            throw new UtilException("参数已超过最大限制，不能进行查询");
        }
        return value;
    }

    /**
     * S-7：校验用作 ${} 动态片段的值是「一个标识符」而不是「一段 SQL」。
     *
     * 为什么需要它：SyncPersistenceMapper 的 ${table}/${col}、SamProcedureItemMapper 的 ${column}
     * 都是表名/列名直拼。原先只有「构造该值的那一层」做白名单（配置解析、枚举映射），
     * 属于跨层隐式契约：任何人新写一个调用方就继承不到保护。把断言放在绑定前的最后一步，
     * 才是本层可验证的。
     *
     * @param identifier 待校验值；空值直接放行（由调用方决定是否必填，如 CARD 分支不用列名）
     * @param what 用于报错定位的字段语义（如“同步目标表名”）
     * @return 原值，便于链式使用
     */
    public static String assertIdentifier(String identifier, String what)
    {
        if (StringUtils.isEmpty(identifier))
        {
            return identifier;
        }
        if (identifier.length() > IDENTIFIER_MAX_LENGTH || !IDENTIFIER_PATTERN.matcher(identifier).matches())
        {
            throw new UtilException("非法的" + what + "（只允许字母/数字/下划线开头标识符）：" + identifier);
        }
        return identifier;
    }

    /**
     * 批量版 {@link #assertIdentifier(String, String)}；null 集合或 null 元素一律放行。
     */
    public static void assertIdentifiers(Collection<String> identifiers, String what)
    {
        if (identifiers == null)
        {
            return;
        }
        for (String identifier : identifiers)
        {
            assertIdentifier(identifier, what);
        }
    }

    /**
     * S-7：GenTableMapper 的 ${sql} 是全仓唯一「整条语句直拼」点，入参来自
     * /tool/gen/createTable 的 @RequestParam("sql")。上游已用 filterKeyword + JSqlParser
     * 只放行 CREATE TABLE，这里再钉一次前缀：避免将来有人在服务层新增调用方，绕过 Controller 的解析。
     */
    public static void assertCreateTableStatement(String sql)
    {
        String trimmed = StringUtils.trim(sql);
        if (StringUtils.isEmpty(trimmed) || !trimmed.regionMatches(true, 0, "create table", 0, 12))
        {
            throw new UtilException("仅允许执行 CREATE TABLE 语句");
        }
    }

    /**
     * 验证 order by 语法是否符合规范
     */
    public static boolean isValidOrderBySql(String value)
    {
        return value.matches(SQL_PATTERN);
    }

    /**
     * SQL关键字检查
     */
    public static void filterKeyword(String value)
    {
        if (StringUtils.isEmpty(value))
        {
            return;
        }
        String normalizedValue = value.replaceAll("\\p{Z}|\\s", "");
        String[] sqlKeywords = StringUtils.split(SQL_REGEX, "\\|");
        for (String sqlKeyword : sqlKeywords)
        {
            if (StringUtils.indexOfIgnoreCase(normalizedValue, sqlKeyword) > -1)
            {
                throw new UtilException("请求参数包含敏感关键词'" + sqlKeyword + "'，可能存在安全风险");
            }
        }
    }
}
