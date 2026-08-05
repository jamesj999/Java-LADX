# ROM-driven Link tunic palette design

## Context

`Link.render` currently uses a hardcoded four-color green palette. The
runtime state already tracks the disassembly's `wTunicType`, and the ROM keeps
the exact object palettes at `ObjectPalettes` (`bank $21:$5518`): green is
palette 0, red is palette 2 (`RedTunicPalette`), and blue is palette 3
(`BlueTunicPalette`). The current implementation therefore renders the green
tunic correctly by accident but cannot render the other two tunics or prove
that the runtime palette matches the shipped ROM.

The source of truth is `src/code/palettes.asm:1477-1487`,
`src/code/bank20.asm:5094-5114`, and `PlayerState`'s `wTunicType` values.

## Goals

1. Load Link's tunic palettes from the ROM at startup.
2. Select the ROM palette from the live `PlayerState.tunicType()` on every
   render, so a tunic change is visible without reconstructing Link.
3. Preserve the existing render API and test-only constructors that do not
   have a ROM palette source.
4. Add a framebuffer regression proving that a red or blue tunic changes only
   the ROM-selected body colors while preserving the decoded Link tiles and
   geometry.

## Design

### Palette source

Add `LinkTunicPalette`, an immutable focused wrapper around the six ROM object
palettes decoded by `EntitySpriteCatalog`. `loadFromRom` reads the existing
`ObjectPalettes` path; it does not duplicate RGB555 decoding or embed runtime
colors. It maps tunic values exactly as the source:

```text
wTunicType  0 (green) -> ObjectPalettes[0]
wTunicType  1 (red)   -> ObjectPalettes[2]
wTunicType  2 (blue)  -> ObjectPalettes[3]
```

The wrapper returns defensive copies because rendering code must not mutate
the ROM-decoded table. A green fallback remains only for existing isolated
fixtures that construct Link without a ROM-backed palette; the live `Main`
construction always supplies `LinkTunicPalette.loadFromRom(romData)`.

### Link integration

Add a constructor overload accepting `LinkTunicPalette`. Existing constructors
delegate to a green compatibility palette. `Link.render` asks the wrapper for
the palette selected by `playerState.tunicType()` and passes it to the existing
tile draw loop. Transparent color index 0 remains skipped exactly as before;
the other three indices are now the ROM's palette values.

This slice does not alter animation-state selection, tile decoding, sword
rendering, tunic acquisition, or Color Dungeon palette-effect state. Those
systems can consume the same palette boundary in later slices.

## Testing

- `LinkTunicPaletteTest` writes synthetic RGB555 values at the source address
  and verifies all three tunic mappings and defensive-copy behavior.
- A Link framebuffer test uses the shipped ROM's Link tiles, renders the same
  frame with green and red state, and verifies that at least one tunic pixel
  changes to the exact ROM palette-2 color while its location remains stable.
- The existing Link, sprite-sheet, and complete Java suites remain required.
