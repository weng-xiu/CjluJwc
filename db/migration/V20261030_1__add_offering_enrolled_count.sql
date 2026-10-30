-- =====================================================================================
-- 版本：V20261030.1
-- 标题：T-3 选课容量真相下沉 —— tpm_course_offering 新增「已占名额」列 enrolled_count
-- 背景：原实现用 Redis DECR/INCR 做容量扣减，数据库只在 Redis 键缺失时「按已选人数现算」回填，
--       两条真相（Redis 计数 / 报名表行数）靠 TTL 与补偿勉强对齐：
--       · 进程在 DECR 之后、INSERT 之前被杀 → 名额永久少一个（TTL 到期前无法自愈）；
--       · Redis 键被清空 / 主从切换丢写 → 计数偏大 → 超卖；
--       · 缓存与库不一致时无法判断谁对。缺陷报告 T-3 / 台账 §3.1。
-- 做法：1) 新增 enrolled_count（NOT NULL DEFAULT 0），作为唯一真相；
--       2) 按报名表真相回填：result_status='1'（已选中）且 del_flag='0' 的行数；
--       3) 此后只由 Mapper 的条件更新原子维护：
--          update ... set enrolled_count = enrolled_count + 1 where enrolled_count < max_students
--          受影响行数 = 0 即视为已满，不存在「先查后改」的并发窗口。
-- 注意：enrolled_count 不得加入通用 updateTpmCourseOffering（会用读到的旧值覆盖并发计数），
--       也不得由应用侧回写绝对值；需要校正时用 recountEnrolled 按报名表全量重算。
-- 影响表：tpm_course_offering（新增 1 列 + 存量回填；不删除、不改写其它列）
-- 幂等性：是（information_schema + PREPARE 判列存在；回填为全量重算，可重复执行）
-- 回滚脚本：db/rollback/V20261030_1__add_offering_enrolled_count.sql
-- 执行说明：本机存量库按既有约定「人工应用 + 带回滚脚本」，FLYWAY_ENABLED 默认 false。
-- =====================================================================================

SET @col_exists = (SELECT COUNT(*) FROM information_schema.columns
    WHERE table_schema = DATABASE() AND table_name = 'tpm_course_offering' AND column_name = 'enrolled_count');
SET @sql = IF(@col_exists = 0,
    'ALTER TABLE `tpm_course_offering` ADD COLUMN `enrolled_count` int NOT NULL DEFAULT 0 COMMENT ''已占名额（DB 条件更新维护的唯一真相；禁止应用侧回写绝对值）'' AFTER `max_students`',
    'SELECT 1');
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 按报名表真相回填（全量重算，幂等）：只数「已选中且未删除」的行，
-- 候补/落选（result_status='2'）与退课（'3'）不占名额。
UPDATE `tpm_course_offering`
   SET `enrolled_count` = (
       SELECT COUNT(1) FROM `tpm_selection_enrollment` e
        WHERE e.`course_offering_id` = `tpm_course_offering`.`offering_id`
          AND e.`result_status` = '1'
          AND e.`del_flag` = '0');

-- 校验：超卖应为 0 行（回填后不应存在占位超过容量的开课）
SELECT offering_id, max_students, enrolled_count
  FROM `tpm_course_offering`
 WHERE `enrolled_count` > `max_students`
 ORDER BY enrolled_count DESC
 LIMIT 20;
