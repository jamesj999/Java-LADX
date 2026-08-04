# ROM-Driven Room Entity Runtime — Implementation Plan

## Goal

Load room entities and their ROM-backed sprite sheets into the Java gameplay path, render the first supported pair/single display-list families pixel-faithfully, and leave unsupported handler types explicit rather than fabricated.

## Architecture

- `linksawakening.world.EntityRoomLoader` reads bank-$16 room entity pointers and returns sixteen slot records.
- `linksawakening.world.RoomEntity` and `RoomEntitySnapshot` carry slot state, source order, type, position, and render definition; `EntitySpriteSelection` carries the four selected sheet bytes and object palettes.
- `linksawakening.entity.EntitySpriteCatalog` selects room sprite groups, decodes `bbtttttt` sheet values, and loads the six resident ROM object palettes plus Eagle's Tower's conditional seventh palette.
- `linksawakening.entity.EntitySpriteDefinition` and `EntitySpriteHandlerCatalog` decode the small set of disassembly display lists supported by this increment; unsupported types remain marked unsupported.
- `GPU.loadEntitySpriteSheets` copies the four `$100`-byte sheet rows to VRAM tiles `$40`, `$50`, `$60`, and `$70` from adjusted GBC banks.
- `EntityRenderLayer` reproduces pair and single OAM placement using `IndexedRenderer` and the interpolated room-scroll offset.
- `LoadedRoom`, `ActiveRoom`, `RoomRenderSnapshot`, `RoomSession`, `GameFrameState`, and `GameFrameSceneBuilder` carry and draw the entity snapshot.

## Tech stack

Java 21, Gradle, JUnit 5, the existing direct indexed framebuffer renderer, and the shipped `/rom/azle.gbc` test resource. No emulator layer and no PNG runtime assets.

## Execution rules

Run all Gradle commands from `java/`. Use `apply_patch` for source edits. Each implementation unit follows red → green → refactor: add the focused failing test, run it and confirm the failure is for the missing contract, implement the smallest production change, rerun the focused test, then run the relevant package tests.

## Steps

### 1. Establish the entity model and parser contract

1. Add `java/src/test/java/linksawakening/world/EntityRoomLoaderTest.java` with synthetic ROM fixtures that place pointer tables at bank-$16 addresses `$4000`, `$4200`, `$4400`, and `$4600`. Assert that a definition byte `$67` becomes `x=0x78`, `y=0x70`, that types are unsigned, and that the source load order is retained.
2. Add tests for `$FF` stream termination, an empty room, the first eight cleared-room masks, skipped definitions still advancing load order, and seventeen definitions filling only sixteen slots.
3. Run the focused test before adding production classes:

   ```text
   cd java
   gradle test --tests linksawakening.world.EntityRoomLoaderTest
   ```

   Confirm the expected compilation failure names the absent loader/model, not a Gradle or ROM-resource problem.
4. Implement `EntityStatus` constants, `RoomEntity`, `RoomEntitySnapshot`, and `EntityRoomLoader`. Validate room IDs, pointer addresses, stream bounds, and termination. Use `RomBank.romOffset(0x16, address)` for every bank-$16 read.
5. Rerun the focused test, then `gradle test --tests linksawakening.world.RoomObjectParserTest --tests linksawakening.world.EntityRoomLoaderTest`.

### 2. Add pointer-table selection coverage against the shipped ROM

1. Extend `EntityRoomLoaderTest` with the real ROM resource and assert the known overworld streams:

   - room `$00`: Mini Moldorm at location `$67`, Heart Piece at `$24`, Crow at `$26`;
   - room `$92`: Kid `$73`, Marin `$3E`, Dog `$6F`, and three Butterfly `$6E` definitions in source order.

2. Assert indoor-A, indoor-B, and Color Dungeon selection against the pointer addresses used by `LoadRoomEntities`; do not copy any generated entity data into Java.
3. Run:

   ```text
   gradle test --tests linksawakening.world.EntityRoomLoaderTest
   ```

### 3. Implement the ROM-backed room sprite catalog

1. Add `java/src/test/java/linksawakening/entity/EntitySpriteCatalogTest.java` with synthetic bank-$20 bytes for the `$70D3`, `$71D3`, `$72D3`, `$73F3`, and `$763B` tables. Assert overworld/indoors-A/indoors-B selection, four-byte group indexing, `$FF` keep-current entries, and the exact bank selector order `[0x00, 0x11, 0x0E, 0x12]` before GBC adjustment.
2. Add a real-ROM assertion for overworld room `$92`: group lookup comes from `RoomSpritesheetGroupsTable` and the four returned sheet bytes match `OverworldEntitySpritesheetsTable[group * 4 .. group * 4 + 3]`.
3. Run the focused test and confirm it fails only because the catalog is absent.
4. Implement `EntitySpriteCatalog` with explicit table bounds checks and `RoomEntitySnapshot` sheet metadata. Keep Color Dungeon on an explicit no-standard-sheets path until its special loader is ported.
5. Add an object-palette test that decodes bank-$21 address `$5518` into six four-color RGB palettes, verifies the first palette's known ROM colors, and covers Eagle's Tower's conditional palette at `$5548`.
6. Run:

   ```text
   gradle test --tests linksawakening.entity.EntitySpriteCatalogTest
   ```

### 4. Load entity sheets into the existing GPU VRAM model

1. Add `java/src/test/java/linksawakening/gpu/GPUEntityTilesTest.java`. Build synthetic ROM bytes at the four adjusted source banks using sheet bytes with distinct `bb` and `tttttt` values. Assert the first and last bytes at destination tile slots `$40`, `$50`, `$60`, and `$70`, and assert `$FF` leaves the destination unchanged.
2. Add a real-ROM check for one selected sheet: source offset is `RomBank.romOffset(adjustedBank, 0x4000 + (sheet & 0x3F) * 0x100)` and destination is `0x40 + slot * 0x10`.
3. Run the focused test and verify the expected missing-method failure.
4. Implement `GPU.loadEntitySpriteSheets(byte[] romData, int[] sheetValues)` using the disassembly's `NpcTilesBankTable`, `bank | 0x20` adjustment for nonzero GBC banks, `$100` source bytes, and sixteen-tile destinations. Reuse the existing tile decode/update path.
5. Run:

   ```text
   gradle test --tests linksawakening.gpu.GPUEntityTilesTest --tests linksawakening.entity.EntitySpriteCatalogTest
   ```

### 5. Decode the first handler display-list families

1. Add `java/src/test/java/linksawakening/entity/EntitySpriteHandlerCatalogTest.java` with synthetic bytes for a four-variant pair list and a two-variant single list. Assert list stride, unsigned tile/attribute bytes, and bounds failures.
2. Add real-ROM assertions for disassembly-backed descriptors:

   - Crow: bank `$06`, address `$5C89`, four pair variants, initial variant `2`;
   - Dog: bank `$19`, address `$48CA`, four pair variants, initial variant `2`;
   - Marin outdoors: bank `$05`, address `$4E2A`, eleven pair variants, initial variant `6`;
   - Marin indoors and Marin-at-Tal-Tal variants using the handler-selected addresses;
   - Kid 70/73: bank `$06`, address `$604D`, four pair variants;
   - Butterfly: bank `$06`, address `$6BBD`, two single-sprite variants.

3. Run the focused test before adding the catalog and confirm the expected absent-class failure.
4. Implement `EntitySpriteDefinition` and `EntitySpriteHandlerCatalog`. Keep all tile/attribute bytes ROM-decoded; only the small mapping from entity type to the disassembly handler's display-list bank/address/shape is Java metadata.
5. Have `EntityRoomLoader` attach the decoded definition and initial variant to each loaded slot, using indoor/outdoor context for Marin.
6. Run:

   ```text
   gradle test --tests linksawakening.entity.EntitySpriteHandlerCatalogTest --tests linksawakening.world.EntityRoomLoaderTest
   ```

### 6. Add renderer-level pixel tests

1. Add `java/src/test/java/linksawakening/render/EntityRenderLayerTest.java`. Seed a `GPU` tile range with distinct 8x8 patterns and a synthetic entity snapshot. Render into a 160x144 buffer and assert:

   - pair columns use X and X+8;
   - 8x16 tile order is top then bottom;
   - color zero is transparent;
   - low attribute bits select the expected object palette;
   - XORed entity flip attributes swap columns and flip pixels;
   - off-screen coordinates clip without writing outside the framebuffer.

2. Add a scroll assertion using the same offsets as `RoomRenderLayer` for a rightward transition.
3. Run the test before creating the layer and confirm the expected missing-class failure.
4. Implement `EntityRenderLayer` and, if needed, the smallest `IndexedRenderer` overload needed for a single 8x16 sprite without changing existing background semantics.
5. Run:

   ```text
   gradle test --tests linksawakening.render.EntityRenderLayerTest --tests linksawakening.render.IndexedRendererTest
   ```

### 7. Integrate entity snapshots into room loading and scene construction

1. Extend `LoadedRoom`, `ActiveRoom`, and `RoomRenderSnapshot` with the immutable entity snapshot, preserving the existing three-argument `RoomRenderSnapshot` constructor for current tests.
2. Update `RoomLoader` to call `EntityRoomLoader` for overworld and indoor loads. Keep the existing public overloads defaulting the cleared-room byte to zero, and add package-level/testable overloads accepting a cleared-room byte.
3. Update `RoomSession.setActiveRoom` to load the selected four entity sheets into GPU VRAM after room-specific BG loads. Ensure every initial load, warp, and adjacent scroll replaces the entity snapshot.
4. Add an `EntityRenderLayer` in `GameFrameSceneBuilder` after Link and before transient effects/rupees/inventory. Do not add it to title or cutscene scenes.
5. Extend `GameFrameSceneBuilderTest` and `RoomSessionTest` to assert entity layer inclusion and room replacement while preserving existing layer counts for empty/title states.
6. Run:

   ```text
   gradle test --tests linksawakening.world.RoomLoaderTest --tests linksawakening.world.RoomSessionTest --tests linksawakening.render.GameFrameSceneBuilderTest --tests linksawakening.render.EntityRenderLayerTest
   ```

### 8. Verify the integrated gameplay slice and record remaining gaps

1. Run the complete Java test suite:

   ```text
   gradle test
   ```

2. Run `git diff --check` and inspect the changed file list. Confirm no PNG files, generated entity literals, emulator classes, or unrelated changes were added.
3. Use the real-ROM room `$92` snapshot to sanity-check that the expected entity types, selected sheet bytes, and visible supported sprites are present while unsupported event/entity handlers remain explicitly non-rendering.
4. Update the implementation roadmap with the next handler families and the missing state/AI systems. Do not describe this slice as complete game parity; report the verified entity foundation and the remaining disassembly work.

## Completion criteria

- All focused and full Gradle tests pass from `java/`.
- Room entity definitions and sheet bytes come from the ROM at the addresses documented above.
- VRAM destination and signed/8x16 sprite addressing are covered by byte-level and pixel-level tests.
- Room transitions replace entity data and sheets without affecting title/cutscene rendering.
- Unsupported entity types are visible in state as unsupported and are not drawn with guessed art.
