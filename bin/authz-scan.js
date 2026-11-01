#!/usr/bin/env node
/**
 * 接口授权门禁（缺陷报告 S-11）
 *
 * 背景：SecurityConfig 已是「默认拒绝 + 白名单」——`anyRequest().authenticated()` 保证了
 * 未登录一律 401。所以 S-11 真正的风险不是"游客能调"，而是**登录即可调**：
 * 一个没有 `@PreAuthorize` 的写接口，任何已登录学生都能打（越权）。
 *
 * 规则（只扫 src/main/java 下的 @RestController / @Controller）：
 *  R1 FAIL：非 GET 的 handler 既没有 `@PreAuthorize` 也没有 `@Anonymous`（类级也算），
 *           且不在 db/authz-baseline.json 里 → 新增了一个"登录即可调"的写接口。
 *  R2 WARN：GET handler 未加授权注解 → 查询接口常见（详情/列表靠 @DataScope 或门户自取），
 *           不判失败但逐条披露，避免"读接口越权"被完全无视。
 *  R3 WARN：基线里有、但代码里已经不存在的条目 → 棘轮该收紧了，提示清理基线。
 *
 * 为什么用「基线 + 只拦新增」而不是直接把现状全判失败：存量写接口是既有业务面，
 * 一次性全加注解需要逐个确认权限编码（改错了就是线上 403）。先把闸门装在新代码上，
 * 存量按业务线逐步迁出 baseline——这和 T-2（rollbackFor）、P-1（索引）同一套棘轮思路。
 *
 * 用法：
 *   node bin/authz-scan.js                  # 全量扫描（CI 用）
 *   node bin/authz-scan.js --json           # 机器可读
 *   node bin/authz-scan.js --paths a.java   # 指定文件（自测用）
 *   node bin/authz-scan.js --baseline f.json  # 指定基线文件（自测用）
 *   node bin/authz-scan.js --update-baseline  # 把当前未注解写接口写回基线（仅限确认过的）
 */
'use strict';
const fs = require('fs');
const path = require('path');

const MODULES = ['yu-common', 'yu-framework', 'yu-system', 'yu-brm', 'yu-tpm', 'yu-aem',
  'yu-sam', 'yu-oa', 'yu-dis', 'yu-portal', 'yu-quartz', 'yu-generator', 'yu-admin'];
const ROOT = path.resolve(__dirname, '..');

const argv = process.argv.slice(2);
const asJson = argv.includes('--json');
const updateBaseline = argv.includes('--update-baseline');

/** 取 `--flag v1 v2` 形式的取值列表（遇到下一个 -- 就停） */
function takeValues(flag) {
  const i = argv.indexOf(flag);
  const out = [];
  if (i < 0) return out;
  for (let k = i + 1; k < argv.length; k++) {
    if (argv[k].startsWith('--')) break;
    out.push(argv[k]);
  }
  return out;
}

const rawPaths = takeValues('--paths');
const explicitFiles = rawPaths.map((p) => path.resolve(p));
const baselineValues = takeValues('--baseline');
const BASELINE = baselineValues.length
  ? path.resolve(baselineValues[0])
  : path.join(ROOT, 'db/authz-baseline.json');

// 取值参数本身不是模块名（否则 `--baseline f.json` 会被当成模块去扫）
const valueArgs = new Set(rawPaths.concat(baselineValues));
const bare = argv.filter((a) => !a.startsWith('--') && !valueArgs.has(a));
const modules = explicitFiles.length ? [] : (bare.length ? bare : MODULES);

function walk(dir, out) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, out);
    else if (e.name.endsWith('.java')) out.push(p);
  }
  return out;
}

/** 注释/字符串掩码（与 bin/tx-scan.js 同一份实现） */
function mask(src) {
  const out = src.split('');
  let i = 0;
  const n = src.length;
  while (i < n) {
    const c = src[i], c2 = src[i + 1];
    if (c === '/' && c2 === '/') {
      while (i < n && src[i] !== '\n') { out[i] = ' '; i++; }
    } else if (c === '/' && c2 === '*') {
      out[i] = ' '; out[i + 1] = ' '; i += 2;
      while (i < n && !(src[i] === '*' && src[i + 1] === '/')) {
        if (src[i] !== '\n') out[i] = ' ';
        i++;
      }
      if (i < n) { out[i] = ' '; out[i + 1] = ' '; i += 2; }
    } else if (c === '"' || c === "'") {
      const q = c;
      out[i] = ' '; i++;
      while (i < n) {
        if (src[i] === '\\') { out[i] = ' '; if (i + 1 < n) out[i + 1] = ' '; i += 2; continue; }
        if (src[i] === q) { out[i] = ' '; i++; break; }
        if (src[i] !== '\n') out[i] = ' ';
        i++;
      }
    } else {
      i++;
    }
  }
  return out.join('');
}

/** 从 idx 往前找最近一条语句边界，返回注解块起点（返回边界字符下标） */
function prefixStart(masked, idx) {
  for (let i = idx - 1; i >= 0; i--) {
    const c = masked[i];
    if (c === ';' || c === '}' || c === '{') return i;
  }
  return -1;
}

/** 注解块里是否出现授权注解 */
function hasAuthz(text) {
  return /@(PreAuthorize|Anonymous|SaIgnore)\b/.test(text);
}

const HANDLER_ANNOS = ['GetMapping', 'PostMapping', 'PutMapping', 'DeleteMapping', 'PatchMapping', 'RequestMapping'];
const rows = [];
let scannedFiles = 0;

function checkFile(file) {
  const src = fs.readFileSync(file, 'utf8');
  if (!src.includes('@RestController') && !src.includes('@Controller')) return;
  const masked = mask(src);
  const classMatch = /\bclass\s+([A-Za-z0-9_]+)/.exec(masked);
  if (!classMatch) return;
  scannedFiles++;
  const classIdx = classMatch.index;
  const className = classMatch[1];
  const classPrefix = src.slice(prefixStart(masked, classIdx) + 1, classIdx);
  const classAuthed = hasAuthz(classPrefix);
  const rel = path.relative(ROOT, file).replace(/\\/g, '/');

  for (const anno of HANDLER_ANNOS) {
    const re = new RegExp('@' + anno + '\\b', 'g');
    let m;
    while ((m = re.exec(masked)) !== null) {
      const at = m.index;
      if (at < classIdx) continue;                       // 类级 @RequestMapping
      const start = prefixStart(masked, at) + 1;
      // 注解可以写在 @GetMapping 前后任意顺序。必须先把 mapping 自身的实参跳过去，
      // 再连续吃掉后续注解，最后落在方法声明上；否则 `@GetMapping(value = "/x")`
      // 后面的 `@PreAuthorize` 既不进授权块，还会被当成方法名（曾误报 PreAuthorize）。
      let j = at + m[0].length;
      const skipArgs = () => {
        while (j < masked.length && /\s/.test(masked[j])) j++;
        if (masked[j] !== '(') return;
        let depth = 0;
        for (; j < masked.length; j++) {
          if (masked[j] === '(') depth++;
          else if (masked[j] === ')') { depth--; if (depth === 0) { j++; break; } }
        }
      };
      skipArgs();
      for (;;) {
        while (j < masked.length && /\s/.test(masked[j])) j++;
        if (masked[j] !== '@') break;
        j++;
        while (j < masked.length && /[A-Za-z0-9_.]/.test(masked[j])) j++;
        skipArgs();
      }
      const declPos = j;
      const nameMatch = /([A-Za-z0-9_]+)\s*\(/.exec(masked.slice(declPos, declPos + 300));
      const methodName = nameMatch ? nameMatch[1] : '?';
      const block = src.slice(start, declPos);
      let kind = anno.replace('Mapping', '').toUpperCase();
      if (anno === 'RequestMapping') {
        const args = src.slice(at, declPos);
        const km = /method\s*=\s*\{?\s*RequestMethod\.([A-Z]+)/.exec(args);
        kind = km ? km[1] : 'ANY';
      }
      const authed = classAuthed || hasAuthz(block);
      rows.push({
        id: rel + '#' + className + '.' + methodName,
        kind,
        authed,
        line: masked.slice(0, at).split('\n').length
      });
    }
  }
}

const files = [];
for (const f of explicitFiles) if (fs.existsSync(f)) files.push(f);
for (const mod of modules) {
  const d = path.join(ROOT, mod, 'src', 'main', 'java');
  if (fs.existsSync(d)) walk(d, files);
}
for (const f of files) checkFile(f);

// 同一方法可能同时有 @RequestMapping 与 @GetMapping（极少），去重保留首个
const seen = new Set();
const unique = rows.filter((r) => (seen.has(r.id) ? false : seen.add(r.id)));

const unannotatedWrites = unique
  .filter((r) => r.kind !== 'GET' && !r.authed)
  .map((r) => r.id)
  .sort();
const unannotatedReads = unique.filter((r) => r.kind === 'GET' && !r.authed);

let baseline = { version: 1, unannotatedWrite: [] };
if (fs.existsSync(BASELINE)) {
  baseline = JSON.parse(fs.readFileSync(BASELINE, 'utf8'));
} else if (!updateBaseline) {
  console.error(`[FAIL] 基线不存在：${path.relative(ROOT, BASELINE)}`);
  console.error('修法：确认过存量写接口后执行 node bin/authz-scan.js --update-baseline 生成基线。');
  process.exit(1);
}
const known = new Set(baseline.unannotatedWrite || []);

if (updateBaseline) {
  const next = {
    version: 1,
    note: 'S-11 授权基线：已人工确认的「登录即可调」写接口。新增接口必须先加 @PreAuthorize，'
      + '确认属于设计如此的才可加到这里。当前存量分三类：①自服务类（个人资料/头像/改密/MFA/消息已读/登录注册解锁屏）'
      + '——只能操作当前登录人自己的数据；②模板下载类（importTemplate）——返回静态表头，不含数据；'
      + '③代码内自校类（PortalAdjustmentController 的 approve/reject/pendingList）'
      + '——不经 @PreAuthorize，而是在方法里调 checkApprover() 校 tpm.adjust.approver 配置。',
    unannotatedWrite: unannotatedWrites
  };
  fs.writeFileSync(BASELINE, JSON.stringify(next, null, 2) + '\n', 'utf8');
  console.log(`[OK] 已回写基线：${unannotatedWrites.length} 个未注解写接口 → db/authz-baseline.json`);
  process.exit(0);
}

const newWrites = unannotatedWrites.filter((id) => !known.has(id));
const stale = (baseline.unannotatedWrite || []).filter((id) => unannotatedWrites.indexOf(id) < 0);

const result = {
  scannedFiles,
  handlers: unique.length,
  unannotatedWrites: unannotatedWrites.length,
  unannotatedReads: unannotatedReads.length,
  fail: newWrites.length,
  warn: unannotatedReads.length + stale.length,
  newWrites,
  stale,
  reads: unannotatedReads.map((r) => `${r.id} (${r.kind})`)
};

if (asJson) {
  console.log(JSON.stringify(result, null, 2));
} else {
  console.log(`扫描 ${scannedFiles} 个 Controller，${unique.length} 个 handler；`
    + `未授权写接口 ${unannotatedWrites.length}（基线内 ${unannotatedWrites.length - newWrites.length}），`
    + `未授权读接口 ${unannotatedReads.length}。`);
  if (stale.length) {
    console.log(`\n[WARN] 基线里 ${stale.length} 条已不存在，可收紧基线:`);
    for (const id of stale) console.log('  - ' + id);
  }
  if (newWrites.length) {
    console.error(`\n[FAIL] 新增 ${newWrites.length} 个未授权写接口（登录即可调，存在越权风险）:`);
    for (const id of newWrites) console.error('  + ' + id);
    console.error('\n修法：加 @PreAuthorize("@ss.hasPermi(\'模块:实体:动作\')")；'
      + '确实需要登录即可调的，确认后用 --update-baseline 记入基线。');
  } else {
    console.log('[OK] 授权门禁通过：没有新增未授权写接口。');
  }
}
process.exit(newWrites.length ? 1 : 0);
