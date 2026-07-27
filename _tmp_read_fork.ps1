$lines = Get-Content 'C:\Users\Administrator\.claude\projects\D--xxqxm\41d6bc27-0dba-4afb-adf0-629c348a0076.jsonl'
$idx = 0
foreach ($line in $lines) {
    $idx++
    if ($idx -lt 305 -or $idx -gt 340) { continue }
    try {
        $obj = $line | ConvertFrom-Json
        Write-Host "=== Entry [Line $idx] Type: $($obj.type) ==="
        if ($obj.message.role) {
            Write-Host "Role: $($obj.message.role)"
        }
        if ($obj.message.content) {
            $content = $obj.message.content
            if ($content -is [string]) {
                if ($content.Length -gt 0) {
                    $txt = $content
                    if ($txt.Length -gt 3000) { $txt = $txt.Substring(0, 3000) }
                    Write-Host "Content: $txt"
                }
            } else {
                foreach ($c in $content) {
                    if ($c.type -eq 'text') {
                        $txt = $c.text
                        if ($txt.Length -gt 4000) { $txt = $txt.Substring(0, 4000) }
                        Write-Host "Text: $txt"
                    } elseif ($c.type -eq 'tool_use') {
                        $name = $c.name
                        $inp = $c.input | ConvertTo-Json -Depth 10 -Compress
                        if ($inp.Length -gt 3000) { $inp = $inp.Substring(0, 3000) }
                        Write-Host "ToolUse: $name"
                        Write-Host "Input: $inp"
                    } elseif ($c.type -eq 'tool_result') {
                        $cnt = $c.content
                        if ($cnt -is [string]) {
                            if ($cnt.Length -gt 1500) { $cnt = $cnt.Substring(0, 1500) }
                            Write-Host "ToolResult: $cnt"
                        } else {
                            foreach ($cc in $cnt) {
                                if ($cc.type -eq 'text') {
                                    $t = $cc.text
                                    if ($t.Length -gt 1500) { $t = $t.Substring(0, 1500) }
                                    Write-Host "ToolResult: $t"
                                }
                            }
                        }
                    }
                }
            }
        }
        Write-Host ""
    } catch {
        Write-Host "Parse error: $_"
    }
}
