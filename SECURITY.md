# NEXUS CONTROL PRO — Security Specification

## Security Principles & Invariants
1. **Full User Transparency**: Every active screen share, location update, or audio assist session displays a non-dismissible persistent notification on Phone A.
2. **Explicit Consent**: MediaProjection screen capture requires explicit user confirmation on every session start. Silent background recording is strictly prohibited.
3. **Emergency Kill Switch**: Phone A user can revoke all admin pairings instantly, terminating all active background services.
4. **Encrypted Communication**: All signaling SDP exchanges and telemetry sync utilize TLS and Firebase Auth security rules.
5. **Least Privilege Permissions**: Scoped Storage SAF and MediaStore photo picker are used instead of broad legacy storage permissions.
