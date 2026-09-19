package com.yu.dis.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;

/**
 * 同步落库Mapper（D1 链路"落库"环节）。
 *
 * 依据解析得到的行数据，对目标业务表执行 upsert。
 * 表名与列名由 {@link com.yu.dis.sync.SyncResponseParser} 做过标识符白名单校验，
 * 值统一以 {@code #{}} 预编译参数绑定，防止 SQL 注入。
 *
 * @author ruoyi
 */
public interface SyncPersistenceMapper
{
    /**
     * 对单表批量 upsert。使用 INSERT ... ON DUPLICATE KEY UPDATE 语义：
     * 命中主键/唯一键则更新，否则插入。
     *
     * @param table     目标表名（已校验的安全标识符）
     * @param columns   列名列表（已校验，顺序与 valueRows 每行对齐）
     * @param valueRows 每行的值列表，顺序与 columns 对齐
     * @return 受影响行数
     */
    public int upsertBatch(@Param("table") String table,
                           @Param("columns") List<String> columns,
                           @Param("valueRows") List<List<Object>> valueRows);
}
