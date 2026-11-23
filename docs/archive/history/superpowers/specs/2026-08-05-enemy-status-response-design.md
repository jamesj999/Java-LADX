# ROM-Backed Enemy Status Response Design

## Goal

Complete the shared status outcomes of the bank-$03 enemy damage routine for
the active room runtime. A ROM result of `$FE` must enter the original burning
state, a result of `$FF` must enter the original stunned state, and numeric
lethal damage must continue through the original dying countdown. These
transitions must be timer-driven and must suppress further combat while the
entity is non-active. The shipped sword table currently exercises `$FF` for
the Color Shell family; `$FE` is retained for the generic damage path used by
other weapons and is tested through a controlled ROM-table fixture.

This is a bounded follow-up to the ROM enemy sword-damage increment. It does
not pretend to implement entity-specific weapon branches, Power Bracelet
lifting, thrown physics, enemy drops, or the generic transient poof renderer.

## ROM source of truth

- `LADX-Disassembly/src/code/entities/bank3.asm:6111`
  `ApplySwordDamagesToEnemy` treats `$FE` as burning, `$FF` as stunned, and
  values below `$F0` as numeric damage.
- `LADX-Disassembly/src/code/entities/bank3.asm:7235`
  The burn branch writes bursting-flame noise `$12`, starts the entity's
  ignore-hits timer, sets status `ENTITY_STATUS_BURNING`, and writes the
  transition countdown `$60`.
- `LADX-Disassembly/src/code/entities/bank3.asm:7260`
  The stun branch starts the ignore-hits timer and falls through to
  `EntityBecomeStunned`, which writes status `ENTITY_STATUS_STUNNED`, private
  countdown 2 `$FF`, and speed Z `0`.
- `LADX-Disassembly/src/code/entities/bank3.asm:7190`
  Numeric lethal damage writes private countdown 3 `$40` before the death
  handler runs.
- `LADX-Disassembly/src/code/entities/bank3.asm:920`
  `EntityBurningHandler` consumes the transition countdown. Non-Gibdo entities
  become dying with private countdown 3 `$1F`, physics flags `4`, and enemy
  destroyed noise `$13`; Gibdo (`$1F`) becomes Stalfos Evasive (`$1E`) and
  returns to active status.
- `LADX-Disassembly/src/code/entities/bank3.asm:1218`
  `EntityStunnedHandler` returns to active status when private countdown 2
  reaches zero. Its countdown is decremented by `UpdateEntityTimers` before
  the status handler executes.
- `LADX-Disassembly/src/code/bank14.asm:699`
  `UpdateEntityTimers` decrements transition countdown, private countdown 2,
  and private countdown 3 once per gameplay frame, when gameplay is not
  paused by a transition, inventory, or dialog.

## Design

### Runtime countdown state

Add one per-slot `transitionCountdown` array to `RoomEntityRuntime` for the
ROM `wEntitiesTransitionCountdownTable` value. Continue using the existing
`dyingCountdown` array for private countdown 3 because it already represents
the `$40` numeric-death lifetime. Add one per-slot `stunnedCountdown` array
for private countdown 2. Each array is unsigned-byte state and is reset when
the slot is cleared or reused.

`tickInternal` decrements the three active countdown arrays at the start of a
loaded entity's frame, matching `UpdateEntityTimers` ordering. It then
dispatches status-specific lifecycle behavior:

- `DYING`: preserve the current `$40` countdown and disable the slot at zero.
- `BURNING`: remain non-interactive while the transition countdown is nonzero;
  when it reaches zero, change Gibdo `$1F` to Stalfos Evasive `$1E` and return
  it to active, otherwise enter `DYING` with countdown `$1F`.
- `STUNNED`: remain non-interactive while private countdown 2 is nonzero;
  return to active at zero and clear the status countdown.
- `ACTIVE` and `INIT`: retain existing motion behavior unchanged.

The active engine's status checks already prevent `resolveCombat` from
processing non-active entities. No additional collision-side workaround is
introduced.

### Sword-result application

In the ROM-backed `resolveCombat` path, branch on the resolved raw sword
result before numeric health subtraction:

1. `$FE`: set status `BURNING`, set transition countdown `$60`, set ignore
   hits to the ROM start value `$0A`, clear the host-side recoil accumulator,
   and emit noise `$12`.
2. `$FF`: set status `STUNNED`, set private countdown 2 `$FF`, clear vertical
   speed/recoil state in the host-side runtime, and emit no guessed sound
   beyond the existing combat jingle ordering.
3. `$FD` and other `$F0..$FC` values: preserve the raw special action in the
   combat event and leave entity state unchanged until their entity-specific
   handlers are ported. They must never be subtracted as health.
4. Numeric values: keep the existing health, flash, ignore, recoil, and death
   behavior.

The shared status code is deliberately independent of enemy-family movement.
While burning or stunned, the runtime does not call an active movement helper;
entity-specific burning fire-sprite rendering and stunned bounce physics are
left as explicit later work rather than approximated with the wrong sprite.

### Sound boundary

Extend `EntityCombatEvent` with one optional secondary raw sound write so a
burning sword hit can preserve both the existing enemy-hit jingle `$03` and
the ROM's bursting-flame noise `$12`. The existing gameplay sound sink routes
both writes; unknown IDs remain ignored. Add the corresponding
`GameplaySoundEvent` mapping from the shipped SFX catalog and keep the current
jingle behavior unchanged. The enemy-destroyed noise `$13` written when the
burning handler expires is intentionally deferred until runtime tick events
exist; the status transition itself is implemented now.

The status increment does not add a VFX event. The current transient VFX
system only renders bush leaves, and creating a generic ROM poof renderer is a
separate asset/rendering increment.

### Gibdo conversion

When a burning Gibdo's countdown expires, replace its type with `$1E` and
resolve the sprite definition through the existing `EntitySpriteHandlerCatalog`
when available. If the runtime is a no-ROM motion fixture, retain the same
status/type transition with an unsupported definition rather than inventing
graphics. The converted entity is active on the following tick and receives
the normal sprite variant selection path.

### Compatibility and reset behavior

Add the three Color Shell entity types `$E9`, `$EA`, and `$EB` to the default
enemy-collision gate. Their initial health and contact damage continue to
come from the ROM health-group tables; their full bank-$36 movement/state
handler remains outside this increment.

Existing no-ROM factories and old `EntityCombatEvent` constructors remain
valid. No-ROM sword fixtures continue to use their current numeric fallback;
the status branches are only reachable when a ROM table supplies a raw special
result. Clearing a slot resets every new countdown and all existing combat
state so a later entity cannot inherit burning or stunned state.

## Verification

Tests will cover:

1. A ROM `$FE` sword result enters burning with countdown `$60`, emits both
   the enemy-hit jingle and noise `$12`, ignores combat while burning, and
   becomes dying with countdown `$1F` when the burn ends.
2. A ROM `$FF` sword result enters stunned with countdown `$FF`, clears the
   vertical/recoil state, ignores combat while stunned, and returns active at
   zero.
3. A burning Gibdo becomes Stalfos Evasive `$1E` and active at the exact
   countdown boundary.
4. `$FD`/unknown special values remain event data and do not change health or
   status.
5. Existing numeric sword death, recoil, projectile, pickup, and full-suite
   behavior remains green.
6. `gradle clean test` and `git diff --check` complete successfully.
