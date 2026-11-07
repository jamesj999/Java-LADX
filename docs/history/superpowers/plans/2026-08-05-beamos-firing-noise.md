# Beamos firing-noise implementation plan

## 1. Lock the source boundary with tests

- Add gameplay audio-consumer and ROM sound-map coverage for noise `$08`.
- Add runtime coverage that countdown `$10` emits one event only when the beam
  slot is available, and a full entity table emits neither beam nor noise.

## 2. Generalize pending entity events

- Rename the burn-specific pending queue/accessors to entity-event names while
  retaining the existing burn event behavior.
- Keep consume-once semantics at the room-session boundary.

## 3. Implement and connect Beamos firing noise

- Return spawn success from `spawnLaserBeam`.
- Enqueue raw noise `$08` only after successful beam creation.
- Map it through `EnemyCombatEventConsumer` and consume it from `Main`.

## 4. Verify and document

- Run focused laser/audio tests, `git diff --check`, and `gradle clean test`.
- Update the roadmap with the source range and preserve remaining laser gaps.
