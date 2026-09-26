$ErrorActionPreference = 'Continue'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Set-Location 'd:\project\CjluJwc\CjluJwc'
& 'D:\websever\apache-maven-3.8.6\bin\mvn.cmd' -o -pl yu-admin -am install -DskipTests *>&1 | Out-File -Encoding utf8 'd:\project\CjluJwc\CjluJwc\bin\build_p34_be.log'
if ($LASTEXITCODE -eq 0) {
    # 用打包好的 fat jar 启动：spring-boot:run 在 devtools 重启类加载器下会把同一 Mapper XML
    # 从源码目录与构建目录各加载一次，触发 Mapped Statements collection already contains key
    & "$env:JAVA_HOME\bin\java.exe" -jar 'd:\project\CjluJwc\CjluJwc\build-temp\yu-admin\yu-admin.jar' *>&1 | Out-File -Encoding utf8 'd:\project\CjluJwc\CjluJwc\bin\backend_run_p34.log'
} else {
    "BUILD FAILED exit=$LASTEXITCODE" | Out-File -Append -Encoding utf8 'd:\project\CjluJwc\CjluJwc\bin\backend_run_p34.log'
}
