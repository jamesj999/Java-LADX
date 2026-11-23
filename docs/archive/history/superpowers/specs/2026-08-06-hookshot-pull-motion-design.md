# Hookshot Pull Motion Design

**Date:** 2026-08-06  
**Status:** Approved for implementation

## Problem

The Java runtime recognizes the ROM hookshot chain's hookshotable collision
and emits an inverse speed vector for Link, but the visible player position
does not change. The live `Hookshot` item deliberately blocks Link's ordinary
input-driven motion while entity `$03` is active. The current event boundary
therefore queues a speed for a future `Link.update()` that returns before
consuming it.

The original handler moves Link in the same entity pass. Its visible position
must advance toward the stopped chain while normal input remains locked.

## Source behavior

The implementation follows these disassembly paths:

- `LADX-Disassembly/src/code/entities/18_hookshot_chain.asm`
  - `HookshotChainEntityHandler` enters the pulling branch when the active
    entity state is nonzero.
  - It calls `GetVectorTowardsLink_trampoline` with `$30`, negates the vector
    into `hLinkSpeedY` and `hLinkSpeedX`, and immediately calls
    `UpdateFinalLinkPosition`.
  - The handler then checks collision with Link and unloads the chain when the
    normal entity hitbox overlaps.
- `LADX-Disassembly/src/code/bank0.asm`
  - `UpdateFinalLinkPosition` updates the vertical and horizontal Link
    coordinates from the two HRAM speed bytes in source order.
  - `ComputeLinkPosition` preserves the ROM's signed fixed-point carry
    behavior through the `wC11A/wC11B` fractional accumulators.
- `LADX-Disassembly/src/code/bank2.asm`
  - `LinkMotionDefault` treats `hLinkInteractiveMotionBlocked == $02` as a
    motion-lock path, clearing ordinary speed and skipping input movement.

The host is not required to emulate the whole CPU or WRAM. It must expose the
small source-shaped boundary needed to apply this post-handler motion.

## Design

### 1. Separate immediate ROM motion from queued response speed

Keep `Link.applyRomSpeed` for handlers whose speed write is consumed by the
next ordinary Link update, such as reflected laser responses. Add a separate
method for a speed write that also applies `UpdateFinalLinkPosition`
immediately:

```java
public void applyRomFinalPosition(int speedX, int speedY)
```

The method validates unsigned speed bytes, applies vertical motion first and
horizontal motion second, and bypasses normal input collision checks. This
matches the source call site: the hookshot handler has already completed the
entity/background collision pass and calls the shared final-position helper
directly. It must not change Link's facing, clear the active Hookshot item, or
consume a queued response speed belonging to another handler.

The existing sub-pixel position representation remains the host storage for
the source coordinate and fractional carry. The implementation must use the
same signed-byte/fixed-point conversion already used by `Link.update()`, with
tests covering positive, negative, and fractional speeds.

### 2. Apply hookshot pull at the existing frame boundary

`EntityProjectileEvent.Kind.HOOKSHOT_PULL` remains the runtime event that
transports the ROM vector across the room/session boundary. In `Main`, after
the entity tick returns and before the frame is rendered, hookshot events call
`link.applyRomFinalPosition(...)`. Non-hookshot events continue to use the
existing `applyRomSpeed(...)` path and collision-ignore handling.

This preserves the existing ordering:

1. Link performs its normal frame update and item dispatch.
2. The entity pass detects the stopped hookshot and computes the inverse
   vector.
3. The hookshot handler applies Link's final position immediately.
4. The next frame's Hookshot item still blocks normal input while the chain
   remains active.

The current runtime's chain overlap check remains authoritative for unloading;
the next entity tick observes the newly moved Link coordinates just as the
source's post-`UpdateFinalLinkPosition` collision check does.

### 3. Preserve rendering and room-coordinate contracts

No new sprite, tile, camera, or collision approximation is introduced. The
existing `Link.pixelX()/pixelY()` values become the updated render origin, and
the already implemented chain OAM calculation continues to use the current
Link ROM entity coordinates on the next snapshot. Hookshot bridge behavior is
unchanged by this slice.

## Non-goals

- No generic Game Boy CPU/PPU or WRAM emulator.
- No redesign of `EntityProjectileEvent` for unrelated projectile timing.
- No change to the existing queued speed semantics for lasers or other entity
  responses.
- No new hookshotable-object table, collision rule, or chain graphics.
- No integration of the isolated feature branch into `main`.

## Invariants and edge handling

- Both speed arguments are unsigned bytes; their signed interpretation must
  match the ROM.
- Vertical motion is applied before horizontal motion.
- Hookshot pull bypasses ordinary Link collision probes and input speeds, but
  does not bypass the existing room transition/inventory gates that prevent
  the entity pass from running.
- A queued `applyRomSpeed` response must remain pending if a different event
  path is not a hookshot pull.
- Existing Link tests and non-hookshot projectile tests must retain their
  behavior.

## Verification matrix

- `LinkTest` verifies immediate positive and negative final-position motion,
  vertical-before-horizontal ordering, fractional carry, and motion while a
  blocking equipped item is active.
- `HookshotChainMotionTest` and `RoomEntityRuntimeHookshotTest` retain the
  event vector and chain-state assertions.
- A source-wiring test verifies that the live `Main` boundary dispatches
  `HOOKSHOT_PULL` to `applyRomFinalPosition` while retaining
  `applyRomSpeed` for other responses.
- The complete Java suite runs with `gradle -p java test` from the isolated
  feature worktree.
