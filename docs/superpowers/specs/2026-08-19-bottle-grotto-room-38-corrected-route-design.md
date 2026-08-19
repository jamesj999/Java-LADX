# Bottle Grotto Room `$38` Corrected Route Design

## Goal

Continue the uninterrupted Bottle Grotto trace after collecting the Stone Beak
in room `$2E`, backtrack through the cleared west branch, use the live crystal
switch to cross room `$34`'s partition, and collect the Small Key in room `$38`.

## Source and route

`MapLayout1` and the loaded room boundaries make the route `$2E -> $30 -> $31
-> $32 -> $33 -> $34 -> $39 -> $38`. The apparent shortcut from `$37` is not
usable, and Link has zero Small Keys at the Stone Beak checkpoint, so room
`$35`'s north key door cannot be the next interaction.

Room `$33` contains the live crystal switch. Its ordinary sword interaction
must put the shared switch-block state in the configuration that makes room
`$34`'s `$DB/$DC` partition and south boundary collision-passable. The trace
then crosses room `$34` south into `$39` and west into `$38`; it must not warp,
rewrite room objects, or set the switch state directly.

`IndoorsA38` contains chest `$A0` at `$43` and the switch-block arrangement.
`IndoorsA38Entities` contains Moblin Sword `$14` at `$62` and crystal switch at
`$45`. `DungeonEventsTable[$38]` is `$00`, so the Moblin is optional and cannot
gate or manufacture the chest. The indoor-A chest table selects
`CHEST_SMALL_KEY` for room `$38`.

## Chosen scope

This slice completes the corrected route and all source-authored interactions
in room `$38`: collision-valid backtracking and partition crossing, ordinary
Moblin combat/death, the complete chest reward/dialog/teardown lifecycle, and
the room's live crystal-switch transition. It ends with one Small Key.

Stopping on entry would not prove the route yields its progression item.
Continuing through room `$39`, room `$35/$2F`'s key door and push-block stairs,
the side-view passage, and Hinox would combine several distinct milestones and
make failures difficult to localize.

## Runtime constraints

- Extend the existing fresh-game ordered regression without resetting the
  session, player inventory, persisted room status, or global switch state.
- Find movement using the live collision map and use normal transition and
  interaction APIs.
- Defeat the Moblin only through ordinary sword collision, damage recovery,
  and death ticks; its removal must leave event `$00` unchanged.
- Open the chest from its real interaction boundary and apply only emitted
  reward events. Observe the ROM-selected dialog and entity teardown.
- Strike the room `$38` crystal switch through its ordinary entity collision
  path and verify the staged state/collision change rather than assigning it.
- Bound every animation, recovery, death, transition, and reward wait with an
  explicit terminal assertion.
- If a RED exposes a shared runtime defect, make the smallest general fix under
  TDD. Do not add room-id or coordinate-specific production exceptions.

## Verification

The slice is complete when the uninterrupted trace reaches `$38`, confirms its
ROM objects/entities/event, removes the Moblin without changing the event,
collects the chest Small Key through the full lifecycle, exercises the local
crystal switch, and finishes with one key. Run the ordered regression, focused
transition/combat/chest/switch suites, the clean Java suite, `git diff --check`,
and independent source-fidelity and code-quality reviews.
