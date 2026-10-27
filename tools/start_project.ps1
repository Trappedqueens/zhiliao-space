param([switch]$Build,[switch]$BackendOnly,[string]$NginxHome=$env:NGINX_HOME)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
Set-Location -LiteralPath $projectRoot
$runtime=Join-Path $projectRoot 'target'
New-Item -ItemType Directory -Force $runtime | Out-Null
$javaCandidates=@("$env:JAVA_HOME\bin\java.exe",'C:\Program Files\Java\jdk-17\bin\java.exe')
$userJdks=Join-Path $env:USERPROFILE '.jdks'
if (Test-Path -LiteralPath $userJdks) {
    $javaCandidates+=@(Get-ChildItem -LiteralPath $userJdks -Directory | Where-Object { $_.Name -match '(^|[-_])17([._-]|$)' } | Sort-Object Name -Descending | ForEach-Object { Join-Path $_.FullName 'bin\java.exe' })
}
if (Get-Command javac.exe -ErrorAction SilentlyContinue) { $javaCandidates+=(Get-Command javac.exe).Source.Replace('javac.exe','java.exe') }
$javaExe=$javaCandidates | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if (-not $javaExe) { throw '请安装 Java 17 并配置 JAVA_HOME。' }
$env:JAVA_HOME=Split-Path (Split-Path $javaExe -Parent) -Parent
$env:Path="$env:JAVA_HOME\bin;$env:Path"
$jarPath=Join-Path $projectRoot 'backend\target\AI-0.0.1-SNAPSHOT.jar'
function Get-Managed([string]$File,[string]$Marker) {
    if (-not (Test-Path -LiteralPath $File)) { return $null }
    $processNumber=0
    if (-not [int]::TryParse((Get-Content -Raw -LiteralPath $File).Trim(),[ref]$processNumber)) { return $null }
    $found=Get-CimInstance Win32_Process -Filter "ProcessId = $processNumber"
    if ($found -and $found.CommandLine -and $found.CommandLine.Replace('\','/').Contains($Marker.Replace('\','/'))) { return $found }
    return $null
}
$backendPid=Join-Path $runtime 'app.pid'
$frontPid=Join-Path $runtime 'frontend.pid'
$nginxPid=Join-Path $runtime 'nginx.pid'
$vitePath=Join-Path $projectRoot 'frontend\node_modules\vite\bin\vite.js'
$nginxConfig=Join-Path $projectRoot 'deploy\nginx.conf'
$runningBackend=Get-Managed $backendPid $jarPath
$runningFrontend=Get-Managed $frontPid $vitePath
$runningNginx=Get-Managed $nginxPid $nginxConfig
if ($Build -and ($runningBackend -or $runningFrontend -or $runningNginx)) { throw '请先运行 ./stop.ps1，再运行 ./start.ps1 -Build。' }
if ($Build -or -not (Test-Path -LiteralPath $jarPath)) {
    $maven=Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if ($maven) { & $maven.Source -f backend/pom.xml package }
    else { & (Join-Path $projectRoot 'backend\mvnw.cmd') -f (Join-Path $projectRoot 'backend\pom.xml') package }
    if ($LASTEXITCODE -ne 0) { throw '后端构建失败。' }
}
if (-not $BackendOnly) {
    $node=Get-Command node.exe -ErrorAction SilentlyContinue
    $npm=Get-Command npm.cmd -ErrorAction SilentlyContinue
    if (-not $node -or -not $npm) { throw '请安装 Node.js 22.12+（或20.19+）与 npm。' }
    if ($Build -or -not (Test-Path -LiteralPath (Join-Path $projectRoot 'frontend\dist\index.html'))) {
        Push-Location -LiteralPath (Join-Path $projectRoot 'frontend')
        try {
            & $npm.Source ci --no-fund
            if ($LASTEXITCODE -ne 0) { throw '前端依赖安装失败。' }
            & $npm.Source test
            if ($LASTEXITCODE -ne 0) { throw '前端测试失败。' }
            & $npm.Source run build
            if ($LASTEXITCODE -ne 0) { throw '前端构建失败。' }
        } finally { Pop-Location }
    }
}
try { Invoke-RestMethod 'http://localhost:11434/api/version' -TimeoutSec 3 | Out-Null } catch { throw '请先打开 Ollama。' }
if (-not $runningBackend) {
    if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) { throw '8080 端口已由其他实例使用，请先停止对应服务。' }
    $backendProcess=Start-Process -FilePath $javaExe -ArgumentList '-jar',('"'+$jarPath+'"') -WorkingDirectory $projectRoot -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtime 'app.log') -RedirectStandardError (Join-Path $runtime 'app-error.log')
    $backendProcess.Id | Set-Content -LiteralPath $backendPid
}
$ready=$false
for ($attempt=0; $attempt -lt 60; $attempt++) {
    try { $capabilities=Invoke-RestMethod 'http://localhost:8080/ai/capabilities' -TimeoutSec 2; $ready=$capabilities.pdfMaxPages -eq 200 } catch { }
    if ($ready) { break }
    Start-Sleep -Seconds 1
}
if (-not $ready) { throw '后端启动失败，请查看 target/app.log 与 target/app-error.log。' }
if ($BackendOnly) { Write-Host '后端API：http://localhost:8080/ai/capabilities。页面请使用前端5173端口。'; return }
if ($NginxHome) {
    $NginxHome=& (Join-Path $projectRoot 'tools\configure_nginx.ps1') -NginxHome $NginxHome
    $nginxExe=Join-Path $NginxHome 'nginx.exe'
    $nginxPrefix=Join-Path $runtime 'nginx-runtime'
    New-Item -ItemType Directory -Force (Join-Path $nginxPrefix 'logs'),(Join-Path $nginxPrefix 'temp') | Out-Null
    & $nginxExe -t -p ($nginxPrefix.Replace('\','/')+'/') -c $nginxConfig.Replace('\','/')
    if ($LASTEXITCODE -ne 0) { throw 'Nginx 配置校验失败。' }
    if (-not $runningNginx) {
        if (Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue) { throw '5173 端口已占用。' }
        Start-Process -FilePath $nginxExe -ArgumentList '-p',('"'+$nginxPrefix.Replace('\','/')+'/"'),'-c',('"'+$nginxConfig.Replace('\','/')+'"') -WorkingDirectory $projectRoot -WindowStyle Hidden | Out-Null
    }
} elseif (-not $runningFrontend) {
    if (Get-NetTCPConnection -LocalPort 5173 -State Listen -ErrorAction SilentlyContinue) { throw '5173 端口已占用。' }
    $frontendProcess=Start-Process -FilePath $node.Source -ArgumentList ('"'+$vitePath+'"'),'preview','--host','127.0.0.1','--port','5173','--strictPort' -WorkingDirectory (Join-Path $projectRoot 'frontend') -WindowStyle Hidden -PassThru -RedirectStandardOutput (Join-Path $runtime 'frontend.log') -RedirectStandardError (Join-Path $runtime 'frontend-error.log')
    $frontendProcess.Id | Set-Content -LiteralPath $frontPid
}
Write-Host '智聊空间已启动：http://localhost:5173/。日志在项目 target/ 目录。'
