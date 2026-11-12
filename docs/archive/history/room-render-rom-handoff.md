# Room render views and typed wiring handoff

Round-5 status: the app/render integration and all owned deprecated caller
migrations are complete. All former owned byte/enum/event delegates and
`Rom.unsafeBytes()` are deleted. Historical tables below describe the earlier
handoff; see [the integrated report](room-integration-round-5.md) for current
lifetimes, budgets, warning counts and remaining immutable/off-limits costs.

Steps 10 and 11 follow the round-4 seam migration. All app, renderer and UI
consumer sources are unchanged. Existing snapshot methods keep copying and
ownership semantics, including the previous-room snapshots retained for scrolls.

## Measurements

ROM-free `AllocationProbe`, with `-XX:-DoEscapeAnalysis` passed by a temporary
local Gradle init script (no committed runner/build changes):

| Producer | Owned/original | New path | Guard |
| --- | ---: | ---: | ---: |
| Populated room render + dungeon inventory context, including indexed tile, BG/OBJ palette, hidden NPC, flags and minimap reads | 10,456 B/frame | 0 B/frame | <256 |
| Sword box overlapping two active enemies, immutable events and owned drain included | 1,992 B/frame | 456 B/frame | <=570 (25% headroom) |
| Repeated falling display-list decode, including dynamic variant copying | 2,480 B/query | 0 B/query after cache warmup | exactly zero |

The render figure includes dungeon copies and synthetic-room state, so it is not
a direct remeasurement of the app engineer's approximately 8.8 KB room-only
sample. App's existing owned-snapshot path remains until its owners apply the
switches below. Changed immutable events, coordinate-dependent Mini-Moldorm /
Moldorm / Rolling Bones composite definitions, and active owned warp/heart visual
values may still allocate. This does not claim zero whole-renderer allocation.

## Read-only APIs and lifetime

- `roomSession.rooms().renderView()` returns one cached `RoomRenderSnapshot.View`
  (or null with no active room). `activeRoom.renderView()` is available directly.
- Tile IDs, attributes and BG/OBJ palettes have indexed getters; no writable
  backing array is exposed. `entities()` is the runtime's existing read-only
  live presentation. Sprite palette copies are captured once per immutable
  `EntitySpriteSelection`, not on each query.
- `roomSession.dungeon().view()` exposes indexed progress flags, current dungeon
  item flags and the 64-cell minimap. `copyTo(progress8, items5, minimap64)` writes
  into caller-retained arrays without allocating. The minimap follows status
  mutations, map changes and Eagle's Tower's collapsed layout bit.
- Both views are same-thread projections valid only until the next simulation
  tick, room change or world mutation. Do not retain them as historical frames
  or across threads. `renderSnapshot()` and the existing `*Snapshot()` dungeon
  methods remain the owned alternatives.
- Recurring immutable entity display lists use a bounded room-owned cache;
  geometry-dependent composites retain their owned-value construction. Combat
  scratch resets every slot, falls back to separate storage on reentry and
  releases frame references in a finally block.

## App consumer switches

Paths below are under `java/src/main/java/linksawakening/`.

| File:method | Switch |
| --- | --- |
| `app/presentation/RenderStateAssembler.java:currentGameFrameState` | `world.roomSession().rooms().renderSnapshot()` -> `world.roomSession().rooms().renderView()` once the render boundary accepts `RoomRenderSnapshot.View` |
| `app/presentation/RenderStateAssembler.java:composeFrameBuffer` | Retain `byte[8] progress`, `byte[5] items`, `byte[64] minimap` fields; call `world.roomSession().dungeon().view().copyTo(progress, items, minimap)`; pass those buffers to the existing `setWorldContext` / `setDungeonContext` in place of the three world `*Snapshot()` calls |

The render type switch is explicit: the current `render.GameFrameState.room`
component and `GameFrameStateUpdates` / `GameFrameSceneUpdates` room signatures
still accept `RoomRenderSnapshot`. They need the `RoomRenderSnapshot.View`
contract. `render.WorldSceneLayers` and `RoomRenderLayer` must read tiles/palette
colors by index (or copy into retained renderer buffers), rather than asking for
owned arrays or constructing another snapshot. Hardware background overloads
can consume those indexed reads/buffers. Keep owned `ScrollController.previousRoom`
data unchanged. Existing owned snapshot accessors should continue to clone;
returning writable world arrays would weaken their contract.

`InventoryMenu.setWorldContext` and `setDungeonContext` still clone their inputs;
the buffer switch above removes world-side allocation while leaving that UI
ownership intact. Removing UI copies is a separate UI-owner change. The app
allocation guard can add a `renderView()` measurement alongside the owned
measurement; do not interpret the unchanged app test as a measurement of the
new view path before adoption.

## Five typed app wiring switches

All five typed entry points already existed; step 11 adds compiler/compatibility
and shared-storage guards and updates their caller documentation. Typed entry
points: 5 before / 5 after; missing entries: 0 before / 0 after. Deprecated byte
forms remain because these off-limits calls and external tests still use them.

| App call site | Replacement expression |
| --- | --- |
| `app/wiring/SubsystemWiring.java:45` (`build`) | `transition.setRomData(rom)` |
| `app/wiring/SubsystemWiring.java:58` (`build`) | `new OverworldBushInteraction(rom, tables)` |
| `app/wiring/WorldWiring.java:53` (`rooms`, first config argument) | `romData` instead of `romData.unsafeBytes()` in `new RoomSessionConfig(...)` |
| `app/wiring/WorldWiring.java:55` (`rooms`) | `new RoomLoader(romData)` |
| `app/wiring/WorldWiring.java:56` (`rooms`) | `new OverworldTilesetTable(romData)` |

Remove the obsolete wiring comment that says the world still requires the ROM
backing array. Both overload sets preserve shared storage, null handling and
legacy partial-buffer validation; no ROM is copied, normalized or realigned.
