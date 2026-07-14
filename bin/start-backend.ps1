$ErrorActionPreference = 'Continue'
$env:JAVA_HOME = 'C:\Program Files\Java\jdk-17'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
Set-Location 'd:\project\CjluJwc\CjluJwc'
& mvn -pl yu-admin -am spring-boot:run -DskipTests
