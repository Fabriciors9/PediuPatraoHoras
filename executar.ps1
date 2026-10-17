$env:JAVA_HOME = "$env:USERPROFILE\tools\jdk-21.0.6+7"
$env:Path = "$env:JAVA_HOME\bin;" + $env:Path
Write-Host "Iniciando Pediu Patrao no Java 21..." -ForegroundColor Green
Set-Location -Path $PSScriptRoot
.\mvnw.cmd spring-boot:run
