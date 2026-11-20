# NEXUS CONTROL PRO — System Architecture

## Architecture Pattern
NEXUS CONTROL PRO follows **MVVM (Model-View-ViewModel)** with **Clean Architecture** principles:

```
┌─────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                     │
│  Jetpack Compose Screens & Reusable Enterprise UI Components│
└──────────────────────────────┬──────────────────────────────┘
                               │ StateFlow & Events
┌──────────────────────────────▼──────────────────────────────┐
│                       VIEWMODEL LAYER                       │
│ MainVM, PairingVM, DashboardVM, ScreenShareVM, AppManagerVM │
└──────────────────────────────┬──────────────────────────────┘
                               │ Coroutines & Repositories
┌──────────────────────────────▼──────────────────────────────┐
│                         DATA LAYER                          │
│ FirestoreRepository, LocalDeviceManager, AppsRepository,    │
│ FilesRepository, LocationRepository, CallLogRepository      │
└──────────────────────────────┬──────────────────────────────┘
                               │ Android OS & Cloud
┌──────────────────────────────▼──────────────────────────────┐
│                   SYSTEM & BACKEND APIS                     │
│ Firebase Firestore, MediaProjection, FusedLocation, SAF     │
└─────────────────────────────────────────────────────────────┘
```

## Core Modules
- `ui.screens`: Screen composables with dark enterprise theme styling.
- `ui.components`: Header, Metric Cards, Buttons, Status Badges, QR Code canvas.
- `ui.viewmodel`: Reactive view models managing UI state via `StateFlow`.
- `data`: Repositories executing system calls and Firestore synchronization.
- `service`: Foreground services for MediaProjection screen capture, location tracking, and microphone audio.
- `receiver`: Android Enterprise Device Admin receiver.
