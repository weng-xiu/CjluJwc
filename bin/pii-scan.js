#!/usr/bin/env node
/**
 * PII 仓库门禁（V5.23 / 缺陷报告 S-10）。
 *
 * 目的：阻止真实身份证号 / 手机号再次进入版本库。S-10 的实操结论是「种子文件里 821 个身份证号
 * 中有 78 个校验位合法」，说明只靠人工评审拦不住，必须有自动门禁。
 *
 * 判定口径（两条，宁可少报也不错报，避免门禁变成噪声后被人绕过）：
 *   1. 身份证：18 位串且**校验位合法**才算命中。合成号段 99010119900101xxxx 除外
 *      （与 sql/desensitize-test-data.sql 的改写公式一致）；校验位不合法的串（文档示例、
 *      流水号、时间戳拼接）不拦——它们不可能是真实证件号。
 *   2. 手机号：11 位移动号段命中，以下除外：
 *      - 合成号段 1380000xxxx（同上，与 desensitize 公式一致）
 *      - 文档示例号：13800138000 / 13900139000 / 15888888888 / 15666666666
 *        （RuoYi 上游演示数据与既有测试用例）
 *
 * 用法：
 *   node bin/pii-scan.js              # 扫描 git 跟踪的文本文件（CI 用）
 *   node bin/pii-scan.js --all        # 连未跟踪文件一起扫（本地自查）
 *   node bin/pii-scan.js path...      # 只扫指定文件
 *
 * 命中即为真实风险：把数据换成合成值（参考 sql/desensitize-test-data.sql），
 * 不要往白名单里加真实号码。
 */
const { execSync } = require('child_process');
const fs = require('fs');

const ID_RE = /(?<!\d)(\d{17}[\dXx])(?!\d)/g;
const PHONE_RE = /(?<!\d)(1[3-9]\d{9})(?!\d)/g;
const SYNTHETIC_ID = /^99010119900101\d{4}$/;
const ALLOWED_PHONES = new Set(['13800138000', '13900139000', '15888888888', '15666666666']);
const SYNTHETIC_PHONE = /^1380000\d{4}$/;

const TEXT_EXT = /\.(sql|ya?ml|json|js|cjs|mjs|ts|vue|java|md|ps1|sh|bat|properties|xml|txt|csv|html)$/i;
const EXCLUDED_DIR = /(^|\/)(node_modules|dist|build-temp|BOOT-INF|META-INF|\.git)\//;
const MAX_BYTES = 8 * 1024 * 1024;

const args = process.argv.slice(2).filter(a => !a.startsWith('--'));
const scanUntracked = process.argv.includes('--all');

function listFiles() {
  if (args.length) return args;
  const cmd = scanUntracked ? 'git ls-files --cached --others --exclude-standard' : 'git ls-files';
  return execSync(cmd, { encoding: 'utf8', maxBuffer: 64 * 1024 * 1024 })
    .split(/\r?\n/)
    .filter(Boolean);
}

function idChecksumValid(id) {
  const weights = [7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2];
  const codes = '10X98765432';
  let sum = 0;
  for (let i = 0; i < 17; i++) sum += Number(id[i]) * weights[i];
  return codes[sum % 11] === id[17].toUpperCase();
}

const maskId = v => v.slice(0, 6) + '*'.repeat(10) + v.slice(16);
const maskPhone = v => v.slice(0, 3) + '*'.repeat(8);

const files = listFiles().filter(f => TEXT_EXT.test(f) && !EXCLUDED_DIR.test(f));
const hits = [];
let scanned = 0;

for (const file of files) {
  let text;
  try {
    if (fs.statSync(file).size > MAX_BYTES) continue;
    text = fs.readFileSync(file, 'utf8');
  } catch {
    continue;
  }
  scanned++;
  const lines = text.split(/\r?\n/);
  for (let i = 0; i < lines.length; i++) {
    const line = lines[i];
    for (const m of line.matchAll(ID_RE)) {
      const v = m[1];
      if (SYNTHETIC_ID.test(v) || !idChecksumValid(v)) continue;
      hits.push({ file, line: i + 1, kind: '身份证（校验位合法）', value: maskId(v) });
    }
    for (const m of line.matchAll(PHONE_RE)) {
      const v = m[1];
      if (ALLOWED_PHONES.has(v) || SYNTHETIC_PHONE.test(v)) continue;
      hits.push({ file, line: i + 1, kind: '手机号', value: maskPhone(v) });
    }
  }
}

if (hits.length) {
  console.error(`[FAIL] 检出 ${hits.length} 处疑似真实个人信息（已掩码显示），请改为合成值：`);
  for (const h of hits.slice(0, 50)) {
    console.error(`  - ${h.file}:${h.line} [${h.kind}] ${h.value}`);
  }
  if (hits.length > 50) console.error(`  ... 另有 ${hits.length - 50} 处`);
  console.error('合成口径见 sql/desensitize-test-data.sql（身份证 99010119900101xxxx / 手机 1380000xxxx）。');
  process.exit(1);
}

console.log(`[OK] PII 门禁通过：扫描 ${scanned} 个文本文件，0 命中（合成号段与文档示例号已豁免）`);
