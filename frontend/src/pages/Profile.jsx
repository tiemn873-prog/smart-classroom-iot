import { useEffect, useState } from 'react'
import { IconApi, IconFigma, IconFilePdf, IconGithub, IconMail, IconMapPin } from '../components/Icons.jsx'
import { api } from '../api/client.js'

/** Thẻ tài nguyên: link lấy từ bảng user_profiles, chưa có thì để '#' */
const RESOURCES = [
  { key: 'githubUrl', title: 'Kho lưu trữ GitHub', sub: 'Source code & project files', icon: IconGithub, tone: 'green' },
  { key: 'reportPdfUrl', title: 'Tài liệu báo cáo', sub: 'Tài liệu tổng hợp hệ thống Smart Class IoT', icon: IconFilePdf, tone: 'yellow' },
  { key: 'figmaUrl', title: 'Bản thiết kế Figma', sub: 'UI/UX design files & components', icon: IconFigma, tone: 'pink' },
  { key: 'apiDocsUrl', title: 'Tham khảo API', sub: 'Tài liệu các REST API của hệ thống', icon: IconApi, tone: 'orange' },
]

function initialsOf(fullName) {
  if (!fullName) return '—'
  const parts = fullName.trim().split(/\s+/)
  const first = parts[0]?.[0] ?? ''
  const last = parts[parts.length - 1]?.[0] ?? ''
  return (first + last).toUpperCase()
}

export default function Profile() {
  const [profile, setProfile] = useState(null)
  const [error, setError] = useState(null)

  useEffect(() => {
    let cancelled = false
    api
      .getProfile()
      .then((data) => !cancelled && setProfile(data))
      .catch((e) => !cancelled && setError(e.message))
    return () => {
      cancelled = true
    }
  }, [])

  return (
    <>
      <header className="page-header">
        <h1 className="page-title">Hồ Sơ & Tài Nguyên Hệ Thống</h1>
      </header>

      {error && <div className="alert">{error}</div>}

      <section className="profile-banner">
        {/* Có ảnh thì hiện ảnh, chưa có thì hiện chữ cái đầu của họ tên */}
        <div className="avatar">
          {profile?.avatarUrl ? (
            <img src={profile.avatarUrl} alt={`Ảnh đại diện ${profile.fullName || ''}`} />
          ) : (
            initialsOf(profile?.fullName)
          )}
        </div>

        <div className="profile-info">
          <h2 className="profile-name">{profile?.fullName || 'Đang tải…'}</h2>
          {profile?.major && <p className="profile-major">{profile.major}</p>}
          <p className="profile-code">PTIT / {profile?.classCode || profile?.studentCode || '—'}</p>

          <div className="profile-meta">
            {profile?.email && (
              <span>
                <IconMail width={15} height={15} />
                {profile.email}
              </span>
            )}
            {profile?.location && (
              <span>
                <IconMapPin width={15} height={15} />
                {profile.location}
              </span>
            )}
          </div>
        </div>
      </section>

      <h3 className="section-label">Tài liệu & Tài nguyên</h3>
      <section className="resource-grid">
        {RESOURCES.map(({ key, title, sub, icon: Icon, tone }) => (
          <a key={key} className="card resource-card" href={profile?.[key] || '#'}
            target={profile?.[key] ? '_blank' : undefined} rel={profile?.[key] ? 'noopener noreferrer' : undefined}>
            <span className={`resource-icon ${tone}`}>
              <Icon width={20} height={20} />
            </span>
            <span>
              <span className="resource-title">{title}</span>
              <span className="resource-sub">{sub}</span>
            </span>
          </a>
        ))}
      </section>
    </>
  )
}
