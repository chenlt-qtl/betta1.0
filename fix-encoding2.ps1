Get-ChildItem -Path "e:/workspace/betta/Betta1.0" -Recurse -File -Include *.java | Where-Object { $_.FullName -notmatch '\\target\\' } | ForEach-Object {
    try {
        $content = [System.IO.File]::ReadAllText($_.FullName, [System.Text.Encoding]::UTF8)
        $original = $content

        # Remove any sequence of question marks that are corruption indicators
        $content = $content -replace '\?{2,}', ''

        # Fix any incomplete string values that end with corrupted characters followed by quote
        # This regex finds unclosed strings at end of lines and closes them
        $content = $content -replace '("(?:[^"\\]|\\.)*?)$', '$1"'

        if ($content -ne $original) {
            [System.IO.File]::WriteAllText($_.FullName, $content, [System.Text.Encoding]::UTF8)
            Write-Host "Fixed: $($_.FullName)"
        }
    } catch {
        Write-Host "Error processing: $($_.FullName) - $_"
    }
}
