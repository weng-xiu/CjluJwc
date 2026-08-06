$ErrorActionPreference = 'Continue'
$env:JAVA_HOME = 'C:\Users\q\.jdks\jbr-17.0.12'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Set-Location 'h:\project_open\CjluJwc'
& 'F:\web\apache-maven-3.8.6\bin\mvn.cmd' -pl yu-admin -am install -DskipTests
Set-Location 'h:\project_open\CjluJwc\yu-admin'
& 'F:\web\apache-maven-3.8.6\bin\mvn.cmd' spring-boot:run -DskipTests
