Add-Type -AssemblyName System.IO.Compression.FileSystem
$outFile = 'd:\project\CjluJwc\CjluJwc\sql\flowable_create_all.sql'
if (Test-Path $outFile) { Remove-Item $outFile -Force }
"-- Flowable 7.1.0 combined MySQL create schema (extracted from JARs)`n" | Out-File -FilePath $outFile -Encoding UTF8

# 排列顺序：common 必须最先（含 ACT_GE_PROPERTY），engine 次之，其它子引擎最后
$orderedJarPatterns = @(
    'flowable-engine-common-*.jar',   # ACT_GE_* (common)
    'flowable-engine-*.jar',          # BPMN engine + history（不含 common）
    'flowable-idm-engine-*.jar',      # IDM
    'flowable-app-engine-*.jar',      # APP
    'flowable-cmmn-engine-*.jar',     # CMMN
    'flowable-dmn-engine-*.jar',      # DMN
    'flowable-event-registry-*.jar'   # EventRegistry
)

foreach ($pattern in $orderedJarPatterns) {
    $jars = Get-ChildItem 'D:\maven-repo\org\flowable' -Recurse -Filter $pattern | Where-Object {
        $_.FullName -match '\\7\.1\.0\\' -and
        $_.Name -notmatch '(sources|javadoc)\.jar$' -and
        # 精确名匹配：flowable-engine-common 不应被 flowable-engine 模式误取
        ($pattern -ne 'flowable-engine-*.jar' -or $_.Name -notmatch '^flowable-engine-(common|common-api|api)-')
    }
    foreach ($jar in $jars) {
        try {
            $z = [System.IO.Compression.ZipFile]::OpenRead($jar.FullName)
            # 只提取 create 目录下的 mysql create SQL，跳过 drop、upgrade
            $entries = $z.Entries | Where-Object {
                $_.FullName -match '/create/.*mysql.*create.*\.sql$'
            } | Sort-Object FullName
            foreach ($e in $entries) {
                "`n-- ============================================================" | Add-Content -Path $outFile -Encoding UTF8
                "-- Source: $($jar.Name) :: $($e.FullName)" | Add-Content -Path $outFile -Encoding UTF8
                "-- ============================================================" | Add-Content -Path $outFile -Encoding UTF8
                $stream = $e.Open()
                $reader = New-Object System.IO.StreamReader($stream, [System.Text.Encoding]::UTF8)
                $content = $reader.ReadToEnd()
                $reader.Close()
                $content | Add-Content -Path $outFile -Encoding UTF8
            }
            $z.Dispose()
        } catch {
            Write-Host "Skip: $($jar.Name) - $_"
        }
    }
}
Write-Host "OK. Output: $outFile"
Write-Host "Size: $((Get-Item $outFile).Length) bytes"
