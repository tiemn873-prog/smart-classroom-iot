import { CartesianGrid, Line, LineChart, ResponsiveContainer, Tooltip, XAxis, YAxis } from 'recharts'
import { IconBarChart, IconBolt, IconBulb, IconChart, IconCloud } from '../components/Icons.jsx'
import { formatClock } from '../data/labels.js'
import { useAppStore } from '../store/AppStore.jsx'

const STAT_CARDS = [
  { key: 'temperature', label: 'Nhiệt độ', unit: '°C', tone: 'temp', icon: IconBarChart, format: (v) => v.toFixed(1) },
  { key: 'humidity', label: 'Độ ẩm', unit: '%', tone: 'hum', icon: IconCloud, format: (v) => Math.round(v) },
  { key: 'light', label: 'Cường độ sáng', unit: 'LUX', tone: 'light', icon: IconBolt, format: (v) => Math.round(v) },
]

// Độ ẩm & nhiệt độ theo trục trái (0-100), ánh sáng theo trục phải (0-1000)
const SERIES = [
  { key: 'humidity', label: 'Độ ẩm', color: '#1c7ed6', axis: 'left' },
  { key: 'light', label: 'Ánh sáng', color: '#e08b0b', axis: 'right' },
  { key: 'temperature', label: 'Nhiệt độ', color: '#d64545', axis: 'left' },
]

const AXIS_COLOR = '#6b7787'
const GRID_COLOR = '#e8ecf3'

export default function Dashboard() {
  const { latest, readings, devices, hardwareOnline, pendingDeviceIds, loading, error, clearError, toggleDevice } =
    useAppStore()

  const chartData = readings.map((r) => ({ ...r, clock: formatClock(r.time) }))

  return (
    <>
      <header className="page-header">
        <h1 className="page-title">Bảng Điều Khiển Lớp Học Thông Minh</h1>
        <span className={'status-pill' + (hardwareOnline ? '' : ' offline')}>
          <span className="status-dot" />
          Trạng thái hệ thống: {hardwareOnline ? 'Đã kết nối' : 'Đã ngắt kết nối'}
        </span>
      </header>

      {error && (
        <div className="alert" role="alert">
          <span>{error}</span>
          <button type="button" onClick={clearError} aria-label="Đóng thông báo">
            ✕
          </button>
        </div>
      )}

      <section className="stat-grid">
        {STAT_CARDS.map(({ key, label, unit, tone, icon: Icon, format }) => {
          const value = latest ? latest[key] : null
          return (
            <div key={key} className={`card stat-card ${tone}`}>
              <div className="stat-head">
                <span className="stat-label">{label}</span>
                <span className={`stat-icon ${tone}`}>
                  <Icon width={16} height={16} />
                </span>
              </div>
              <div className="stat-value">
                {value == null ? '—' : format(value)}
                <span className="stat-unit">{unit}</span>
              </div>
            </div>
          )
        })}
      </section>

      <section className="card chart-card">
        <h2 className="card-title">
          <IconChart width={16} height={16} />
          Lịch sử cảm biến
        </h2>
        <div className="chart-box">
          {chartData.length === 0 ? (
            <div className="chart-empty">
              {loading ? 'Đang tải dữ liệu…' : 'Chưa có số liệu. Hãy bật broker MQTT và ESP32 (hoặc script giả lập).'}
            </div>
          ) : (
            <ResponsiveContainer width="100%" height="100%">
              <LineChart data={chartData} margin={{ top: 8, right: 4, bottom: 0, left: -8 }}>
                {/* Chi ke duong ngang, bo duong doc cho do roi mat */}
                <CartesianGrid stroke={GRID_COLOR} vertical={false} />
                <XAxis
                  dataKey="clock"
                  stroke={AXIS_COLOR}
                  tick={{ fontSize: 11 }}
                  tickLine={false}
                  axisLine={{ stroke: GRID_COLOR }}
                  minTickGap={48}
                />
                <YAxis
                  yAxisId="left"
                  domain={[0, 100]}
                  ticks={[0, 25, 50, 75, 100]}
                  stroke={AXIS_COLOR}
                  tick={{ fontSize: 11 }}
                  tickLine={false}
                  axisLine={false}
                  width={40}
                />
                {/* Truc phai (anh sang) tu co gian theo gia tri thuc te.
                    Neu de co dinh 0-1000 thi phong trong nha chi vai chuc lux
                    se nam bep sat day, khong thay duoc bien dong. */}
                <YAxis
                  yAxisId="right"
                  orientation="right"
                  domain={[0, (dataMax) => Math.max(20, Math.ceil((dataMax * 1.3) / 10) * 10)]}
                  stroke={AXIS_COLOR}
                  tick={{ fontSize: 11 }}
                  tickLine={false}
                  axisLine={false}
                  width={48}
                  allowDecimals={false}
                  tickFormatter={(v) => v.toLocaleString('en-US')}
                />
                <Tooltip
                  contentStyle={{
                    background: '#ffffff',
                    border: `1px solid ${GRID_COLOR}`,
                    borderRadius: 8,
                    boxShadow: '0 2px 8px rgba(16, 24, 40, 0.1)',
                    fontSize: 12,
                  }}
                  labelStyle={{ color: AXIS_COLOR, marginBottom: 4 }}
                />
                {SERIES.map((s) => (
                  <Line
                    key={s.key}
                    yAxisId={s.axis}
                    type="monotone"
                    dataKey={s.key}
                    name={s.label}
                    stroke={s.color}
                    strokeWidth={2}
                    dot={false}
                    isAnimationActive={false}
                  />
                ))}
              </LineChart>
            </ResponsiveContainer>
          )}
        </div>
        <div className="legend">
          {SERIES.map((s) => (
            <span key={s.key} className="legend-item">
              <i style={{ borderColor: s.color }} />
              {s.label}
            </span>
          ))}
        </div>
      </section>

      <section className="device-grid">
        {devices.map((device) => {
          const isOn = device.currentState === 'ON'
          const pending = pendingDeviceIds.includes(device.id)
          const statusText = pending ? 'Đang xử lý...' : isOn ? 'Đang bật' : 'Đang tắt'

          return (
            <div key={device.id} className="card device-card">
              <div className="device-info">
                <span className={'device-icon' + (isOn ? ' on' : '')}>
                  <IconBulb />
                </span>
                <span>
                  <span className="device-name">{device.name}</span>
                  <span className="device-code">{device.deviceCode}</span>
                </span>
              </div>
              <div className="device-control">
                <button
                  type="button"
                  role="switch"
                  aria-checked={isOn}
                  aria-label={`Bật/tắt ${device.name}`}
                  className={'switch' + (isOn ? ' on' : '') + (pending ? ' pending' : '')}
                  disabled={pending}
                  onClick={() => toggleDevice(device)}
                >
                  <span className="knob" />
                </button>
                <span className="switch-label">{statusText}</span>
              </div>
            </div>
          )
        })}
        {devices.length === 0 && !loading && (
          <div className="card device-card">
            <span className="dim">Chưa lấy được danh sách thiết bị từ Backend.</span>
          </div>
        )}
      </section>
    </>
  )
}
