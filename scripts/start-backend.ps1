# 启动后端：优先使用 JAVA_HOME，找不到就尝试本机常见 JDK 安装位置
param(
    [string]$Profile = 'dev',
    [int]$Port = 8081,
    [string]$DbPassword = $env:DB_PASSWORD
)

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$backend = Join-Path $root 'backend'

function Find-Java([string]$home) {
    if ($home -and (Test-Path (Join-Path $home 'bin\java.exe'))) {
        return (Join-Path $home 'bin\java.exe')
    }
    $userProfile = [Environment]::GetFolderPath('UserProfile')
    foreach ($candidate in @(
        (Join-Path $userProfile '.jdks\azul-22.0.2'),
        'D:\jdk',
        'C:\Program Files\Java\jdk-17',
        'C:\Program Files\Eclipse Adoptium\jdk-17'
    )) {
        if (Test-Path (Join-Path $candidate 'bin\java.exe')) {
            return (Join-Path $candidate 'bin\java.exe')
        }
    }
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    throw '没有找到 Java，请先安装 JDK 17 或以上，并设置 JAVA_HOME'
}

function Find-Maven([string]$home) {
    if ($home -and (Test-Path (Join-Path $home 'bin\mvn.cmd'))) {
        return (Join-Path $home 'bin\mvn.cmd')
    }
    foreach ($candidate in @('D:\apache-maven-3.10.0', 'C:\Program Files\apache-maven')) {
        if (Test-Path (Join-Path $candidate 'bin\mvn.cmd')) {
            return (Join-Path $candidate 'bin\mvn.cmd')
        }
    }
    $cmd = Get-Command mvn -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    throw '没有找到 Maven，请先安装 Maven 并加入 PATH'
}

$java = Find-Java $env:JAVA_HOME
$env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $java)
$jar = Join-Path $backend 'target\trainlog-backend-1.0.0.jar'

if (-not (Test-Path $jar)) {
    $mvn = Find-Maven $env:MAVEN_HOME
    Write-Host "正在编译后端：$mvn" -ForegroundColor Cyan
    & $mvn -f (Join-Path $backend 'pom.xml') '-Dmaven.test.skip=true' package
    if ($LASTEXITCODE -ne 0) { throw '后端编译失败' }
}

$arguments = @('-jar', $jar, "--spring.profiles.active=$Profile", "--server.port=$Port")
if ($Profile -eq 'mysql' -and $DbPassword) {
    $arguments += "--spring.datasource.password=$DbPassword"
}

Write-Host "启动后端（$Profile，端口 $Port）…" -ForegroundColor Cyan
& $java @arguments
