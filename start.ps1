param([switch]$Build,[switch]$BackendOnly,[string]$NginxHome=$env:NGINX_HOME)
& (Join-Path $PSScriptRoot 'tools\start_project.ps1') @PSBoundParameters
