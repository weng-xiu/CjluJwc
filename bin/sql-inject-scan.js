// S-7 动态 SQL 拼接面门禁（V5.36）
//
// 为什么需要这个门禁：MyBatis 的 ${} 是字面拼接，它安不安全不取决于写法，而取决于「值从哪来」。
// 本项目实测 22 处 SQL ${}（16 处 ${params.dataScope} + 5 处表名/列名 + 1 处整条语句）：
//   - ${params.dataScope} 的值本应只由 DataScopeAspect 生成，但 params 是请求可绑定的 Map，
//     一旦某条调用路径漏挂 @DataScope，请求值就直接进 WHERE（实测 28 个 mapper 调用点，11 个不在注解内）；
//   - 表名/列名/整条语句则是「跨层隐式契约」：白名单在上游某一层，新写一个调用方就继承不到。
// 所以门禁守四件事，而不是数命中个数：
//   R1 新增未登记的 ${} 位置          -> FAIL（覆盖面棘轮）
//   R2 BaseEntity 的 dataScope 双层守卫被摘 -> FAIL（根因防线）
//   R3 有 ${params.dataScope} 的 statement 出现「不在 @DataScope 方法内」的调用点且未登记 -> FAIL
//   R4 动态标识符 / 整条语句类 ${} 的「本层断言」不在位 -> FAIL
//   R5 基线里的登记项已消失            -> WARN（陈旧，提示 --update-baseline）
//
// 用法：
//   node bin/sql-inject-scan.js
//   node bin/sql-inject-scan.js --update-baseline
//   node bin/sql-inject-scan.js --list --json
//   node bin/sql-inject-scan.js --paths <repoRoot> --baseline <file> --base-entity <file>
'use strict';

const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const MODULES = [
  'yu-common', 'yu-framework', 'yu-system', 'yu-brm', 'yu-tpm', 'yu-aem',
  'yu-sam', 'yu-oa', 'yu-dis', 'yu-portal', 'yu-quartz', 'yu-generator', 'yu-admin',
];
const DATA_SCOPE_EXPR = 'params.dataScope';

function arg(name, fallback) {
  const i = process.argv.indexOf(name);
  return i > -1 && process.argv[i + 1] ? process.argv[i + 1] : fallback;
}
const SCAN_ROOT = path.resolve(arg('--paths', ROOT));
const BASELINE_PATH = path.resolve(arg('--baseline', path.join(ROOT, 'db', 'sql-inject-baseline.json')));
const BASE_ENTITY_PATH = path.resolve(
  arg('--base-entity', path.join(SCAN_ROOT, 'yu-common/src/main/java/com/yu/common/core/domain/BaseEntity.java')),
);

function walk(dir, out) {
  let ents;
  try {
    ents = fs.readdirSync(dir, { withFileTypes: true });
  } catch (e) {
    return out;
  }
  for (const ent of ents) {
    const p = path.join(dir, ent.name);
    if (ent.isDirectory()) {
      if (['node_modules', 'target', '.git', 'build-temp'].includes(ent.name)) continue;
      walk(p, out);
    } else {
      out.push(p);
    }
  }
  return out;
}

function readText(file) {
  try {
    return fs.readFileSync(file, 'utf8');
  } catch (e) {
    return '';
  }
}

function rel(file) {
  return path.relative(SCAN_ROOT, file).replace(/\\/g, '/');
}

function maskJavaSource(src) {
  return src
    .replace(/\/\*[\s\S]*?\*\//g, (m) => m.replace(/[^\n]/g, ' '))
    .replace(/\/\/[^\n]*/g, (m) => ' '.repeat(m.length));
}

function stripXmlComments(src) {
  return src.replace(/<!--[\s\S]*?-->/g, (m) => m.replace(/[^\n]/g, ' '));
}

// 表达式分类：决定该位置适用哪条规则
function classify(expr) {
  const e = expr.trim();
  if (e === DATA_SCOPE_EXPR) return 'DATASCOPE';
  if (/^(table|tableName|col|cols|column|columns|orderBy|orderByColumn|sort|sortName|field|fields|sql)$/.test(e)) return 'IDENT';
  if (/^params\./.test(e)) return 'REQ_PARAM';
  return 'VAR';
}

// ---------- 采集：SQL 类 ${} 位置 ----------
function collectSqlSites() {
  const sites = new Map();
  for (const mod of MODULES) {
    const resDir = path.join(SCAN_ROOT, mod, 'src', 'main', 'resources');
    for (const file of walk(resDir, [])) {
      const ext = path.extname(file);
      if (!['.xml', '.java'].includes(ext)) continue;
      if (/\.bpmn20\.xml$/.test(file)) continue; // BPMN 的 ${} 是变量解析，不参与 SQL 拼接
      const raw = readText(file);
      if (!raw.includes('${')) continue;
      const isMapper = /[\\/]mapper[\\/]/.test(file) || /<mapper\s+namespace/.test(raw);
      if (!isMapper) continue; // logback/配置里的 ${} 与 SQL 无关
      const src = ext === '.xml' ? stripXmlComments(raw) : maskJavaSource(raw);
      const names = (src.match(/<mapper\s+namespace="[^"]+"/g) || []).length;
      const re = /\$\{([^}]*)\}/g;
      let m;
      while ((m = re.exec(src)) !== null) {
        if (ext === '.java') {
          const line = src.slice(0, m.index).split('\n').length;
          const lineText = (src.split('\n')[line - 1] || '');
          if (!/@(Select|Update|Insert|Delete)|select |insert |update |delete |where |from /i.test(lineText)) continue;
        }
        const expr = m[1].trim();
        const key = rel(file) + '::' + expr;
        const cur = sites.get(key) || { file: rel(file), expr, kind: classify(expr), count: 0 };
        cur.count += 1;
        sites.set(key, cur);
      }
      void names;
    }
  }
  return [...sites.values()].sort((a, b) => (a.file + a.expr).localeCompare(b.file + b.expr));
}

// ---------- 采集：含 ${params.dataScope} 的 statement -> mapper 调用点 ----------
function dataScopeStatements() {
  const out = [];
  for (const mod of MODULES) {
    const resDir = path.join(SCAN_ROOT, mod, 'src', 'main', 'resources');
    for (const file of walk(resDir, [])) {
      if (!file.endsWith('.xml')) continue;
      const raw = readText(file);
      if (!raw.includes('${' + DATA_SCOPE_EXPR + '}')) continue;
      const src = stripXmlComments(raw);
      const re = /\$\{params\.dataScope\}/g;
      let m;
      while ((m = re.exec(src)) !== null) {
        const ids = [...src.slice(0, m.index).matchAll(/<(select|update|insert|delete)\s+id="([^"]+)"/g)];
        const last = ids[ids.length - 1];
        if (last) out.push({ file: rel(file), stmt: last[2] });
      }
    }
  }
  return out;
}

// 取方法签名前的注解块：向前一直走到上一个成员边界（} 或 ;）。
// 不能用「前 400 个字符」——那样上一个方法的 @DataScope 会被算成本方法的，造成假绿。
function annotationBlockBefore(src, index) {
  for (let i = index - 1; i >= 0; i -= 1) {
    if (src[i] === '}' || src[i] === ';') return src.slice(i + 1, index);
  }
  return src.slice(0, index);
}

// 收集 mapper 直调点，并判断所在方法是否挂了 @DataScope
function mapperCallers(stmtNames) {
  const rows = [];
  for (const mod of MODULES) {
    const javaDir = path.join(SCAN_ROOT, mod, 'src', 'main', 'java');
    for (const file of walk(javaDir, [])) {
      if (!file.endsWith('.java')) continue;
      const src = maskJavaSource(readText(file));
      for (const stmt of stmtNames) {
        const re = new RegExp('\\b\\w*[Mm]apper\\.' + stmt + '\\s*\\(', 'g');
        let m;
        while ((m = re.exec(src)) !== null) {
          const before = src.slice(0, m.index);
          const sigs = [...before.matchAll(/(public|private|protected)\s+[\w<>\[\],.\s]+?\s+(\w+)\s*\([^)]*\)\s*(throws [\w,\s.]+)?\s*\{/g)];
          const sig = sigs[sigs.length - 1];
          const annoBlock = sig ? annotationBlockBefore(src, sig.index) : '';
          // 本层显式置空（applyDataScope("")）同样算受控：它是可验证的、写死在代码里的，
          // 不依赖「上游调用方恰好拼对了参数」。
          const body = sig ? bodyFromBrace(src, sig.index + sig[0].length - 1) : '';
          const explicit = /applyDataScope\s*\(/.test(body);
          rows.push({
            file: rel(file),
            method: sig ? sig[2] : '?',
            stmt,
            explicit,
            hasScope: /@(?:com\.yu\.common\.annotation\.)?DataScope\b/.test(annoBlock) || explicit,
          });
        }
      }
    }
  }
  return rows;
}

// 取从 '{' 开始到配对大括号结束的方法体。
function bodyFromBrace(src, braceIdx) {
  let depth = 0;
  for (let i = braceIdx; i < src.length; i += 1) {
    if (src[i] === '{') depth += 1;
    else if (src[i] === '}') {
      depth -= 1;
      if (depth === 0) return src.slice(braceIdx, i + 1);
    }
  }
  return src.slice(braceIdx);
}

// 按大括号配对取方法体：非贪婪正则会在第一个 } （内层 if 的收尾）就截断，抓不到后面的语句。
function methodBody(src, re) {
  const m = re.exec(src);
  if (!m) return '';
  const open = src.indexOf('{', m.index);
  return open < 0 ? '' : bodyFromBrace(src, open);
}

// ---------- 采集：动态标识符 / 整条语句的「本层断言」是否在位 ----------
function guardInPlace(guardFile, guardCall) {
  const p = path.resolve(SCAN_ROOT, guardFile);
  if (!fs.existsSync(p)) return false;
  return maskJavaSource(readText(p)).includes(guardCall);
}

function baseEntityGuardIssues() {
  const src = maskJavaSource(readText(BASE_ENTITY_PATH));
  const issues = [];
  if (!src) {
    issues.push('BaseEntity 源码不存在：' + BASE_ENTITY_PATH);
    return issues;
  }
  if (!/private\s+static\s+final\s+String\s+DATA_SCOPE_KEY\s*=/.test(src)) {
    issues.push('缺少 DATA_SCOPE_KEY 常量（dataScope 键名定义）');
  }
  if (!/public\s+void\s+applyDataScope\s*\(/.test(src)) {
    issues.push('缺少 applyDataScope()——切面写入可信片段的唯一入口');
  }
  const getter = methodBody(src, /public\s+Map<String,\s*Object>\s+getParams\s*\([^)]*\)[^{]*\{/);
  if (!getter.includes('DATA_SCOPE_KEY')) {
    issues.push('getParams() 里的取用覆盖不在位——WebDataBinder 会绕过 setter 直接写 Map，只靠 setter 剥是假防护');
  }
  if (!/trustedDataScope/.test(getter)) {
    issues.push('getParams() 未以可信字段为准覆盖 dataScope');
  }
  return issues;
}

// ---------- 主流程 ----------
const sites = collectSqlSites();
const statements = dataScopeStatements();
const callers = mapperCallers([...new Set(statements.map((s) => s.stmt))]);
const unprotected = callers.filter((c) => !c.hasScope);

if (process.argv.includes('--update-baseline')) {
  const guards = [];
  for (const s of sites) {
    if (s.kind !== 'IDENT') continue;
    const guess = s.expr === 'sql'
      ? { guardFile: 'yu-generator/src/main/java/com/yu/generator/service/GenTableServiceImpl.java', guardCall: 'SqlUtil.assertCreateTableStatement' }
      : s.file.includes('dis/SyncPersistenceMapper')
        ? { guardFile: 'yu-dis/src/main/java/com/yu/dis/service/impl/DisSyncTaskServiceImpl.java', guardCall: 'SqlUtil.assertIdentifier' }
        : s.file.includes('sam/SamProcedureItemMapper')
          ? { guardFile: 'yu-sam/src/main/java/com/yu/sam/service/impl/SamGraduationProcedureServiceImpl.java', guardCall: 'SqlUtil.assertIdentifier' }
          : null;
    if (guess) guards.push(Object.assign({ file: s.file, expr: s.expr }, guess));
  }
  const baseline = {
    updatedAt: new Date().toISOString().slice(0, 10),
    note: 'S-7 动态 SQL 拼接面基线：SQL 类 ${} 位置 + 允许的无 @DataScope mapper 调用点 + 动态标识符的本层断言落点。新增未登记即 FAIL。',
    sqlSites: sites,
    dataScopeCallers: unprotected.map((c) => ({
      file: c.file,
      method: c.method,
      stmt: c.stmt,
      reason: '内部构造查询对象，无外部输入（params 由本方法 new 出，不来自请求绑定）',
    })),
    identifierGuards: guards,
  };
  fs.mkdirSync(path.dirname(BASELINE_PATH), { recursive: true });
  fs.writeFileSync(BASELINE_PATH, JSON.stringify(baseline, null, 2) + '\n', 'utf8');
  console.log(`[OK] 已生成基线 ${BASELINE_PATH}`);
  console.log(`     SQL \${} ${sites.length} 条（DATASCOPE ${sites.filter((s) => s.kind === 'DATASCOPE').length}）、无注解调用点 ${unprotected.length} 条、标识符守卫 ${guards.length} 条`);
  process.exit(0);
}

if (!fs.existsSync(BASELINE_PATH)) {
  console.error(`[FAIL] 基线不存在：${BASELINE_PATH}`);
  console.error('       先跑 node bin/sql-inject-scan.js --update-baseline 并逐条人工确认；缺失即失败是为了防止 CI 空跑。');
  process.exit(1);
}

let baseline;
try {
  baseline = JSON.parse(fs.readFileSync(BASELINE_PATH, 'utf8'));
} catch (e) {
  console.error(`[FAIL] 基线解析失败：${e.message}`);
  process.exit(1);
}

const fails = [];
const warns = [];
const baseSiteMap = new Map((baseline.sqlSites || []).map((s) => [s.file + '::' + s.expr, s]));
const curSiteKeys = new Set(sites.map((s) => s.file + '::' + s.expr));

// R1：新增未登记的 ${} 位置。
// 注意 site 是按 file::expr 归并的（同一文件里 3 处 ${params.dataScope} 只占一条），
// 所以还要比 count——否则在已有 mapper 里再加一处同类 ${} 会静默通过。
const newSites = sites.filter((s) => !baseSiteMap.has(s.file + '::' + s.expr));
if (newSites.length) {
  fails.push('R1 新增未登记的 ${}：' + newSites.length + ' 处\n' + newSites.map((s) => `     ${s.file}  \${${s.expr}} [${s.kind}]`).join('\n'));
}
const grownSites = sites.filter((s) => {
  const b = baseSiteMap.get(s.file + '::' + s.expr);
  return b && s.count > b.count;
});
if (grownSites.length) {
  fails.push('R1 已有位置新增了 ${} 次数：' + grownSites.length + ' 处\n'
    + grownSites.map((s) => `     ${s.file}  \${${s.expr}} 基线 ${baseSiteMap.get(s.file + '::' + s.expr).count} -> 现在 ${s.count}`).join('\n'));
}

// R2：BaseEntity 守卫
const guardIssues = baseEntityGuardIssues();
if (guardIssues.length) {
  fails.push('R2 BaseEntity 的 dataScope 守卫缺失：\n' + guardIssues.map((i) => '     ' + i).join('\n'));
}

// R3：${params.dataScope} 的无注解调用点
const allowedCallers = new Set((baseline.dataScopeCallers || []).map((c) => c.file + '::' + c.method + '::' + c.stmt));
const newUnprotected = unprotected.filter((c) => !allowedCallers.has(c.file + '::' + c.method + '::' + c.stmt));
if (newUnprotected.length) {
  fails.push('R3 新增的「无 @DataScope 的 mapper 调用点」：' + newUnprotected.length + ' 处\n'
    + newUnprotected.map((c) => `     ${c.file}  ${c.method} -> ${c.stmt}`).join('\n')
    + '\n     该 statement 含 ${params.dataScope}；不经切面就无人清空该键 → 请求值可能被拼进 WHERE。');
}

// R4：动态标识符 / 整条语句的本层断言
const guardMap = new Map((baseline.identifierGuards || []).map((g) => [g.file + '::' + g.expr, g]));
for (const s of sites) {
  if (s.kind !== 'IDENT') continue;
  const g = guardMap.get(s.file + '::' + s.expr);
  if (!g) {
    fails.push(`R4 动态标识符 ${s.file} \${${s.expr}} 未登记本层断言（表名/列名直拼必须有可验证的守卫）`);
    continue;
  }
  if (!guardInPlace(g.guardFile, g.guardCall)) {
    fails.push(`R4 断言不在位：${s.file} \${${s.expr}} 期望 ${g.guardFile} 内出现 ${g.guardCall}`);
  }
}

// R5：基线陈旧
const stale = (baseline.sqlSites || []).filter((s) => !curSiteKeys.has(s.file + '::' + s.expr));
if (stale.length) {
  warns.push('R5 基线陈旧（登记项已消失，可 --update-baseline 收敛）：' + stale.length + ' 处\n'
    + stale.map((s) => `     ${s.file}  \${${s.expr}}`).join('\n'));
}
const shrunk = sites.filter((s) => {
  const b = baseSiteMap.get(s.file + '::' + s.expr);
  return b && s.count < b.count;
});
if (shrunk.length) {
  warns.push('R5 基线计数偏高（部分 ${} 已移除，可 --update-baseline 收敛）：' + shrunk.length + ' 处\n'
    + shrunk.map((s) => `     ${s.file}  \${${s.expr}} 基线 ${baseSiteMap.get(s.file + '::' + s.expr).count} -> 现在 ${s.count}`).join('\n'));
}
const staleCallers = (baseline.dataScopeCallers || []).filter(
  (c) => !unprotected.some((u) => u.file === c.file && u.method === c.method && u.stmt === c.stmt));
if (staleCallers.length) {
  warns.push('R5 基线陈旧（无注解调用点已被消灭，可 --update-baseline 收敛）：' + staleCallers.length + ' 处\n'
    + staleCallers.map((c) => `     ${c.file}  ${c.method}`).join('\n'));
}

const countByKind = (k) => sites.filter((s) => s.kind === k).length;
console.log(`扫描 SQL 类 \${} 共 ${sites.length} 处（DATASCOPE ${countByKind('DATASCOPE')} / IDENT ${countByKind('IDENT')} / VAR ${countByKind('VAR')}），涉及 ${new Set(sites.map((s) => s.file)).size} 个 mapper 文件`);
console.log(`含 \${params.dataScope} 的 statement ${new Set(statements.map((s) => s.stmt)).size} 个；mapper 调用点 ${callers.length} 个（无 @DataScope ${unprotected.length} 个，基线内 ${allowedCallers.size} 个）`);

if (process.argv.includes('--list')) {
  console.log('\n明细：');
  for (const s of sites) console.log(`  [${s.kind}] ${s.file}  \${${s.expr}} × ${s.count}`);
  console.log('\n无 @DataScope 的调用点：');
  for (const c of unprotected) console.log(`  ${c.file}  ${c.method} -> ${c.stmt}`);
}
if (process.argv.includes('--json')) {
  console.log('\n' + JSON.stringify({ sites, statements, callers, unprotected, fails, warns }, null, 2));
}

for (const w of warns) console.log('[WARN] ' + w);
if (fails.length) {
  for (const f of fails) console.error('[FAIL] ' + f);
  process.exit(1);
}
console.log('[OK] S-7 动态 SQL 拼接面门禁通过：无新增 ${} 位置、无新增无注解调用点、守卫均在位。');
process.exit(0);
