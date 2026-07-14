@echo off
set JAVA_HOME=C:\Program Files\Java\jdk-17
set PATH=%JAVA_HOME%\bin;%PATH%
echo JAVA_HOME=%JAVA_HOME%
java -version 2>&1
cd /d d:\project\CjluJwc\CjluJwc
echo === Compiling yu-oa ===
call mvn -pl yu-oa -am install -DskipTests -q
echo === Starting yu-admin ===
cd yu-admin
call mvn spring-boot:run -DskipTests > d:\project\CjluJwc\CjluJwc\backend.log 2> d:\project\CjluJwc\CjluJwc\backend_err.log
