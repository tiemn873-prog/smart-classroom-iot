# Smart Class IoT — Frontend (ReactJS)

Giao diện theo Chương 3.2 của báo cáo: Dashboard, Data Sensor, Action History, Profile. Không có trang đăng nhập.

## Chạy

Cần Node.js LTS. Mở PowerShell tại thư mục `frontend`:

```powershell
npm install      # chỉ chạy lần đầu
npm run dev      # mở http://localhost:5173
```

## Cấu trúc

```
src/
  main.jsx               khởi tạo React + Router + store
  App.jsx                khai báo route
  components/            Sidebar, Layout, Pagination, Icons
  pages/                 Dashboard, DataSensor, ActionHistory, Profile
  store/AppStore.jsx     trạng thái dùng chung (cảm biến, thiết bị, nhật ký)
  data/mockData.js       dữ liệu giả lập
```

## Dữ liệu hiện tại là giả lập

- Cảm biến cập nhật mỗi 2 giây, giống nhịp ESP32 gửi `data/sensor`.
- Bấm công tắc sẽ ghi nhật ký `PENDING`, sau ~1.2 giây mới chuyển `SUCCESS` và đổi trạng thái thiết bị — mô phỏng thời gian chờ ESP32 báo trạng thái thật về.

Khi làm Backend, chỉ cần thay phần trong `store/AppStore.jsx` bằng lời gọi API (`GET /api/devices`, `POST /api/devices/control/{id}`, `GET /api/sensors`, `GET /api/action-history`) và WebSocket; các trang không phải sửa.
