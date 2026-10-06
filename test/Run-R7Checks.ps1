param(
    [string]$JdkPath = $env:JAVA_HOME,
    [switch]$Render,
    [string]$NativePath
)

& (Join-Path $PSScriptRoot 'Run-R6Checks.ps1') -JdkPath $JdkPath -Render:$Render -NativePath $NativePath -BuildDirectory 'out\r7-checks'
