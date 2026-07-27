$lines = Get-Content 'C:\Users\Administrator\.claude\projects\D--xxqxm\41d6bc27-0dba-4afb-adf0-629c348a0076.jsonl' -Tail 10
foreach ($line in $lines) {
    try {
        $obj = $line | ConvertFrom-Json
        Write-Host "=== Entry ==="
        Write-Host "Type: $($obj.type)"
        if ($obj.message.role) {
            Write-Host "Role: $($obj.message.role)"
        }
        if ($obj.message.content) {
            foreach ($c in $obj.message.content) {
                if ($c.type -eq 'text') {
                    $txt = $c.text
                    if ($txt.Length -gt 3000) { $txt = $txt.Substring(0, 3000) }
                    Write-Host "Text: $txt"
                } elseif ($c.type -eq 'tool_use') {
                    Write-Host "ToolUse: $($c.name)"
                    $inp = $c.input | ConvertTo-Json -Depth 5 -Compress
                    if ($inp.Length -gt 2000) { $inp = $inp.Substring(0, 2000) }
                    Write-Host "Input: $inp"
                } elseif ($c.type -eq 'tool_result') {
                    $cnt = $c.content
                    if ($cnt -is [string]) {
                        if ($cnt.Length -gt 1000) { $cnt = $cnt.Substring(0, 1000) }
                        Write-Host "ToolResult: $cnt"
                    } else {
                        foreach ($cc in $cnt) {
                            if ($cc.type -eq 'text') {
                                $t = $cc.text
                                if ($t.Length -gt 1000) { $t = $t.Substring(0, 1000) }
                                Write-Host "ToolResult: $t"
                            }
                        }
                    }
                }
            }
        }
    } catch {
        Write-Host "Parse error: $_"
    }
}
