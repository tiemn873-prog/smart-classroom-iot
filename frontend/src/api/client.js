/* Goi REST API cua Backend Spring Boot.
   Duong dan bat dau bang /api, Vite chuyen tiep sang http://localhost:8080 (xem vite.config.js) */

const BASE = '/api'

function toQuery(params) {
  const clean = Object.entries(params || {}).filter(
    ([, value]) => value !== undefined && value !== null && value !== '',
  )
  return clean.length ? '?' + new URLSearchParams(clean).toString() : ''
}

async function request(path, options) {
  let response
  try {
    response = await fetch(BASE + path, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    })
  } catch {
    const error = new Error('Không kết nối được tới Backend. Kiểm tra server cổng 8080 còn chạy không.')
    error.status = 0
    throw error
  }

  if (!response.ok) {
    let message = `Backend trả lỗi ${response.status}`
    try {
      const body = await response.json()
      if (body && body.message) message = body.message
    } catch {
      // body không phải JSON thì giữ nguyên thông báo mặc định
    }
    const error = new Error(message)
    error.status = response.status
    throw error
  }

  return response.status === 204 ? null : response.json()
}

export const api = {
  getProfile: () => request('/profile'),
  getDevices: () => request('/devices'),
  controlDevice: (id, command) =>
    request(`/devices/control/${id}`, { method: 'POST', body: JSON.stringify({ command }) }),
  getLatestSensors: () => request('/sensors/latest'),
  getSensors: (params) => request('/sensors' + toQuery(params)),
  getActionHistory: (params) => request('/action-history' + toQuery(params)),
}
