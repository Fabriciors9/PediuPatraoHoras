@echo off
title Pediu Patrao - Inicializacao
echo Iniciando Pediu Patrao com Java 21...
set JAVA_HOME=%USERPROFILE%\tools\jdk-21.0.6+7
set PATH=%JAVA_HOME%\bin;%PATH%
cd /d "%~dp0"
call mvnw.cmd spring-boot:run
pause
