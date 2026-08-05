# Beamos firing-noise design

## Scope

Route the missing Beamos firing noise through the existing ROM parent/sensor/
beam runtime. Parent rotation, sensor triggering, beam spawn, beam movement,
and beam collision are already implemented.

## ROM behavior

`LaserEntityHandler` in bank-$04 at `$6C61-$6CB3` checks the parent
transition countdown. When it equals `$10`, it calls `SpawnNewEntity` for beam
`$2B`. If the spawn fails, the handler returns immediately and emits no sound.
If the spawn succeeds, it writes `NOISE_SFX_BEAMOS_LASER` (`$08`) and copies
the parent position, direction, and speed into the new beam.

## Java contract

Keep `LaserMotion.ParentUpdate.spawnBeam` as the exact `$10` boundary signal,
but make `spawnLaserBeam` report whether a slot was actually created. Only a
successful creation appends an entity event carrying noise `$08`.

The existing pending entity-event queue is generalized from its burn-only
name. `RoomSession` exposes a consume-once accessor, and `Main` sends the raw
event through `EnemyCombatEventConsumer` at the same post-entity-tick
boundary. Add the explicit `BEAMOS_LASER` gameplay sound mapped to the shipped
ROM noise effect.

## Non-goals

- Changing parent countdown/rotation or sensor trigger timing.
- Adding a second sound on sensor creation or beam collision.
- Guessing audio for any other unimplemented entity handler.
