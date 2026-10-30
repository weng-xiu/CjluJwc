#!/usr/bin/env node
/**
 * 事务回滚语义门禁（缺陷报告 T-2 / 附录 B 第 2 条）
 *
 * 规则：`src/main/java` 下每个 `@Transactional` 必须显式声明 `rollbackFor`
 * （或 `rollbackForClassName`）。唯一豁免：`readOnly = true`（只读事务没有需要
 * 回滚的写，加回滚语义属噪音）。
 *
 * 为什么需要这道门禁：Spring 默认只对 RuntimeException/Error 回滚，受检异常
 * 会**提交**。本仓 V5.24 前 422 处注解里 404 处没声明 rollbackFor，"检查失败却
 * 已落库" 的孤儿行就是这类缺省语义导致的（见缺陷报告 T-2 / 教室借用孤儿行）。
 * 靠人工 review 拦不住，必须机器拦。
 *
 * 判据细节（每条都有 fixture 锁定，见 bin/_v524_check_validator.js）：
 *  - 注释、字符串里的 `@Transactional` 不是注解，不参与判定（词法掩码）；
 *  - `@TransactionalEventListener` 等前缀相同的其他注解不算（前后两侧都做标识符边界检查）；
 *  - 实参可跨行，必须括号配对后再找 rollbackFor；
 *  - 只扫 src/main/java，src/test 的 @Transactional 是测试自动回滚语义。
 *
 * 用法：
 *   node bin/tx-scan.js                     # 扫全部模块（CI 用）
 *   node bin/tx-scan.js yu-tpm yu-sam       # 扫指定模块
 *   node bin/tx-scan.js --paths a/b.java    # 扫指定文件（负例自证用）
 * 命中退出码 1。
 */
'use strict';
const fs = require('fs');
const path = require('path');

const MODULES = ['yu-common', 'yu-framework', 'yu-system', 'yu-brm', 'yu-tpm', 'yu-aem',
  'yu-sam', 'yu-oa', 'yu-dis', 'yu-portal', 'yu-quartz', 'yu-generator', 'yu-admin'];
const ROOT = path.resolve(__dirname, '..');
const ANNO = '@Transactional';

const argv = process.argv.slice(2);
const pathArg = argv.indexOf('--paths');
const explicitFiles = [];
if (pathArg >= 0) {
  for (let i = pathArg + 1; i < argv.length; i++) {
    if (argv[i].startsWith('--')) break;
    explicitFiles.push(path.resolve(argv[i]));
  }
}
const bare = argv.filter(a => !a.startsWith('--') && explicitFiles.indexOf(path.resolve(a)) < 0);
const modules = explicitFiles.length ? [] : (bare.length ? bare : MODULES);

function walk(dir, out) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, out);
    else if (e.name.endsWith('.java')) out.push(p);
  }
  return out;
}

/** 注释/字符串掩码：内容替换为空格，保留换行与长度 */
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

const hits = [];
let scanned = 0, ok = 0, skippedReadOnly = 0;

function checkFile(file) {
  const src = fs.readFileSync(file, 'utf8');
  if (!src.includes(ANNO)) return;
  scanned++;
  const masked = mask(src);
  let from = 0;
  for (;;) {
    const at = masked.indexOf(ANNO, from);
    if (at < 0) break;
    from = at + ANNO.length;
    const prev = at > 0 ? masked[at - 1] : '\n';
    if (/[A-Za-z0-9_.]/.test(prev)) continue;
    if (/[A-Za-z0-9_$]/.test(masked[at + ANNO.length] || '')) continue;

    let j = at + ANNO.length;
    while (j < masked.length && /\s/.test(masked[j])) j++;
    let args = '';
    if (masked[j] === '(') {
      let depth = 0;
      for (; j < masked.length; j++) {
        if (masked[j] === '(') depth++;
        else if (masked[j] === ')') { depth--; if (depth === 0) break; }
      }
      args = src.slice(at + ANNO.length + 1, j);
    }
    const readOnly = /readOnly\s*=\s*true/.test(args);
    const hasRb = /rollbackFor\s*=|rollbackForClassName\s*=/.test(args);
    const line = masked.slice(0, at).split('\n').length;
    if (hasRb) ok++;
    else if (readOnly) skippedReadOnly++;
    else hits.push({ file: path.relative(ROOT, file).replace(/\\/g, '/'), line });
  }
}

const files = [];
for (const f of explicitFiles) if (fs.existsSync(f)) files.push(f);
for (const m of modules) {
  const d = path.join(ROOT, m, 'src', 'main', 'java');
  if (fs.existsSync(d)) walk(d, files);
}
for (const f of files) checkFile(f);

if (hits.length) {
  console.error('[FAIL] 以下 @Transactional 缺少 rollbackFor（受检异常见 T-2：异常会被提交）:');
  for (const h of hits) console.error(`  ${h.file}:${h.line}`);
  console.error(`\n共 ${hits.length} 处；修法：@Transactional(rollbackFor = Exception.class)；只读方法用 readOnly = true 豁免。`);
  process.exit(1);
}
console.log(`[OK] 事务回滚语义门禁通过：${scanned} 个含注解文件，${ok} 处已声明 rollbackFor，${skippedReadOnly} 处 readOnly 豁免，0 处缺省。`);
