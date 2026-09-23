# =====================================================================================
#  BAT FRONTEND REACT (cong 5173)
#  Chay:  powershell -ExecutionPolicy Bypass -File "D:\iot-dashboard-mockup\frontend\chay-frontend.ps1"
#  Dung:  Ctrl+C
# =====================================================================================

$host.UI.RawUI.WindowTitle = "Frontend React 5173 - Smart Classroom"

$env:Path = "C:\Program Files\nodejs;" + $env:Path

Set-Location "D:\iot-dashboard-mockup\frontend"

if (-not (Test-Path "node_modules")) {
    Write-Host "Chua co node_modules, dang cai thu vien..." -ForegroundColor Yellow
    npm install
}

Write-Host "Dang khoi dong Frontend tai http://localhost:5173" -ForegroundColor Cyan
Write-Host ""

npm run dev
