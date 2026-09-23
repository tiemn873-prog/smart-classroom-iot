/* Ket noi WebSocket (STOMP) toi Backend de nhan du lieu day xuong theo thoi gian thuc.
   Backend cong bo 3 kenh:
     /topic/sensor  - moi lan ESP32 gui data/sensor
     /topic/device  - khi trang thai thiet bi thuc su doi
     /topic/status  - khi phan cung ONLINE / OFFLINE */

import { Client } from '@stomp/stompjs'

function buildBrokerUrl() {
  const scheme = window.location.protocol === 'https:' ? 'wss' : 'ws'
  return `${scheme}://${window.location.host}/ws`
}

export function createRealtimeClient({ onSensor, onDevice, onStatus, onConnectionChange }) {
  const client = new Client({
    brokerURL: buildBrokerUrl(),
    reconnectDelay: 4000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,

    onConnect: () => {
      onConnectionChange?.(true)
      client.subscribe('/topic/sensor', (message) => onSensor?.(JSON.parse(message.body)))
      client.subscribe('/topic/device', (message) => onDevice?.(JSON.parse(message.body)))
      client.subscribe('/topic/status', (message) => onStatus?.(JSON.parse(message.body)))
    },

    onWebSocketClose: () => onConnectionChange?.(false),
    onStompError: () => onConnectionChange?.(false),
  })

  client.activate()
  return client
}
