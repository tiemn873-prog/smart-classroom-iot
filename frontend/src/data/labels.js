/* Nhan hien thi va ham dinh dang dung chung cho cac trang.
   Du lieu that lay tu Backend qua src/api/client.js */

export const SENSOR_TYPES = {
  temperature: { label: 'Nhiệt độ', unit: '°C', badge: 'temp', decimals: 1 },
  humidity: { label: 'Độ ẩm', unit: '%', badge: 'hum', decimals: 1 },
  light: { label: 'Ánh sáng', unit: 'lux', badge: 'light', decimals: 0 },
}

export const COMMAND_LABELS = {
  TURN_ON: 'BẬT',
  TURN_OFF: 'TẮT',
}

// Khớp cột sync_status của bảng control_logs
export const SYNC_STATUS = {
  SUCCESS: { label: 'Thành công', badge: 'success' },
  TIMEOUT: { label: 'Thất bại', badge: 'fail' },
  PENDING: { label: 'Đang xử lý', badge: 'pending' },
}

const pad = (n) => String(n).padStart(2, '0')

const toDate = (value) => (value instanceof Date ? value : new Date(value))

export function formatDateTime(value) {
  if (!value) return '—'
  const date = toDate(value)
  return `${pad(date.getDate())}-${pad(date.getMonth() + 1)}-${date.getFullYear()} ${formatClock(date)}`
}

export function formatClock(value) {
  if (!value) return ''
  const date = toDate(value)
  return `${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

export function formatValue(type, value) {
  if (value == null) return '—'
  const meta = SENSOR_TYPES[type]
  return value.toFixed(meta ? meta.decimals : 1)
}
