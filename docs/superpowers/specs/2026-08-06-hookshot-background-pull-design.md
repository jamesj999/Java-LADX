# Hookshot background interaction and pull design

## Scope

Extend the live ROM entity `$03` hookshot chain from open-room outbound/return
motion to the shared background-interaction boundary. This slice covers the
hookshot chain's source state `$01`, hookshotable-object detection from the
rich entity collision result, the `$26` point-blank unload threshold, the
deferred wall-poke response, Link's forced pull vector, hookshot noise, and
the existing sword-poke sound/VFX event boundary.

Hookshot bridge spawning and the three dynamic chain-link OAM entries remain
separate slices. They require entity `$68` spawning and a renderer-side
dynamic OAM allocation contract respectively; neither is needed to make the
background collision and pull state source-correct.

## Source of truth

- `src/code/entities/18_hookshot_chain.asm:7-164` — handler ordering,
  outbound/pulling branches, collision-table wall poke, pull speed writes,
  hookshot noise, and sword-poke VFX coordinates.
- `src/code/entities/bank3.asm:7809-8192` — collision-point sampling,
  hookshotable physics `$60`, the `$26` point-blank guard, state `$01`, switch
  block exception, collision-table flags, and position rollback.
- `src/code/entities/bank3.asm:8497-8626` — infinity-norm vector helper used
  to pull Link toward the chain.
- `src/code/entities/bank3.asm:8662-8684` — Link Y distance includes the
  entity Z position.
- `src/code/bank0.asm:3856-3900` — `UpdateFinalLinkPosition`, which consumes
  the forced Link speed after the entity handler writes it.
- `src/constants/physics.asm:35` — `OBJ_PHYSICS_HOOKSHOTABLE = $60`.

## ROM-facing behavior

1. The entity handler continues to run after the chain launch and writes the
   hookshot noise request (`NOISE_SFX_HOOKSHOT = $0B`) every fourth frame.
2. State `0` is the outbound/return state. While its effective transition
   countdown is nonzero, it advances with the existing fixed-point speed and
   samples the shared background probe. When the countdown reaches zero it
   changes to the return vector path and no longer performs outbound wall
   interaction.
3. A blocked `$60` sample is handled from the physics byte, never from a
   hardcoded object-ID list:
   - if the post-decrement countdown is at least `$26`, the chain unloads;
   - otherwise the chain rolls back to its pre-move position and latches
     entity state `$01`.
4. A blocked non-`$60` sample rolls back to the pre-move position and leaves a
   collision-table wall flag pending. On the next hookshot handler tick the
   pending flag writes transition countdown `0`, jingle `$07`, and the
   sword-poke transient VFX at `(entity X, entity Y - entity Z)`. The chain
   then follows its return path on the subsequent tick.
5. A hookshot chain passes a switch-block sample when the synchronized
   `wLinkStandingOnSwitchBlock` value is nonzero, matching the source special
   case. Other switch-block state/object rules remain in the shared resolver.
6. State `1` does not move the chain. If the source hitbox overlaps Link it
   unloads; otherwise it computes the ROM `$30` infinity-norm vector from the
   chain to Link, negates both components, and emits the forced Link speeds.
   The gameplay loop applies that event after the entity pass so the next Link
   update consumes the same speed write as `UpdateFinalLinkPosition`.

## Host design

- `RoomEntityBackgroundInteraction` remains the single rich probe seam. The
  runtime keeps the existing boolean adapter for other entity families but
  gives hookshot `$03` the complete `EntityBackgroundCollisionResult`,
  including `physicsFlag`, `objectId`, `direction`, and sample coordinates.
- `HookshotChainMotion.State` owns the source entity state and the pending
  collision-table wall flag beside the existing position, speed,
  accumulator, and countdown fields. The pure motion class exposes the vector
  and state transitions without knowing about Link, audio, or rendering.
- `EntityProjectileEvent.Kind.HOOKSHOT_PULL` carries only the raw unsigned
  X/Y speed write. `EntityCombatEvent` carries the wall jingle and existing
  `SwordPokeVfx` coordinate payload, preserving the established gameplay
  event consumers.
- `Main` applies hookshot pull speed at the same post-entity boundary used for
  reflected projectile speed writes. It does not apply a collision-ignore
  countdown or reset sword spin for this event.
- The `$0B` noise write is mapped to a new `GameplaySoundEvent.HOOKSHOT` using
  the existing ROM sound-effect catalog entry.

## Invariants and explicit non-goals

- Hookshotable detection is `physicsFlag == 0x60`; object IDs are diagnostic
  data, not a replacement for the ROM physics table.
- State `$01` is only entered after a non-point-blank hookshotable collision.
- Wall poke does not unload the chain; it changes the countdown to zero and
  lets the normal return path perform the eventual Link-hit unload.
- The existing boolean background-collision overload remains behaviorally
  valid for tests and callers that do not provide a rich probe; it can model a
  wall stop but cannot identify a hookshotable object.
- Bridge entity `$68` creation and dynamic chain-link OAM are not part of this
  slice.
