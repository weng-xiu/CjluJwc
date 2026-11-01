#!/usr/bin/env node
/**
 * 慢 SQL 聚合与基线门禁（P-1 最后一项余面：慢查询日志回归）
 *
 * 规则：
 *  R1 采集通道在位（--check-config）：application-druid.yml 必须有 log-slow-sql: true、
 *     slow-sql-millis（环境变量可覆盖）与 slow-sql-log-level；logback.xml 必须有
 *     file_slow_sql appender 指向 logs/slow-sql.log，且 logger
 *     com.alibaba.druid.filter.stat.StatFilter 直连该 appender。
 *  R2 阈值口径一致：yml 的 slow-sql-millis 必须等于基线 thresholds.absoluteMs，
 *     否则"文件里的阈值"与"运行时实际阈值"各说一套，聚合结论没有意义。
 *  R3 指纹聚合：把每条慢 SQL 归一化（去注释/字面量/数字、IN 列表折叠）后按指纹统计
 *     count / p50 / p95 / max，按总耗时降序输出 top N。
 *  R4 基线回归：指纹已在基线中且样本数 >= minSamples 时，p95 > 基线 p95 × regressionRatio
 *     → FAIL；max 超线但样本不足 → WARN（样本太少的"回归"多半是抖动）。
 *  R5 新指纹：基线里没有的指纹只报 WARN，不判失败——没有流量时基线本来就不该有内容，
 *     确认无回归后用 --write-baseline 固化。
 *
 * 为什么需要这套东西：
 *  1) V5.31/V5.32 只做到"合成数据量级的 EXPLAIN 判据"，索引到底有没有被真实流量选中、
 *     慢 SQL 有没有真的消失，只有慢日志聚合能回答；没有它，P-1 永远是"理论已优化"。
 *  2) Druid 的 slowSqlLogLevel 默认 ERROR，慢 SQL 会混进 sys-error.log 与错误告警；
 *     改为 WARN 后 file_info（LevelFilter INFO 精确匹配）与 file_error（只收 ERROR）
 *     都不收，故必须有独立 appender，否则文本日志整段丢失（这正是本脚本要吃的那份文件）。
 *  3) 基线一旦入库，改索引、改 Mapper、加 join 导致的回归在 CI/发版前就能看到，
 *     而不是等业务方报"系统变慢"。
 *
 * 用法：
 *   node bin/slow-sql-report.js --check-config      # 纯静态门禁（CI 用，不需要数据库/流量）
 *   node bin/slow-sql-report.js                     # 聚合 logs/slow-sql*.log 并与基线对比
 *   node bin/slow-sql-report.js --input f1.log,f2.log
 *   node bin/slow-sql-report.js --write-baseline    # 把当前指纹 p95 固化进基线
 *   node bin/slow-sql-report.js --json
 * FAIL 退出码 1；无慢日志时打印 [SKIP] 并退出 0（CI 里没有流量是常态，不能因此变红）。
 */
'use strict';
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

const ROOT = path.resolve(__dirname, '..');
const DEFAULT_LOG_DIR = path.join(ROOT, 'logs');
const DEFAULT_BASELINE = path.join(ROOT, 'db/slow-sql-baseline.json');
const DEFAULT_DRUID_YML = path.join(ROOT, 'yu-admin/src/main/resources/application-druid.yml');
const DEFAULT_LOGBACK = path.join(ROOT, 'yu-admin/src/main/resources/logback.xml');
const LOGGER_NAME = 'com.alibaba.druid.filter.stat.StatFilter';

const argv = process.argv.slice(2);
const argVal = (name) => {
  const i = argv.indexOf(name);
  return i >= 0 && argv[i + 1] && !argv[i + 1].startsWith('--') ? argv[i + 1] : null;
};
const quiet = argv.includes('--quiet');
const asJson = argv.includes('--json');
const checkConfigOnly = argv.includes('--check-config');
const writeBaseline = argv.includes('--write-baseline');
const topN = Number(argVal('--top') || 20);
const baselinePath = path.resolve(argVal('--baseline') || DEFAULT_BASELINE);
const druidYmlPath = path.resolve(argVal('--druid-config') || DEFAULT_DRUID_YML);
const logbackPath = path.resolve(argVal('--logback') || DEFAULT_LOGBACK);
const inputArg = argVal('--input');

let fail = 0;
let warn = 0;
const log = (s) => { if (!quiet && !asJson) console.log(s); };
const FAIL = (s) => { fail++; if (!asJson) console.log('[FAIL] ' + s); };
const WARN = (s) => { warn++; if (!asJson) console.log('[WARN] ' + s); };
const OK = (s) => { if (!asJson) console.log('[ OK ] ' + s); };
const SKIP = (s) => { if (!asJson) console.log('[SKIP] ' + s); };

/* ------------------------------------------------------------------ *
 * 一、慢 SQL 日志解析
 * ------------------------------------------------------------------ */

/** 日志行起始（logback pattern 首列 %d{HH:mm:ss.SSS}）；跨行 SQL 靠它切事件 */
const EVENT_START = /^\d{2}:\d{2}:\d{2}\.\d{3}\s/;
/** Druid StatFilter 的慢 SQL 前缀 */
const SLOW_MARK = /slow sql (\d+) millis\./;
/**
 * 从事件尾部剥出参数段。buildSlowParameters 输出 JSON 数组（[] / ["0"] / ["a",1,false]），
 * 无参数时是字面 null。不用贪婪正则：SQL 本体也可能出现 '['（JSON 路径、数组下标），
 * 故从行尾取最后一个 '['，且必须能 JSON.parse 才认为是参数段。
 */
function splitParams(body) {
  const t = body.replace(/\s+$/, '');
  if (!t) return { sql: '', params: '' };
  if (/(?:^|\s)null$/.test(t)) return { sql: t.slice(0, t.length - 4), params: '' };
  if (t.endsWith(']')) {
    const i = t.lastIndexOf('[');
    if (i > 0) {
      const cand = t.slice(i);
      try {
        JSON.parse(cand);
        return { sql: t.slice(0, i), params: cand };
      } catch (e) { /* 不是参数数组，当 SQL 一部分 */ }
    }
  }
  return { sql: t, params: '' };
}

/**
 * 解析慢 SQL 事件。
 *
 * 真实消息形态（javap 反编译 druid-1.2.28 StatFilter.internalAfterStatementExecute 取证）：
 *   "slow sql" + millis + " millis." + getLastExecuteSql() + " " + buildSlowParameters()
 * 注意三点，任何一点假设错都会让聚合结论失真：
 *  1) SQL 与参数之间是**一个空格**（字节码常量 #546），不是换行；
 *  2) SQL 本身含换行——MyBatis 的 boundSql 保留 XML 里的缩进与折行，故一条慢 SQL 在日志里
 *     跨多行，「首行即 SQL」会把 `SELECT RES.*` 截成 `select *`，把不同语句合并成一个指纹；
 *  3) 参数段是**行尾**的 JSON 数组，只能从尾部剥，不能按行切。
 * 一条事件 = 从时间戳行到下一个时间戳行之间的全部内容（跨行 SQL 靠这一点归堆）。
 *
 * 实机样本（logs/slow-sql.log）：
 *   04:33:48.318 [main] [] WARN  c.a.d.f.s.StatFilter - [internalAfterStatementExecute,509] - slow sql 1 millis. SELECT RES.*
 *
 *       from ACT_RE_DEPLOYMENT RES
 *        WHERE  RES.NAME_ = ?
 *
 *       order by RES.DEPLOY_TIME_ desc
 *        LIMIT ? OFFSET ? ["SpringBootAutoDeployment",1,0]
 */
function parseEvents(text) {
  const out = [];
  let buf = [];
  const flush = () => {
    if (!buf.length) return;
    const joined = buf.join('\n');
    buf = [];
    const m = SLOW_MARK.exec(joined);
    if (!m) return;
    const body = joined.slice(m.index + m[0].length);
    const { sql: rawSql, params } = splitParams(body);
    const sql = rawSql.trim();
    if (!sql) return;
    out.push({ millis: Number(m[1]), sql, params });
  };
  for (const line of text.split(/\r?\n/)) {
    if (EVENT_START.test(line)) flush();
    buf.push(line);
  }
  flush();
  return out;
}

/* ------------------------------------------------------------------ *
 * 二、SQL 指纹：让"同一条语句不同参数"归成一个
 * ------------------------------------------------------------------ */
function fingerprint(sql) {
  let s = sql.toLowerCase();
  s = s.replace(/\/\*[\s\S]*?\*\//g, ' ');            // 块注释（含 /*+ hint */）
  s = s.replace(/--[^\n]*/g, ' ');                     // 行注释
  s = s.replace(/'(?:''|[^'])*'/g, '?');               // 字符串字面量
  s = s.replace(/"(?:\\"|[^"])*"/g, '?');              // 标识符引号形式
  s = s.replace(/\b\d+(?:\.\d+)?\b/g, '?');            // 数字（t1/a2 这类标识符不受影响）
  s = s.replace(/\?\s*(?:,\s*\?)+/g, '?');             // in (?, ?, ?) → in (?)
  s = s.replace(/\s+/g, ' ').trim();
  return s;
}

const hashOf = (s) => crypto.createHash('sha1').update(s).digest('hex').slice(0, 12);
const percentile = (sorted, p) => sorted[Math.min(sorted.length - 1, Math.max(0, Math.ceil((p / 100) * sorted.length) - 1))];

function summarize(events) {
  const map = new Map();
  for (const e of events) {
    const fp = fingerprint(e.sql);
    let s = map.get(fp);
    if (!s) {
      s = { key: hashOf(fp), fp, count: 0, totalMs: 0, maxMs: 0, sample: e.sql, times: [] };
      map.set(fp, s);
    }
    s.count++;
    s.totalMs += e.millis;
    s.maxMs = Math.max(s.maxMs, e.millis);
    s.times.push(e.millis);
  }
  for (const s of map.values()) {
    s.times.sort((a, b) => a - b);
    s.p50 = percentile(s.times, 50);
    s.p95 = percentile(s.times, 95);
    delete s.times;
  }
  return [...map.values()].sort((a, b) => b.totalMs - a.totalMs);
}

/* ------------------------------------------------------------------ *
 * 三、日志文件定位
 * ------------------------------------------------------------------ */
function listLogFiles(input) {
  const files = [];
  const push = (p) => {
    const abs = path.resolve(p);
    if (!fs.existsSync(abs)) return;
    if (fs.statSync(abs).isDirectory()) {
      for (const name of fs.readdirSync(abs)) {
        // 只吃慢 SQL 通道：slow-sql.log / slow-sql.2026-11-01.log（logback 按天滚动）
        if (/^slow-sql.*\.log$/.test(name)) push(path.join(abs, name));
      }
    } else {
      files.push(abs);
    }
  };
  if (input) input.split(',').forEach((p) => push(p.trim()));
  else push(DEFAULT_LOG_DIR);
  return [...new Set(files)];
}

/* ------------------------------------------------------------------ *
 * 四、基线读写
 * ------------------------------------------------------------------ */
function readBaseline() {
  if (!fs.existsSync(baselinePath)) return null;
  try {
    const b = JSON.parse(fs.readFileSync(baselinePath, 'utf8').replace(/^\uFEFF/, ''));
    b.fingerprints = b.fingerprints || {};
    b.thresholds = b.thresholds || {};
    return b;
  } catch (e) {
    FAIL(`基线文件无法解析：${path.relative(ROOT, baselinePath)} → ${e.message}`);
    return null;
  }
}

function saveBaseline(baseline, stats) {
  baseline.version = baseline.version || 1;
  baseline.description = baseline.description
    || '慢 SQL 指纹基线（P-1 余面）。thresholds.absoluteMs 必须与 application-druid.yml 的 slow-sql-millis 一致；fingerprints 由 bin/slow-sql-report.js --write-baseline 固化。';
  for (const s of stats) {
    const prev = baseline.fingerprints[s.key];
    baseline.fingerprints[s.key] = {
      sql: s.fp.length > 400 ? s.fp.slice(0, 400) + '…' : s.fp,
      p95Ms: prev && prev.p95Ms ? Math.max(prev.p95Ms, s.p95) : s.p95,
      maxMs: prev && prev.maxMs ? Math.max(prev.maxMs, s.maxMs) : s.maxMs,
      count: s.count
    };
  }
  const sorted = {};
  for (const k of Object.keys(baseline.fingerprints).sort()) sorted[k] = baseline.fingerprints[k];
  baseline.fingerprints = sorted;
  fs.writeFileSync(baselinePath, JSON.stringify(baseline, null, 2) + '\n', 'utf8');
}

/* ------------------------------------------------------------------ *
 * 五、R1/R2 静态配置门禁（CI 用，不需要数据库与流量）
 * ------------------------------------------------------------------ */
function sliceBlock(text, startRe) {
  const m = startRe.exec(text);
  if (!m) return '';
  const rest = text.slice(m.index + m[0].length);
  const indent = m[0].length - m[0].replace(/^\n\s*/, '').length;
  const lines = rest.split(/\r?\n/);
  const out = [];
  for (const line of lines) {
    if (!line.trim()) { out.push(line); continue; }
    const lineIndent = line.length - line.trimStart().length;
    if (lineIndent <= indent) break;
    out.push(line);
  }
  return out.join('\n');
}

function checkConfig() {
  let yml = '';
  let logback = '';
  if (!fs.existsSync(druidYmlPath)) FAIL(`数据源配置不存在：${path.relative(ROOT, druidYmlPath)}`);
  else yml = fs.readFileSync(druidYmlPath, 'utf8');
  if (!fs.existsSync(logbackPath)) FAIL(`日志配置不存在：${path.relative(ROOT, logbackPath)}`);
  else logback = fs.readFileSync(logbackPath, 'utf8');

  const stat = sliceBlock(yml, /filter:\s*\n\s*stat:/);
  if (!/log-slow-sql:\s*true/.test(stat)) {
    FAIL('filter.stat.log-slow-sql 未开启（或不在 filter.stat 段内）——慢 SQL 根本不会产生');
  } else OK('filter.stat.log-slow-sql: true');

  const millisMatch = /slow-sql-millis:\s*(?:\$\{[^:}]+:)?(\d+)\}?/.exec(stat);
  const ymlMillis = millisMatch ? Number(millisMatch[1]) : null;
  if (ymlMillis === null) {
    FAIL('filter.stat.slow-sql-millis 缺失或不可解析（应当写成 ${SLOW_SQL_MILLIS:1000} 或纯数字）');
  } else OK(`filter.stat.slow-sql-millis = ${ymlMillis}ms（可被环境变量覆盖）`);

  const levelMatch = /slow-sql-log-level:\s*(?:\$\{[^:}]+:)?([A-Za-z]+)\}?/.exec(stat);
  if (!levelMatch) {
    WARN('filter.stat.slow-sql-log-level 未显式声明——Druid 默认 ERROR，慢 SQL 会混进 sys-error 与错误告警');
  } else if (levelMatch[1].toUpperCase() === 'ERROR') {
    FAIL('filter.stat.slow-sql-log-level = ERROR：慢 SQL 会进 sys-error.log 并触发错误告警，应改为 WARN');
  } else {
    OK(`filter.stat.slow-sql-log-level = ${levelMatch[1].toUpperCase()}`);
  }

  const appender = /<appender\s+name="file_slow_sql"[\s\S]*?<\/appender>/.exec(logback);
  if (!appender) {
    FAIL(`logback.xml 缺少 appender file_slow_sql——慢 SQL（WARN）会被 file_info 的 LevelFilter INFO 精确匹配挡掉`);
  } else if (!/slow-sql\.log/.test(appender[0])) {
    FAIL('appender file_slow_sql 的输出文件不是 logs/slow-sql.log（聚合脚本按该名前缀找文件）');
  } else OK('logback appender file_slow_sql → ${log.path}/slow-sql.log');

  const loggerMatch = new RegExp(`<logger\\s+name="${LOGGER_NAME.replace(/\./g, '\\.')}"[\\s\\S]*?</logger>`).exec(logback);
  if (!loggerMatch) {
    FAIL(`logback.xml 缺少 logger ${LOGGER_NAME}——该类名来自 Druid 的 LogFactory.getLog(StatFilter.class)，写错就收不到`);
  } else if (!/appender-ref\s+ref="file_slow_sql"/.test(loggerMatch[0])) {
    FAIL(`logger ${LOGGER_NAME} 未引用 file_slow_sql`);
  } else if (/additivity="false"/.test(loggerMatch[0])) {
    WARN(`logger ${LOGGER_NAME} additivity="false"：专用文件有了，但 app.json 采集链路收不到慢 SQL`);
  } else OK(`logback logger ${LOGGER_NAME} → file_slow_sql`);

  const baseline = readBaseline();
  if (!baseline) {
    if (writeBaseline && !checkConfigOnly) {
      // 首次固化基线：按当前配置新建骨架（其余字段由 saveBaseline 填）
      WARN(`基线文件不存在，将按当前配置新建：${path.relative(ROOT, baselinePath)}`);
      return { version: 1, thresholds: { absoluteMs: ymlMillis || 1000, regressionRatio: 1.5, minSamples: 3 }, fingerprints: {} };
    }
    FAIL(`基线文件不存在或不可解析：${path.relative(ROOT, baselinePath)}（阈值口径无处可查）`);
    return null;
  }
  const t = baseline.thresholds;
  if (typeof t.absoluteMs !== 'number') {
    FAIL('基线 thresholds.absoluteMs 必须是数字');
  } else if (ymlMillis !== null && t.absoluteMs !== ymlMillis) {
    FAIL(`阈值口径漂移：application-druid.yml = ${ymlMillis}ms，基线 = ${t.absoluteMs}ms（必须同步）`);
  } else OK(`基线 thresholds.absoluteMs = ${t.absoluteMs}ms，与运行时一致`);
  if (!(typeof t.regressionRatio === 'number' && t.regressionRatio >= 1)) {
    FAIL('基线 thresholds.regressionRatio 必须是 >= 1 的数字');
  } else OK(`基线 thresholds.regressionRatio = ${t.regressionRatio}`);
  if (!(typeof t.minSamples === 'number' && t.minSamples >= 1)) {
    FAIL('基线 thresholds.minSamples 必须是 >= 1 的数字');
  } else OK(`基线 thresholds.minSamples = ${t.minSamples}`);
  return baseline;
}

/* ------------------------------------------------------------------ *
 * 六、主流程
 * ------------------------------------------------------------------ */
function main() {
  const baseline = checkConfig();
  if (checkConfigOnly) return finish({ mode: 'check-config' });
  if (!baseline) return finish({ mode: 'report', blocked: 'baseline-invalid' });

  const files = listLogFiles(inputArg);
  if (!files.length) {
    SKIP(`未采集到慢 SQL 日志（找的是 ${inputArg || path.relative(ROOT, DEFAULT_LOG_DIR) + '/slow-sql*.log'}）：`
      + '没有流量时属正常，先把门禁跑绿；有流量后再 node bin/slow-sql-report.js 对账');
    return finish({ mode: 'report', files: 0, events: 0 });
  }

  const events = [];
  for (const f of files) events.push(...parseEvents(fs.readFileSync(f, 'utf8')));
  if (!events.length) {
    SKIP(`${files.length} 个日志文件里没有 "slow sql N millis." 事件：阈值 ${baseline.thresholds.absoluteMs}ms 内没有慢查询`);
    return finish({ mode: 'report', files: files.length, events: 0 });
  }

  const stats = summarize(events);
  OK(`解析 ${files.length} 个文件 / ${events.length} 条慢 SQL / ${stats.length} 个指纹`);

  const t = baseline.thresholds;
  for (const s of stats) {
    const base = baseline.fingerprints[s.key];
    if (!base) {
      WARN(`新指纹 ${s.key}（${s.count} 次，p95 ${s.p95}ms，max ${s.maxMs}ms）未登记基线：确认无回归后 --write-baseline 固化`);
      continue;
    }
    if (s.count >= t.minSamples && s.p95 > base.p95Ms * t.regressionRatio) {
      FAIL(`回归 ${s.key}：p95 ${s.p95}ms > 基线 ${base.p95Ms}ms × ${t.regressionRatio}（${s.count} 次）\n         ${s.fp.slice(0, 160)}`);
    } else if (s.maxMs > base.maxMs * t.regressionRatio) {
      WARN(`抖动待观察 ${s.key}：max ${s.maxMs}ms > 基线 max ${base.maxMs}ms × ${t.regressionRatio}（样本 ${s.count} < ${t.minSamples}）`);
    }
  }

  const missing = Object.keys(baseline.fingerprints).filter((k) => !stats.some((s) => s.key === k));
  if (missing.length) log(`[INFO] 基线中 ${missing.length} 个指纹本轮未出现（流量差异，不算失败）`);

  if (!asJson) {
    console.log('');
    console.log(`top ${Math.min(topN, stats.length)}（按总耗时降序）`);
    console.log('  总耗时ms  次数   p50    p95    max    指纹');
    for (const s of stats.slice(0, topN)) {
      console.log(`  ${String(s.totalMs).padStart(8)}  ${String(s.count).padStart(4)}  `
        + `${String(s.p50).padStart(5)}  ${String(s.p95).padStart(5)}  ${String(s.maxMs).padStart(5)}    ${s.key}  ${s.fp.slice(0, 80)}`);
    }
    console.log('');
  }

  if (writeBaseline) {
    saveBaseline(baseline, stats);
    OK(`基线已更新：${path.relative(ROOT, baselinePath)}（${Object.keys(baseline.fingerprints).length} 个指纹）`);
  }

  return finish({ mode: 'report', files: files.length, events: events.length, fingerprints: stats.length, stats: asJson ? stats : undefined });
}

function finish(result) {
  if (asJson) {
    console.log(JSON.stringify({ ...result, fail, warn, checked: path.relative(ROOT, druidYmlPath) }, null, 2));
  } else if (!fail && !warn) {
    console.log(`[PASS] 0 项失败 / 0 项警告`);
  } else {
    console.log(`[SUMMARY] ${fail} 项失败 / ${warn} 项警告`);
  }
  process.exit(fail ? 1 : 0);
}

main();
