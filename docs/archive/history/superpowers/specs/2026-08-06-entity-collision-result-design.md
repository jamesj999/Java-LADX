# Entity background collision result design

## Context

The Java entity handlers currently receive a boolean wall-collision callback. That is enough to retain a coordinate when a probe is blocked, but it loses the other outputs of the disassembly's `ApplyEntityInteractionWithBackground` path:

- the sampled room-object ID;
- the object's ROM physics flag;
- the directional collision bit (`$01`, `$02`, `$04`, or `$08`); and
- the separate horizontal and vertical collided-object fields.

The missing outputs matter because entity handlers do not all react to collision in the same way. The bank-$03 roaming handler, for example, consumes the collision byte on the next state-0 dispatch and then clears speed and enters state 1. Other handlers inspect only selected direction bits, while some special entities use the collided object ID.

The source of truth is `LADX-Disassembly/src/code/entities/bank3.asm`, especially `ApplyEntityInteractionWithBackground`, `ApplyEntityCollisionWithObject`, `CollisionsTableFlagPerDirection`, and `FineCollisionShapes`. This design introduces the shared result boundary without pretending that the existing Link-oriented `OverworldCollision.pointBlocked` policy is already the complete entity collision implementation.

## Goals

1. Preserve the existing boolean callback and existing motion-handler tests.
2. Add a result-shaped collision boundary that can carry ROM collision outputs.
3. Port the bank-$03 roaming Octorok/Moblin/Iron Mask consumer to directional collision state while preserving the ROM's one-frame consumption timing.
4. Make `RoomSession` provide the result boundary from the same ROM-selected collision probe already used by the current engine.
5. Leave room for the subsequent exact entity-physics resolver (fine shapes, doors, ledges, switch blocks, pits, and special entity exceptions) without embedding those rules in every motion class.

## Non-goals for this slice

- Complete replacement of every handler-local boolean collision query.
- Claiming that Link's `pointBlocked` overrides are valid for every entity.
- Implementing hookshot-chain behavior, ledge state (`wEntitiesUnknowTableJ`), switch-block state, or fine-shape lookup in this change.
- Adding a generic post-handler rollback pass before each handler has a defined movement ownership boundary.

Those are follow-up slices on the same result interface. Keeping them out of this first port prevents duplicate collision application and makes each source branch testable.

## Design

### Result value

Add `EntityBackgroundCollisionResult`, an immutable package-level value with:

```text
blocked             boolean
objectId            unsigned byte, or $FF when no room object was sampled
physicsFlag         unsigned byte from the active ROM physics table
direction           0=right, 1=left, 2=up, 3=down
collisionFlag       one of $00, $01, $02, $04, $08
sampleX/sampleY     the pixel coordinates used for the room-object lookup
```

The direction flag is derived from the disassembly's `CollisionsTableFlagPerDirection`, not from a Java enum or a guessed axis convention. A passable result carries `collisionFlag == 0`; a blocked result carries exactly the bit for the probe direction.

Add `RoomEntityBackgroundInteraction` as a functional interface returning this value. Existing `RoomEntityBackgroundCollision` remains the compatibility API for handlers that have not yet been migrated.

### Runtime wiring

`RoomEntityRuntime` stores an optional rich interaction. Its existing handler calls continue to use the boolean callback. The roaming handler receives the rich interaction when configured and falls back to adapting the boolean callback when it is not.

`RoomSession` installs the rich interaction when it creates or recreates an entity runtime. The rich query reuses `EntityCollisionPointProbe` and the active `OverworldCollision` room, so the sample coordinates and room object are observable without duplicating coordinate math.

### Roaming state

`RoamingEnemyMotion` replaces the semantic role of `collisionPending` with a per-slot collision byte and horizontal/vertical collided-object fields:

1. A blocked probe leaves that axis at its pre-update coordinate, records the object and ORs the direction bit.
2. The next state-0 dispatch checks the low nibble, exactly like `RoamingEnemyState0Handler`.
3. If a collision is pending, it consumes the byte, chooses the `$10 | random & $0F` transition countdown, enters state 1, and clears both speeds.
4. The collision byte is cleared when consumed; the object fields remain the latest recorded values until the next collision result resets them, matching the separation between the collision table and the collided-object WRAM fields closely enough for this staged port.

The old overloads and launch behavior remain intact. This change only replaces the information lost at the collision boundary and the roaming consumer's boolean pending bit.

## Verification

Tests will cover:

- direction-to-collision-bit mapping and passable-result defaults;
- rich-query fallback to the existing boolean callback;
- a blocked right/left/up/down roaming probe recording the correct bit and object;
- the next-frame roaming state-0 transition consuming the bit, clearing speed, and preserving the ROM random range; and
- `RoomSession` rich-query samples continuing to use the ROM collision-point tables.

The full Gradle suite remains the final gate.
