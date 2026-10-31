-- =====================================================================================
-- 变更：V20261031.2 —— 清洗 sam_student 撞号学号，并给 student_no 加唯一索引（P-2）
-- 背景：sam_student 只有 PRIMARY KEY，student_no（学号）既没有唯一约束也没有普通索引。
--       实测（bin/_v530_p2_scan.js）：821 条学籍里 115 组 / 176 条行共享同一个学号。
--       这些**不是重复行**——同号两行的姓名、身份证、班级、专业都不同，是两个真人撞了号
--       （抽样 2024003020101：学生 10137 龚琪 vs 10157 万俊杰）。因此不能删行（会丢学生），
--       只能把后出现的一条重编号。
-- 做法：ROW_NUMBER() 按 student_no 分组、student_id 升序排序，rn > 1 的行改学号为
--       '<原学号>-D<student_id>'，并在 remark 追加「原学号 → 新学号」的审计说明；随后加
--       UNIQUE KEY uk_sam_student_no (student_no)。
--       后缀取 student_id 而不是 -1/-2：student_id 在表内唯一，改号结果可反查回具体学生；
--       顺序号则会在多次清洗后与历史值混淆。
-- 影响表：sam_student（176 条学号被重编号；无行被删除）
-- 配套改动：sql/yangtze_seed_data.sql 已用同一口径改好（bin/_v530_seed_student_no.js），
--           否则 CI 用 seed 建库时这条唯一索引会直接建不起来。
-- 幂等性：是（第二遍每组只剩 1 行，临时表为空 → UPDATE 影响 0 行；索引按存在性跳过）
-- 回滚脚本：db/rollback/V20261031_2__clean_and_unique_student_no.sql
-- 执行方式：mysql -h127.0.0.1 -P3306 -uroot -p yu-cjlujwc < db/migration/V20261031_2__clean_and_unique_student_no.sql
-- 注意事项：
--   1) 被改号的 176 名学生对外展示的学号会变（带 -D 后缀）。这是在「同一学号指向两个人」
--      既成事实下的最小代价方案；如需换成正式学号，请以 student_id 为准从教务主数据重新导入。
--   2) 学号不参与登录（学生登录走 sys_user.user_name），改号不影响账号。
--   3) 本脚本只改库；seed 文件必须同步改（见配套改动），两处口径必须一致。
-- =====================================================================================

-- ---------------------------------------------------------------------------
-- 0) 体检：列出撞号组（执行后这一段应返回 0 行）
-- ---------------------------------------------------------------------------
SELECT `student_no`, COUNT(*) AS rows_with_same_no, GROUP_CONCAT(`student_id` ORDER BY `student_id`) AS student_ids
FROM `sam_student`
GROUP BY `student_no`
HAVING COUNT(*) > 1;

-- ---------------------------------------------------------------------------
-- 1) 标记每组的「次条起」行（rn > 1），student_id 升序保留第一条原号
-- ---------------------------------------------------------------------------
DROP TEMPORARY TABLE IF EXISTS `_v20261031_2_dup_student_no`;
CREATE TEMPORARY TABLE `_v20261031_2_dup_student_no` (
    `student_id` bigint NOT NULL,
    `student_no` varchar(50) NOT NULL,
    PRIMARY KEY (`student_id`)
) ENGINE = InnoDB;

INSERT INTO `_v20261031_2_dup_student_no` (`student_id`, `student_no`)
SELECT t.`student_id`, t.`student_no`
FROM (
    SELECT `student_id`,
           `student_no`,
           ROW_NUMBER() OVER (PARTITION BY `student_no` ORDER BY `student_id`) AS rn
    FROM `sam_student`
) t
WHERE t.rn > 1;

-- ---------------------------------------------------------------------------
-- 2) 重编号 + 写审计说明
--    注意赋值顺序：MySQL 单表 UPDATE 的 SET 从左到右求值，remark 必须写在 student_no
--    之前才能拿到「旧学号」，否则拼出来的是「新 → 新」。
-- ---------------------------------------------------------------------------
UPDATE `sam_student` s
JOIN `_v20261031_2_dup_student_no` d ON d.`student_id` = s.`student_id`
SET s.`remark` = LEFT(CONCAT(
        IFNULL(s.`remark`, ''),
        CASE WHEN s.`remark` IS NULL OR s.`remark` = '' THEN '' ELSE '; ' END,
        'V20261031.2 学号撞号重编号 ', s.`student_no`, ' -> ',
        CONCAT(s.`student_no`, '-D', s.`student_id`)), 500),
    s.`student_no` = CONCAT(s.`student_no`, '-D', s.`student_id`),
    s.`update_time` = NOW();

-- ---------------------------------------------------------------------------
-- 3) 新增唯一索引 uk_sam_student_no（幂等）
-- ---------------------------------------------------------------------------
SET @idx_exists = (SELECT COUNT(*) FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'sam_student'
      AND index_name = 'uk_sam_student_no');

SET @sql = IF(@idx_exists = 0,
    'ALTER TABLE `sam_student` ADD UNIQUE KEY `uk_sam_student_no` (`student_no`)',
    'SELECT 1');
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

DROP TEMPORARY TABLE IF EXISTS `_v20261031_2_dup_student_no`;

-- ---------------------------------------------------------------------------
-- 4) 校验：撞号组为 0，且唯一索引已就位
-- ---------------------------------------------------------------------------
SELECT
    (SELECT COUNT(*) FROM (
        SELECT 1 FROM `sam_student` GROUP BY `student_no` HAVING COUNT(*) > 1) d) AS duplicate_student_no_groups,
    (SELECT COUNT(*) FROM information_schema.statistics
      WHERE table_schema = DATABASE() AND table_name = 'sam_student'
        AND index_name = 'uk_sam_student_no') AS uk_sam_student_no_cols,
    (SELECT COUNT(*) FROM `sam_student`) AS total_students;
