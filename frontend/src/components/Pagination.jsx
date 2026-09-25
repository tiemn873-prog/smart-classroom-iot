// Trả về danh sách nút trang, vd trang 1/294 -> [1, 2, 3, '…', 294]
function buildPageList(current, total) {
  if (total <= 5) return Array.from({ length: total }, (_, i) => i + 1)

  const pages = new Set([1, total, current - 1, current, current + 1])
  if (current <= 3) [2, 3].forEach((p) => pages.add(p))
  if (current >= total - 2) [total - 1, total - 2].forEach((p) => pages.add(p))

  const sorted = [...pages].filter((p) => p >= 1 && p <= total).sort((a, b) => a - b)
  const result = []
  sorted.forEach((p, i) => {
    if (i > 0 && p - sorted[i - 1] > 1) result.push('…')
    result.push(p)
  })
  return result
}

export default function Pagination({ page, totalPages, onChange, prevLabel = 'Trang trước', nextLabel = 'Trang sau' }) {
  return (
    <div className="pages">
      <button type="button" className="page-btn" disabled={page <= 1} onClick={() => onChange(page - 1)}>
        {prevLabel}
      </button>
      {buildPageList(page, totalPages).map((p, i) =>
        p === '…' ? (
          <span key={`gap-${i}`} className="page-gap">…</span>
        ) : (
          <button
            key={p}
            type="button"
            className={'page-btn' + (p === page ? ' active' : '')}
            aria-current={p === page ? 'page' : undefined}
            onClick={() => onChange(p)}
          >
            {p}
          </button>
        ),
      )}
      <button type="button" className="page-btn" disabled={page >= totalPages} onClick={() => onChange(page + 1)}>
        {nextLabel}
      </button>
    </div>
  )
}
