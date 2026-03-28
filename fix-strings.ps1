Get-ChildItem -Path "e:/workspace/betta/Betta1.0" -Recurse -File -Include *.java | Where-Object { $_.FullName -notmatch '\\target\\' } | ForEach-Object {
    $content = Get-Content $_.FullName -Raw
    $original = $content

    # Fix patterns where string values are truncated mid-character
    # These are patterns like: "绔?浠€ or "閭€ that should be properly closed strings
    $content = $content -replace '("(?:[^"\\]|\\.)*)$', '"'

    if ($content -ne $original) {
        $content | Set-Content -Path $_.FullName -Encoding UTF8 -NoNewline
        Write-Host "Fixed: $($_.FullName)"
    }
}
