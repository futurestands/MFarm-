# =====================================================================
# MFarm Stage D — Mandatory Security Acceptance Tests
# =====================================================================

$ErrorActionPreference = "Stop"
$env:PGPASSWORD = "postgres"

Write-Host "=== STARTING MANDATORY SECURITY ACCEPTANCE TESTS ===" -ForegroundColor Cyan

# Ensure clean database test setup
& "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -d postgres -c "DROP DATABASE IF EXISTS mfarm_val_db; CREATE DATABASE mfarm_val_db;"
Get-Content (Join-Path $PSScriptRoot "schema.sql") | & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -X -v ON_ERROR_STOP=1 -U postgres -d mfarm_val_db
Get-Content (Join-Path $PSScriptRoot "test_harness.sql") | & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -X -v ON_ERROR_STOP=1 -U postgres -d mfarm_val_db

# 1. Correct email & password -> Successful login
try {
    $loginBody = @{ email = "owner@mfarm.dev"; password = "secret" } | ConvertTo-Json
    $res = Invoke-RestMethod -Uri "http://localhost:4000/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json"
    if (-not $res.token) { throw "No token returned" }
    Write-Host "TEST 1 PASSED: Correct email & password resulted in successful login." -ForegroundColor Green
} catch {
    Write-Host "TEST 1 FAILED: $_" -ForegroundColor Red
    exit 1
}

# 2. Correct email & incorrect password -> HTTP 401
try {
    $loginBody = @{ email = "owner@mfarm.dev"; password = "wrongpassword" } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri "http://localhost:4000/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json" -ErrorAction Stop
    Write-Host "TEST 2 FAILED: Incorrect password was accepted!" -ForegroundColor Red
    exit 1
} catch {
    $status = $_.Response.StatusCode.value__
    if ($status -eq 401) {
        Write-Host "TEST 2 PASSED: Incorrect password rejected with HTTP 401." -ForegroundColor Green
    } else {
        Write-Host "TEST 2 FAILED: Expected HTTP 401, got $status" -ForegroundColor Red
        exit 1
    }
}

# 3. Unknown email -> HTTP 401
try {
    $loginBody = @{ email = "nobody@mfarm.dev"; password = "secret" } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri "http://localhost:4000/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json" -ErrorAction Stop
    Write-Host "TEST 3 FAILED: Unknown email was accepted!" -ForegroundColor Red
    exit 1
} catch {
    $status = $_.Response.StatusCode.value__
    if ($status -eq 401) {
        Write-Host "TEST 3 PASSED: Unknown email rejected with HTTP 401." -ForegroundColor Green
    } else {
        Write-Host "TEST 3 FAILED: Expected HTTP 401, got $status" -ForegroundColor Red
        exit 1
    }
}

# 4. Email-only request -> HTTP 400
try {
    $loginBody = @{ email = "owner@mfarm.dev" } | ConvertTo-Json
    $null = Invoke-RestMethod -Uri "http://localhost:4000/api/auth/login" -Method Post -Body $loginBody -ContentType "application/json" -ErrorAction Stop
    Write-Host "TEST 4 FAILED: Email-only request accepted!" -ForegroundColor Red
    exit 1
} catch {
    $status = $_.Response.StatusCode.value__
    if ($status -eq 400) {
        Write-Host "TEST 4 PASSED: Email-only request rejected with HTTP 400." -ForegroundColor Green
    } else {
        Write-Host "TEST 4 FAILED: Expected HTTP 400, got $status" -ForegroundColor Red
        exit 1
    }
}

# 5. Expired or revoked session -> HTTP 401
$expiredToken = & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -d mfarm_val_db -t -A -c "SELECT session_token FROM active_sessions WHERE expires_at < NOW() LIMIT 1;"
$farmA = & "C:\Program Files\PostgreSQL\17\bin\psql.exe" -U postgres -d mfarm_val_db -t -A -c "SELECT farm_id FROM active_sessions WHERE expires_at < NOW() LIMIT 1;"
if ($expiredToken) {
    try {
        $headers = @{ "Authorization" = "Bearer $expiredToken"; "x-farm-id" = $farmA.Trim() }
        $null = Invoke-RestMethod -Uri "http://localhost:4000/api/livestock" -Method Get -Headers $headers -ErrorAction Stop
        Write-Host "TEST 5 FAILED: Expired session token accepted!" -ForegroundColor Red
        exit 1
    } catch {
        $status = $_.Response.StatusCode.value__
        if ($status -eq 401) {
            Write-Host "TEST 5 PASSED: Expired session token rejected with HTTP 401." -ForegroundColor Green
        } else {
            Write-Host "TEST 5 FAILED: Expected HTTP 401, got $status" -ForegroundColor Red
            exit 1
        }
    }
} else {
    Write-Host "TEST 5 SKIPPED: No expired token in test context." -ForegroundColor Yellow
}

# 6. Unauthenticated livestock access -> HTTP 401
try {
    $null = Invoke-RestMethod -Uri "http://localhost:4000/api/livestock" -Method Get -ErrorAction Stop
    Write-Host "TEST 6 FAILED: Unauthenticated livestock access accepted!" -ForegroundColor Red
    exit 1
} catch {
    $status = $_.Response.StatusCode.value__
    if ($status -eq 401) {
        Write-Host "TEST 6 PASSED: Unauthenticated livestock access rejected with HTTP 401." -ForegroundColor Green
    } else {
        Write-Host "TEST 6 FAILED: Expected HTTP 401, got $status" -ForegroundColor Red
        exit 1
    }
}

Write-Host "`n=== ALL MANDATORY SECURITY ACCEPTANCE TESTS PASSED (EXIT CODE 0) ===" -ForegroundColor Green
exit 0
