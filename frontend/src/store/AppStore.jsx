import { createContext, useCallback, useContext, useEffect, useRef, useState } from 'react'
import { api } from '../api/client.js'
import { createRealtimeClient } from '../api/realtime.js'

const CHART_POINTS = 12

const AppStoreContext = createContext(null)

/** Doi mot diem du lieu tu Backend (thoi gian dang chuoi ISO) thanh dang dung trong giao dien */
function toChartPoint(raw) {
  return {
    time: new Date(raw.time ?? raw.timestamp),
    temperature: raw.temperature,
    humidity: raw.humidity,
    light: raw.light,
  }
}

export function AppStoreProvider({ children }) {
  const [latest, setLatest] = useState(null)
  const [readings, setReadings] = useState([])
  const [devices, setDevices] = useState([])
  const [hardwareOnline, setHardwareOnline] = useState(false)
  const [serverConnected, setServerConnected] = useState(false)
  const [pendingDeviceIds, setPendingDeviceIds] = useState([])
  const [error, setError] = useState(null)
  const [loading, setLoading] = useState(true)

  const pendingRef = useRef(pendingDeviceIds)
  pendingRef.current = pendingDeviceIds

  // Nap trang thai ban dau: buoc 2-6 Hinh 4 va so do moi nhat cho bieu do
  useEffect(() => {
    let cancelled = false

    async function loadInitialData() {
      try {
        const [sensors, deviceList] = await Promise.all([api.getLatestSensors(), api.getDevices()])
        if (cancelled) return

        setLatest({
          temperature: sensors.temperature,
          humidity: sensors.humidity,
          light: sensors.light,
        })
        setReadings((sensors.history || []).map(toChartPoint))
        setHardwareOnline(Boolean(sensors.hardwareOnline))
        setDevices(deviceList)
        setError(null)
      } catch (e) {
        if (!cancelled) setError(e.message)
      } finally {
        if (!cancelled) setLoading(false)
      }
    }

    loadInitialData()
    return () => {
      cancelled = true
    }
  }, [])

  // Nhan du lieu day xuong qua WebSocket
  useEffect(() => {
    const client = createRealtimeClient({
      onSensor: (point) => {
        const reading = toChartPoint(point)
        setLatest({
          temperature: reading.temperature,
          humidity: reading.humidity,
          light: reading.light,
        })
        setReadings((prev) => [...prev, reading].slice(-CHART_POINTS))
        setHardwareOnline(true)
      },
      onDevice: (device) => {
        setDevices((prev) => prev.map((d) => (d.id === device.id ? { ...d, ...device } : d)))
      },
      onStatus: (status) => setHardwareOnline(status.status === 'ONLINE'),
      onConnectionChange: setServerConnected,
    })

    return () => {
      client.deactivate()
    }
  }, [])

  /**
   * Bam cong tac: goi POST /api/devices/control/{id}.
   * Backend giu ket noi cho den khi ESP32 bao trang thai that (200) hoac qua han (503),
   * nen trong khoang do nut o trang thai "Dang xu ly".
   */
  const toggleDevice = useCallback(async (device) => {
    if (pendingRef.current.includes(device.id)) return

    const command = device.currentState === 'ON' ? 'TURN_OFF' : 'TURN_ON'
    setPendingDeviceIds((prev) => [...prev, device.id])
    setError(null)

    try {
      const result = await api.controlDevice(device.id, command)
      setDevices((prev) =>
        prev.map((d) => (d.id === device.id ? { ...d, currentState: result.state } : d)),
      )
    } catch (e) {
      setError(
        e.status === 503
          ? `Thiết bị "${device.name}" không phản hồi. Kiểm tra ESP32 và broker MQTT còn chạy không.`
          : e.message,
      )
      // Lay lai trang thai that tu Backend de giao dien khong hien sai
      api.getDevices().then(setDevices).catch(() => {})
    } finally {
      setPendingDeviceIds((prev) => prev.filter((id) => id !== device.id))
    }
  }, [])

  const value = {
    latest,
    readings,
    devices,
    hardwareOnline,
    serverConnected,
    pendingDeviceIds,
    loading,
    error,
    clearError: () => setError(null),
    toggleDevice,
  }

  return <AppStoreContext.Provider value={value}>{children}</AppStoreContext.Provider>
}

export function useAppStore() {
  const ctx = useContext(AppStoreContext)
  if (!ctx) throw new Error('useAppStore phải được dùng bên trong AppStoreProvider')
  return ctx
}
