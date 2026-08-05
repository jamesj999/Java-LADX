# Spike Trap audio side effects design

## Scope

Complete the already-portable Spike Trap state machine with the two raw sound
writes emitted by the bank-$06 handler. Movement, four-state timing, random
direction selection, background blocking, and sword/contact behavior already
exist. This slice only carries the source sound writes through the existing
entity-event and gameplay audio boundary.

## ROM behavior

`SpikeTrapState1Handler` at bank-$06 `$753F-$759E` enters state 2 only when
the selected launch path has no background collision. The success branch at
`$759A` writes `NOISE_SFX_WHOOSH` (`$0A`) to `hNoiseSfx` and increments the
state. A blocked launch clears the transition countdown and returns without a
sound.

`SpikeTrapState2Handler` at `$75A1-$75C1` moves once per active frame. When
the transition countdown reaches zero, or when the background collision check
sets a collision bit, `$75A9` writes `JINGLE_SWORD_POKING` (`$07`) to
`hJingle`, loads countdown `$20`, and increments the state to 3. The jingle
is emitted once for that state-2-to-state-3 transition.

## Java contract

`SpikeTrapMotion.advance` returns the updated entity plus at most one raw
sound write for the handler invocation. `RoomEntityRuntime` appends that raw
write to its existing pending `EntityCombatEvent` list. No VFX, damage, or
new collision behavior is introduced here.

The existing gameplay consumer already maps jingle `$07` to
`GameplaySoundEvent.SWORD_POKE`. Noise `$0A` gets the explicit
`SPIKE_TRAP_WHOOSH` gameplay event and resolves through the shipped ROM
`NOISE_SFX_WHOOSH` catalog entry.

## Non-goals

- No speculative generic sound mapping for unknown ROM IDs.
- No changes to Spike Trap launch windows, fixed-point movement, or collision
  predicates.
- No claim that every entity writing sound registers is now routed.
