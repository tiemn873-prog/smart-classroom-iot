# Smart Class IoT — Backend (Java Spring Boot)

Tầng giữa theo Chương 2–3 báo cáo: nhận MQTT từ ESP32 → lưu MySQL → đẩy WebSocket xuống web, kèm 6 REST API và cơ chế điều khiển một pha có xác nhận trạng thái thật.

## Cần có trước khi chạy

| Thành phần | Ghi chú |
|---|---|
| Java 23 | đã cài (`java -version`) |
| Maven | `C:\Users\Admin\tools\apache-maven-3.9.9\bin\mvn.cmd` |
| MySQL 8.4 | bật bằng `net start MySQL84` (PowerShell admin) |
| Mosquitto | cổng **8813**, tài khoản `NguyenDungTiem` |

## Tạo database và tài khoản (chạy một lần trong MySQL Workbench)

Ứng dụng dùng tài khoản riêng `smartclass`, không dùng `root`:

```sql
CREATE DATABASE IF NOT EXISTS smart_class
  CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

CREATE USER IF NOT EXISTS 'smartclass'@'localhost'
  IDENTIFIED BY 'YOUR_DATABASE_PASSWORD';

GRANT ALL PRIVILEGES ON smart_class.* TO 'smartclass'@'localhost';

FLUSH PRIVILEGES;
```

Tài khoản này đã được điền sẵn trong `src/main/resources/application.properties`. Đổi mật khẩu thì sửa cả hai chỗ.

## Chạy

```powershell
$env:Path = "C:\Users\Admin\tools\apache-maven-3.9.9\bin;" + $env:Path
cd D:\iot-dashboard-mockup\backend
mvn spring-boot:run
```

Khởi động xong, log in `Started SmartClassApplication`. Backend nghe ở **http://localhost:8080**.

Database `smart_class` tự được tạo, 5 bảng tự sinh từ các `@Entity`, và `DataSeeder` nạp sẵn 1 hồ sơ + 2 thiết bị + 3 cảm biến.

## REST API (mục 3.4 báo cáo)

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/profile` | Hồ sơ tác giả |
| GET | `/api/devices` | Danh sách thiết bị + trạng thái |
| POST | `/api/devices/control/{id}` | Bật/tắt thiết bị, body `{"command":"TURN_ON"}` |
| POST | `/api/devices/control-all` | Bật/tắt toàn bộ thiết bị, body `{"action":"ON"}` hoặc `{"action":"OFF"}`; 200 nếu tất cả thành công, 503 nếu có thiết bị quá hạn; `data` chứa kết quả từng thiết bị |
| GET | `/api/sensors/latest` | Số đo mới nhất + 12 điểm cho biểu đồ |
| GET | `/api/sensors` | Lịch sử cảm biến, tham số `measuredAt`, `value`, `sensorType`, `sort`, `page`, `size` (h? tr? th?m `keyword`) |
| GET | `/api/action-history` | Nhật ký điều khiển, tham số `operatedAt`, `deviceId`, `command`, `status`, `sort`, `page`, `size` (h? tr? th?m `keyword`) |

## Điều khiển toàn bộ thiết bị

Công tắc tổng gửi một yêu cầu tới `/api/devices/control-all`. Backend gửi lệnh song song cho từng thiết bị qua MQTT và chờ phản hồi tối đa 3 giây cho mỗi lệnh. Mỗi thiết bị có `logId` (requestId) và nhật ký riêng. Phản hồi chứa `success`, `code`, `message`, `data`; mỗi phần tử `data` có `logId`, `deviceId`, `deviceCode`, `state`, `status` (SUCCESS/TIMEOUT). Khi có thiết bị quá hạn, phản hồi 503 vẫn chứa kết quả các thiết bị đã thành công; không hoàn tác thiết bị đó.

## WebSocket

Endpoint `ws://localhost:8080/ws` (STOMP), 3 kênh:

- `/topic/sensor` — mỗi lần ESP32 gửi `data/sensor`
- `/topic/device` — khi trạng thái thiết bị thực sự đổi
- `/topic/status` — phần cứng ONLINE / OFFLINE

## Luồng điều khiển một pha

```
POST /api/devices/control/1
  → ghi control_logs = PENDING (id này chính là requestId)
  → publish device/control/1 {"command":"TURN_ON","requestId":"7"}
  → ESP32 đóng thiết bị NGAY, trả device/status {"state":"ON","source":"USER"}
  → cập nhật smart_devices + control_logs = SUCCESS → trả 200 OK
```

Điểm đáng nói: API **không** trả 200 ngay khi gửi lệnh đi, mà chờ đến khi ESP32 báo trạng thái thật. Nhờ vậy web chỉ đổi màu công tắc khi thiết bị ngoài đời đã thực sự đổi.

Quá `app.control.timeout-ms` (mặc định 3000ms) mà không thấy `device/status` → ghi `control_logs = TIMEOUT`, trả **503**, giữ nguyên `current_state` vì hệ thống không biết thiết bị đang bật hay tắt. Trường hợp này xảy ra khi ESP32 mất điện hoặc rớt mạng.

## Kiểm thử không cần phần cứng

```powershell
# 1. Bật broker
& "C:\Program Files\mosquitto\mosquitto.exe" -c "D:\iot-dashboard-mockup\mqtt\mosquitto.conf" -v

# 2. Giả lập ESP32 gửi dữ liệu cảm biến mỗi 2 giây
powershell -ExecutionPolicy Bypass -File "D:\iot-dashboard-mockup\mqtt\gia-lap-esp32.ps1"

# 3. Gọi thử toàn bộ API
powershell -ExecutionPolicy Bypass -File "D:\iot-dashboard-mockup\backend\test-api.ps1"
```

Lưu ý: script giả lập chỉ gửi `data/sensor`, **không** trả lời `device/status`. Nên khi bấm công tắc sẽ ra TIMEOUT + 503 — đúng như thiết kế. Muốn thấy đường thành công thì cắm ESP32 thật.
