# Celebration review — 2026-11-14

Independent critic reviewed persistence migration, catch correlation, presentation isolation, audio worker lifecycle, and local-only preview controls. It added four tests and ran the complete suite: 76 passing tests, no failures/errors.

Finding fixed: an exception in celebration presentation was inside the persistence error path, incorrectly pausing tracking even after a successful save. Presentation errors are now logged separately, and the regression verifies tracking continues.

Audio device loading and cleanup are worker-thread operations. Cancellation invalidates in-flight loading. Custom PCM WAV resources replace all third-party audition audio. Test controls are under src/test and excluded from the production JAR.

No remaining blocking findings were reported. This is code/test verification, not live-game or audio-hardware certification. See VALIDATION.md for owner checks and schema upgrade/rollback notes.

## Release verification - 2026-11-15

The clean public-source checkout passed 81 tests with zero failures/errors and a complete Gradle build. Added event-level coverage for single-message fallback, owned-ground singles/doubles, split inventory/ground doubles and matching audio/visual selection. An independent second review ran 48 targeted tests and found no blocking issues. Moldead confirmed the previews and updated text in game. The preview plugin source and launcher registration have been removed; the production JAR contains only the tracker and bundled custom audio. Real rare-drop packet timing remains a documented live boundary.
