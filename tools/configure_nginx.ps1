param(
    [string]$NginxHome=$env:NGINX_HOME,
    [string]$OutputPath
)
$ErrorActionPreference='Stop'
$projectRoot=Split-Path $PSScriptRoot -Parent
if (-not $NginxHome) {
    $nginxCommand=Get-Command nginx.exe -ErrorAction SilentlyContinue
    if ($nginxCommand) { $NginxHome=Split-Path $nginxCommand.Source -Parent }
}
if (-not $NginxHome -and (Test-Path -LiteralPath 'C:\Program Files\software\spring-ai-nginx\nginx.exe')) {
    $NginxHome='C:\Program Files\software\spring-ai-nginx'
}
if (-not $NginxHome -or -not (Test-Path -LiteralPath (Join-Path $NginxHome 'nginx.exe'))) {
    throw '未找到 Nginx。请设置 NGINX_HOME 为包含 nginx.exe 的目录，或使用 -NginxHome 指定。'
}
$NginxHome=(Resolve-Path -LiteralPath $NginxHome).Path
if (-not (Test-Path -LiteralPath (Join-Path $NginxHome 'conf\mime.types'))) { throw 'Nginx 目录缺少 conf/mime.types。' }
if (-not $OutputPath) { $OutputPath=Join-Path $projectRoot 'deploy\nginx.conf' }
$template=Get-Content -Raw -LiteralPath (Join-Path $projectRoot 'deploy\nginx.conf.template')
$generated=$template.Replace('@@PROJECT_ROOT@@',$projectRoot.Replace('\','/')).Replace('@@NGINX_HOME@@',$NginxHome.Replace('\','/'))
[IO.File]::WriteAllText($OutputPath,$generated,[Text.UTF8Encoding]::new($false))
Write-Output $NginxHome
