# ROM-backed sword palettes

## Goal

Make the Java sword renderer use the exact Game Boy Color object palettes that
the disassembly selects for the normal and fully charged sword. The live game
must read the palette bytes from the shipped ROM; no hand-authored color table
may determine the live sword colors.

## Source of truth

`src/code/palettes.asm:ObjectPalettes` is at bank `$21`, address `$5518` and
contains six four-color RGB555 rows. The disassembly's object-palette index is
the authoritative selection:

- row `3` (`BlueTunicPalette`) is the normal sword palette;
- row `4` is the red/orange charging-sword palette selected by the GBC OAM
  palette-4 path.

Color index zero remains transparent in the host renderer, just as it is for
the existing sword draw path. Tile selection, OAM coordinates, flips,
animation state transitions, and alpha behavior are outside this slice and
must not change. The charged palette phase itself must remain tied to the
global `hFrameCounter` sampled by the disassembly, rather than to a
sword-local holding counter.

## Design

Add an immutable `SwordPalette` value object that loads the existing decoded
object-palette table and exposes defensive copies of its normal and charged
rows. `Sword` receives this value through a constructor used by the live
startup path. Existing constructors without a ROM palette continue to use an
explicit compatibility palette so unit fixtures remain usable without
silently changing the live ROM-driven path.

`Main` constructs the ROM palette beside the ROM-backed sword sprite sheet and
passes it to `Sword`. The equipment controller forwards the live global frame
counter to equipped items, and `Sword` uses bit 2 of that counter when its
existing max-charge condition is true. `Sword.render` chooses the charged row
only on that source phase; otherwise it chooses the normal row. The old
approximate arrays are removed from the live implementation.

## Verification

Tests will verify:

1. a synthetic ROM decodes rows 3 and 4 from the exact bank/address and maps
   them to normal/charged sword colors without exposing mutable storage;
2. the shipped-ROM sword render uses the decoded normal row at visible blade
   pixels;
3. the charged render uses row 4 on global frame phase 4 and the normal row on
   phase 0 while preserving the same tile geometry and transparency mask;
4. the equipment controller forwards the global frame counter;
5. the complete Gradle Java suite still passes.

## Non-goals

This slice does not implement the broader GBC palette RAM/VRAM upload system,
Color Dungeon room palette transitions, or palette effects for other equipped
items and transient effects. Those remain separate ROM-backed work.
