$files = Get-ChildItem -Path . -Recurse -Include *.kt,*.kts,*.xml,*.md,*.pro,*.properties
foreach ($file in $files) {
    if ($file.FullName -match "\\.git\\" -or $file.FullName -match "\\build\\") { continue }
    $content = Get-Content -Path $file.FullName -Raw
    $newContent = $content -replace 'com\.aleph', 'com.anegan' -replace 'Aleph', 'Anegan'
    if ($content -cne $newContent) {
        Set-Content -Path $file.FullName -Value $newContent -NoNewline
    }
}

$dirs = Get-ChildItem -Path . -Recurse -Directory -Filter 'aleph' | Where-Object { $_.FullName -match 'src[\\/]main[\\/]java[\\/]com[\\/]aleph' -or $_.FullName -match 'src[\\/]androidTest[\\/]java[\\/]com[\\/]aleph' -or $_.FullName -match 'src[\\/]test[\\/]java[\\/]com[\\/]aleph' }
foreach ($dir in $dirs) {
    Rename-Item -Path $dir.FullName -NewName 'anegan'
}
