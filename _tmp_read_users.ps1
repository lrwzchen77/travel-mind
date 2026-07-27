$lines = Get-Content 'C:\Users\Administrator\.claude\projects\D--xxqxm\41d6bc27-0dba-4afb-adf0-629c348a0076.jsonl'
$idx = 0
foreach ($line in $lines) {
    $idx++
    try {
        $obj = $line | ConvertFrom-Json
        if ($obj.type -eq 'user' -and $obj.message.role -eq 'user') {
            $content = $obj.message.content
            if ($content -is [string]) {
                if ($content -notmatch 'tool_result' -and $content.Length -gt 5 -and $content -notmatch '^\{') {
                    $txt = $content
                    if ($txt.Length -gt 1500) { $txt = $txt.Substring(0, 1500) }
                    Write-Host "=== User Message [Line $idx] ==="
                    Write-Host $txt
                    Write-Host ""
                }
            } else {
                foreach ($c in $content) {
                    if ($c.type -eq 'text') {
                        $txt = $c.text
                        if ($txt.Length -gt 5 -and $txt -notmatch 'system-reminder') {
                            if ($txt.Length -gt 1500) { $txt = $txt.Substring(0, 1500) }
                            Write-Host "=== User Message [Line $idx] ==="
                            Write-Host $txt
                            Write-Host ""
                        }
                    }
                }
            }
        }
    } catch {
        # Skip
    }
}
