param([switch]$Build,[switch]$BackendOnly,[string]$NginxHome=$env:NGINX_HOME)
$ErrorActionPreference='Stop'
Set-Location -LiteralPath $PSScriptRoot
$javaCandidates=@(
    "$env:JAVA_HOME\bin\java.exe",
    'C:\Program Files\Java\jdk-17\bin\java.exe'
)
$userJdks=Join-Path $env:USERPROFILE '.jdks'
if (Test-Path -LiteralPath $userJdks) {
    $javaCandidates+=@(Get-ChildItem -LiteralPath $userJdks -Directory | Where-Object { $_.Name -match '(^|[-_])17([._-]|$)' } | Sort-Object Name -Descending | ForEach-Object { Join-Path $_.FullName 'bin\java.exe' })
}
if (Get-Command javac.exe -ErrorAction SilentlyContinue) { $javaCandidates+=(Get-Command javac.exe).Source.Replace('javac.exe','java.exe') }
$javaExe=$javaCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $javaExe) { throw '请安装 Java 17 并配置 JAVA_HOME。' }
$env:JAVA_HOME=Split-Path (Split-Path $javaExe -Parent) -Parent
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$jarPath=Join-Path $PSScriptRoot 'target\AI-0.0.1-SNAPSHOT.jar'
$backendReady=$false
try { $capabilities=Invoke-RestMethod 'http://localhost:8080/ai/capabilities' -TimeoutSec 3; $backendReady=$capabilities.pdfMaxPages -eq 200 }
catch { }
if ($Build -and $backendReady) { throw '后端正在运行。请先运行 ./stop.ps1，再运行 ./start.ps1 -Build。' }
if ($Build -or -not (Test-Path -LiteralPath $jarPath)) {
    if (Get-Command mvn -ErrorAction SilentlyContinue) { & mvn package }
    else { & .\mvnw.cmd package }
    if ($LASTEXITCODE -ne 0) { throw '构建失败，请查看上方日志。' }
}
try { Invoke-RestMethod 'http://localhost:11434/api/version' -TimeoutSec 3 | Out-Null }
catch { throw '请先打开 Ollama。' }
if (-not $backendReady) {
    $backendProcess=Start-Process -FilePath $javaExe -ArgumentList '-jar',('"'+$jarPath+'"') -WorkingDirectory $PSScriptRoot -WindowStyle Hidden -PassThru `
        -RedirectStandardOutput (Join-Path $PSScriptRoot 'target\app.log') `
        -RedirectStandardError (Join-Path $PSScriptRoot 'target\app-error.log')
    $backendProcess.Id | Set-Content -LiteralPath (Join-Path $PSScriptRoot 'target\app.pid')
}
if ($BackendOnly) { Write-Host '后端与内置前端已启动： http://localhost:8080/'; return }
$NginxHome=& (Join-Path $PSScriptRoot 'tools\configure_nginx.ps1') -NginxHome $NginxHome
$nginxExe=Join-Path $NginxHome 'nginx.exe'
$nginxPrefix=Join-Path $PSScriptRoot 'target\nginx-runtime'
New-Item -ItemType Directory -Force (Join-Path $nginxPrefix 'logs'), (Join-Path $nginxPrefix 'temp') | Out-Null
$nginxConfig=Join-Path $PSScriptRoot 'deploy\nginx.conf'
$nginxPid=Join-Path $PSScriptRoot 'target\nginx.pid'
& $nginxExe -t -p ($nginxPrefix.Replace('\','/')+'/') -c $nginxConfig.Replace('\','/')
if ($LASTEXITCODE -ne 0) { throw 'Nginx 配置校验失败。' }
$nginxRunning=$false
if (Test-Path -LiteralPath $nginxPid) {
    $savedNginxId=[int](Get-Content -LiteralPath $nginxPid)
    $savedNginx=Get-Process -Id $savedNginxId -ErrorAction SilentlyContinue
    $nginxRunning=$null -ne $savedNginx -and $savedNginx.ProcessName -eq 'nginx'
}
if (-not $nginxRunning) {
    Start-Process -FilePath $nginxExe -ArgumentList '-p',('"'+$nginxPrefix.Replace('\','/')+'/"'),'-c',('"'+$nginxConfig.Replace('\','/')+'"') -WorkingDirectory $PSScriptRoot -WindowStyle Hidden | Out-Null
}
Write-Host '项目启动中。访问 http://localhost:5173/；运行日志位于 target/app.log。'
