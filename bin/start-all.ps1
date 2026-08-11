$ErrorActionPreference = 'Continue'

$env:JAVA_HOME = 'C:\Users\q\.jdks\jbr-17.0.12'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
$MVN = 'F:\web\apache-maven-3.8.6\bin\mvn.cmd'
$ROOT = 'h:\project_open\CjluJwc'
$ALT = "$ROOT\yu-admin\target_alt"

New-Item -ItemType Directory -Path "$ALT\classes" -Force | Out-Null
New-Item -ItemType Directory -Path "$ALT\test-classes" -Force | Out-Null

# 1. backend yu-admin (port 8080) -> target_alt to avoid restricted target/i18n
# Use Start-Process with real ArgumentList array so -D flags are preserved
$backendArgs = @(
    'spring-boot:run',
    '-DskipTests',
    '-Dmaven.test.skip=true',
    "-Dproject.build.outputDirectory=$ALT\classes",
    "-Dproject.build.testOutputDirectory=$ALT\test-classes"
)
Start-Process -FilePath $MVN -ArgumentList $backendArgs -WorkingDirectory "$ROOT\yu-admin" -RedirectStandardOutput "$ROOT\backend_run.log" -RedirectStandardError "$ROOT\backend_err.log" -WindowStyle Normal

# 2. frontend yu-ui (port 80)
Start-Process powershell -ArgumentList @('-NoExit', "cd '$ROOT\yu-ui'; npm run dev") -WindowStyle Normal

# 3. frontend yu-portal-ui (port 81)
Start-Process powershell -ArgumentList @('-NoExit', "cd '$ROOT\yu-portal-ui'; npm run dev") -WindowStyle Normal

Write-Host 'ALL_LAUNCHED'
