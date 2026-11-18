# Mabe children validation, 2026-11-07

Implemented the bank 06 ball-game state machine for entities 71/72, using
ROM-decoded display lists at 606D and 607D, the asymmetric Kid71 initializer,
80/60-frame throw cycle, 24-frame projectile lifetime and fixed-point arc.
Normal early-game hints cycle through dialogs 118–11B; after the cello the
children select 11D/11E. The kidnapped BowWow path uses dialog 220, approaches
Link at speed 08 for at most 30 frames, and jumps while waiting. Its automatic
speech runs once per room load; talking can repeat it, as in the ROM.

The full ROM-backed suite completed 2,481 tests with 3 failures. The new
projectile-lifetime and automatic-warning regression tests passed. A separate
run on unchanged HEAD reproduced the same three failing tests:
MainOpeningRouteTest.newGameCanDefeatRollingBonesAndWarpBackToTheEntrance,
BeachOpeningRuntimeTest.swordPickupUsesTheRomFanfareSequenceBeforeGrantingTheSword,
and RoomSessionTest.openingBeachEventsCompleteInPlayOrderAndStayGoneAfterReload.
The focused RoomEntityRuntimeTest suite passed again after restoring changes.

The opening route was tools/opening/tail-cave-warp-route.tsv, SHA-256
79c179a1bce0e23d63c84238a6da404b9ddc8f2786b8751550873dcfffb4bf80.
It failed its route-progress assertion; this run does not establish a completed
opening replay. No emulator frame/state comparison was performed. Pixel parity,
Link pushing/facing interaction details, and later-game conditional kid speeches
remain unverified; these tests do not establish game-wide parity.
