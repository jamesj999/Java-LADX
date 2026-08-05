# Enemy Status Presentation Implementation Plan

## Goal

Render the ROM's burning fire pair over entities whose runtime status is
`BURNING`, with exact bank/address, frame cadence, palette, tile-offset, flip,
Z, and room-scroll behavior.

## Tasks

### 1. Add the ROM fire display definition and selection plumbing

- Write a failing catalog test for bank `$03:$4C44` and the two expected pair
  variants.
- Add `EntitySpriteHandlerCatalog.forBurningEntity()` using the existing ROM
  pair decoder.
- Add an optional burning overlay definition to `EntitySpriteSelection`,
  preserving it through `withSpriteOverrides` and existing constructors.
- Make `EntitySpriteCatalog.load` install the real ROM definition, including
  the Color Dungeon selection path.

### 2. Carry the source frame counter into rendering

- Write a failing scene/render test that can select both fire phases.
- Add an unsigned frame-counter field and `withFrameCounter` helper to
  `GameFrameState`.
- Supply `Main.frameCounter` and pass it through `GameFrameSceneBuilder` to
  `EntityRenderLayer`, retaining the existing constructor for tests and
  callers that do not need animation.

### 3. Render the burning overlay without changing normal entity art

- Write failing framebuffer assertions for fire tile/palette, body order,
  position, visual Z, entity flip, tile offset, and frame phase.
- Extend `EntityRenderLayer` to draw the normal entity and then the burning
  overlay from the room's selection. Use the room's own selection when
  rendering the previous room during scroll.
- Keep non-burning entities and selections without an overlay byte-for-byte
  unchanged.

### 4. Verify and document the visible increment

- Run focused catalog/render/scene tests, `git diff --check`, and a fresh
  `gradle clean test`.
- Add a roadmap entry stating that burn feedback is visible only when a
  source-backed attack produces `BURNING`; do not claim general status or
  enemy-handler parity.
- Commit the implementation and documentation as a coherent increment.

## Source references

- `LADX-Disassembly/src/code/entities/bank3.asm:912-940`
- `LADX-Disassembly/src/code/home/entities.asm:435-598`
- `LADX-Disassembly/src/code/home/main.asm` frame-counter update path
- `java/src/main/java/linksawakening/entity/EntitySpriteHandlerCatalog.java`
- `java/src/main/java/linksawakening/render/EntityRenderLayer.java`
