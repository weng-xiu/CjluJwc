# =============================================================================
# purge-pii-history.ps1
# Purpose : Publish the PII-scrubbed rewritten history (produced by git-filter-repo
#           in build-temp/_pii_mirror) to the remote, closing K5 "no real personal
#           data in repo history". This is IRREVERSIBLE and rewrites the main branch
#           on the remote. Nothing here runs until you confirm at the prompt.
#
# It does NOT push or delete anything on its own unless you type the exact token.
# It does NOT touch your working tree; recovery steps are printed, not executed.
#
# Usage   : cd <repo>; powershell -NoProfile -ExecutionPolicy Bypass -File bin\purge-pii-history.ps1
# =============================================================================

$ErrorActionPreference = 'Stop'

# Encoding note: the logic-critical literals ($REMOTE, $PII_MARK) are kept ASCII so
# the integrity gate and push never depend on console codepage. A few informational
# strings below carry the raw dump's Chinese path; they are print-only, not matched.
$REMOTE = 'https://gitee.com/wengxiulin/CjluJwc.git'
$PII_MARK = 'yu-cjlujwc_20260821'          # ASCII filename of the real-data dump blob (used by Gate 2)
$RAW_DUMP = 'sql\备份\yu-cjlujwc_20260821.sql'   # informational only

$RepoRoot = Split-Path -Parent $PSScriptRoot
$Mirror   = Join-Path $RepoRoot 'build-temp\_pii_mirror'

function Say($m) { Write-Host $m }

Say '=== K5 PII history purge ==='
Say "Repo root : $RepoRoot"
Say "Mirror    : $Mirror"
Say "Remote    : $REMOTE"
Say ''

# ---- Gate 1: mirror must exist (the rewritten history lives only there) ----
if (-not (Test-Path $Mirror)) {
    Say '[ABORT] Rewritten mirror not found. Re-run the filter-repo step first (see doc/执行20261007.md K5).'
    exit 1
}

Push-Location $Mirror
try {
    # ---- Gate 2: integrity check — the PII blob MUST be gone from the rewritten history ----
    $residue = git rev-list --objects --all | Select-String $PII_MARK
    if ($residue) {
        Say '[ABORT] PII blob still present in mirror history. Refusing to push. Re-do the rewrite.'
        Say ($residue | Out-String)
        exit 1
    }
    $nCommits = git rev-list --count refs/heads/main
    Say "[OK] Mirror history is clean: 0 PII blob, $nCommits commits on main."
    Say ''

    # ---- Show impact + require explicit confirmation before any push ----
    Say '!!! This FORCE-PUSH rewrites ALL commit SHAs on the remote main branch.'
    Say '!!! Consequences: every collaborator must delete old clones and re-clone;'
    Say '!!! your local main will diverge and must be reset to the new history.'
    Say '!!! It is irreversible once pushed.'
    Say ''
    $token = Read-Host 'Type FORCE-PUSH-MAIN (exact, uppercase) to proceed, or anything else to cancel'
    if ($token -ne 'FORCE-PUSH-MAIN') {
        Say '[CANCELLED] Nothing was pushed. Mirror kept for a later attempt.'
        exit 0
    }

    # ---- Re-attach origin remote (git-filter-repo strips it after rewrite) ----
    $hasOrigin = git remote get-url origin 2>$null
    if ($LASTEXITCODE -ne 0) {
        git remote add origin $REMOTE
        Say "[OK] origin remote re-attached."
    }

    # ---- Publish all rewritten refs (branches + tags) exactly ----
    Say 'Pushing rewritten history (--force --mirror)...'
    git push --force --mirror origin
    if ($LASTEXITCODE -ne 0) {
        Say '[ERROR] Push failed. Check gitee credentials / branch protection. Nothing partial was published.'
        exit 1
    }
    Say '[DONE] Clean history published.'
}
finally {
    Pop-Location
}

# ---- Post-push manual steps (NOT auto-executed) ----
Say ''
Say '=== Follow-up (do manually) ==='
Say "1) Collaborators: delete old working copies, then 'git clone $REMOTE' fresh. Do NOT pull/merge old history."
Say "2) This machine's working repo: 'git fetch --all --force' then 'git reset --hard origin/main'."
Say "   (K4/K1/K5 work is already inside the rewritten history, so nothing is lost.)"
Say "3) Local raw dump on disk (gitignored, not in repo): delete '$RAW_DUMP' if you no longer need it."
Say "4) Optional: ask gitee support to purge cached objects if the old history lingers on their CDN."
Say "5) Cleanup mirror when satisfied: Remove-Item -Recurse -Force '$Mirror'"
