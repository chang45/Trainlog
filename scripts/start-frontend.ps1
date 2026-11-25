# 启动前端：自动寻找 Node 18+，缺少依赖时先安装
param([string]$Registry = 'https://registry.npmmirror.com')

$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $PSScriptRoot
$frontend = Join-Path $root 'frontend'

function Find-Node {
    foreach ($candidate in @(
        'D:\HBuilderX\plugins\node18\node.exe',
        'D:\HBuilderX\plugins\node\node.exe',
        'C:\Program Files\nodejs\node.exe',
        'C:\Program Files (x86)\nodejs\node.exe'
    )) {
        if (Test-Path $candidate) { return $candidate }
    }
    $cmd = Get-Command node -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    throw '没有找到 Node.js，请先安装 Node 18 或以上'
}

$node = Find-Node
$nodeDir = Split-Path -Parent $node
$npmCli = $null
foreach ($candidate in @(
    (Join-Path $nodeDir 'node_modules\npm\bin\npm-cli.js'),
    (Join-Path $nodeDir 'npm\node_modules\npm\bin\npm-cli.js'),
    'D:\HBuilderX\plugins\npm\node_modules\npm\bin\npm-cli.js'
)) {
    if (Test-Path $candidate) { $npmCli = $candidate; break }
}

Push-Location $frontend
try {
    if (-not (Test-Path 'node_modules')) {
        if (-not $npmCli) { throw '没有找到 npm-cli.js，请先安装前端依赖' }
        Write-Host '正在安装前端依赖…' -ForegroundColor Cyan
        & $node $npmCli install "--registry=$Registry" --no-audit --no-fund
        if ($LASTEXITCODE -ne 0) { throw '前端依赖安装失败' }
    }
    & $node (Join-Path $frontend 'node_modules\vite\bin\vite.js') --host 127.0.0.1 --port 5173
} finally {
    Pop-Location
}
