# Bomb core runtime design

## Goal

Port the ordinary player bomb's placement and entity lifecycle into the
ROM-backed Java room runtime. The implementation must keep the original
entity type and slot, use the shipped ROM's display data, and preserve the
source countdown boundaries for the fuse, pre-explosion warning, explosion,
and unload phases.

This is a bounded runtime increment toward complete LADX behavior. It makes
the bomb usable and visually/temporally faithful in the existing Java engine;
it does not claim that every bomb-dependent room script is complete.

## Source of truth

The implementation follows:

- `LADX-Disassembly/src/code/bank0.asm`, `PlaceBomb` at bank `$00:$135A`,
  `SpawnPlayerProjectile` at `$00:$142F`, and the base item-use path;
- `LADX-Disassembly/src/code/bank20.asm`,
  `ConvertToBombArrowIfNeeded` at bank `$20:$4B81`, including the ordinary
  bomb countdown `$A0` and private countdown `$10`;
- `LADX-Disassembly/src/code/entities/03_bomb.asm`, including
  `BombEntityHandler`, `BombExplosionHandler`, `BombExplosionVisuals`,
  `RenderBomb`, `ExplosionSpriteVariantFrames`, and the wall-bounce path;
- the bank-$03 display data `BombSprite`, `BombRightBeforeExplodingSprite`,
  and `ExplosionSpriteRect`;
- the entity physics, options, hitbox, and damage tables in
  `src/data/entities/`; and
- the existing Java `RoomEntityRuntime`, `RoomSession`, item registry,
  sound-event map, ROM bank helpers, and rectangle OAM renderer.

The fuse value is not inferred from the visual timing: the source's ordinary
placement path writes `wEntitiesTransitionCountdown[newBomb] = $A0` in
`ConvertToBombArrowIfNeeded`. The Java implementation will preserve that
provenance and decrement the same transition-countdown value once per entity
tick.

## Behavior

### Placement

The Bomb equipment handler will use the existing A/B edge-dispatch path.
Placement will:

- reject the press when the source one-bomb guard (`wHasPlacedBomb`) is set;
- play the existing wrong-answer sound when the bomb count is zero;
- decrement the BCD bomb count before spawning, matching `PlaceBomb`'s order;
- spawn entity type `$02` at Link's source position, Z, direction, and the
  source player-bomb private state; and
- initialize the ROM-backed countdowns and the player's item attack state
  through the existing runtime bridge.

The item will remain usable only when the Java Link/item state permits the
  source item-use path. Placement will not invent a separate movement lock or
  a second bomb inventory; it will reuse the current `PlayerState` and
  `EquipmentController` contracts.

### One entity, three visual phases

The original keeps a bomb, its warning animation, and its explosion in the
same entity slot. Java will do the same. A dedicated `BombMotion` state helper
will select the state for the current transition countdown, while the room
entity snapshot keeps the slot/type and the renderer consumes the selected
ROM display definition.

The source boundaries are:

- countdown `$A0` down through `$23`: normal bomb presentation and bouncing
  physics;
- countdown `$22` down through `$18`: the small two-sprite
  `BombRightBeforeExplodingSprite` warning presentation is rendered in the
  same handler;
- countdown `$18`: decrement and play `NOISE_SFX_EXPLOSION` (`$0C`), matching
  `BombEntityHandler`'s transition into the explosion handler;
- countdown `$17` down through `$00`: the explosion rectangle is selected by
  the exact ROM `ExplosionSpriteVariantFrames` table; and
- countdown `$00`: unload the entity.

The explosion handler's source interaction window, countdown `$16` through
`$0E`, will be represented as an explicit runtime event/phase boundary. The
initial increment will wire the event to the existing entity combat/audio
path only where the current Java model can apply the source result without
fabricating room state. Unsupported destroyable-object mutations remain
tracked as a follow-up rather than being silently treated as ordinary enemy
damage.

### ROM display data

`EntitySpriteHandlerCatalog` will decode, by bank/address, the source's:

- one-entry `BombSprite` list;
- two-entry pre-explosion pair; and
- four-variant, eight-entry explosion rectangle.

Signed offsets, raw tile bytes, palette bits, and flip bits remain in the
decoded OAM definitions. Java will not paste PNG-derived coordinates or apply
an additional generic flip to the explosion rectangle.

### Thrown and lifted bombs

The source bomb handler still participates while a bomb is being lifted or
thrown. The Java runtime will keep the bomb fuse alive across those statuses,
use the existing ROM-derived `ThrownEntityMotion` tables for the throw, and
restore the ordinary bomb countdown `$A0` when the source throw path does so.
Bomb wall reversal and the source grabbable/terrain gates will be applied by
the existing background and lift/throw infrastructure. A thrown bomb's
special enemy-bomb conversion and bomb-arrow conversion are outside this
increment unless the source path is already represented by the current item
runtime.

### Audio and palette effects

The explosion sound will use the existing noise catalog entry `$0C`, exposed
through the Java gameplay sound event path. `$0B` remains the hookshot noise
and must not be reused for bombs. Indoor DMG palette flashing is a
separate presentation subsystem; it will only be wired here if the existing
palette API can express the source flash without bypassing the renderer. The
ROM explosion geometry and timing are required regardless.

## Runtime integration

- Add a `Bomb` equipment handler and register it for inventory item `$02`.
- Add a narrow placement method on `RoomSession` that delegates to
  `RoomEntityRuntime` and refreshes the immutable room snapshot.
- Add a per-slot bomb motion/state value to `RoomEntityRuntime`, or an
  equivalent helper-owned state keyed by slot, rather than creating a
  transient explosion entity.
- Extend the existing sprite-definition selection and renderer contracts only
  as needed for the bomb's single, pair, and rectangle phases.
- Preserve the current frame order: equipment edge dispatch occurs before
  Link movement, then room combat/entity ticks consume the resulting bomb
  state.
- Keep existing callers that do not place bombs source-compatible and retain
  the current disabled-slot behavior after the countdown reaches zero.

## Non-goals for this increment

The following remain separate parity slices and must not be claimed as part of
the core bomb implementation:

- all `CheckForBombDestroyableObjectBasic` and puzzle object mutations,
  including persistent room-object/status/tilemap updates;
- every enemy-specific bomb reaction and the complete
  `CheckExplosionInteractionWithEntities` parity path if the existing combat
  abstractions cannot represent it yet;
- bomb-arrow conversion, enemy bombs, and other entities that reuse the bomb
  handler with a nonzero private state;
- complete indoor DMG palette flash timing when no matching Java palette
  contract exists; and
- unrelated room scripts, warp behavior, or emulator-level Game Boy hardware.

These exclusions are intentional boundaries for a testable increment, not a
claim that the original game treats those paths as optional.

## Invariants

1. An ordinary placed bomb is ROM entity type `$02` and uses one persistent
   room-entity slot from placement through unload.
2. The ordinary fuse starts at `$A0`; countdown `$18` is the explosion sound
   boundary; countdown `$17..$00` selects the explosion frames; and zero
   unloads.
3. The pre-explosion and explosion OAM bytes come from the shipped ROM
   display lists and retain their raw palette/flip bits.
4. A failed free-slot allocation does not leave a second visible bomb or
   stale `wHasPlacedBomb` equivalent.
5. Existing entity, equipment, sound, and renderer tests remain green.

## Verification

Focused tests will cover:

1. shipped-ROM bank/address decoding for all bomb display definitions and the
   exact explosion frame table;
2. inventory count, one-active-bomb guard, wrong-answer sound, and successful
   placement;
3. countdown boundaries, pre-explosion pair, explosion rectangle variants,
   sound emission, and unload;
4. thrown/lifted bomb countdown and ROM-derived bounce behavior; and
5. regression compatibility with existing room-entity and equipment tests.

The complete Gradle suite, `git diff --check`, and a clean worktree check must
pass before this slice is documented as verified. The roadmap will explicitly
list the deferred bombable-object, enemy-bomb, and bomb-arrow work.
