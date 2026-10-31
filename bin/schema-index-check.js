#!/usr/bin/env node
/**
 * DDL 索引与主键门禁（缺陷报告附录 B 第 6 条 / P-1 / A-7）
 *
 * 规则：
 *  R1 主键必有：db/ci/01-schema.sql 里每张 CREATE TABLE 必须声明 PRIMARY KEY。
 *  R2 基线必同步：db/migration/*.sql 里 `ADD INDEX|KEY` 的每条索引，必须在
 *     db/ci/01-schema.sql 中以**完全相同的列序**存在。
 *  R3 热点必在位：REQUIRED_HOT_INDEXES 登记的索引（P-1 两批热点表的性能索引——
 *     V20261031.3 的选课/课表/成绩/用户四表 + V20261031.4 的开课/教师/调停课/用户角色四表，
 *     以及 V20261031.1 的 uk_enroll_active）必须在基线中存在且列序一致。
 *  R4 不许重复：同一张表上两个索引列集合完全相同 → FAIL（纯写放大）。
 *     非唯一索引是另一索引的严格左前缀 → WARN（有冗余，但历史债不全在本批清）。
 *
 * 为什么需要这道门禁：
 *  1) 本仓 CI 用 db/ci/01-schema.sql 建空库跑集成测试。只在 db/migration 里加索引、
 *     忘了同步基线，CI 空库就**没有**该索引——性能回归永远测不出来，索引等于没加。
 *  2) 手工在开发库 `ALTER TABLE ... ADD INDEX` 而不落脚本，是"库里能跑、仓库里没有"
 *     的经典漂移来源；R2/R3 让迁移脚本与基线互为见证。
 *  3) P-1 的整改一旦没有判据守护，下次有人改表结构（重建表、改主键、加列）极易
 *     顺手把组合索引丢掉，且不会有任何编译错误。靠人工 review 拦不住，必须机器拦。
 *
 * 用法：
 *   node bin/schema-index-check.js                 # 校验仓库当前基线（CI 用）
 *   node bin/schema-index-check.js --schema x.sql  # 只校验指定 DDL（反例自证用）
 *   node bin/schema-index-check.js --quiet
 * 命中 FAIL 退出码 1。
 */
'use strict';
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const SCHEMA = path.join(ROOT, 'db/ci/01-schema.sql');
const MIGRATION_DIR = path.join(ROOT, 'db/migration');

/**
 * 热点表关键索引清单 —— 本门禁的判据锚点。
 * [表, 索引名, 列序, 是否唯一, 来源迁移版本]
 * 这里只登记"删掉就会明确退化"的索引，不做全量索引清单（全量由 R2 双向见证覆盖）。
 */
const REQUIRED_HOT_INDEXES = [
  // V20261031.1：选课活跃行唯一约束（V5.30 批次 C 第一项）
  ['tpm_selection_enrollment', 'uk_enroll_active',
    ['round_id', 'student_id', 'course_offering_id', 'active_slot'], true, 'V20261031_1'],
  // V20261031.3：选课按开课计数 / 按学生取已选（V5.31 批次 C 第二项）
  ['tpm_selection_enrollment', 'idx_enroll_offering',
    ['course_offering_id', 'result_status', 'del_flag'], false, 'V20261031_3'],
  ['tpm_selection_enrollment', 'idx_enroll_student',
    ['student_id', 'result_status', 'del_flag'], false, 'V20261031_3'],
  // V20261031.3：排课按开课 / 按教室做时间冲突检测
  ['tpm_schedule', 'idx_schedule_offering',
    ['offering_id', 'week_day', 'start_period'], false, 'V20261031_3'],
  ['tpm_schedule', 'idx_schedule_classroom',
    ['classroom_id', 'week_day', 'start_period'], false, 'V20261031_3'],
  // V20261031.3：成绩按学生取学期 / 按学期取课程
  ['aem_grade_record', 'idx_grade_student_semester',
    ['student_id', 'semester_id'], false, 'V20261031_3'],
  ['aem_grade_record', 'idx_grade_semester_course',
    ['semester_id', 'course_id'], false, 'V20261031_3'],
  // V20261031.3：用户登录取用户 / 部门树筛选（普通索引，不是 UNIQUE，见迁移头注）
  ['sys_user', 'idx_user_name', ['user_name', 'del_flag'], false, 'V20261031_3'],
  ['sys_user', 'idx_user_dept_status', ['dept_id', 'status'], false, 'V20261031_3'],
  // V20261031.4：P-1 余面四表（V5.32 批次 C 第三项）
  // 开课表：学期+开课状态（选课/排课/门户三条链路）、教师打头（只给教师时也能用）
  ['tpm_course_offering', 'idx_offering_semester_status',
    ['semester_id', 'del_flag', 'offering_status', 'status'], false, 'V20261031_4'],
  ['tpm_course_offering', 'idx_offering_teacher',
    ['teacher_id', 'semester_id'], false, 'V20261031_4'],
  // 教师表：工号（导入逐行校验，N+1）、用户（门户按登录用户取档案）、部门+状态（列表/候选池）
  ['brm_teacher', 'idx_teacher_code', ['teacher_code'], false, 'V20261031_4'],
  ['brm_teacher', 'idx_teacher_user', ['user_id'], false, 'V20261031_4'],
  ['brm_teacher', 'idx_teacher_dept_status', ['dept_id', 'status'], false, 'V20261031_4'],
  // 调停课：待审列表按 approve_status、门户「我的申请」按 create_by（**不是**报告的 approve_by——
  // 该列全仓只在 SET 里出现，零 WHERE 过滤，见迁移头注 ①）
  ['tpm_schedule_adjustment', 'idx_adjust_appr_status',
    ['approve_status', 'del_flag', 'create_time'], false, 'V20261031_4'],
  ['tpm_schedule_adjustment', 'idx_adjust_creator',
    ['create_by', 'del_flag', 'create_time'], false, 'V20261031_4'],
  // 用户角色：主键 (user_id, role_id) 服务不了「按角色查用户」；带 user_id 成覆盖索引
  ['sys_user_role', 'idx_user_role_role', ['role_id', 'user_id'], false, 'V20261031_4']
];

const argv = process.argv.slice(2);
const quiet = argv.includes('--quiet');
const schemaArg = argv.indexOf('--schema');
const schemaPath = schemaArg >= 0 && argv[schemaArg + 1] ? path.resolve(argv[schemaArg + 1]) : SCHEMA;

let fail = 0;
let warn = 0;
const log = (s) => { if (!quiet) console.log(s); };
const FAIL = (s) => { fail++; console.log('[FAIL] ' + s); };
const WARN = (s) => { warn++; console.log('[WARN] ' + s); };
const OK = (s) => log('[ OK ] ' + s);

/** 取出索引定义括号内的列名（反引号标识符），丢掉前缀长度与排序修饰 */
function parseCols(s) {
  const out = [];
  const re = /`([^`]+)`/g;
  let m;
  while ((m = re.exec(s)) !== null) out.push(m[1]);
  return out;
}

/**
 * 解析 DDL 文件 → Map<table, [{name, cols, unique}]>
 * 只认 CREATE TABLE 块内的 PRIMARY KEY / UNIQUE KEY / UNIQUE INDEX / KEY / INDEX 行。
 * 生成列定义行不在此列（它以列名开头），所以不会被误当索引。
 */
function parseSchema(file) {
  // 去 BOM：Windows 下用 Out-File 导出的副本会带 U+FEFF，否则首行 CREATE TABLE 可能匹配失败
  const text = fs.readFileSync(file, 'utf8').replace(/^\uFEFF/, '');
  const tables = new Map();
  let cur = null;
  for (const raw of text.split(/\r?\n/)) {
    const line = raw.trim();
    const head = line.match(/^CREATE TABLE\s+`([^`]+)`\s*\(/i);
    if (head) { cur = { table: head[1], indexes: [] }; continue; }
    if (cur && /^\)\s*ENGINE/i.test(line)) {
      tables.set(cur.table, cur.indexes);
      cur = null;
      continue;
    }
    if (!cur) continue;
    if (line.startsWith('--') || line.startsWith('/*')) continue;

    let m = line.match(/^PRIMARY\s+KEY\s*\(([^)]*)\)/i);
    if (m) {
      cur.indexes.push({ name: 'PRIMARY', cols: parseCols(m[1]), unique: true });
      continue;
    }
    m = line.match(/^UNIQUE\s+(?:KEY|INDEX)\s+`([^`]+)`\s*\(([^)]*)\)/i);
    if (m) {
      cur.indexes.push({ name: m[1], cols: parseCols(m[2]), unique: true });
      continue;
    }
    m = line.match(/^(?:KEY|INDEX)\s+`([^`]+)`\s*\(([^)]*)\)/i);
    if (m) {
      cur.indexes.push({ name: m[1], cols: parseCols(m[2]), unique: false });
      continue;
    }
  }
  return tables;
}

/**
 * 解析 db/migration/*.sql 里 `ALTER TABLE `t` ... ADD [UNIQUE] INDEX|KEY `n` (cols)`。
 * 本仓幂等迁移把这些 DDL 放在字符串字面量里（PREPARE/EXECUTE），所以按行扫而不是按语句切。
 */
function parseMigrations(dir) {
  const found = [];
  if (!fs.existsSync(dir)) return found;
  for (const f of fs.readdirSync(dir).filter((n) => n.endsWith('.sql'))) {
    const version = (f.match(/^V([^_]+(?:\.\d+)?)__/) || [])[1] || f;
    let curTable = null;
    for (const line of fs.readFileSync(path.join(dir, f), 'utf8').split(/\r?\n/)) {
      if (line.trim().startsWith('--')) continue;
      const t = line.match(/ALTER\s+TABLE\s+`([^`]+)`/i);
      if (t) { curTable = t[1]; continue; }
      const m = line.match(/ADD\s+(UNIQUE\s+)?(?:INDEX|KEY)\s+`([^`]+)`\s*\(([^)]*)\)/i);
      if (!m) continue;
      if (!curTable) { FAIL(`${f}: 发现 ADD INDEX \`${m[2]}\` 但无法确定所属表（同一行内缺少 ALTER TABLE）`); continue; }
      found.push({ file: f, version, table: curTable, name: m[2], cols: parseCols(m[3]), unique: !!m[1] });
    }
  }
  return found;
}

const sameCols = (a, b) => a.length === b.length && a.every((c, i) => c === b[i]);

// ---------------------------------------------------------------------------
// 主流程
// ---------------------------------------------------------------------------
log(`=== schema-index-check：${path.relative(ROOT, schemaPath).replace(/\\/g, '/')} ===`);
if (!fs.existsSync(schemaPath)) {
  FAIL(`基线 DDL 不存在：${schemaPath}`);
  console.log('\n结果：FAIL（1 项失败，0 项警告）');
  process.exit(1);
}
const tables = parseSchema(schemaPath);
if (tables.size === 0) {
  FAIL('未从基线解析出任何 CREATE TABLE（解析器失效，门禁形同虚设）');
  console.log('\n结果：FAIL（1 项失败，0 项警告）');
  process.exit(1);
}
const tableCount = tables.size;
let indexCount = 0;
for (const list of tables.values()) indexCount += list.length;
OK(`解析出 ${tableCount} 张表、${indexCount} 个索引定义（含主键）`);

// R1 主键必有
let noPk = 0;
for (const [name, list] of tables) {
  if (!list.some((i) => i.name === 'PRIMARY')) { FAIL(`表 ${name} 没有主键（InnoDB 会隐式建 6 字节 rowid，且无法做行级定位）`); noPk++; }
}
if (noPk === 0) OK('R1 每张表都有主键');

// R2 迁移脚本新增索引必须已同步到基线
const migIndexes = parseMigrations(MIGRATION_DIR);
let r2bad = 0;
for (const mi of migIndexes) {
  const owner = tables.get(mi.table);
  const hit = owner ? owner.find((i) => i.name === mi.name) : undefined;
  if (!owner) {
    FAIL(`R2 ${mi.file}: 迁移里的表 ${mi.table} 不在基线中（基线缺表，CI 空库会跑不起来）`);
    r2bad++;
  } else if (!hit) {
    FAIL(`R2 ${mi.file}: ADD INDEX \`${mi.name}\` 未同步到 db/ci/01-schema.sql 的 ${mi.table}（CI 空库不会有该索引）`);
    r2bad++;
  } else if (!sameCols(hit.cols, mi.cols)) {
    FAIL(`R2 ${mi.file}: \`${mi.name}\` 列序不一致 —— 迁移 (${mi.cols.join(', ')}) vs 基线 (${hit.cols.join(', ')})`);
    r2bad++;
  } else if (hit.unique !== mi.unique) {
    FAIL(`R2 ${mi.file}: \`${mi.name}\` 唯一性不一致 —— 迁移 ${mi.unique ? 'UNIQUE' : '普通'} vs 基线 ${hit.unique ? 'UNIQUE' : '普通'}`);
    r2bad++;
  }
}
if (r2bad === 0) OK(`R2 db/migration 中 ${migIndexes.length} 条新增索引均已同步到基线`);

// R3 热点索引登记表必须在位（反例自证的主要判据）
let r3bad = 0;
for (const [table, name, cols, unique, version] of REQUIRED_HOT_INDEXES) {
  const owner = tables.get(table);
  if (!owner) {
    FAIL(`R3 热点表 ${table} 不在基线中（\`${name}\` 来自 ${version}）`);
    r3bad++;
    continue;
  }
  const hit = owner.find((i) => i.name === name);
  if (!hit) {
    FAIL(`R3 基线缺少热点索引 ${table}.\`${name}\`（${version}）—— 该查询会退化为全表扫描`);
    r3bad++;
  } else if (!sameCols(hit.cols, cols)) {
    FAIL(`R3 热点索引 ${table}.\`${name}\` 列序被改动 —— 期望 (${cols.join(', ')})，实际 (${hit.cols.join(', ')})`);
    r3bad++;
  } else if (hit.unique !== unique) {
    FAIL(`R3 热点索引 ${table}.\`${name}\` 唯一性被改动 —— 期望 ${unique ? 'UNIQUE' : '普通索引'}`);
    r3bad++;
  }
}
if (r3bad === 0) OK(`R3 ${REQUIRED_HOT_INDEXES.length} 条热点索引全部在位且列序一致`);

// R4 重复索引（同列集 FAIL；严格左前缀 WARN）
const HOT_TABLES = new Set(REQUIRED_HOT_INDEXES.map((e) => e[0]));
let dupFail = 0;
let dupWarn = 0;
for (const [table, list] of tables) {
  for (let i = 0; i < list.length; i++) {
    for (let j = i + 1; j < list.length; j++) {
      const a = list[i];
      const b = list[j];
      if (sameCols(a.cols, b.cols)) {
        FAIL(`R4 ${table}: 索引 \`${a.name}\` 与 \`${b.name}\` 列完全相同 (${a.cols.join(', ')})，纯写放大`);
        dupFail++;
        continue;
      }
      const [short, long] = a.cols.length < b.cols.length ? [a, b] : [b, a];
      if (short.cols.length && long.cols.slice(0, short.cols.length).every((c, k) => c === short.cols[k])) {
        // 唯一索引做前缀不能省（(a) UNIQUE 与 (a,b) 语义不同）；主键同理
        if (short.unique) continue;
        const tag = HOT_TABLES.has(table) ? '热点表' : '历史表';
        WARN(`R4 ${table}: \`${short.name}\` (${short.cols.join(', ')}) 是 \`${long.name}\` 的左前缀，可被后者覆盖（${tag}）`);
        dupWarn++;
      }
    }
  }
}
if (dupFail === 0) OK(`R4 无列集完全相同的重复索引（左前缀冗余 ${dupWarn} 条，仅告警）`);

// ---------------------------------------------------------------------------
// 汇总
// ---------------------------------------------------------------------------
console.log('');
if (fail > 0) {
  console.log(`结果：FAIL（${fail} 项失败，${warn} 项警告）`);
  process.exit(1);
}
console.log(`结果：PASS（${warn} 项警告）`);
process.exit(0);
