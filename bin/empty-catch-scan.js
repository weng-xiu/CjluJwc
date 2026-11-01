#!/usr/bin/env node
/**
 * 空 catch 门禁（缺陷报告 Q-1 / Q-6）
 *
 * 规则（只扫 src/main/java；src/test 的 catch 属测试断言手法，不参与判定）：
 *  R1 FAIL：catch 体为空或只有注释，且没有豁免标记 → 异常被完全吞掉，线上
 *           既没有日志也没有指标，出问题时无从复盘。
 *  R2 WARN：catch 体非空但既不记日志也不抛异常（典型是 `return null` / `continue`）
 *           → 异常被静默降级。WARN 只披露不判失败：这类里确有"可选操作"的正当场景
 *           （如解析配置失败回落默认值），逐条人工定夺，不搞一刀切。
 *  R3 WARN：catch 里用 `e.printStackTrace()` 而不是日志框架 → 生产环境失去结构化
 *           采集（RuoYi 遗留风格）。
 *
 * 豁免机制：确实需要静默的地方，必须在 catch 体内写
 *   // empty-catch-ok: <至少 4 个字的理由>
 * 例如「流关闭失败不影响已提交的解析结果」。理由文本会原样进报告，评审时能看见
 * 为什么可以不记日志——这比"改成随便打一句 log"更诚实。
 *
 * 为什么要这道门禁：Q-1 的 34 处空 catch 靠人工 review 拦不住，且新代码会持续长回来。
 * 判据本身先用 fixture 自证（bin/_v534_empty_catch_selftest.js），避免"扫出 0 处"
 * 其实是解析器失效造成的假绿——V5.31 的 schema 门禁就在这上面踩过。
 *
 * 用法：
 *   node bin/empty-catch-scan.js                 # 扫全部模块（CI 用）
 *   node bin/empty-catch-scan.js yu-tpm yu-sam   # 扫指定模块
 *   node bin/empty-catch-scan.js --paths a.java  # 扫指定文件（反例自证用）
 *   node bin/empty-catch-scan.js --json          # 机器可读输出
 * R1 命中退出码 1；只有 R2/R3 时退出码 0。
 */
'use strict';
const fs = require('fs');
const path = require('path');

const MODULES = ['yu-common', 'yu-framework', 'yu-system', 'yu-brm', 'yu-tpm', 'yu-aem',
  'yu-sam', 'yu-oa', 'yu-dis', 'yu-portal', 'yu-quartz', 'yu-generator', 'yu-admin'];
const ROOT = path.resolve(__dirname, '..');
const WAIVER = /empty-catch-ok\s*:\s*(\S.{3,})/;

const argv = process.argv.slice(2);
const asJson = argv.includes('--json');
const pathArg = argv.indexOf('--paths');
const explicitFiles = [];
if (pathArg >= 0) {
  for (let i = pathArg + 1; i < argv.length; i++) {
    if (argv[i].startsWith('--')) break;
    explicitFiles.push(path.resolve(argv[i]));
  }
}
const bare = argv.filter((a) => !a.startsWith('--')
  && explicitFiles.indexOf(path.resolve(a)) < 0);
const modules = explicitFiles.length ? [] : (bare.length ? bare : MODULES);

function walk(dir, out) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, out);
    else if (e.name.endsWith('.java')) out.push(p);
  }
  return out;
}

/** 注释/字符串掩码：内容替换为空格，保留换行与长度（与 bin/tx-scan.js 同一份实现） */
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

/** 从 open 处的 { 起做花括号配对，返回匹配的 } 下标（掩码文本上做，字符串里的括号已抹平） */
function matchBrace(masked, open) {
  let depth = 0;
  for (let i = open; i < masked.length; i++) {
    if (masked[i] === '{') depth++;
    else if (masked[i] === '}') { depth--; if (depth === 0) return i; }
  }
  return -1;
}

/** catch 体内是否"有动作"：记日志、抛异常都算；纯 return/continue/赋值不算 */
function logsOrThrows(body) {
  return /\b(?:log|logger|LOG|LOGGER)\s*\.\s*(?:trace|debug|info|warn|error|log)\s*\(/.test(body)
    || /\bthrow\b/.test(body);
}

const fails = [];
const warns = [];
let scanned = 0, catchBlocks = 0, waived = 0, okLogged = 0;

function checkFile(file) {
  const src = fs.readFileSync(file, 'utf8');
  if (!src.includes('catch')) return;
  scanned++;
  const masked = mask(src);
  const rel = path.relative(ROOT, file).replace(/\\/g, '/');
  let from = 0;
  for (;;) {
    const at = masked.indexOf('catch', from);
    if (at < 0) break;
    from = at + 5;
    // 标识符边界：跳过 "xxxcatch" 这类；catch 前面必须是闭合括号/空白/}（try 块之后）
    const prev = at > 0 ? masked[at - 1] : '\n';
    if (/[A-Za-z0-9_$.]/.test(prev)) continue;
    let j = at + 5;
    while (j < masked.length && /\s/.test(masked[j])) j++;
    if (masked[j] !== '(') continue;              // 不是 catch 子句
    let depth = 0, k = j;
    for (; k < masked.length; k++) {
      if (masked[k] === '(') depth++;
      else if (masked[k] === ')') { depth--; if (depth === 0) break; }
    }
    if (k >= masked.length) continue;
    let b = k + 1;
    while (b < masked.length && /\s/.test(masked[b])) b++;
    if (masked[b] !== '{') continue;              // 语法上必有块，防御性跳过
    const close = matchBrace(masked, b);
    if (close < 0) continue;
    catchBlocks++;
    const line = masked.slice(0, at).split('\n').length;
    const bodyMasked = masked.slice(b + 1, close);
    const bodyRaw = src.slice(b + 1, close);
    const empty = bodyMasked.trim() === '';
    if (empty) {
      if (WAIVER.test(bodyRaw)) { waived++; continue; }
      fails.push({ file: rel, line });
      continue;
    }
    if (logsOrThrows(bodyMasked)) {
      okLogged++;
      if (/\bprintStackTrace\s*\(/.test(bodyMasked)) warns.push({ file: rel, line, kind: 'printStackTrace' });
      continue;
    }
    warns.push({ file: rel, line, kind: 'swallow' });
  }
}

const files = [];
for (const f of explicitFiles) if (fs.existsSync(f)) files.push(f);
for (const m of modules) {
  const d = path.join(ROOT, m, 'src', 'main', 'java');
  if (fs.existsSync(d)) walk(d, files);
}
for (const f of files) checkFile(f);

if (asJson) {
  console.log(JSON.stringify({
    scannedFiles: scanned, catchBlocks, okLogged, waived,
    fail: fails.length, warn: warns.length, fails, warns
  }, null, 2));
} else {
  if (warns.length) {
    console.log(`[WARN] ${warns.length} 处 catch 吞异常或用了 printStackTrace（不判失败，逐条定夺）:`);
    for (const w of warns) {
      console.log(`  ${w.file}:${w.line}  ${w.kind === 'swallow' ? '既不记日志也不抛异常' : 'printStackTrace'}`);
    }
    console.log('');
  }
  if (fails.length) {
    console.error('[FAIL] 以下 catch 体为空（异常被完全吞掉，线上无迹可查）:');
    for (const h of fails) console.error(`  ${h.file}:${h.line}`);
    console.error(`\n共 ${fails.length} 处；修法：加 log.warn/log.error 记录上下文，`
      + '或在体内写 `// empty-catch-ok: <理由>` 声明为何可以静默。');
  } else {
    const tail = warns.length ? `（另 ${warns.length} 处 WARN）` : '，0 处警告';
    console.log(`[OK] 空 catch 门禁通过：${scanned} 个含 catch 的文件，${catchBlocks} 个 catch 块，`
      + `${okLogged} 处已记日志/已抛出，${waived} 处显式豁免，0 处空吞${tail}。`);
  }
}
process.exit(fails.length ? 1 : 0);
