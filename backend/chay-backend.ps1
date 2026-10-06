# =====================================================================================
#  BAT BACKEND SPRING BOOT (cong 8080)
#  Chay:  powershell -ExecutionPolicy Bypass -File "D:\iot-dashboard-mockup\backend\chay-backend.ps1"
#  Dung:  Ctrl+C
# =====================================================================================

$host.UI.RawUI.WindowTitle = "Backend Spring Boot 8080 - Smart Classroom"

$Mvn = "C:\Users\Admin\tools\apache-maven-3.9.9\bin\mvn.cmd"

if (-not (Test-Path $Mvn)) {
    Write-Host "Khong thay Maven tai: $Mvn" -ForegroundColor Red
    Read-Host "Bam Enter de dong"
    exit 1
}

# Backend can MySQL de khoi dong
if (-not (Get-NetTCPConnection -LocalPort 3306 -State Listen -ErrorAction SilentlyContinue)) {
    Write-Host "MySQL chua chay. Mo PowerShell admin va chay:  net start MySQL84" -ForegroundColor Red
    Read-Host "Bam Enter de dong"
    exit 1
}

Set-Location "D:\iot-dashboard-mockup\backend"

Write-Host "Dang khoi dong Backend... (lan dau hoi lau vi Maven tai thu vien)" -ForegroundColor Cyan
Write-Host ""

& $Mvn spring-boot:run
