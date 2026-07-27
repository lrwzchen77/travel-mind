$lines = Get-Content 'C:\Users\Administrator\.claude\projects\D--xxqxm\41d6bc27-0dba-4afb-adf0-629c348a0076.jsonl'
$idx = 0
$firstDone = $false
foreach ($line in $lines) {
    $idx++
    if ($idx -lt 5 -or $idx -lt 80) { continue }
    try {
        $obj = $line | ConvertFrom-Json
        if ($obj.type -eq 'assistant' -and $obj.message.role -eq 'assistant') {
            $content = $obj.message.content
            foreach ($c in $content) {
                if ($c.type -eq 'text') {
                    $txt = $c.text
                    if ($txt -notmatch 'API Error' -and $txt.Length -gt 20) {
                        if ($txt.Length -gt 4000) { $txt = $txt.Substring(0, 4000) }
                        Write-Host "=== Assistant Text [Line $idx] ==="
                        Write-Host $txt
                        Write-Host ""
                    }
                } elseif ($c.type -eq 'tool_use') {
                    $name = $c.name
                    $inp = $c.input | ConvertTo-Json -Depth 5 -Compress
                    if ($inp.Length -gt 800) { $inp = $inp.Substring(0, 800) }
                    Write-Host "=== ToolUse [Line $idx]: $name ==="
                    Write-Host $inp
                    Write-Host ""
                }
            }
        }
    } catch {
        # Skip
    }
}
