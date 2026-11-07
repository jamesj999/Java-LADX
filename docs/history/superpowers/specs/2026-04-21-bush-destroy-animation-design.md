# Bush Destroy Animation Design

**Date:** 2026-04-21

## Goal

Add the overworld bush destroy animation so cutting a bush with the sword produces the ROM-equivalent cut-leaves visual, while introducing a reusable transient-VFX framework that can support additional short-lived gameplay effects later.

## Source Of Truth

- `CheckStaticSwordCollision` in `LADX-Disassembly/src/code/bank0.asm`
- `LiftableRockStartSmashingAnimation` in `LADX-Disassembly/src/code/entities/03_liftable_rock.asm`
- `func_019_7C50` and `CutLeavesSpriteRect` in `LADX-Disassembly/src/code/entities/bank19.asm`
- `AddTranscientVfx` in `LADX-Disassembly/src/code/bank0.asm`
- `TRANSCIENT_VFX_*` constants in `LADX-Disassembly/src/constants/vfx.asm`

## Behavior

When a cuttable bush is removed by the sword:

- the bush tile mutation logic remains unchanged
- a transient visual effect is spawned at the bush's world position
- the effect renders as the liftable-rock cut-leaves animation, not as a custom particle system
- no sound is added in this slice

The effect should be equivalent to the ROM behavior in the ways that matter visually:

- it uses the cut-leaves sprite rect data rather than guessed offsets
- it is countdown-driven rather than free-running
- it renders as a four-sprite rect sequence
- it expires automatically after its countdown reaches zero

## Architecture

### Transient VFX System

Add a small gameplay-runtime subsystem that mirrors the ROM conceptually:

- fixed-capacity slot array
- each slot stores effect type, remaining countdown, and world position
- update step decrements active effects and clears expired slots
- render step draws active effects in the overworld sprite pass

The Java system does not need to mirror WRAM addresses literally, but its model should stay close enough that future ROM-backed effects fit naturally.

### Effect Types

The first implemented type is `BUSH_LEAVES`.

The system should be structured so additional effect types can be added without changing the public API shape. A narrow enum or integer type ID is sufficient.

### Sprite Data

The bush animation uses the cut-leaves branch of `func_019_7C50`:

- sprite tile: `$28`
- 8 animation frames
- 4 sprite records per frame
- frame selection derived from countdown using the ROM expression:
  - `(countdown & 0x1C) ^ 0x1C`
  - shifted left twice to index 16-byte frame records

The runtime should store or decode the cut-leaves sprite rect data from ROM-backed constants rather than hand-authored Java art data.

### Rendering

Render transient VFX during the overworld sprite phase, after the room BG and alongside Link/item sprites. Effects use world coordinates converted through the same screen-offset path already used for Link during scrolling.

For this slice, effects only need to render in overworld gameplay. Indoor/title/menu usage remains out of scope.

## File Responsibilities

- `java/src/main/java/linksawakening/vfx/TransientVfxSystem.java`
  Manages active transient effect slots, ticking, spawning, and dispatching render.
- `java/src/main/java/linksawakening/vfx/TransientVfxType.java`
  Declares supported transient effect IDs.
- `java/src/main/java/linksawakening/vfx/TransientVfxSpriteSheet.java`
  Loads ROM-backed OAM tiles needed by transient effects.
- `java/src/main/java/linksawakening/vfx/CutLeavesEffectRenderer.java`
  Contains the ROM-equivalent rect/frame logic for bush leaves.
- `java/src/main/java/linksawakening/Main.java`
  Owns the transient VFX system, ticks it, renders it, and spawns bush effects from the bush-cut path.

Tests:

- `java/src/test/java/linksawakening/vfx/TransientVfxSystemTest.java`
- `java/src/test/java/linksawakening/world/OverworldBushInteractionTest.java`

## Testing Strategy

Use TDD.

- lifecycle tests for slot allocation, ticking, expiry, and reuse
- bush integration test proving that cutting a bush enqueues one bush-leaves effect
- render-oriented test(s) proving countdown-to-frame mapping for the cut-leaves branch

## Out Of Scope

- sound effects
- generic ROM-wide support for every transient VFX type
- indoor/menu/title-screen transient VFX rendering
- broader entity-system refactoring
