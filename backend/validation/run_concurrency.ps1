# =====================================================================
# MFarm Stage D — Deterministic Concurrency & Rollback Recovery Script
# =====================================================================

$ErrorActionPreference = "Stop"
$scriptDir = $PSScriptRoot

Write-Host "=== STARTING STAGE D DETERMINISTIC CONCURRENCY & BARRIER TEST ===" -ForegroundColor Cyan
Write-Host "Script Directory: $scriptDir" -ForegroundColor DarkCyan

$sessionInfo = docker exec mfarm-val-postgres-16 psql -X -t -A -U postgres -d mfarm_val_db -c "SELECT session_token || '|' || farm_id FROM active_sessions LIMIT 1;"
if (-not $sessionInfo) {
    throw "No active sessions found in database for concurrency test!"
}
$parts = $sessionInfo.Trim().Split("|")
$token = $parts[0]
$farmId = $parts[1]
$maskedToken = $token.Substring(0, 8) + "-[REDACTED]"

Write-Host "Using Farm ID: $farmId with Session Token: $maskedToken" -ForegroundColor DarkCyan

# -------------------------------------------------------------------------
# SCENARIO 1: Connection A commits, Connection B commits concurrently
# -------------------------------------------------------------------------
Write-Host "`n--- SCENARIO 1: Connection A Commits, Connection B Commits ---" -ForegroundColor Yellow

$scriptA1 = @'
SELECT auth.authenticate_session('__TOKEN__', '__FARMID__');
BEGIN;
SELECT create_livestock('__FARMID__', 'BARRIER-TAG-A1', 'Cattle', 'Boran');
SELECT pg_sleep(1.5);
COMMIT;
'@ -replace '__TOKEN__', $token -replace '__FARMID__', $farmId

$scriptB1 = @'
SELECT auth.authenticate_session('__TOKEN__', '__FARMID__');
SELECT pg_sleep(0.3);
SELECT create_livestock('__FARMID__', 'BARRIER-TAG-B1', 'Cattle', 'Friesian');
'@ -replace '__TOKEN__', $token -replace '__FARMID__', $farmId

$job1 = Start-Job -ScriptBlock {
    param($cmd)
    $tsStart = Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff"
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $out = docker exec mfarm-val-postgres-16 psql -X -v ON_ERROR_STOP=1 -U postgres -d mfarm_val_db -c "$cmd" 2>&1
    $sw.Stop()
    [PSCustomObject]@{ Connection = "A1"; Start = $tsStart; DurationMs = $sw.ElapsedMilliseconds; ExitCode = $LASTEXITCODE; Output = "$out" }
} -ArgumentList $scriptA1

Start-Sleep -Milliseconds 200

$job2 = Start-Job -ScriptBlock {
    param($cmd)
    $tsStart = Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff"
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $out = docker exec mfarm-val-postgres-16 psql -X -v ON_ERROR_STOP=1 -U postgres -d mfarm_val_db -c "$cmd" 2>&1
    $sw.Stop()
    [PSCustomObject]@{ Connection = "B1"; Start = $tsStart; DurationMs = $sw.ElapsedMilliseconds; ExitCode = $LASTEXITCODE; Output = "$out" }
} -ArgumentList $scriptB1

$completed = $job1, $job2 | Wait-Job -Timeout 20
if (-not $completed) {
    Stop-Job $job1, $job2
    throw "Scenario 1 jobs timed out!"
}

$resA1 = Receive-Job $job1
$resB1 = Receive-Job $job2
Remove-Job $job1, $job2

Write-Host "Connection A1 ExitCode: $($resA1.ExitCode), Duration: $($resA1.DurationMs)ms"
Write-Host "Connection B1 ExitCode: $($resB1.ExitCode), Duration: $($resB1.DurationMs)ms"

if ($resA1.ExitCode -ne 0 -or $resB1.ExitCode -ne 0) {
    Write-Host "Connection A1 Output:`n$($resA1.Output)"
    Write-Host "Connection B1 Output:`n$($resB1.Output)"
    throw "Scenario 1 concurrency test failed!"
}

# -------------------------------------------------------------------------
# SCENARIO 2: Connection A rolls back, Connection B succeeds
# -------------------------------------------------------------------------
Write-Host "`n--- SCENARIO 2: Connection A Rolls Back, Connection B Succeeds ---" -ForegroundColor Yellow

$scriptA2 = @'
SELECT auth.authenticate_session('__TOKEN__', '__FARMID__');
BEGIN;
SELECT create_livestock('__FARMID__', 'BARRIER-TAG-A2', 'Cattle', 'Ankole');
SELECT pg_sleep(1.5);
ROLLBACK;
'@ -replace '__TOKEN__', $token -replace '__FARMID__', $farmId

$scriptB2 = @'
SELECT auth.authenticate_session('__TOKEN__', '__FARMID__');
SELECT pg_sleep(0.3);
SELECT create_livestock('__FARMID__', 'BARRIER-TAG-B2', 'Cattle', 'Jersey');
'@ -replace '__TOKEN__', $token -replace '__FARMID__', $farmId

$job3 = Start-Job -ScriptBlock {
    param($cmd)
    $tsStart = Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff"
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $out = docker exec mfarm-val-postgres-16 psql -X -v ON_ERROR_STOP=1 -U postgres -d mfarm_val_db -c "$cmd" 2>&1
    $sw.Stop()
    [PSCustomObject]@{ Connection = "A2"; Start = $tsStart; DurationMs = $sw.ElapsedMilliseconds; ExitCode = $LASTEXITCODE; Output = "$out" }
} -ArgumentList $scriptA2

Start-Sleep -Milliseconds 200

$job4 = Start-Job -ScriptBlock {
    param($cmd)
    $tsStart = Get-Date -Format "yyyy-MM-dd HH:mm:ss.fff"
    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $out = docker exec mfarm-val-postgres-16 psql -X -v ON_ERROR_STOP=1 -U postgres -d mfarm_val_db -c "$cmd" 2>&1
    $sw.Stop()
    [PSCustomObject]@{ Connection = "B2"; Start = $tsStart; DurationMs = $sw.ElapsedMilliseconds; ExitCode = $LASTEXITCODE; Output = "$out" }
} -ArgumentList $scriptB2

$completed2 = $job3, $job4 | Wait-Job -Timeout 20
if (-not $completed2) {
    Stop-Job $job3, $job4
    throw "Scenario 2 jobs timed out!"
}

$resA2 = Receive-Job $job3
$resB2 = Receive-Job $job4
Remove-Job $job3, $job4

Write-Host "Connection A2 ExitCode: $($resA2.ExitCode), Duration: $($resA2.DurationMs)ms"
Write-Host "Connection B2 ExitCode: $($resB2.ExitCode), Duration: $($resB2.DurationMs)ms"

if ($resA2.ExitCode -ne 0 -or $resB2.ExitCode -ne 0) {
    Write-Host "Connection A2 Output:`n$($resA2.Output)"
    Write-Host "Connection B2 Output:`n$($resB2.Output)"
    throw "Scenario 2 concurrency test failed!"
}

$tagCount = docker exec mfarm-val-postgres-16 psql -X -t -A -U postgres -d mfarm_val_db -c "SELECT COUNT(*) FROM livestock WHERE tag = 'BARRIER-TAG-A2';"
if ([int]$tagCount -ne 0) {
    throw "Rolled back mutation BARRIER-TAG-A2 was incorrectly persisted!"
}

Write-Host "`n=== DETERMINISTIC CONCURRENCY & ROLLBACK RECOVERY: PASS ===" -ForegroundColor Green
