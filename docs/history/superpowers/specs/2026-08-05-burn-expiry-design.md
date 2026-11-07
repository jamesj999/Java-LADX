# Burn-expiry side-effect design

## Scope

Complete the non-Gibdo burn-expiry side effects that are missing from the
Java entity runtime. The existing implementation already reaches DYING with
the ROM `$1F` countdown and converts a burned Gibdo into Stalfos Evasive.

## ROM behavior

`EntityBurningHandler` in bank-$03 at `$4C4C-$4CA3` checks the transition
countdown. When it reaches zero:

- Gibdo `$1F` becomes Stalfos Evasive `$1E` and returns active without the
  destruction noise.
- Every other burned entity receives private countdown 3 `$1F`, status
  `DYING`, physics flags exactly `$04`, and `NOISE_SFX_ENEMY_DESTROYED`
  (`$13`).

The branch does not call `AddTranscientVfx`; the death handler renders the
countdown animation and later calls `DidKillEnemy`. A poof here would be an
invented effect.

## Java contract

Add a pending entity-status event represented by the existing raw
`EntityCombatEvent` sound contract. On non-Gibdo burn expiry it carries:

- the entity slot/type;
- no Link or enemy damage;
- noise channel `$13`;
- no secondary sound or VFX.

The room runtime exposes the pending events once per entity tick. `RoomSession`
passes them to `Main`, where the existing `EnemyCombatEventConsumer` routes
the raw noise to a new explicit `ENEMY_DESTROYED` gameplay sound. This keeps
the world layer independent of the audio sink while preserving the timing at
the status transition boundary.

Track the burn-expiry physics byte in the runtime's per-slot state and expose
it to package-level tests. Clear it with the other entity state on slot
cleanup. The byte is not added to `RoomEntity`; it is handler-owned mutable
state, matching the existing countdown/recoil arrays.

## Non-goals

- Replacing the existing death animation/countdown implementation.
- Adding a poof to burn expiry.
- Implementing lifting/thrown physics or the broader death/drop matrix.
