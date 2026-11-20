# NEXUS CONTROL PRO — Testing Plan

## Test Strategy & Coverage
1. **Unit Tests (`app/src/test/java/com/example/ExampleUnitTest.kt`)**:
   - Model initialization tests.
   - Pairing code format verification (6 numeric digits).
   - Telemetry percentage calculations.

2. **Robolectric Integration Tests (`app/src/test/java/com/example/ExampleRobolectricTest.kt`)**:
   - Application context startup.
   - Preference DataStore role persistence.
   - Local device manager telemetry extraction.

3. **Compilation Verification (`compile_applet`)**:
   - Full Gradle Kotlin DSL build check.
