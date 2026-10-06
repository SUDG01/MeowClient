param(
    [string]$JdkPath = $env:JAVA_HOME,
    [switch]$Render,
    [string]$NativePath,
    [string]$BuildDirectory = 'out\r6-checks'
)

$ErrorActionPreference = 'Stop'
$taskRoot = Split-Path -Parent $PSScriptRoot
$taskOutput = Join-Path $taskRoot $BuildDirectory

if (-not $JdkPath) {
    $taskJavaDirectory = Join-Path $env:ProgramFiles 'Java'
    if (Test-Path -LiteralPath $taskJavaDirectory) {
        $taskJdk = Get-ChildItem -LiteralPath $taskJavaDirectory -Directory |
            Where-Object { $_.Name -like 'jdk1.8*' } | Sort-Object Name -Descending | Select-Object -First 1
        if ($taskJdk) { $JdkPath = $taskJdk.FullName }
    }
}

$taskJavac = if ($JdkPath) { Join-Path $JdkPath 'bin\javac.exe' } else { (Get-Command javac).Source }
$taskJava = Join-Path (Split-Path -Parent $taskJavac) 'java.exe'
if (-not (Test-Path -LiteralPath $taskJavac)) { throw 'JDK 8 was not found. Supply -JdkPath.' }
if (-not $NativePath) { $NativePath = Join-Path $taskRoot 'jars\versions\1.8.8\1.8.8-natives' }
if (-not [System.IO.Path]::IsPathRooted($NativePath)) { $NativePath = Join-Path $taskRoot $NativePath }

New-Item -ItemType Directory -Force $taskOutput | Out-Null
$taskSourceList = Join-Path $taskOutput 'sources.txt'
$taskSources = Get-ChildItem -LiteralPath (Join-Path $taskRoot 'src'), $PSScriptRoot -Recurse -Filter '*.java' |
    ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' }
[System.IO.File]::WriteAllLines($taskSourceList, [string[]]$taskSources, (New-Object System.Text.UTF8Encoding($false)))
$taskLibs = Join-Path $taskRoot 'lib\*'
$taskClasspath = $taskOutput + ';' + $taskLibs + ';' + (Join-Path $taskRoot 'resources')

& $taskJavac -encoding UTF-8 -source 8 -target 8 -cp $taskLibs -d $taskOutput ('@' + $taskSourceList)
if ($LASTEXITCODE -ne 0) { throw 'Java compilation failed.' }

# Keep Minecraft's logger output in the ignored build directory.
Push-Location -LiteralPath $taskOutput
try {
    foreach ($taskTest in @('R5RegressionTest', 'ClientUiRegressionTest', 'R6FeatureRegressionTest', 'HighPollingInputRegressionTest', 'R7CombatRegressionTest')) {
        & $taskJava -cp $taskClasspath $taskTest
        if ($LASTEXITCODE -ne 0) { throw "$taskTest failed." }
    }
    if ($Render) {
        if (-not (Test-Path -LiteralPath $NativePath)) { throw 'LWJGL natives were not found. Supply -NativePath.' }
        & $taskJava ('-Djava.library.path=' + $NativePath) -cp $taskClasspath R6RenderRegressionTest (Join-Path $taskOutput 'render-previews')
        if ($LASTEXITCODE -ne 0) { throw 'R6RenderRegressionTest failed.' }
        if ($env:OS -eq 'Windows_NT') {
            & $taskJava ('-Djava.library.path=' + $NativePath) '-Dmeow.test.nativeInput=true' -cp $taskClasspath HighPollingInputRegressionTest
            if ($LASTEXITCODE -ne 0) { throw 'Native mouse buffer checks failed.' }
            & $taskJava -cp $taskClasspath RawMouseBackendSmokeTest
            if ($LASTEXITCODE -ne 0) { throw 'RawMouseBackendSmokeTest failed.' }
        }
    }
} finally {
    Pop-Location
}
