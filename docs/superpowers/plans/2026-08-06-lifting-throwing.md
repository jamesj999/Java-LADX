# ROM Entity Lifting and Throwing Plan

**Date:** 2026-08-06  
**Spec:** `docs/superpowers/specs/2026-08-06-lifting-throwing-design.md`

## Objective

Complete the status-7/status-8 entity path using the disassembly's lift phase,
carry-position, animation, and throw-speed tables, with no guessed shapes or
emulator layer.

## Implementation sequence

### 1. Add failing pure motion tests

Create tests for the lifted phase tables, direction mapping, side-scrolling
offsets, throw velocity windows, fixed-point accumulators, gravity, and wall
response. Run the focused tests red before adding production behavior.

### 2. Implement source-table motion helpers

Add `LiftedEntityMotion` and `ThrownEntityMotion` with checked ROM constants and
immutable update records. Run their focused tests and correct arithmetic against
the assembly before integrating them into the mutable runtime.

### 3. Integrate status 7/8 into the room runtime

Add per-slot phase/countdown/source-direction state, the lifted-state snapshot,
stunned-grabbable lift gate, explicit throw handoff, status dispatch, and room
session forwarding. Preserve legacy overloads and existing entity behavior.

### 4. Integrate Link and the main frame boundary

Add Link carry-state animation selection and item-use gating. Wire exact A/B
Power Bracelet detection, pre-`AnimateEntities` throwing, and post-tick carry
state synchronization in `Main`/`RoomSession`.

### 5. Verify and document

Run focused tests, then `gradle clean test`, `git diff --check`, and status
checks. Update `docs/reconstruction-roadmap.md` with the verified status-7/8
increment and explicitly record deferred entity-specific throw effects.

