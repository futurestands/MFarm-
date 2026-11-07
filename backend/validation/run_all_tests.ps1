# =====================================================================
# MFarm Stage D — Complete Acceptance Suite Execution (Run 1 & Run 2)
# =====================================================================

$ErrorActionPreference = "Stop"
$scriptDir = $PSScriptRoot

function Run-AcceptanceSuite($runName) {
    Write-Host "`n=====================================================" -ForegroundColor Yellow
    Write-Host "STARTING STAGE D ACCEPTANCE SUITE: $runName" -ForegroundColor Yellow
    Write-Host "=====================================================" -ForegroundColor Yellow

    Write-Host "Terminating active connections and recreating mfarm_val_db..." -ForegroundColor DarkCyan
    docker exec -i mfarm-val-postgres-16 psql -U postgres -d postgres -c "SELECT pg_terminate_backend(pid) FROM pg_stat_activity WHERE datname = 'mfarm_val_db';"
    docker exec -i mfarm-val-postgres-16 psql -U postgres -d postgres -c "DROP DATABASE IF EXISTS mfarm_val_db;"
    docker exec -i mfarm-val-postgres-16 psql -U postgres -d postgres -c "CREATE DATABASE mfarm_val_db;"

    Write-Host "Deploying schema.sql..." -ForegroundColor DarkCyan
    Get-Content (Join-Path $scriptDir "schema.sql") | docker exec -i mfarm-val-postgres-16 psql -X -v ON_ERROR_STOP=1 -U postgres -d mfarm_val_db
    if ($LASTEXITCODE -ne 0) { throw "Schema deployment failed in $runName" }

    Write-Host "Executing test_harness.sql..." -ForegroundColor DarkCyan
    Get-Content (Join-Path $scriptDir "test_harness.sql") | docker exec -i mfarm-val-postgres-16 psql -X -v ON_ERROR_STOP=1 -U postgres -d mfarm_val_db
    if ($LASTEXITCODE -ne 0) { throw "Test harness failed in $runName" }

    Write-Host "Executing run_concurrency.ps1..." -ForegroundColor DarkCyan
    & (Join-Path $scriptDir "run_concurrency.ps1")
    if ($LASTEXITCODE -ne 0) { throw "Concurrency script failed in $runName" }

    Write-Host "=== $runName COMPLETED SUCCESSFULLY (EXIT CODE 0) ===" -ForegroundColor Green
}

Run-AcceptanceSuite "RUN 1"
Run-AcceptanceSuite "RUN 2"

Write-Host "`n=====================================================" -ForegroundColor Green
Write-Host "ALL STAGE D ACCEPTANCE SUITES (RUN 1 & RUN 2) PASSED SUCCESSFULLY." -ForegroundColor Green
Write-Host "=====================================================" -ForegroundColor Green
