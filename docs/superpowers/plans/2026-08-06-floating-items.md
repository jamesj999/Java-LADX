# Floating item entity runtime

## Goal

Port the ordinary gameplay path for `ENTITY_FLOATING_ITEM` (`$86`) and
`ENTITY_FLOATING_ITEM_2` (`$E5`) from the LADX disassembly into the Java room
entity model. The increment must make these entities ROM-renderable, animate
their vertical position, gate top-down collection on Link's ROM Z position,
resolve their position-derived item variants, and apply the resource effects
that the handler writes.

## Source of truth

- `LADX-Disassembly/src/code/entities/bank3.asm:49E6` supplies both init
  handlers and the `$13` initial Z write.
- `LADX-Disassembly/src/code/entities/06_floating_item.asm` supplies the
  handler, display lists, Z tables, collision gate, variant dispatch, and
  resource writes.
- `Data_006_7ADD` at bank `$06:$7ADD` is seven single OAM entries. The Java
  mixed pair definition will preserve all seven ROM entries, using a null
  second entry for the single-sprite variants.
- Variant `$05` intentionally reads `Data_006_7AD1 + 2` at `$06:$7AD3`, even
  though that address lies inside the preceding Boo Buddy code. The decoder
  must preserve those four ROM bytes rather than replacing the source quirk.
- `Data_006_7AEB` at bank `$06:$7AEB` is a two-frame, two-sprite rectangle
  overlay selected by `hFrameCounter & $08`.
- Top-down Z values are `{ $0F, $0F, $10, $11, $11, $11, $10, $0F }` and
  side-scroll Z values are `{ $00, $00, $01, $02, $02, $02, $01, $00 }`,
  indexed by `(hFrameCounter >> 3) & $07`.

## Design boundary

1. Add a source-backed floating-item helper for position-to-variant, Z-table,
   variant-to-item, and pickup-effect mapping. Keep the helper pure so the
   byte-level rules can be tested independently of the room loop.
2. Extend the ROM display-list catalog for `$86` and `$E5`, and attach the ROM
   rectangle overlay through immutable room sprite selection metadata. The
   renderer will draw the normal mixed main list and the frame-selected
   rectangle overlay without hardcoding tile or attribute bytes in the
   renderer.
3. Port initialization into `EntityRoomLoader`: choose the variant from the
   initialized entity position and set the handler's initial Z. Preserve the
   existing `$86` toadstool-status check as a documented follow-up because
   the current room-loading API does not yet expose `wHasToadstool`.
4. Advance floating-item Z on every active handler tick, including the first
   rendered active frame, while leaving X/Y unchanged. Use the existing
   side-scroll room flag when choosing the table.
5. Extend collection with the two floating types, the ROM top-down Link-Z
   gate (`hLinkPositionZ >= $0C`), visual-Y collision (`entityY - entityZ`),
   and a pickup event variant so the gameplay layer can distinguish the
   position-derived effect from the physical entity type. Preserve the
   existing cadence, persistence mask, and held-pickup behavior for other
   entities.
6. Add exact floating resource effects to `PlayerState`: ten rupees, ten
   arrows, ten bombs with capacity, powder inventory/count, and the `$18`
   health buffer. Keep existing static pickup behavior unchanged.
7. Leave Color Dungeon `$86`'s `func_036_4F9B` branch, indoor `$E5` room `$1C`
   `wDE00` side effect, `DidKillEnemy` global flags, and wave-sound delivery
   as explicit follow-ups unless the existing event boundary can carry them
   without inventing semantics.

## Implementation and verification tasks

Each production change begins with a failing test and is implemented in the
same small unit as that test.

1. Add helper tests for all four `$86` position parities, both `$E5` X
   parities, both Z tables, all six effect mappings, and the `$0C` gate.
   Run the focused test class and observe the expected RED failure.
2. Implement the helper and rerun the focused test class to GREEN.
3. Add catalog tests against the shipped ROM for the mixed seven-entry main
   list, the `$7AD3` source quirk, and the two-frame `$7AEB` overlay. Add
   selection tests proving the overlay survives `withSpriteOverrides`.
4. Implement catalog/selection/renderer overlay support and run the catalog
   and renderer tests.
5. Add loader/runtime tests for initial variants/Z, frame-indexed Z updates,
   top-down versus side-scroll collection, visual-Y collision, persistence,
   and the returned pickup variant. Run `RoomEntityRuntimeTest` and
   `EntityRoomLoaderTest` before integrating gameplay effects.
6. Add `PlayerStateTest` coverage for every floating effect, implement the
   effect dispatch, and run the focused state/runtime suite.
7. Have a spec reviewer inspect the cumulative diff against the listed ROM
   labels and a quality reviewer inspect API compatibility, tests, and
   renderer layering. Address findings with the same implementer and rerun
   both reviews.
8. Run `gradle -p java compileJava`, the focused Gradle tests, the complete
   `gradle -p java test --no-build-cache`, and `git diff --check`. Record the
   verified slice and explicit deferrals in `docs/reconstruction-roadmap.md`.

