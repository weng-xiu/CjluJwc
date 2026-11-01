#!/usr/bin/env node
/**
 * S-6 接口入参校验棘轮门禁
 *
 * 为什么要有这个门禁（V5.35 实测口径，不是文档里的 101/252）：
 *  - 全仓 @RequestBody 250 处，带 @Valid/@Validated 151 处、未带 99 处；
 *  - 其中直接绑领域实体 204 处 —— 这才是 mass assignment 的真实入口：
 *    控制器把整个实体交给 Service，Service 的 update 语句又会写 update_by = #{updateBy}
 *    （实测 97 条），客户端自带 createBy/updateBy 就能把「谁改的、算谁的」一起改掉，
 *    从而让按 createBy 判归属的过滤（S-5）失效。
 *  - @Validated 本身防不住 mass assignment：它只校验已声明字段，不丢弃多传字段。
 *    所以本门禁盯三件事：新增未校验入参 / 新增实体直绑 / 审计字段只读被摘。
 *
 * 规则：
 *  R1 FAIL  新增的 @RequestBody 既无参数级 @Valid/@Validated、类上也无 @Validated，且不在基线
 *  R2 FAIL  新增的 @RequestBody 直接绑定领域实体（domain 包、非 dto/vo/form），且不在基线
 *  R3 FAIL  BaseEntity 的 createBy/createTime/updateBy/updateTime 丢掉 @JsonProperty(READ_ONLY)
 *           —— 这条是「守卫守卫」，改回可写就等于把 R1/R2 的成果一次性抹掉
 *  R4 WARN  基线里有、代码里已不存在的条目（陈旧），提示执行 --update-baseline 收敛
 *
 * 用法：
 *   node bin/body-validation-scan.js                      # 门禁（CI 用）
 *   node bin/body-validation-scan.js --list               # 附未校验清单
 *   node bin/body-validation-scan.js --update-baseline    # 复核存量后刷新基线
 *   node bin/body-validation-scan.js --paths a.java       # 只扫指定文件（自测 fixture 用）
 *   node bin/body-validation-scan.js --baseline b.json    # 指定基线（自测 fixture 用）
 *   node bin/body-validation-scan.js --base-entity X.java # 指定 BaseEntity（自测 fixture 用）
 *   node bin/body-validation-scan.js --class-root dir       # 额外类索引根（自测 fixture 用）
 *
 * 两个必须避开的解析坑（首版都踩过）：
 *  - 不能从 @RequestBody 往前找最近的 '('：@PathVariable("id") 自身带括号，
 *    会把方法名解析成 PathVariable、类型解析成 ?（真仓曾出现伪条目）。
 *  - 不能只靠单点 import 解析实体：控制器大量用 import ...domain.*;
 *    必须走全仓类名索引 + 通配符包前缀匹配，否则实体识别率会掉到 0。
 */
'use strict';
const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const MODULES = ['yu-common', 'yu-framework', 'yu-system', 'yu-brm', 'yu-tpm', 'yu-aem',
  'yu-sam', 'yu-oa', 'yu-dis', 'yu-portal', 'yu-quartz', 'yu-generator', 'yu-admin'];
const BASE_ENTITY = 'yu-common/src/main/java/com/yu/common/core/domain/BaseEntity.java';
const AUDIT_FIELDS = ['createBy', 'createTime', 'updateBy', 'updateTime'];

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

const argv = process.argv.slice(2);
const asJson = argv.includes('--json');
const wantList = argv.includes('--list');
const updateBaseline = argv.includes('--update-baseline');
const rawPaths = takeValues('--paths');
const baselineValues = takeValues('--baseline');
const baseEntityValues = takeValues('--base-entity');
const classRoots = takeValues('--class-root').map((p) => path.resolve(p));
const explicitFiles = rawPaths.map((p) => path.resolve(p));
const BASELINE = baselineValues.length
  ? path.resolve(baselineValues[0])
  : path.join(ROOT, 'db/body-validation-baseline.json');
const BASE_ENTITY_FILE = baseEntityValues.length ? path.resolve(baseEntityValues[0]) : null;
// 这些取值不是模块名，不能参与 bare 参数判定
const valueArgs = new Set(rawPaths.concat(baselineValues, baseEntityValues, takeValues('--class-root')));

const CONTROL = new Set(['if', 'for', 'while', 'switch', 'catch', 'synchronized', 'return',
  'new', 'assert', 'try', 'do', 'else']);

function walk(dir, out) {
  for (const e of fs.readdirSync(dir, { withFileTypes: true })) {
    const p = path.join(dir, e.name);
    if (e.isDirectory()) walk(p, out);
    else if (e.name.endsWith('.java')) out.push(p);
  }
  return out;
}

/** 注释/字符串掩码：只用于「位置」判定，取值时回到原文 */
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

function splitTop(text, sep) {
  const parts = [];
  let depth = 0;
  let start = 0;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (c === '(' || c === '<' || c === '{' || c === '[') depth++;
    else if (c === ')' || c === '>' || c === '}' || c === ']') depth--;
    else if (c === sep && depth === 0) { parts.push(text.slice(start, i)); start = i + 1; }
  }
  parts.push(text.slice(start));
  return parts;
}

function parenBody(text, openIdx) {
  let depth = 0;
  for (let i = openIdx; i < text.length; i++) {
    if (text[i] === '(') depth++;
    else if (text[i] === ')') { depth--; if (depth === 0) return text.slice(openIdx + 1, i); }
  }
  return null;
}

function pkgOf(file) {
  const head = fs.readFileSync(file, 'utf8').slice(0, 2000);
  const m = /^\s*package\s+([A-Za-z0-9_.]+)\s*;/m.exec(head);
  return m ? m[1] : '';
}

function isEntityPkg(pkg) {
  return /(^|\.)domain$/.test(pkg)
    || (/(^|\.)domain\./.test(pkg) && !/\.(dto|vo|req|request|param|query|form|excel)(\.|$)/.test(pkg));
}

/** 收集待扫文件：显式 --paths 优先，否则全仓模块 */
function collectFiles() {
  if (explicitFiles.length) {
    return explicitFiles.filter((f) => fs.existsSync(f));
  }
  const all = [];
  for (const mod of MODULES) {
    const d = path.join(ROOT, mod, 'src', 'main', 'java');
    if (fs.existsSync(d)) walk(d, all);
  }
  return all;
}

function buildClassIndex(files) {
  const idx = {};
  for (const f of files) {
    const name = path.basename(f, '.java');
    (idx[name] = idx[name] || []).push(f);
  }
  return idx;
}

/** 类索引全集：全仓模块 + （自测时）显式路径所在目录与 --class-root，保证 fixture 里的通配符 import 也能解析 */
function classIndexFiles() {
  const all = [];
  for (const mod of MODULES) {
    const d = path.join(ROOT, mod, 'src', 'main', 'java');
    if (fs.existsSync(d)) walk(d, all);
  }
  const extraRoots = classRoots.slice();
  for (const f of explicitFiles) {
    const d = path.dirname(f);
    if (fs.existsSync(d)) extraRoots.push(d);
  }
  for (const d of extraRoots) {
    for (const extra of walk(d, [])) if (!all.includes(extra)) all.push(extra);
  }
  return all;
}

function scan() {
  const classIndex = buildClassIndex(classIndexFiles());
  const pkgCache = {};
  const rows = [];
  let controllers = 0;

  for (const file of collectFiles()) {
    const src = fs.readFileSync(file, 'utf8');
    if (!src.includes('@RestController') && !src.includes('@Controller')) continue;
    const masked = mask(src);
    const cm = /\bclass\s+([A-Za-z0-9_]+)/.exec(masked);
    if (!cm) continue;
    controllers++;
    const className = cm[1];
    const rel = path.relative(ROOT, file).replace(/\\/g, '/');
    const classValidated = /@Validated\b/.test(masked.slice(0, cm.index));

    const exact = {};
    const wild = [];
    const impRe = /import\s+(?:static\s+)?([A-Za-z0-9_.*]+)\s*;/g;
    let im;
    while ((im = impRe.exec(masked)) !== null) {
      const fq = im[1];
      if (fq.endsWith('.*')) wild.push(fq.slice(0, -2));
      else exact[fq.split('.').pop()] = fq;
    }

    const sigRe = /([A-Za-z0-9_$]+)\s*\(/g;
    let s;
    while ((s = sigRe.exec(masked)) !== null) {
      const name = s[1];
      if (CONTROL.has(name)) continue;
      const prev = masked.slice(Math.max(0, s.index - 3), s.index).trimEnd();
      if (prev.endsWith('@') || prev.endsWith('.')) continue;
      const open = s.index + name.length + (s[0].length - name.length - 1);
      const body = parenBody(masked, open);
      if (body === null || !/@RequestBody\b/.test(body)) continue;
      sigRe.lastIndex = open + body.length + 1;

      for (const raw of splitTop(body, ',')) {
        const p = raw.replace(/\s+/g, ' ').trim();
        if (!/@RequestBody\b/.test(p)) continue;
        const hasParamValid = /@(Valid|Validated)\b/.test(p);
        const decl = p.replace(/@[A-Za-z0-9_.]+(\s*\([^)]*\))?/g, ' ').replace(/\s+/g, ' ').trim();
        const tm = /([A-Za-z0-9_.]+(?:<[^>]*>)?)\s+[A-Za-z0-9_]+\s*$/.exec(decl);
        const rawType = tm ? tm[1] : '?';
        const baseType = rawType.replace(/<.*$/, '').split('.').pop();
        let pkg = exact[baseType] || '';
        if (!pkg && classIndex[baseType]) {
          for (const f of classIndex[baseType]) {
            const pk = pkgCache[f] || (pkgCache[f] = pkgOf(f));
            if (!wild.length || wild.some((w) => pk === w || pk.startsWith(w + '.'))) { pkg = pk; break; }
          }
        }
        rows.push({
          id: rel + '#' + className + '.' + name,
          type: baseType,
          pkg,
          isEntity: isEntityPkg(pkg),
          isContainer: /^(String|Map|JSONObject|Object|List|Set|Collection|JsonNode|HttpServletRequest|MultipartFile)$/.test(baseType),
          validated: hasParamValid || classValidated,
          line: masked.slice(0, s.index).split('\n').length
        });
      }
    }
  }
  return { rows, controllers };
}

/** R3：BaseEntity 审计字段必须保持 Jackson 只读 */
function checkBaseEntity() {
  const rel = BASE_ENTITY_FILE ? path.relative(ROOT, BASE_ENTITY_FILE).replace(/\\/g, '/') : BASE_ENTITY;
  const f = BASE_ENTITY_FILE || path.join(ROOT, BASE_ENTITY);
  if (!fs.existsSync(f)) return ['BaseEntity 不存在：' + rel];
  const src = fs.readFileSync(f, 'utf8');
  const masked = mask(src);
  const bad = [];
  for (const field of AUDIT_FIELDS) {
    const re = new RegExp('(@[A-Za-z0-9_.]+\\([^)]*\\)\\s*)*private\\s+[A-Za-z0-9_.<>]+\\s+' + field + '\\s*;');
    const m = re.exec(masked);
    if (!m) { bad.push(field + '（未找到字段声明）'); continue; }
    const annos = m[0].slice(0, m[0].indexOf('private'));
    if (!/@JsonProperty\s*\(\s*access\s*=\s*(JsonProperty\.Access\.)?READ_ONLY\s*\)/.test(annos)) {
      bad.push(field + '（缺少 @JsonProperty(access = READ_ONLY)）');
    }
  }
  return bad;
}

function loadBaseline() {
  if (!fs.existsSync(BASELINE)) return null;
  try {
    return JSON.parse(fs.readFileSync(BASELINE, 'utf8'));
  } catch (e) {
    console.error('[FAIL] 基线无法解析：' + path.relative(ROOT, BASELINE) + '  ' + e.message);
    process.exit(1);
  }
}

function main() {
  const { rows, controllers } = scan();
  const unvalidated = rows.filter((r) => !r.validated);
  const entityBound = rows.filter((r) => r.isEntity);
  const unvalidatedIds = unvalidated.map((r) => r.id);
  const entityIds = entityBound.map((r) => r.id);
  const baseEntityBad = checkBaseEntity();

  if (asJson) {
    console.log(JSON.stringify({
      controllers,
      requestBody: rows.length,
      validated: rows.length - unvalidated.length,
      unvalidated: unvalidated.length,
      entityBound: entityBound.length,
      containerUnvalidated: unvalidated.filter((r) => r.isContainer).length,
      baseEntityBad,
      unvalidatedList: unvalidatedIds,
      entityList: entityIds
    }, null, 2));
    return;
  }

  console.log(`扫描 ${controllers} 个 Controller，@RequestBody ${rows.length} 处：`
    + `已校验 ${rows.length - unvalidated.length}，未校验 ${unvalidated.length}`
    + `（其中容器型 ${unvalidated.filter((r) => r.isContainer).length}）；`
    + `直接绑领域实体 ${entityBound.length} 处`);

  if (updateBaseline) {
    const payload = {
      updatedAt: new Date().toISOString().slice(0, 10),
      note: 'S-6 入参校验存量基线。unvalidated=未带 @Valid/@Validated 的请求体；'
        + 'entityBound=直接绑定领域实体的请求体（mass assignment 面）。'
        + '新增接口若落入其中任一，先补 DTO/校验或显式说明理由再进基线。',
      unvalidated: unvalidatedIds,
      entityBound: entityIds
    };
    fs.mkdirSync(path.dirname(BASELINE), { recursive: true });
    fs.writeFileSync(BASELINE, JSON.stringify(payload, null, 2) + '\n', 'utf8');
    console.log(`[OK] 基线已写入 ${path.relative(ROOT, BASELINE)}：unvalidated ${unvalidatedIds.length}，entityBound ${entityIds.length}`);
  }

  const baseline = loadBaseline();
  if (!baseline) {
    if (!updateBaseline) {
      console.error(`[FAIL] 基线不存在：${path.relative(ROOT, BASELINE)}`);
      console.error('修法：确认过存量入参后执行 node bin/body-validation-scan.js --update-baseline 生成基线。');
      process.exit(1);
    }
    return;
  }
  const baseUnvalidated = new Set(baseline.unvalidated || []);
  const baseEntity = new Set(baseline.entityBound || []);

  const newUnvalidated = unvalidatedIds.filter((id) => !baseUnvalidated.has(id));
  const newEntity = entityIds.filter((id) => !baseEntity.has(id));
  const staleUnvalidated = [...baseUnvalidated].filter((id) => !unvalidatedIds.includes(id));
  const staleEntity = [...baseEntity].filter((id) => !entityIds.includes(id));

  if (wantList && newUnvalidated.length) {
    console.log('\n-- 新增未校验请求体 --');
    for (const l of newUnvalidated) console.log('  ' + l);
  }
  if (wantList && newEntity.length) {
    console.log('\n-- 新增实体直绑 --');
    for (const l of newEntity) console.log('  ' + l);
  }

  const failReasons = [];
  if (newUnvalidated.length) {
    failReasons.push(`R1 新增未校验请求体 ${newUnvalidated.length} 处：`
      + newUnvalidated.slice(0, 8).join(', ') + (newUnvalidated.length > 8 ? ' ...' : '')
      + '\n   修法：入参加 @Validated + DTO 字段约束；确无需校验则在提交说明里写明理由后 --update-baseline。');
  }
  if (newEntity.length) {
    failReasons.push(`R2 新增实体直绑请求体 ${newEntity.length} 处：`
      + newEntity.slice(0, 8).join(', ') + (newEntity.length > 8 ? ' ...' : '')
      + '\n   修法：改用 XxxDTO/Form（只声明客户端可写字段），别把领域实体当入参。');
  }
  if (baseEntityBad.length) {
    failReasons.push(`R3 BaseEntity 审计字段不再只读：${baseEntityBad.join('、')}`
      + '\n   修法：恢复 @JsonProperty(access = JsonProperty.Access.READ_ONLY)，'
      + '否则客户端可自带 createBy/updateBy 伪造归属（S-5 的判据随之失效）。');
  }

  const staleNotes = [];
  if (staleUnvalidated.length) staleNotes.push(`基线陈旧(未校验) ${staleUnvalidated.length} 条`);
  if (staleEntity.length) staleNotes.push(`基线陈旧(实体直绑) ${staleEntity.length} 条`);

  if (failReasons.length) {
    for (const r of failReasons) console.error('[FAIL] ' + r);
    console.error(`\n[FAIL] S-6 入参校验门禁未通过（基线 ${path.relative(ROOT, BASELINE)}）`);
    process.exit(1);
  }
  for (const n of staleNotes) console.warn('[WARN] ' + n + '，可执行 --update-baseline 收敛');
  console.log(`[OK] S-6 入参校验门禁通过（基线内：未校验 ${baseUnvalidated.size} 条、实体直绑 ${baseEntity.size} 条）`);
}

main();
