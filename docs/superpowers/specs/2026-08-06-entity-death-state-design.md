# ROM Entity Death-State Presentation Design

## Goal

Bring the common non-boss enemy death presentation in the Java renderer into
agreement with the LADX disassembly, using the ROM's death rectangle tables and
status countdown rather than a host-authored animation.

This is one bounded parity increment toward the complete reconstruction. It is
not intended to finish all entity death behavior in one change.

## Source evidence and scope

The authoritative status dispatcher in
`LADX-Disassembly/src/code/home/entities.asm` sends status `$01` to
`EntityDeathHandler`. In `LADX-Disassembly/src/code/entities/bank3.asm`, that
handler:

1. Treats boss entities as active handlers, which remains out of scope here.
2. Reads `wEntitiesPrivateCountdown3Table` and removes the entity when it
   reaches zero, which the Java runtime already models as `dyingCountdown`.
3. Selects `Data_003_5488` for ordinary recoil and `Data_003_54C8` for power
   recoil.
4. For countdown values below `$20`, selects a rectangle-list offset with
   `(countdown << 1) & $30`, renders four OAM entries, and applies the source
   recoil boundary.
5. For countdown values at least `$20`, executes the active handler before
   applying the source recoil boundary.

The same file's `EntityBurningHandler` changes ordinary burning entities to
death countdown `$1F` after its transition expires; the existing Java runtime
already models that transition and the Gibdo-to-evasive-Stalfos exception.

Included in this increment:

- ROM decoding of both death display-list tables.
- The normal four-frame death rectangle presentation.
- The power-recoil rectangle presentation, including the source's eight-sprite
  final rectangle frame.
- Countdown-to-frame selection and removal at the source boundary.
- Snapshot/render integration for standard and Color Dungeon selections.
- Focused ROM-byte, runtime, and framebuffer tests.

Explicitly deferred to later source-backed increments:

- `DidKillEnemy`, room persistence, random drops, guardian acorn/piece of
  power rewards, and drop entity spawning.
- Boss active-handler behavior while status `$01` is set.
- Entity-specific death handlers that replace the common rectangle path.
- A CPU/emulator implementation.

## Architecture

### ROM display-list decoding

Extend `EntitySpriteHandlerCatalog` with two named definitions:

- `forDeathEntity()` reads bank `$03`, address `$5488`, four variants of four
  `[signed Y offset, signed X offset, tile, attributes]` entries.
- `forPowerRecoilDeathEntity()` reads bank `$03`, address `$54C8`, preserving
  the first three four-entry variants and exposing the source's final eight
  entry frame by concatenating the fourth and fifth ROM groups.

The decoder continues to use the existing `EntitySpriteDefinition.Shape.RECTANGLE`
representation. Hidden `$FF` tile entries remain in the decoded definition so
the renderer can apply the same hidden-OAM rule already used for other ROM
rectangle lists. The definitions carry the bank/address metadata used by the
existing catalog tests; no Java literal art is introduced.

### Selection and rendering

Extend `EntitySpriteSelection` with normal and power-recoil death definitions,
plus immutable `with...` builders. `EntitySpriteCatalog.load` attaches these
definitions for the standard and Color Dungeon selections.

`EntityRenderLayer` chooses a death definition whenever an entity has status
`DYING`. Its rectangle variant comes from the `RoomEntity.spriteVariant` value
maintained by the runtime. It chooses the power-recoil definition from a
per-entity death presentation flag exposed by the snapshot entity, while all
other statuses retain their existing body and burning-overlay behavior.

The render path preserves the current OAM coordinate convention:

```
screenX = entity.x + roomOffsetX - $08 + signedXOffset
screenY = entity.y + roomOffsetY - $10 - entity.z + signedYOffset
```

The entity flip attribute is XORed with each ROM attribute byte exactly as it
is for existing rectangle handlers. Tile offsets and hidden `$FF` entries use
the existing `withTileOffset` and hidden-OAM handling.

### Runtime state and frame boundary

Add a small per-slot death presentation state to `RoomEntityRuntime`:

- whether the lethal hit used power recoil;
- the current source countdown (`$40` for a normal lethal hit, `$1F` after
  burning); and
- the derived rectangle variant for the current snapshot.

On entry to `DYING`, initialize the state before publishing the next entity
snapshot. On each death tick, retain the entity and update the display variant
according to the source table selection while the countdown is nonzero. The
runtime removes the entity at the same zero-countdown boundary it currently
uses, without yet emitting drops or persistence mutations. The existing
countdown decrement remains at the frame boundary used by the current room
session, and tests will lock down the first and last visible frames to avoid an
off-by-one animation shift.

The `$20` active-handler phase will remain a distinct runtime branch. For the
supported common enemies it will preserve their existing position/state and
recoil plumbing; it will not invent a generic active handler for unsupported
entities. This keeps the visual slice faithful without silently claiming boss
or entity-specific death parity.

## Error handling and compatibility

- ROM reads use the existing checked bank/address validation. A malformed or
  truncated ROM fails during catalog construction rather than silently
  substituting art.
- A selection without death definitions falls back to the current body
  rendering and countdown behavior, preserving synthetic/unit-test snapshots
  that do not load a full sprite catalog.
- Unsupported entities and boss-marked entities are not given fabricated death
  rectangles.
- Existing burning, Color Shell, Pairodd, tile-offset, palette, flip, and room
  transition behavior remains unchanged outside the new `DYING` branch.

## Testing and verification

1. `EntitySpriteHandlerCatalogTest` will assert the ROM bank/address, signed
   offsets, attributes, hidden entries, and the eight-entry power frame.
2. Runtime tests will assert lethal normal hits initialize `$40`, power hits
   select the power table, burning initializes `$1F`, variant transitions occur
   at the source countdown boundaries, and zero removes the slot.
3. `EntityRenderLayerTest` will render a synthetic death snapshot and verify
   the expected tile/palette pixels, signed offsets, XOR flips, and hidden
   entries for both normal and power frames.
4. The complete Java suite will run with `./gradlew clean test`, followed by
   `git diff --check` and a clean worktree check before handoff.

