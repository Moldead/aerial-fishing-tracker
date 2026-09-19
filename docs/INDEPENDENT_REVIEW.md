# Independent critic review

Reviewed 2026-09-18 by a separate critic agent after the implementation's first passing build. This review inspected all production Java files, the existing tests, build configuration, README, validation checklist, and targeted upstream RuneLite lifecycle/tick code. The critic authored `IndependentCriticTest.java`; no production code was edited by the critic.

## Initial independent result

Ran `gradlew.bat test --offline` with the workspace JDK 17 and Gradle cache. **37 tests ran: 33 existing tests passed, four new adversarial tests failed.** These are simulated event/model tests, not live OSRS observations.

### P2: A completed catch can be lost when disabling the plugin

`AerialFishingPlugin.shutDown()` originally cleared `pending` before processing it. Reproduction: register a successful catch signal, disable the plugin before the next `GameTick`, enable it, and advance one tick. Expected lifetime catches: 1; actual: 0. The existing toggle test completed the tick before disabling and therefore missed this window. Regression: `disablingBetweenCompletionAndTickMustPreserveTheCompletedCatch`.

There is a related threading defect: RuneLite `PluginManager` explicitly runs `startUp`/`shutDown` on the Swing EDT, whereas game events run on the client thread. Original shutdown mutated `pending` and `ready` directly, and `running` crossed threads without synchronization. Move tracking-state lifecycle operations onto the client thread and handle rapidly queued toggles carefully. Synchronous test mocks do not establish concurrency safety.

### P2: Separate bonus tench events are collapsed

`CatchAccumulator.ground()` and `.inventory()` originally kept the maximum delta instead of accumulating distinct positive deltas. Two nonstackable tench spawns, each of quantity 1, were recorded as one tench. Likewise, two separate inventory increases of 1 in a catch tick produced one item. This does not alter the tench-roll denominator but understates the explicitly supported bonus-item total. Regressions: `separateGroundTenchItemsFromOneBonusCatchBothCount` and `interleavedSeparateInventoryTenchGainsBothCount`.

These regressions test possible distinct RuneLite reward events; they do not prove which inventory packet batching or ground representation the live server uses for bonus tenches. Correct reconciliation should support the distinct-event case and still avoid counting ground-to-inventory transfers twice.

### P3: Missing-primary backup recovery silently decreases totals

After saving catches 1 and then 2, deleting only the primary configuration key causes `StatsStore.load()` to return the previous backup with catches 1 and resume silently. Regression: `backupPreservesLatestSnapshotWhenPrimaryKeyIsMissing`. The original backup was intentionally a previous snapshot, and deletion/disk failures were already excluded from guarantees; this is a resilience concern rather than ordinary-session data loss. Prefer an additional latest mirrored snapshot, or fail closed with a recovery notice instead of silently reducing the lifetime count.

## Additional boundaries requiring attention

- The original accumulator ignored negative ground/inventory deltas. A pickup of an old pearl in the same tick as a catch can look like a newly caught pearl. Sum matched owned-ground and inventory changes to cancel simple transfers; include ground despawns as well as quantity changes. Live testing must verify that despawn/ownership/location data are sufficient and that scene unloads are not interpreted as transfers.
- Fishing XP originally accepted any increase up to 1000 while holding the glove. This includes unrelated XP rewards and is broader than the four fish's XP values. A stricter set of ordinary XP deltas, or corroboration from Hunter XP, reduces false positives. Capped XP and special world multipliers complicate strict matching and must be handled explicitly.
- The catch signal accumulator assumes a maximum of one successful action per server tick. Current supplied wiki source describes a minimum ordinary interval of one tick and a frenzied interval of three ticks, consistent with this assumption. It does not establish actual game-message/XP/reward packet ordering.
- The documentation's assertion that the return-message fallback supports 200-million-XP accounts needs live verification, especially frenzied pools and chat-filter settings. Mocked emission of that message cannot verify that it exists in all those cases.
- Probability math, historical-count isolation, ordinary session timeout behavior, colour grouping and labels were consistent with the stated design. The estimated pearl formula is explicitly provisional, matching the inspected wiki text.

## Release assessment

Do not present the initial build as independently approved: the critic reproduced actual failures. After fixes, rerun all tests and record the independent retest below. Regardless of automated results, the current live acceptance checklist remains necessary before public release. No logged-in game session, credentials, game input automation, or live rare-drop event capture was used in this review.

## Retest

After the implementation fixes, a fresh independent critic reran a clean offline build against RuneLite 1.12.39. **All 53 tests passed with zero failures or errors.** The critic rechecked catch/reward reconciliation, persistence validation, lifecycle handling, session behavior, labels, documentation, and the `build=standard` release metadata and found no remaining reproducible production-code defect.

During the final review, the critic briefly proposed that simultaneous Rada's blessing and spirit-flake activations could award three fish. The user corrected this mechanic: the proc chances stack, but the resulting reward remains capped at two. The invalid triple-tench tests were removed, the production two-item limit was retained, and the independent clean build was rerun after that correction.

This approval covers source inspection and simulated RuneLite events. It does not replace the live acceptance pass for current server event order, frenzied pools, full inventories, owned ground loot, capped XP, the exact always-on pearl message, or a doubled golden-tench event.

## 2026-09-19 update review

A separate critic reviewed the font/label changes, colon formatting, historical actual-rate calculation, level-dependent expected pearl estimate, cumulative tench probability, overlay visibility modes, hourly rates, full reset, and revised longest-dry convention. The critic confirmed that 16,000 independent 1/20,000 rolls produce a 55.068% chance of at least one tench and that the level formula uses `floor(((Fishing * 2) + Hunter) / 3)` with visible boosts. Jagex describes the 19 August 2026 pearl-rate increase as approximately 50%, so the applied 1.5 multiplier is a documented estimate rather than a claimed exact server implementation.

The critic found that the first reset implementation added a second control while RuneLite's built-in settings **Reset** button still preserved lifetime data. The plugin now overrides RuneLite's reset hook, so the existing confirmed Reset action deletes the selected character's primary and backup snapshots, clears its loaded session, and restores overlay placement, including when the plugin is disabled. The duplicate control was removed. The critic also reviewed the hourly-rate timing and the ending-roll convention for pearl intervals. After these fixes and the schema-1 dry-streak migration, the clean build passed all **61 tests** with zero failures or errors.

## Release preparation review — 2026-09-19

A separate critic reviewed the current source and independently ran the 13 critic/event tests; all passed. It found a historical-addition dialog race: changing characters while the modal dialog was open could redirect the addition to the newly displayed character. The caller now captures the original profile before opening the dialog, allowing the plugin's existing profile check to reject a stale action. The existing stale-profile event test covers rejection, but the modal Swing interaction itself has not been automated.

The review also corrected stale documentation about overlay visibility, pearl notifications and timeout timing. No further confirmed reset or overlay blocker was reported. This review does not certify RuneLite approval or live server event ordering.
