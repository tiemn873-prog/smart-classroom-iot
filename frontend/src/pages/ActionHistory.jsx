import { useEffect, useState } from 'react'
import Pagination from '../components/Pagination.jsx'
import { IconSearch } from '../components/Icons.jsx'
import { COMMAND_LABELS, SYNC_STATUS, formatDateTime } from '../data/labels.js'
import { api } from '../api/client.js'
import { useAppStore } from '../store/AppStore.jsx'

const EMPTY_FILTER = { keyword: '', deviceId: 'ALL', command: 'ALL', status: 'ALL', sort: 'DESC' }
const EMPTY_PAGE = { items: [], total: 0, totalPages: 1 }

export default function ActionHistory() {
  const { devices } = useAppStore()
  const [draft, setDraft] = useState(EMPTY_FILTER)
  const [applied, setApplied] = useState(EMPTY_FILTER)
  const [page, setPage] = useState(1)
  const [size, setSize] = useState(10)
  const [sizeInput, setSizeInput] = useState('10')
  const [data, setData] = useState(EMPTY_PAGE)
  // He thong chi co mot nguoi dung nen lay thang ho ten trong bang user_profiles
  const [userName, setUserName] = useState('')
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)

    api
      .getActionHistory({
        keyword: applied.keyword,
        deviceId: applied.deviceId === 'ALL' ? undefined : applied.deviceId,
        command: applied.command,
        status: applied.status,
        sort: applied.sort,
        page,
        size,
      })
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

  // Lay ho ten nguoi dung mot lan de dien vao cot Người thực hiện
  useEffect(() => {
    let cancelled = false
    api
      .getProfile()
      .then((p) => {
        if (!cancelled) setUserName(p?.fullName || '')
      })
      .catch(() => {})
    return () => {
      cancelled = true
    }
  }, [])

  const update = (field) => (e) => setDraft((d) => ({ ...d, [field]: e.target.value }))

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
        <h1 className="page-title">Lịch Sử Hoạt Động Hệ Thống</h1>
      </header>

      {error && <div className="alert">{error}</div>}

      <section className="card table-card history-card">
        <form className="filter-area" onSubmit={handleSubmit}>
          <label className="search-box">
            <IconSearch width={15} height={15} />
            <input
              type="text"
              placeholder="Tìm kiếm theo thời gian..."
              value={draft.keyword}
              onChange={update('keyword')}
            />
          </label>
          <div className="filter-row">
            <select value={draft.deviceId} onChange={update('deviceId')} aria-label="Thiết bị">
              <option value="ALL">Tất cả thiết bị</option>
              {devices.map((d) => (
                <option key={d.id} value={String(d.id)}>
                  {d.name}
                </option>
              ))}
            </select>
            <select value={draft.command} onChange={update('command')} aria-label="Hành động">
              <option value="ALL">Tất cả hành động</option>
              <option value="TURN_ON">Bật</option>
              <option value="TURN_OFF">Tắt</option>
            </select>
            <select value={draft.status} onChange={update('status')} aria-label="Trạng thái">
              <option value="ALL">Tất cả trạng thái</option>
              {Object.entries(SYNC_STATUS).map(([value, meta]) => (
                <option key={value} value={value}>
                  {meta.label}
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

        <div className="table-scroll">
          <table className="data-table">
            <thead>
              <tr>
                <th>Mã số</th>
                <th>Thời gian</th>
                <th>Thiết bị</th>
                <th>Người thực hiện</th>
                <th>Hành động</th>
                <th>Trạng thái</th>
              </tr>
            </thead>
            <tbody>
              {data.items.map((row) => {
                const status = SYNC_STATUS[row.status] || { label: row.status, badge: 'pending' }
                return (
                  <tr key={row.id}>
                    <td className="mono strong">#{row.id}</td>
                    <td className="mono dim">{formatDateTime(row.requestedAt)}</td>
                    <td>{row.deviceName}</td>
                    <td className="dim">{userName || '—'}</td>
                    <td>
                      <span className={'badge ' + (row.command === 'TURN_ON' ? 'on' : 'off')}>
                        {COMMAND_LABELS[row.command] || row.command}
                      </span>
                    </td>
                    <td>
                      <span className={`badge ${status.badge}`}>{status.label}</span>
                    </td>
                  </tr>
                )
              })}
              {data.items.length === 0 && (
                <tr>
                  <td colSpan={6} className="empty">
                    {loading ? 'Đang tải…' : 'Không có lịch sử phù hợp'}
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
          <Pagination
            page={page}
            totalPages={Math.max(1, data.totalPages)}
            onChange={setPage}
            prevLabel="Trước"
            nextLabel="Sau"
          />
        </div>
      </section>
    </>
  )
}
