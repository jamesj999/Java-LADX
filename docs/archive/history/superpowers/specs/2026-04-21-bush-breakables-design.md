# Bush Breakables Design

**Date:** 2026-04-21

**Goal**

Add overworld sword interaction for the two bush-style breakable objects used by Link's Awakening DX:

- `OBJECT_BUSH` (`0x5C`)
- `OBJECT_BUSH_GROUND_STAIRS` (`0xD3`)

The implementation must follow the disassembly's object-selection and reveal behavior instead of inventing a generic destructible-tile system.

**Relevant ROM Paths**

- `bank0.asm:CheckStaticSwordCollision` computes the sword-hit room-object cell from Link position and facing/spin state.
- `bank14.asm:RevealObjectUnderObject` mutates the room object, updates the GBC backup copy, and chooses the revealed under-object.
- `bank14.asm:label_014_5743` handles the special bush-covered stairs object, revealing either stairs or a pit depending on the room.

**Scope**

This slice covers:

- Overworld sword collision against static breakable bush objects.
- Mutation of `roomObjectsArea` when a bush is cut.
- Synchronizing the mutable GBC overworld backup object state so redraws do not revert the cut bush.
- Rebuilding the affected rendered room tile/attr data after mutation.
- Updating local stairs/warp trigger data when a bush-covered stairs tile reveals stairs.

This slice does not cover:

- Random heart/rupee drops from cut bushes.
- Bush-smash particle/VFX entities.
- Bomb / magic rod / boomerang / Pegasus Boots breakable-object paths.
- Indoor breakable blocks or general removable-obstacle support.

**Behavior**

**1. Sword-hit cell selection**

When the sword is in an active collision window, compute the intersected overworld object cell using the same sword collision offset tables the ROM uses:

- `LinkDirectionToSwordCollisionRangeX` (`bank0.asm:16BA`)
- `LinkDirectionToSwordCollisionRangeY` (`bank0.asm:16BE`)

The hit cell is derived from Link's pixel position plus the direction-specific offset, converted to the active `10x8` object grid within the padded `roomObjectsArea`.

For this first slice, use Link's facing direction and active sword state. Spin-attack-specific eight-direction handling is out of scope until spin attack is implemented.

**2. Supported breakable objects**

Only two overworld object ids should react:

- `0x5C` plain bush
- `0xD3` bush covering a special reveal

All other object ids are ignored by this interaction layer.

**3. Plain bush reveal**

Cutting `0x5C` should mirror the overworld branch in `RevealObjectUnderObject`:

- Most rooms reveal `OBJECT_SHORT_GRASS` (`0x04`).
- Specific room groups reveal `OBJECT_DIRT` (`0x03`) instead.
- The existing room object must be replaced in `roomObjectsArea`.
- On GBC overworld, the mutated object must also be copied into the overlay-backed mutable object buffer equivalent so later redraw paths preserve the revealed state.

The room exceptions must be encoded from the disassembly logic rather than guessed from screenshots.

**4. Bush-covered stairs reveal**

Cutting `0xD3` should mirror `bank14.asm:5743+`:

- In rooms `0x75`, `0x07`, `0xAA`, and `0x4A`, reveal `OBJECT_GROUND_STAIRS` (`0xC6`).
- In all other overworld rooms, reveal object `0xE8` (pit).
- If stairs are revealed, update local stairs/warp trigger state so the stairs can be used immediately.

**5. Rendering synchronization**

After mutation, refresh room rendering from the authoritative room object state:

- update the 2x2 room tile ids
- update the 2x2 room tile attrs
- keep GBC overlay-backed lookup behavior intact

The refresh should remain object-table driven. Do not hardcode replacement tile ids directly into the gameplay interaction path.

**6. Architecture**

Introduce a focused overworld interaction helper instead of embedding room mutation into `Sword` or `Link`:

- `Sword` remains animation/state only.
- `Link` remains movement/collision/animation only.
- A new static-object interaction class computes the sword-hit cell and applies the bush reveal rules against the mutable room state.

This keeps the current codebase aligned with the ROM split between combat animation and map-object mutation.

**7. Tests**

Add focused automated tests for:

- plain bush reveals short grass in a normal room
- plain bush reveals dirt in a room covered by the ROM exceptions
- bush-covered stairs reveals stairs in a stairs room
- bush-covered stairs reveals pit in a non-stairs room
- GBC mutable backup object state stays synchronized with the revealed object
- room tile/attr output changes after the reveal instead of remaining stale

**Execution Note**

The normal brainstorming workflow would stop for spec review here. The user explicitly requested non-interactive continuation, so implementation proceeds immediately from this written spec.
