# Spike Trap audio implementation plan

## 1. Lock the source timing with tests

- Assert a clear horizontal launch emits exactly one raw noise `$0A` event.
- Assert a blocked launch emits no noise.
- Assert the state-2 countdown boundary emits exactly one raw jingle `$07`.
- Assert the gameplay consumer and ROM sound catalog resolve noise `$0A` to
  `NOISE_SFX_WHOOSH`.

## 2. Carry one motion result through the runtime

- Return the existing entity update together with an optional raw sound from
  `SpikeTrapMotion`.
- Append it to `RoomEntityRuntime`'s existing pending-event queue without
  changing old tick overloads or the live `RoomSession` boundary.

## 3. Complete explicit gameplay mapping

- Add `SPIKE_TRAP_WHOOSH` to the gameplay event enum and map it to noise `$0A`.
- Teach `EnemyCombatEventConsumer` the source noise ID while keeping unknown
  IDs silent.

## 4. Verify and document

- Run focused Spike Trap/runtime/audio tests, `git diff --check`, and
  `gradle clean test`.
- Update the reconstruction roadmap with the covered source addresses and
  remaining entity gaps.
- Commit the clean checkpoint on the isolated branch.
