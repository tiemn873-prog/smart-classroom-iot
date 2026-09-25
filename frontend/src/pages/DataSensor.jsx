import { useEffect, useState } from 'react'
import Pagination from '../components/Pagination.jsx'
import { IconSearch } from '../components/Icons.jsx'
import { SENSOR_TYPES, formatDateTime, formatValue } from '../data/labels.js'
import { api } from '../api/client.js'

const PAGE_SIZE = 10
const EMPTY_FILTER = { keyword: '', sensorType: 'ALL', sort: 'DESC' }
const EMPTY_PAGE = { items: [], total: 0, totalPages: 1 }

export default function DataSensor() {
  const [draft, setDraft] = useState(EMPTY_FILTER)
  const [applied, setApplied] = useState(EMPTY_FILTER)
  const [page, setPage] = useState(1)
  const [data, setData] = useState(EMPTY_PAGE)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  // Moi lan doi bo loc hoac doi trang deu goi lai GET /api/sensors
  useEffect(() => {
    let cancelled = false
    setLoading(true)

    api
      .getSensors({ ...applied, page, size: PAGE_SIZE })
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
  }, [applied, page])

  const update = (field) => (e) => setDraft((d) => ({ ...d, [field]: e.target.value }))

  const handleSubmit = (e) => {
    e.preventDefault()
    setApplied(draft)
    setPage(1)
  }

  return (
    <>
      <header className="page-header">
        <h1 className="page-title">Dữ Liệu Cảm Biến Chi Tiết</h1>
      </header>

      {error && <div className="alert">{error}</div>}

      <form className="card filter-card" onSubmit={handleSubmit}>
        <div className="filter-row">
          <label className="search-box">
            <IconSearch width={15} height={15} />
            <input type="text" placeholder="Tìm kiếm dữ liệu..." value={draft.keyword} onChange={update('keyword')} />
          </label>
          <select value={draft.sensorType} onChange={update('sensorType')} aria-label="Loại cảm biến">
            <option value="ALL">Tất cả loại cảm biến</option>
            {Object.entries(SENSOR_TYPES).map(([value, type]) => (
              <option key={value} value={value}>
                {type.label}
              </option>
            ))}
          </select>
          <select value={draft.sort} onChange={update('sort')} aria-label="Sắp xếp">
            <option value="DESC">Thời gian / ID (Mới nhất)</option>
            <option value="ASC">Thời gian / ID (Cũ nhất)</option>
          </select>
          <button type="submit" className="btn-filter">
            Lọc
          </button>
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
            Hiển thị {data.items.length} của {data.total}
          </span>
          <Pagination page={page} totalPages={Math.max(1, data.totalPages)} onChange={setPage} />
        </div>
      </section>
    </>
  )
}
