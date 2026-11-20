# NEXUS CONTROL PRO — Firestore Database Schema

## Collections

### 1. `/devices/{deviceId}`
- `deviceId`: String
- `deviceName`: String
- `manufacturer`: String
- `model`: String
- `androidVersion`: String
- `batteryLevel`: Int
- `isCharging`: Boolean
- `networkType`: String
- `isOnline`: Boolean
- `totalStorageGb`: Double
- `usedStorageGb`: Double
- `totalRamGb`: Double
- `usedRamGb`: Double
- `uptimeHours`: Long
- `lastSeenTimestamp`: Long
- `role`: String ("ADMIN" | "MANAGED")

### 2. `/pairings/{pairingId}`
- `pairingId`: String
- `managedDeviceId`: String
- `adminDeviceId`: String
- `pairingCode`: String (6 digits)
- `status`: String ("GENERATED" | "CONNECTING" | "PAIRED" | "REVOKED")
- `createdAt`: Long
- `expiresAt`: Long

### 3. `/signaling/{sessionId}`
- `sessionId`: String
- `pairingId`: String
- `managedDeviceId`: String
- `adminDeviceId`: String
- `sessionType`: String
- `status`: String
- `sdpOffer`: String
- `sdpAnswer`: String
- `iceCandidates`: Array<String>
- `timestamp`: Long

### 4. `/audit_logs/{logId}`
- `id`: String
- `timestamp`: Long
- `action`: String
- `initiatorRole`: String
- `targetDeviceId`: String
- `details`: String
- `success`: Boolean
