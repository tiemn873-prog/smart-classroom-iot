/* Toàn bộ biểu tượng của giao diện, vẽ bằng SVG viết thẳng trong code
   (không dùng file ảnh hay thư viện icon ngoài). */

// Thiết lập chung cho mọi biểu tượng: cỡ mặc định 18x18, chỉ vẽ nét, nét bo tròn.
// stroke 'currentColor' = lấy theo màu chữ của phần tử chứa nó, nên icon tự đổi màu theo chỗ đặt.
const base = {
  width: 18,
  height: 18,
  viewBox: '0 0 24 24',
  fill: 'none',
  stroke: 'currentColor',
  strokeWidth: 2,
  strokeLinecap: 'round',
  strokeLinejoin: 'round',
  'aria-hidden': true,
}

/* ===== Thanh menu bên trái (Sidebar.jsx) ===== */

// Bóng đèn: logo "Smart Class" ở đầu menu + biểu tượng trên mỗi thẻ thiết bị ở Bảng điều khiển
export const IconBulb = (props) => (
  <svg {...base} {...props}>
    <path d="M9 18h6M10 22h4M12 2a7 7 0 0 0-4 12.7c.5.4.8 1 .8 1.8h6.4c0-.7.3-1.4.8-1.8A7 7 0 0 0 12 2z" />
  </svg>
)

// Bốn ô vuông: mục menu "Bảng điều khiển"
export const IconGrid = (props) => (
  <svg {...base} {...props}>
    <rect x="3" y="3" width="7" height="7" rx="1.5" />
    <rect x="14" y="3" width="7" height="7" rx="1.5" />
    <rect x="3" y="14" width="7" height="7" rx="1.5" />
    <rect x="14" y="14" width="7" height="7" rx="1.5" />
  </svg>
)

// Cơ sở dữ liệu: mục menu "Dữ liệu cảm biến"
export const IconDatabase = (props) => (
  <svg {...base} {...props}>
    <ellipse cx="12" cy="5" rx="8" ry="3" />
    <path d="M4 5v14c0 1.7 3.6 3 8 3s8-1.3 8-3V5" />
    <path d="M4 12c0 1.7 3.6 3 8 3s8-1.3 8-3" />
  </svg>
)

// Đồng hồ quay ngược: mục menu "Lịch sử hoạt động"
export const IconHistory = (props) => (
  <svg {...base} {...props}>
    <path d="M3 12a9 9 0 1 0 3-6.7L3 8" />
    <path d="M3 3v5h5" />
    <path d="M12 7v5l3 2" />
  </svg>
)

// Hình người: mục menu "Hồ sơ"
export const IconUser = (props) => (
  <svg {...base} {...props}>
    <circle cx="12" cy="8" r="4" />
    <path d="M4 21c0-4.4 3.6-8 8-8s8 3.6 8 8" />
  </svg>
)

/* ===== Trang Bảng điều khiển (Dashboard.jsx) ===== */

// Ba cột: thẻ số liệu "Nhiệt độ"
export const IconBarChart = (props) => (
  <svg {...base} {...props}>
    <path d="M6 20v-6M12 20V10M18 20V4" />
  </svg>
)

// Đám mây: thẻ số liệu "Độ ẩm"
export const IconCloud = (props) => (
  <svg {...base} {...props}>
    <path d="M17.5 19a4.5 4.5 0 1 0-1.4-8.8A6 6 0 0 0 4 12.5 3.5 3.5 0 0 0 6.5 19z" />
  </svg>
)

// Tia sét: thẻ số liệu "Cường độ sáng"
export const IconBolt = (props) => (
  <svg {...base} {...props}>
    <path d="M13 2 4 14h6l-1 8 9-12h-6l1-8z" />
  </svg>
)

// Màn hình có đường biểu đồ: cạnh tiêu đề "Lịch sử cảm biến" của biểu đồ
export const IconChart = (props) => (
  <svg {...base} {...props}>
    <rect x="3" y="4" width="18" height="12" rx="1" />
    <path d="m7 12 3-3 3 2 4-4M12 16v4M8 20h8" />
  </svg>
)

/* ===== Trang Dữ liệu cảm biến + Lịch sử hoạt động ===== */

// Kính lúp: trong ô tìm kiếm của cả hai trang (DataSensor.jsx, ActionHistory.jsx)
export const IconSearch = (props) => (
  <svg {...base} {...props}>
    <circle cx="11" cy="11" r="7" />
    <path d="m21 21-4.3-4.3" />
  </svg>
)

/* ===== Trang Hồ sơ (Profile.jsx) ===== */

// Logo GitHub: thẻ "Kho lưu trữ GitHub"
export const IconGithub = (props) => (
  <svg {...base} {...props}>
    <path d="M9 19c-4.3 1.4-4.3-2.5-6-3m12 5v-3.5c0-1 .1-1.4-.5-2 2.8-.3 5.5-1.4 5.5-6a4.6 4.6 0 0 0-1.3-3.2 4.2 4.2 0 0 0-.1-3.2s-1.1-.3-3.5 1.3a12.3 12.3 0 0 0-6.2 0C6.5 2.8 5.4 3.1 5.4 3.1a4.2 4.2 0 0 0-.1 3.2A4.6 4.6 0 0 0 4 9.5c0 4.6 2.7 5.7 5.5 6-.6.6-.6 1.2-.5 2V21" />
  </svg>
)

// Tờ tài liệu có dòng chữ: thẻ "Tài liệu SRS (PDF)"
export const IconFilePdf = (props) => (
  <svg {...base} {...props}>
    <path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z" />
    <path d="M14 3v6h6M8 14h8M8 18h5" />
  </svg>
)

// Logo Figma: thẻ "Bản thiết kế Figma"
export const IconFigma = (props) => (
  <svg {...base} {...props}>
    <path d="M5 5.5A3.5 3.5 0 0 1 8.5 2H12v7H8.5A3.5 3.5 0 0 1 5 5.5z" />
    <path d="M12 2h3.5a3.5 3.5 0 1 1 0 7H12V2z" />
    <path d="M12 12.5a3.5 3.5 0 1 1 7 0 3.5 3.5 0 1 1-7 0z" />
    <path d="M5 19.5A3.5 3.5 0 0 1 8.5 16H12v3.5a3.5 3.5 0 1 1-7 0z" />
    <path d="M5 12.5A3.5 3.5 0 0 1 8.5 9H12v7H8.5A3.5 3.5 0 0 1 5 12.5z" />
  </svg>
)

// Tờ tài liệu có dấu < >: thẻ "Tham khảo API (Postman)"
export const IconApi = (props) => (
  <svg {...base} {...props}>
    <path d="M14 3H6a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V9z" />
    <path d="M14 3v6h6M10 13l-2 2 2 2M14 13l2 2-2 2" />
  </svg>
)

// Phong bì thư: cạnh địa chỉ email trong khối nền tím
export const IconMail = (props) => (
  <svg {...base} {...props}>
    <rect x="2" y="4" width="20" height="16" rx="2" />
    <path d="m2 7 10 6 10-6" />
  </svg>
)

// Ghim bản đồ: cạnh địa chỉ "Hà Nội, Việt Nam" trong khối nền tím
export const IconMapPin = (props) => (
  <svg {...base} {...props}>
    <path d="M20 10c0 6-8 12-8 12s-8-6-8-12a8 8 0 0 1 16 0z" />
    <circle cx="12" cy="10" r="3" />
  </svg>
)
