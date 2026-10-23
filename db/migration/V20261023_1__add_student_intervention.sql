-- =====================================================================================
-- V20261023.1 —— F4 学生数字档案与干预闭环：新增干预留痕表 sam_student_intervention
-- 背景：学生数字档案（N6）此前只读聚合，无"预警→帮扶→效果回收"的留痕载体，
--       无法量化干预前后学业变化（§7 F4）。
-- 内容：1) 建 sam_student_intervention 表（幂等 CREATE TABLE IF NOT EXISTS）
--       2) 干预前后效果以"基线学期/效果学期"口径由 aem_grade_record 现算 GPA 与及格率，
--          本表只存口径（学期ID）与定性结论，避免成绩回写后统计值失真
-- 影响表：sam_student_intervention（新建）
-- 幂等性：是（IF NOT EXISTS）
-- 回滚：db/rollback/V20261023_1__add_student_intervention.sql
-- =====================================================================================

CREATE TABLE IF NOT EXISTS sam_student_intervention (
  intervention_id      BIGINT       NOT NULL AUTO_INCREMENT COMMENT '干预记录ID',
  student_id           BIGINT       NOT NULL                COMMENT '学生ID（关联sam_student）',
  warning_type         CHAR(1)      DEFAULT NULL            COMMENT '预警类型（0学业预警 1成绩预警 2出勤预警 3其他，可空）',
  intervention_type    CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '干预类型（0学业辅导 1心理疏导 2家校联系 3课程重修 4学习计划 5其他）',
  measure              VARCHAR(500) NOT NULL                COMMENT '干预措施',
  helper_name          VARCHAR(50)  DEFAULT NULL            COMMENT '帮扶人',
  baseline_semester_id BIGINT       DEFAULT NULL            COMMENT '干预前（基线）学期ID（关联brm_semester）',
  effect_semester_id   BIGINT       DEFAULT NULL            COMMENT '效果考察学期ID（关联brm_semester）',
  start_date           DATE         DEFAULT NULL            COMMENT '干预开始日期',
  end_date             DATE         DEFAULT NULL            COMMENT '干预结束日期',
  status               CHAR(1)      NOT NULL DEFAULT '0'    COMMENT '状态（0进行中 1已完成 2已关闭）',
  effect_note          VARCHAR(500) DEFAULT NULL            COMMENT '效果结论（定性描述，定量值由学期口径现算）',
  del_flag             CHAR(1)      DEFAULT '0'             COMMENT '删除标志（0存在 2删除）',
  create_by            VARCHAR(64)  DEFAULT ''              COMMENT '创建者',
  create_time          DATETIME     DEFAULT NULL            COMMENT '创建时间',
  update_by            VARCHAR(64)  DEFAULT ''              COMMENT '更新者',
  update_time          DATETIME     DEFAULT NULL            COMMENT '更新时间',
  remark               VARCHAR(500) DEFAULT NULL            COMMENT '备注',
  PRIMARY KEY (intervention_id),
  KEY idx_student (student_id),
  KEY idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='学生干预闭环记录（F4 预警-帮扶-效果回收）';

-- 验证
SELECT intervention_id, student_id, intervention_type, measure, status, baseline_semester_id, effect_semester_id
FROM sam_student_intervention ORDER BY intervention_id DESC;
