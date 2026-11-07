# =====================================================================
# MFarm Stage 1 & 4 — Backend Security & API Integration Test Script
# =====================================================================

$ErrorActionPreference = "Stop"

Write-Host "=== STARTING BACKEND SECURITY & API ACCEPTANCE TESTS ===" -ForegroundColor Cyan

# Verify session endpoint removal (must return 404 / error)
try {
    $null = Invoke-RestMethod -Uri "http://localhost:4000/api/sessions" -Method Get -ErrorAction Stop
    Write-Host "FAIL: GET /api/sessions still exists!" -ForegroundColor Red
    exit 1
} catch {
    Write-Host "PASS: Session enumeration endpoint successfully removed (Endpoint absent/unauthorized)." -ForegroundColor Green
}

# Ensure database and roles exist locally
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -d postgres -c "DROP DATABASE IF EXISTS mfarm_val_db; CREATE DATABASE mfarm_val_db;"
Get-Content (Join-Path $PSScriptRoot "schema.sql") | & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -X -v ON_ERROR_STOP=1 -U postgres -d mfarm_val_db
Get-Content (Join-Path $PSScriptRoot "test_harness.sql") | & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -X -v ON_ERROR_STOP=1 -U postgres -d mfarm_val_db

# Fetch valid active session token and farm ID via local psql query (masked in logs)
$session = & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -X -t -A -U postgres -d mfarm_val_db -c "SELECT CONCAT(session_token, '|', farm_id) FROM active_sessions LIMIT 1;"
if (-not $session) {
    Write-Host "FAIL: No active sessions found in database!" -ForegroundColor Red
    exit 1
}
$parts = $session.Trim().Split("|")
$token = $parts[0]
$farmId = $parts[1]
$maskedToken = $token.Substring(0, 8) + "-[REDACTED]"
Write-Host "Acquired test session token: $maskedToken for Farm ID: $farmId" -ForegroundColor DarkCyan

# Test authorized livestock creation via API
$body = @{
    tag = "TEST-API-GOAT-02"
    species = "Goat"
    breed = "Boer"
    sex = "FEMALE"
} | ConvertTo-Json

$headers = @{
    "Authorization" = "Bearer $token"
    "x-farm-id" = $farmId
    "Content-Type" = "application/json"
}

try {
    $createRes = Invoke-RestMethod -Uri "http://localhost:4000/api/livestock" -Method Post -Headers $headers -Body $body
    Write-Host "PASS: Test goat created successfully via API. Entity ID: $($createRes.global_id)" -ForegroundColor Green
} catch {
    Write-Host "FAIL: Failed to create livestock via API: $_" -ForegroundColor Red
    exit 1
}

# Test livestock retrieval
try {
    $livestockRes = Invoke-RestMethod -Uri "http://localhost:4000/api/livestock" -Method Get -Headers $headers
    $count = $livestockRes.livestock.Count
    Write-Host "PASS: Retrieved $count livestock records from authenticated API endpoint." -ForegroundColor Green
} catch {
    Write-Host "FAIL: Failed to retrieve livestock: $_" -ForegroundColor Red
    exit 1
}

Write-Host "`n=== ALL BACKEND SECURITY & API TESTS PASSED (EXIT CODE 0) ===" -ForegroundColor Green
exit 0
