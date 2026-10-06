# =====================================================================================
#  KIEM THU TOAN BO REST API CUA BACKEND
#  Chay:  powershell -ExecutionPolicy Bypass -File "D:\iot-dashboard-mockup\backend\test-api.ps1"
#  Yeu cau: Backend dang chay o http://localhost:8080
# =====================================================================================

$Base = "http://localhost:8080/api"
$pass = 0
$fail = 0

# Hien tieng Viet co dau cho dung (PowerShell 5.1 mac dinh khong dung UTF-8)
try {
    [Console]::OutputEncoding = [System.Text.Encoding]::UTF8
    $OutputEncoding = [System.Text.Encoding]::UTF8
} catch {
    # khong doi duoc thi thoi, chi anh huong hien thi
}

function Test-Api($name, $method, $path, $body) {
    Write-Host ""
    Write-Host "--- $name" -ForegroundColor Cyan
    Write-Host "    $method $path" -ForegroundColor DarkGray
    try {
        if ($body) {
            $result = Invoke-RestMethod -Uri ($Base + $path) -Method $method -Body $body -ContentType "application/json" -TimeoutSec 20
        } else {
            $result = Invoke-RestMethod -Uri ($Base + $path) -Method $method -TimeoutSec 20
        }
        Write-Host "    OK" -ForegroundColor Green
        $json = $result | ConvertTo-Json -Depth 4 -Compress
        if ($json.Length -gt 400) { $json = $json.Substring(0, 400) + "..." }
        Write-Host "    $json" -ForegroundColor Gray
        $script:pass++
        return $result
    } catch {
        $code = $null
        if ($_.Exception.Response) { $code = [int]$_.Exception.Response.StatusCode }
        if ($code) {
            Write-Host "    HTTP $code - $($_.Exception.Message)" -ForegroundColor Yellow
        } else {
            Write-Host "    LOI: $($_.Exception.Message)" -ForegroundColor Red
        }
        $script:fail++
        return $null
    }
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor White
Write-Host "  KIEM THU API BACKEND - SMART CLASSROOM" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor White

Test-Api "Ho so tac gia"            "GET"  "/profile"
$devices = Test-Api "Danh sach thiet bi" "GET" "/devices"
Test-Api "So do moi nhat"           "GET"  "/sensors/latest"
Test-Api "Lich su cam bien"         "GET"  "/sensors?page=1&size=5"
Test-Api "Loc rieng nhiet do"       "GET"  "/sensors?sensorType=temperature&page=1&size=3"
Test-Api "Nhat ky dieu khien"       "GET"  "/action-history?page=1&size=5"

Write-Host ""
Write-Host "--- Dieu khien thiet bi (can ESP32 that de thanh cong)" -ForegroundColor Cyan
if ($devices -and $devices.Count -gt 0) {
    $id = $devices[0].id
    $current = $devices[0].currentState
    $command = if ($current -eq "ON") { "TURN_OFF" } else { "TURN_ON" }
    Write-Host "    Thiet bi id=$id dang $current, se gui $command" -ForegroundColor DarkGray
    Write-Host "    (khong co ESP32 that thi se cho ~3 giay roi tra 503 - DUNG nhu thiet ke)" -ForegroundColor DarkGray
    Test-Api "POST dieu khien" "POST" "/devices/control/$id" "{`"command`":`"$command`"}"
} else {
    Write-Host "    Bo qua vi chua lay duoc danh sach thiet bi" -ForegroundColor Yellow
}

Write-Host ""
Write-Host "==========================================================" -ForegroundColor White
Write-Host "  KET QUA: $pass thanh cong / $fail that bai" -ForegroundColor White
Write-Host "==========================================================" -ForegroundColor White
Write-Host ""
