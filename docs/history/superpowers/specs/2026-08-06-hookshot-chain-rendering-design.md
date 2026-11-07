# Hookshot Chain and Indoor Bridge Rendering

**Date:** 2026-08-06  
**Status:** Approved for implementation

## Problem

The Java runtime already simulates the hookshot entity's outbound and pulling
motion, but the visible result is incomplete. The ROM's hookshot handler also
emits three dynamic chain-link OAM entries, and indoor hookshot bridge objects
(`$9E`/`$9F`) spawn entity `$68`, rewrite the padded room-object grid to `$9D`,
and update the corresponding background tiles. Those two paths are currently
absent from the host runtime, so a hookshot can move without producing the
same chain and bridge visuals as the game.

## Source behavior

The implementation follows these disassembly paths as the source of truth:

- `src/code/entities/18_hookshot_chain.asm`
  - `RenderHookshotChain` computes signed quarter-distance deltas from Link to
    the hook, emits three links using tile `$24`, and makes each link visible
    on the ROM's two-frame cadence.
  - The indoor interaction path calls `GetObjectUnderEntity` after a
    successful movement step. A moving hookshot crossing `$9E` (pull down) or
    `$9F` (pull up) spawns entity `$68`.
- `src/code/entities/bank15.asm`
  - `HookshotBridgeHandler` moves entity `$68` with speed `$30` or `$D0`,
    rewrites the object under the bridge to `$9D`, and applies the two-column
    tile rewrite.
- `src/code/entities/18_zombie.asm`
  - `GetObjectUnderEntity` samples `x - 1`, `y - 7`, rounds to the object-cell
    origin, and indexes the padded `wRoomObjectsArea` buffer with a `$10`
    stride.
- `src/code/home/copy_data.asm`
  - Draw command `$81` copies a two-byte vertical column with a `$20`-byte
    background-map row stride.
- `src/data/objects_tilemaps/indoor.cgb.asm` and
  `src/data/objects_tilemaps/indoor.dmg.asm`
  - Object `$9D` is the ROM-backed base tilemap. The bridge handler's active
    draw commands then produce the direction-specific visible pattern.

## Design

### 1. Chain OAM is a source-shaped render value

Add an immutable hookshot-chain OAM value to `RoomEntitySnapshot`. It contains
the three raw OAM entries produced by `RenderHookshotChain`, rather than a
renderer-side procedural effect:

- signed 8-bit `(hook - link)` deltas, arithmetic-shifted right twice;
- each subsequent link adds the original quarter-delta;
- raw X is `linkX + deltaX + 4` and raw Y is `linkY + deltaY`;
- tile is `$24` and attributes are `$00`;
- entry visibility follows the ROM's `(count XOR hFrameCounter) & 1` test.

`EntityRenderLayer` converts each visible raw entry using the same `$08` X and
`$10` Y OAM origins used by the existing entity path. Tile `$24` is resolved
from the live `GPU` Link-character tile load, preserving the ROM-only runtime
asset boundary. The host does not introduce a generic OAM allocator or a full
OAM-buffer emulator for this slice; the chain's source-shaped entries are the
smallest stable boundary needed by the renderer.

### 2. Object lookup is an explicit runtime boundary

Add a `RoomEntityObjectQuery` callback returning the object ID, physics byte,
and rounded object-cell coordinates for an entity position. `RoomSession`
implements it from the active room's padded object area and active physics
table. This keeps the exact `$11` base and `$10` row stride in the room/session
boundary, rather than making the entity runtime know how rooms are stored.

The hookshot chain uses this query only after an outbound step succeeds. It
checks the returned object against the direction-specific bridge ID:

- negative hookshot Y speed (`$D0`) -> object `$9E`, bridge direction 0,
  moving down with speed `$30`;
- positive hookshot Y speed (`$30`) -> object `$9F`, bridge direction 1,
  moving up with speed `$D0`.

Wall/blocking results still enter the existing wall-poke or pulling paths and
do not spawn a bridge.

### 3. Bridge motion and side effects are explicit

Add a small fixed-point `HookshotBridgeMotion` model matching the entity
handler's one-axis speed update. `RoomEntityRuntime` owns bridge state for
entity `$68` and:

- spawns it at the ROM-computed object-cell origin plus `(8, 16)`;
- carries the chain transition countdown into the entity;
- advances it with `$30`/`$D0` fixed-point motion;
- clears it when the sampled object is no longer `$9D` and has no physics;
- emits the current object-cell rewrite and tile update each active handler
  tick.

The bridge target uses the ROM's pre-move visual Y convention, while object
validation uses the post-move entity position. This mirrors the source's
`CopyEntityPositionToActivePosition` ordering and its later call to
`GetObjectUnderEntity`.

### 4. RoomSession applies ROM-shaped room mutations

`RoomSession` consumes bridge side effects after the entity tick:

1. write `$9D` into the exact padded room-object cell;
2. rebuild the normal ROM-backed room tilemap, so `$9D` supplies the base
   tile IDs and attributes;
3. reapply persistent bridge tile overrides for cells already rewritten by a
   bridge entity;
4. publish the entity snapshot containing the current dynamic chain OAM.

The active bridge patterns are the two columns emitted by the ROM handler:

```text
direction 0: 04 05    direction 1: 0A 0B
              08 09                  04 05
```

Only tile IDs are directly rewritten by the host-side equivalent of the `$81`
draw commands. Attributes remain those produced by the ROM-backed `$9D`
tilemap builder. The override map retains the latest ROM draw-command state
after the bridge entity clears: the source writes the base `[04,05;04,05]`
pattern when its status has just been cleared, rather than undoing the object
rewrite.

## Non-goals

- No generic Game Boy OAM allocator or complete `AnimateEntities` emulator.
- No procedural replacement for ROM object tilemaps, palettes, physics, or
  macro expansion.
- No new enemy behavior, save-state behavior, or broad room-system rewrite.
- No replacement of the existing entity sprite catalog. The chain's dynamic
  tile is intentionally sourced from the live Link-character VRAM load.

## Invariants and edge handling

- Arithmetic shifts must operate on wrapped signed 8-bit deltas, not on an
  unbounded Java integer difference.
- Object lookup must use the padded room buffer and must treat object ID `0`
  as valid.
- A bridge is triggered by a passable `$9E`/`$9F` cell after movement, never by
  a generic wall collision.
- Room/session code must guard tile writes at the active room tilemap bounds;
  malformed or off-room data must not corrupt neighboring arrays.
- Changing rooms clears transient bridge override state. Rebuilding a room
  reapplies only overrides belonging to the current room.
- Existing entity snapshots and constructors remain source-compatible for
  callers that do not use hookshot rendering.

## Verification matrix

- Pure OAM tests cover signed wraparound, three-link spacing, tile/attribute
  constants, and cadence.
- Pure bridge tests cover direction speed, fixed-point carry, object-cell
  target coordinates, and clear conditions.
- Runtime tests cover successful `$9E`/`$9F` bridge triggers, no trigger on a
  blocked wall, bridge entity motion, status-dependent draw patterns, and
  emitted room mutations.
- Room/session tests cover padded-cell writes, ROM-backed `$9D` rebuilds,
  direction-specific tile overrides, persistence after bridge clear, and
  bounds guards.
- The Java test suite is run with `gradle -p java test` from the isolated
  feature worktree.
