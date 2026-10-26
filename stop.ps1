$ErrorActionPreference='Stop'
Set-Location -LiteralPath $PSScriptRoot
$appPidFile=Join-Path $PSScriptRoot 'target\app.pid'
if (Test-Path -LiteralPath $appPidFile) {
    $projectProcessId=[int](Get-Content -LiteralPath $appPidFile)
    $projectProcess=Get-CimInstance Win32_Process -Filter "ProcessId = $projectProcessId"
    $expectedJar=Join-Path $PSScriptRoot 'target\AI-0.0.1-SNAPSHOT.jar'
    if ($projectProcess -and $projectProcess.Name -eq 'java.exe' -and $projectProcess.CommandLine.Contains($expectedJar)) {
        Stop-Process -Id $projectProcessId
        Write-Host '本项目后端已停止。'
    }
}
$nginxPidFile=Join-Path $PSScriptRoot 'target\nginx.pid'
if (Test-Path -LiteralPath $nginxPidFile) {
    $nginxProcessId=[int](Get-Content -LiteralPath $nginxPidFile)
    $nginxProcess=Get-CimInstance Win32_Process -Filter "ProcessId = $nginxProcessId"
    $expectedConfig=(Join-Path $PSScriptRoot 'deploy\nginx.conf').Replace('\','/')
    if ($nginxProcess -and $nginxProcess.Name -eq 'nginx.exe' -and $nginxProcess.CommandLine.Replace('\','/').Contains($expectedConfig)) {
        & $nginxProcess.ExecutablePath -s quit -p ((Join-Path $PSScriptRoot 'target\nginx-runtime').Replace('\','/')+'/') -c $expectedConfig
        Write-Host '本项目 Nginx 已发送停止请求。'
    }
}
