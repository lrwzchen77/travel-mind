param(
    [string]$Destination = (Join-Path $PSScriptRoot "..\backups\$(Get-Date -Format 'yyyyMMdd-HHmmss')")
)

$ErrorActionPreference = 'Stop'
$compose = Join-Path $PSScriptRoot '..\compose.prod.yml'
$destinationPath = [System.IO.Path]::GetFullPath($Destination)
New-Item -ItemType Directory -Force -Path $destinationPath | Out-Null

& docker compose -f $compose exec -T mysql sh -c 'exec mysqldump --single-transaction --routines --triggers -u"$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DATABASE"' |
    Set-Content -Encoding utf8 -Path (Join-Path $destinationPath 'mysql.sql')
if ($LASTEXITCODE -ne 0) { throw 'MySQL 备份失败。' }

$qdrant = (& docker compose -f $compose ps -q qdrant).Trim()
$backend = (& docker compose -f $compose ps -q backend).Trim()
if (!$qdrant -or !$backend) { throw 'Qdrant 或后端容器未运行。' }

& docker run --rm --volumes-from $qdrant -v "${destinationPath}:/backup" alpine tar -czf /backup/qdrant.tgz -C /qdrant/storage .
if ($LASTEXITCODE -ne 0) { throw 'Qdrant 备份失败。' }
& docker run --rm --volumes-from $backend -v "${destinationPath}:/backup" alpine tar -czf /backup/uploads.tgz -C /app/uploads .
if ($LASTEXITCODE -ne 0) { throw '上传文件备份失败。' }

Get-ChildItem -LiteralPath $destinationPath -File | ForEach-Object {
    [ordered]@{ file = $_.Name; sha256 = (Get-FileHash -Algorithm SHA256 -LiteralPath $_.FullName).Hash; bytes = $_.Length }
} | ConvertTo-Json | Set-Content -Encoding utf8 -Path (Join-Path $destinationPath 'manifest.json')

Write-Host "备份完成：$destinationPath"
