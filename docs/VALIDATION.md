# Validation and release checklist

## Automated checks

Built against RuneLite 1.12.39 using JDK 17 and Java 11 bytecode. Compilation enables all javac warnings and treats warnings as errors. The development launcher narrowly suppresses RuneLite's generic-varargs API warning.

Tests cover:

- Cumulative tench probability reference values, monotonicity, zero catches and large totals.
- Current pearl estimate at minimum levels, integer weighted-level boundaries, 99/99, and boosts above 99.
- The exact configurable-timeout boundary, the first catch after timeout, and session fish/pearl hourly rates.
- Streak definitions, adjacent pearls, unfinished dry runs and incomplete first intervals.
- Additive history, character isolation, tracked-streak isolation, negative values and overflow.
- Persistence round trips, latest-snapshot backup, explicit character reset, malformed/unsupported data and prevention of decreasing stored totals.
- Game message filtering, full-inventory completion, paired Fishing/Hunter XP, XP/chat deduplication, inventory pearl detection, separate tench bonus events, own/other-player ground drops, trade-ins and ground/inventory transfer cancellation.
- World hopping, teleporting, character switching, stale history actions, manual session reset and plugin toggling.
- Overlay rendering, hidden rows, both probability scopes, stable dimensions and graphics-state isolation.
- RuneLite-style Guice child-injector construction with real plugin/overlay/store bindings.

These tests simulate RuneLite events. They do **not** establish the game's actual packet/message order; the following live pass is still required before public release.

## Live acceptance checklist (individual cases remain to be recorded)

1. Start the development client and enable the plugin. Confirm the overlay shows on Molch Island and hides elsewhere. There is no always-visible option.
2. Count 50 ordinary successful catches manually. Check lifetime and session both advance by 50; launch messages must not add extra catches.
3. Repeat at a frenzied spot; check every automatic catch is counted once.
4. Repeat with a full inventory and verify discarded catches still count.
5. Test Rada's blessing and spirit flakes separately and together. Confirm bonus fish give no extra Fishing/Hunter XP, the reward quantity never exceeds two, and the catch-roll total still advances by one. Test a doubled tench quantity if a live drop or trustworthy event recording becomes available.
6. With untradeable-drop notifications disabled, verify a pearl entering inventory increments once. Enable notifications and verify the inventory event plus `Untradeable drop: Molch pearl` chat line still increment only once. Record any separate always-on pearl message verbatim so it can be added as a corroborating signal without broad text matching.
7. At full inventory, verify a pearl placed on the ground increments once; picking it up later must not increment again. Repeat when a pearl ground stack already exists. Golden-tench ground handling needs equivalent verification when possible.
8. Test picking up or dropping old pearls/tenches in the same server tick as a fresh catch. Signed ground/inventory changes should cancel only the transferred loot. Also test natural ground expiry during a fresh catch: it must not cancel the newly caught reward.
9. Trade a tench for 100 pearls, buy rewards, and claim any refund. None should affect caught-pearl statistics.
10. Hop worlds; leave and return; log out and back in within the configured idle period. Session values should survive. Disable/re-enable the plugin and check again.
11. Wait for the configured idle period: the next catch should be session catch 1. Restart the client: the session should start at zero and lifetime totals/streaks should survive.
12. Reset the session manually; lifetime counts must remain unchanged. Separately use RuneLite's **Reset** button at the bottom of plugin settings, confirm the warning, and verify all current-character totals and plugin/overlay settings return to defaults while another character's totals remain intact.
13. Add historical fish and pearls covering the same period, confirm the character in the dialog, and verify actual pearl rate uses the combined lifetime totals while streaks remain unchanged. Switch characters and confirm isolation.
14. At 200 million Fishing XP, verify return chat still arrives for normal/full-inventory/frenzied catches. Without a live capped account, do not claim this is game-verified.
15. Resize/move the overlay, try font sizes 11, 15, and 22, hide every row, and confirm no artifacts or client errors. Verify every label ends with a colon and fish/pearl hourly estimates begin after the second catch.

## Current verification status

The owner reported successful login, relaunch, and normal operation after removal of the configurable overlay visibility option. The precise cause of that option's freeze has not been established. These reports do not establish every edge case above. On 2026-09-19, Moldead confirmed the five-second hourly display works and reported the live checklist passed for accessible cases. Extreme cases, including 200 million Fishing XP, remain unverified in game. This is owner-reported validation, not independently observed gameplay.

## Known boundaries

- Catch correlation assumes at most one successful aerial catch per server tick and reward signals within the same tick. This needs live verification with the current game revision, especially frenzied pools and full inventories.
- A non-fishing source awarding both Fishing and Hunter XP in the same tick while the cormorant is held could resemble a catch. A one-skill reward is rejected unless the other skill is already at the XP cap. This is a boundary of XP-based inference.
- Reward transfer cancellation requires the corresponding owned-ground events in the same tick. Scene reloads, ownership changes, or unusual packet ordering still need live verification. Mixed shop refunds/trades and catches in the same tick can be ambiguous without reward chat.
- The level-dependent portion of the pearl estimate follows the documented wiki formula, including integer division and visible boosts. Jagex described the August 2026 increase as approximately 50%, so the post-update multiplier remains an estimate rather than a published exact server formula.
- Persistence uses RuneLite's normal configuration flush. Abrupt termination can lose recent unflushed data. Deleted profiles/disk failures are outside the plugin's control.
- No logged-in gameplay or authenticated Jagex account was automated during development.

See [INDEPENDENT_REVIEW.md](INDEPENDENT_REVIEW.md) for the independent critic's findings and retest results.
