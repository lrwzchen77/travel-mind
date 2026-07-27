param(
    [Parameter(Mandatory = $true)][string]$BackupDirectory,
    [switch]$ConfirmRestore
)

$ErrorActionPreference = 'Stop'
if (!$ConfirmRestore) { throw '恢复会覆盖现有数据，请显式传入 -ConfirmRestore。' }
$compose = Join-Path $PSScriptRoot '..\compose.prod.yml'
$backup = [System.IO.Path]::GetFullPath($BackupDirectory)
foreach ($name in 'mysql.sql', 'qdrant.tgz', 'uploads.tgz') {
    if (!(Test-Path -LiteralPath (Join-Path $backup $name) -PathType Leaf)) { throw "缺少备份文件：$name" }
}

$qdrant = (& docker compose -f $compose ps -aq qdrant).Trim()
$backend = (& docker compose -f $compose ps -aq backend).Trim()
if (!$qdrant -or !$backend) { throw '找不到 Qdrant 或后端容器，请先创建生产服务。' }

& docker compose -f $compose stop backend python-ai qdrant
try {
    Get-Content -Raw -LiteralPath (Join-Path $backup 'mysql.sql') |
        & docker compose -f $compose exec -T mysql sh -c 'exec mysql -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"'
    if ($LASTEXITCODE -ne 0) { throw 'MySQL 恢复失败。' }
    & docker run --rm --volumes-from $qdrant -v "${backup}:/backup:ro" alpine sh -c 'rm -rf /qdrant/storage/* && tar -xzf /backup/qdrant.tgz -C /qdrant/storage'
    if ($LASTEXITCODE -ne 0) { throw 'Qdrant 恢复失败。' }
    & docker run --rm --volumes-from $backend -v "${backup}:/backup:ro" alpine sh -c 'rm -rf /app/uploads/* && tar -xzf /backup/uploads.tgz -C /app/uploads'
    if ($LASTEXITCODE -ne 0) { throw '上传文件恢复失败。' }
} finally {
    & docker compose -f $compose up -d qdrant python-ai backend
}

Write-Host '恢复完成，请检查 /ready 并执行冒烟测试。'
