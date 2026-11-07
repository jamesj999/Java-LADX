# Entity Pit Falling

## Goal

Port the pit-entry and `EntityFallHandler` state machine from bank `$03` into
the Java room entity runtime. An eligible entity must enter status `$02` with
the same aligned target coordinates, transition countdown, fixed-point vector
movement, fall animation cadence, falling jingle, and unload boundary as the
ROM.

Wall collision rollback and the complete execution of each entity's active
handler during the first `$30` falling frames remain separate increments. They
require the complete `ApplyEntityCollisionWithObject` shape path and must not be
invented as part of this state-machine port.

## Source behavior

The authoritative code is `LADX-Disassembly/src/code/entities/bank3.asm`:

- `ApplyEntityInteractionWithBackground` reaches `.onPit` when the sampled
  object is `OBJECT_WELL` (`$61`) or the physics flag is `OBJ_PHYSICS_PIT`
  (`$50`) / `OBJ_PHYSICS_PIT_WARP` (`$51`).
- BowWow (`$6D`), Rooster (`$D5`), and Heart Container (`$36`) never fall.
  Marin (`$C1`) falls only when Link's motion state is
  `LINK_MOTION_FALLING_DOWN` (`$06`) and the sampled object is the well.
- The source requires a nonzero ignore-hits countdown. It decrements that
  countdown, clears the flash countdown, writes entity status `FALLING` (`$02`),
  and stores `hIntersectedObjectLeft + $08` and
  `hIntersectedObjectTop + $10` as the falling target X/Y.
- The transition countdown is `$6F` for Moblin Sword (`$14`), Moblin (`$0B`),
  and Octorok (`$09`). Other falling entities use `$48`; when their ignore-hits
  countdown reaches zero, the source changes that countdown to `$2F` and
  writes jingle `$18` (`JINGLE_ITEM_FALLING`).
- `EntityFallHandler` unloads when the transition countdown reaches zero. For
  countdown values below `$40`, it selects `(countdown >> 4) & $03`, applies
  the ROM visual-Y table `[0, 0, 4, 0]`, renders the corresponding falling
  display, writes jingle `$18` when the countdown equals `$3F`, and moves toward
  the saved target using `ApplyVectorTowardsLink` with vector length selected
  from `[0, 1, 3, 6]` by the same animation phase.
- `ApplyVectorTowardsLink` compares absolute X/Y distances, divides the
  smaller axis by the larger using the ROM's repeated-remainder algorithm,
  assigns the full vector length to the dominant axis, applies the original
  signs, and `UpdateEntityPosWithSpeed_03` advances fixed-point positions.
- For countdown values at least `$40`, the source runs the entity's active
  handler before returning. The Java slice records this as an explicit
  deferred branch; it does not run a handler under the wrong status or guess
  family-specific behavior.

## Design

`RoomEntityRuntime` owns two per-slot falling target arrays. The existing
`enemyTransitionCountdown` and `enemyIgnoreHitsCountdown` arrays remain the
single countdown sources so their decrement order stays aligned with the
existing `AnimateEntities` port. The room callback returns a result that can
request a pit transition in addition to terrain status, unload, and water
splash effects.

The runtime handles `FALLING` before ordinary `ACTIVE` dispatch:

1. decrement shared countdowns at the existing frame boundary;
2. unload at transition zero;
3. for the current countdown, select the ROM phase and vector length;
4. move toward the saved target using ROM signed-byte and fixed-point math;
5. preserve status `FALLING` and the entity's renderer-facing position.

The pit callback is only invoked after the entity-family handler for an active
entity. It receives the source sample's raw object and physics flag plus the
aligned cell coordinates already exposed by `GroundInteractionSample`. This
avoids a second, potentially different terrain lookup.

The callback result carries:

- the updated entity;
- the resulting ground-status byte;
- an optional falling transition;
- unload and water-splash side effects.

When a falling transition is accepted, the runtime clears flash state, stores
the target, writes the countdown/status, and emits the `$18` jingle only at the
source's ignore-hits expiry boundary. It does not emit the normal water splash
for a pit transition.

## Renderer boundary

The current `RoomEntity` model has one hardware-position Y coordinate and a Z
offset. This slice preserves the exact countdown/variant state and target
movement, and exposes the ROM visual-Y phase to the runtime tests. A later
renderer increment will add the separate `hActiveEntityVisualPosY` offset for
the phase-$02 `+4` pixel correction without conflating it with entity Z.

## Tests and acceptance criteria

- Room/session tests prove a pit sample creates a falling transition with the
  aligned target and correct exception behavior.
- Runtime tests prove the `$48`/`$6F` countdown choices, ignore-hit decrement,
  flash clear, status `$02`, unload at zero, jingle `$18` timing, and target
  vector movement.
- Vector tests cover dominant-axis selection, sign handling, zero distance,
  and the ROM fixed-point position update.
- The existing clean Java suite passes with no unrelated test changes.
