# =====================================================================================
#  DONG GOI CA HE THONG THANH MOT FILE JAR DUY NHAT
#
#  Chay:  powershell -ExecutionPolicy Bypass -File "D:\iot-dashboard-mockup\backend\dong-goi.ps1"
#
#  Cac buoc:
#    1. Build React thanh file tinh (frontend/dist)
#    2. Chep sang backend/src/main/resources/static
#    3. Dong goi Backend thanh file .jar (da bao gom giao dien)
#
#  Chay lai script nay moi khi sua giao dien React.
# =====================================================================================

$ErrorActionPreference = "Stop"

$Root     = "D:\iot-dashboard-mockup"
$Frontend = "$Root\frontend"
$Backend  = "$Root\backend"
$Static   = "$Backend\src\main\resources\static"
$Mvn      = "C:\Users\Admin\tools\apache-maven-3.9.9\bin\mvn.cmd"

$env:Path = "C:\Program Files\nodejs;" + $env:Path

Write-Host ""
Write-Host "==========================================================" -ForegroundColor White
Write-Host "  DONG GOI SMART CLASSROOM" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor White

# --- 1. Build React ---
Write-Host ""
Write-Host "  [1/3] Build giao dien React..." -ForegroundColor Cyan
Set-Location $Frontend
& npm run build
if ($LASTEXITCODE -ne 0) {
    Write-Host "  Build React that bai." -ForegroundColor Red
    exit 1
}

# --- 2. Chep sang thu muc static cua Backend ---
Write-Host ""
Write-Host "  [2/3] Chep giao dien vao Backend..." -ForegroundColor Cyan
if (Test-Path $Static) {
    Remove-Item "$Static\*" -Recurse -Force -ErrorAction SilentlyContinue
} else {
    New-Item -ItemType Directory -Path $Static -Force | Out-Null
}
Copy-Item "$Frontend\dist\*" -Destination $Static -Recurse -Force
Write-Host "        Da chep sang $Static" -ForegroundColor Green

# --- 3. Dong goi Backend ---
Write-Host ""
Write-Host "  [3/3] Dong goi Backend thanh file .jar..." -ForegroundColor Cyan
Set-Location $Backend
& $Mvn -B -q clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "  Dong goi that bai." -ForegroundColor Red
    exit 1
}

$jar = Get-ChildItem "$Backend\target\*.jar" | Where-Object { $_.Name -notlike "*sources*" } | Select-Object -First 1

Write-Host ""
Write-Host "==========================================================" -ForegroundColor White
Write-Host "  XONG" -ForegroundColor Green
Write-Host "  File: $($jar.FullName)" -ForegroundColor Gray
Write-Host "  Kich thuoc: $([math]::Round($jar.Length/1MB,1)) MB" -ForegroundColor Gray
Write-Host ""
Write-Host "  Chay he thong: bam dup file CHAY-WEB.bat" -ForegroundColor Cyan
Write-Host "  Dia chi web:   http://localhost:8080" -ForegroundColor Cyan
Write-Host "==========================================================" -ForegroundColor White
Write-Host ""
