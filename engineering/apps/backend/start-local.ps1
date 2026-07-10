#!/usr/bin/env pwsh
# Türkü Video Backend — Lokal başlatma scripti
# .env dosyasından environment variable'ları yükler

$envFile = Join-Path $PSScriptRoot ".env"

if (Test-Path $envFile) {
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and !$line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $key = $parts[0].Trim()
            $value = $parts[1].Trim()
            [System.Environment]::SetEnvironmentVariable($key, $value, "Process")
            Write-Host "  Loaded: $key = $($value.Substring(0, [Math]::Min(8, $value.Length)))..." -ForegroundColor DarkGray
        }
    }
    Write-Host "`n.env yuklendi" -ForegroundColor Green
} else {
    Write-Host ".env dosyasi bulunamadi! Olusturun: $envFile" -ForegroundColor Red
    Write-Host "Icerik ornegi: OPENAI_API_KEY=sk-..." -ForegroundColor Yellow
    exit 1
}

Write-Host "`nBackend baslatiliyor (OpenAI modunda)..." -ForegroundColor Cyan
mvn spring-boot:run "-Dspring-boot.run.profiles=local" -DskipTests
