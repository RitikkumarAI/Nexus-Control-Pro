# NEXUS CONTROL PRO — API & Repository Documentation

## System API Contracts

### 1. `FirestoreRepository`
- `updateDeviceTelemetry(device: NexusDevice)`: Broadcasts real-time battery, storage, RAM, and network metrics to Firestore.
- `generatePairingCode(managedDeviceId, managedDeviceName)`: Creates a cryptographically random 6-digit PIN and QR payload.
- `submitPairingCode(adminDeviceId, adminDeviceName, code)`: Authenticates 6-digit PIN from Phone B.
- `confirmPairing(pairingId, accept)`: Managed phone confirms or rejects pairing request.
- `sendSdpOffer(...)` & `sendSdpAnswer(...)`: WebRTC PeerConnection signaling over Firestore snapshot listeners.

### 2. `AppsRepository`
- `getInstalledApplications()`: Queries Package Manager activities to resolve launcher apps, labels, package names, versions, and system app flags.
- `hasUsageAccessPermission()`: Checks if Usage Access permission is granted for `UsageStatsManager`.

### 3. `FilesRepository`
- `listDirectory(dirPath)`: Navigates directory hierarchy using Scoped Storage SAF.
- `getMediaItems()`: Queries MediaStore external content provider for photos and videos.

### 4. `LocationRepository`
- `getCurrentLocation()`: Fetches GPS coordinates via FusedLocationProviderClient.
