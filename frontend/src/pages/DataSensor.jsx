import { useEffect, useState } from 'react'
import Pagination from '../components/Pagination.jsx'
import { SENSOR_TYPES, formatDateTime, formatValue } from '../data/labels.js'
import { api } from '../api/client.js'

const EMPTY_FILTER = { measuredAt: '', value: '', sensorType: 'ALL', sort: 'DESC' }
const EMPTY_PAGE = { items: [], total: 0, totalPages: 1 }

export default function DataSensor() {
  const [draft, setDraft] = useState(EMPTY_FILTER)
  const [applied, setApplied] = useState(EMPTY_FILTER)
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(10)
  const [sizeInput, setSizeInput] = useState('10')
  const [data, setData] = useState(EMPTY_PAGE)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Moi lan doi bo loc hoac doi trang deu goi lai GET /api/sensors
  useEffect(() => {
    let cancelled = false
    setLoading(true)

    api
      .getSensors({ ...applied, page, size })
      .then((result) => {
        if (cancelled) return
        setData(result)
        setError(null)
      })
      .catch((e) => {
        if (cancelled) return
        setData(EMPTY_PAGE)
        setError(e.message)
      })
      .finally(() => {
        if (!cancelled) setLoading(false)
      })

    return () => {
      cancelled = true
    }
  }, [applied, page, size])

  const update = (field) => (e) => setDraft((d) => ({ ...d, [field]: e.target.value }))

  const resetFilters = () => {
    setDraft({ ...EMPTY_FILTER })
    setApplied({ ...EMPTY_FILTER })
    setPage(1)
  }

  const handleSubmit = (e) => {
    e.preventDefault()
    setApplied(draft)
    setPage(1)
  }

  /* Người dùng gõ số tuỳ ý rồi Enter hoặc bấm ra ngoài thì mới áp dụng.
     Chặn trong khoảng 1..200 vì Backend cũng giới hạn tối đa 200 dòng. */
  const applySize = () => {
    const n = Number(sizeInput)
    if (!Number.isFinite(n) || n < 1) {
      setSizeInput(String(size))
      return
    }
    const hopLe = Math.min(Math.trunc(n), 200)
    setSizeInput(String(hopLe))
    if (hopLe !== size) {
      setSize(hopLe)
      setPage(1)
    }
  }

  return (
    <>
      <header className="page-header">
        <h1 className="page-title">Dữ Liệu Cảm Biến Chi Tiết</h1>
      </header>

      {error && <div className="alert">{error}</div>}

      <form className="card filter-card filter-panel" onSubmit={handleSubmit}>
        <h2 className="filter-title">Bộ lọc dữ liệu cảm biến</h2>
        <div className="filter-fields">
          <label className="filter-field">
            <span>Loại cảm biến</span>
            <select value={draft.sensorType} onChange={update('sensorType')}>
              <option value="ALL">Tất cả loại cảm biến</option>
              {Object.entries(SENSOR_TYPES).map(([value, type]) => (
                <option key={value} value={value}>{type.label}</option>
              ))}
            </select>
          </label>
          <label className="filter-field">
            <span>Sắp xếp theo thời gian</span>
            <select value={draft.sort} onChange={update('sort')}>
              <option value="DESC">Mới nhất trước (Giảm dần)</option>
              <option value="ASC">Cũ nhất trước (Tăng dần)</option>
            </select>
          </label>
          <label className="filter-field">
            <span>Thời điểm đo</span>
            <input type="datetime-local" step="60" value={draft.measuredAt} onChange={update('measuredAt')} />
          </label>
          <label className="filter-field">
            <span>Giá trị cảm biến</span>
            <input type="number" step="any" placeholder="Nhập giá trị" value={draft.value} onChange={update('value')} />
          </label>
        </div>
        <div className="filter-actions">
          <button type="button" className="btn-reset" onClick={resetFilters}>Đặt lại</button>
          <button type="submit" className="btn-filter">Tìm kiếm</button>
        </div>
      </form>

      <section className="card table-card">
        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Mã số</th>
                <th>Thời gian</th>
                <th>Loại cảm biến</th>
                <th>Giá trị</th>
              </tr>
            </thead>
            <tbody>
              {data.items.map((row) => {
                const meta = SENSOR_TYPES[row.type] || { label: row.type, badge: 'hum' }
                return (
                  <tr key={row.id}>
                    <td className="mono">#{row.id}</td>
                    <td className="mono">{formatDateTime(row.timestamp)}</td>
                    <td>
                      <span className={`badge ${meta.badge}`}>{meta.label}</span>
                    </td>
                    <td className="mono">
                      {formatValue(row.type, row.value)} {row.unit}
                    </td>
                  </tr>
                )
              })}
              {data.items.length === 0 && (
                <tr>
                  <td colSpan={4} className="empty">
                    {loading ? 'Đang tải…' : 'Không có dữ liệu phù hợp'}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="table-footer">
          <span>
            Tổng cộng <strong>{data.total}</strong> bản ghi
            <label className="page-size">
              Hiển thị
              <input
                type="number"
                min="1"
                max="200"
                value={sizeInput}
                onChange={(e) => setSizeInput(e.target.value)}
                onBlur={applySize}
                onKeyDown={(e) => {
                  if (e.key === 'Enter') {
                    e.preventDefault()
                    applySize()
                  }
                }}
                aria-label="Số dòng mỗi trang"
              />
              / trang
            </label>
          </span>
          <Pagination page={page} totalPages={Math.max(1, data.totalPages)} onChange={setPage} />
        </div>
      </section>
    </>
  )
}
