-- =====================================================================================
-- 回滚：V20261007.2 —— 移除「数据分类分级台账」表与其字典
-- 对应变更：db/migration/V20261007_2__add_sys_data_class.sql
-- 逆向操作：DROP TABLE（台账为独立新增表，删除不影响任何业务主数据）；DELETE 三个字典类型及明细。
-- 影响表：删除 sys_data_class；清除 sys_data_category / sys_data_security_level / sys_data_protection 字典。
-- 回滚后果：回到无分类分级台账状态；业务表数据本身不受影响（台账仅为登记元数据）。
-- 幂等性：是
-- 执行后必做：DELETE FROM flyway_schema_history WHERE version = '20261007.2';
-- =====================================================================================

DROP TABLE IF EXISTS sys_data_class;

DELETE FROM sys_dict_data WHERE dict_type IN ('sys_data_category', 'sys_data_security_level', 'sys_data_protection');
DELETE FROM sys_dict_type WHERE dict_type IN ('sys_data_category', 'sys_data_security_level', 'sys_data_protection');

-- 校验：应返回 0
SELECT COUNT(*) AS remaining FROM information_schema.tables
 WHERE table_schema = DATABASE() AND table_name = 'sys_data_class';
