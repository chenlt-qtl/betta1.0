Get-ChildItem -Path "e:/workspace/betta/Betta1.0" -Recurse -File -Include *.java | Where-Object { $_.FullName -notmatch '\\target\\' } | ForEach-Object {
    $content = Get-Content $_.FullName -Raw -Encoding UTF8
    $originalContent = $content

    # Replace all non-ASCII garbled characters with empty string
    # Common garbled patterns in this file encoding corruption
    $garbledPatterns = '锟\?q\?|閿\?|浠\?s\?€\?|绛\?|浠撯€\?|澶€€\?|鍒€浠\?|椤\?|浠撱€\?|娴e€\?|娴e\[q\?|鍒€浠\?q\?|绔\?\[q\?|鑴戦€\?|绫汇€\?|杈撴€\?|杈撴垨\?|涓€銆€\?|涓€鍥句富\?|娴e巻\?|涓€鍥剧被\?|閿€浠\?|閿€澶\?|閿€绫\?|閿€绛\?|閿€绔\?|閿€椤\?|閿€鑴\?|绛€閿\?|绛€澶\?|绔\?浠\?|鍥句富\?|绫婚敭\?|浠躲€\?|娴犳挶\?|娴犳€\?|浠€\?|閿€|锟\?q\?|锟\?q\?|锟\?q\?|锟\?q\?|锟\?q\?|锟\?q\?|锟\?q\?|锟\?q\?|锟\?q\?|锟\?q\?|锟\?q\?|锟\?q\?|浠撯€\?|澶€€\?|鍒€浠\?|浠撱€\?|娴e€\?|鍒€浠\?|浠撯€\?|澶€€\?|娴e\[q\?|锟\?q\?|锟\?q\?|锟\?q\?|锟\?q\?|锟\?q\?|锟\?q\?|锟\?q\?|锟\?q\?|锟\?q\?|锟\?q\?|浠撱€\?|娴e\[q\?|閿€浠\?|閿€澶\?|閿€绫\?|閿€绛\?|閿€绔\?|閿€椤\?|閿€鑴\?|绛€閿\?|绛€澶\?|绔\?浠\?|浠€|閿€|娴犳\?|娴犳挶|娴€\?'

    $content = $content -replace $garbledPatterns, ''

    # Remove any remaining ? that appear in sequences (likely from corrupted chars)
    $content = $content -replace '\?{2,}', ''

    # If content changed, write back
    if ($content -ne $originalContent) {
        $content | Set-Content -Path $_.FullName -Encoding UTF8 -NoNewline
        Write-Host "Fixed: $($_.FullName)"
    }
}
