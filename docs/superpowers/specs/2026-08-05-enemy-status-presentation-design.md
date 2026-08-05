# Enemy Status Presentation Design

## Context

The Java entity runtime already routes the bank-$03 sword damage results for
burning, stunning, and dying entities into `EntityStatus`, but its render path
only has one normal display definition per entity. The original game renders a
burning entity with an additional fire pair before executing the entity's
normal handler. The missing fire pair is therefore a visible parity gap even
though the status transition itself is already source-backed.

The authoritative source is `LADX-Disassembly/src/code/entities/bank3.asm`:

- `FireSpriteVariants` at bank `$03:$4C44` contains two pair variants. Each
  variant uses tile `$34`; the attributes are palette `$02`/`$04` and their
  X-flipped counterparts.
- `EntityBurningHandler` at `$4C4C` selects variant
  `(hFrameCounter >> 3) & 1`, renders that pair at the active entity's OAM
  position, restores the entity's normal sprite variant, then executes the
  normal handler.
- The fire pair uses the same entity tile offset and flip attribute as the
  active entity. It follows the entity's current position and visual Z.

## Options considered

1. Add a status-aware overlay definition to `EntitySpriteSelection` and have
   `EntityRenderLayer` render it for `BURNING`. This keeps the normal entity
   definition intact, decodes the exact ROM bytes, and supports future
   status-specific overlays without changing every `RoomEntity` constructor.
2. Add a list of arbitrary composite sprites to `RoomEntity`. This would model
   more future cases directly, but would require every motion helper and status
   transition copier to preserve the new mutable render state.
3. Spawn fire through `TransientVfxSystem`. This would reuse an existing
   renderer but would make the fire a detached world effect, losing the
   entity-relative position, tile-offset, and source OAM ordering semantics.

Option 1 is selected.

## Design

### ROM catalog

`EntitySpriteHandlerCatalog.forBurningEntity()` decodes bank `$03:$4C44` as a
two-variant pair list. The returned definition uses a neutral entity type only
as metadata; it is never used as the entity's normal definition.

`EntitySpriteSelection` gains an optional burning overlay definition and a
copy-preserving `withBurningSpriteDefinition` method. The ROM-backed
`EntitySpriteCatalog.load` installs the fire definition for real room
selections. Existing synthetic/test selections remain valid without an
overlay.

### Frame state and rendering

`GameFrameState` carries the current unsigned frame counter. `Main` supplies
it when composing a frame, and `GameFrameSceneBuilder` passes it to
`EntityRenderLayer`.

For each loaded entity, the renderer first draws its normal definition and,
when its status is `BURNING`, draws the fire overlay with variant
`(frameCounter >> 3) & 1`. Drawing the overlay last makes its non-transparent
fire pixels visible over the body, matching the source's lower-OAM-priority
fire sprites without changing the normal entity order. The overlay uses the
entity's position, Z, flip attribute, and tile offset, and resolves palettes
from the same object-palette table.

Previous-room rendering during a scroll uses that room's own overlay
definition and the same frame counter.

### Scope boundary

This slice only adds the burning fire presentation. It does not change the
already-tested status timers, burning-to-Gibdo/Stalfos replacement, death
countdown, or the skipped active-handler movement behavior. Those remain
separate source audits; no generic fire effect is used for other statuses.

## Verification

- Decode the real ROM bytes at bank `$03:$4C44` and assert both pair variants,
  tiles, palettes, and X-flip flags.
- Render an active body plus a burning overlay at frame phases 0 and 8 and
  assert the fire tile, palette, position, Z subtraction, flip, and tile
  offset.
- Assert non-burning entities and selections without an overlay are unchanged.
- Run the complete Java test suite and `git diff --check`.
