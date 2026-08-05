# ROM-Backed Evasive Stalfos Design

## Goal

Make the Gibdo burn-expiry result (`$1E`, Stalfos Evasive) render and move
through its ROM handler instead of becoming an active but inert entity.

## ROM source of truth

- `LADX-Disassembly/src/code/entities/15_stalfos_evasive.asm` defines the
  normal pair at bank `$15:$4E7D` and the fleeing pair at `$15:$4E8E`.
- The normal handler uses `wEntitiesPrivateState1Table` to select the fleeing
  branch, `wEntitiesInertiaTable` to select normal versus airborne motion,
  `Data_015_4E89 = [$00,$06,$FA,$FA,$06]` for random walk speeds, and
  `J_A|J_B` to trigger the jump-away state.
- A jump writes transition countdown `$08`, vertical speed `$15`, vector
  length `$12` away from Link, and sprite variant `$02`. The airborne branch
  adds Z speed, decrements that speed each frame, and lands when the signed Z
  position becomes negative; landing writes X/Y speeds `$08` and private
  countdown `$10`.
- The fleeing branch animates variants `(hFrameCounter >> 3) & 1`, moves with
  its current speed, clears on background collision or an ignored-hit sword
  response, and unloads when its screen coordinates cross `$A8`/`$84`.

## Design

Add `StalfosEvasiveMotion` as a state holder for the ROM private state,
inertia, countdown, signed fixed-point X/Y/Z speeds, and accumulators. The
runtime owns the shared transition countdown and combat arrays, while the
motion result reports the updated transition value and whether the handler
requested an unload or sword-poke event.

Decode the two ROM display-list pairs separately. The normal definition is
selected for type `$1E`; the runtime swaps to the fleeing definition while
private state 1 is nonzero. Add an action-button-held flag at the
`RoomSession`/`Main` boundary so the live tick supplies the same held `A|B`
condition as `hJoypadState`.

The Angler's Tunnel-only clone path and its noise effect are not included in
this increment because they require map-id-aware dynamic entity initialization
and a new live spawn event contract. They remain explicit follow-up work;
normal walking, jump-away movement, landing, sprite selection, and the
Gibdo-conversion display path are covered here.

## Tests

- Decode and assert both Evasive ROM display-list addresses and bytes from the
  shipped ROM fixture.
- Verify an initialized Evasive Stalfos performs the ROM random walk and
  selects its two normal animation variants.
- Verify held A/B enters the airborne jump state, writes variant `$02`, adds
  Z speed, and returns to normal motion with landing speeds and countdown.
- Verify the existing real-ROM burn-expiry test now leaves the converted slot
  with the normal Evasive display definition rather than an unsupported one.

## Scope boundary

This increment does not claim full Evasive parity: Angler's Tunnel cloning,
clone-driven fleeing-state entry/side-effect integration, jump/whoosh sound
routing, and generic ground-status/water/pit behavior remain separate
source-backed slices.
