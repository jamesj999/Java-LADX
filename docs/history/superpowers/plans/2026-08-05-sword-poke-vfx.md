# ROM Sword-Poke VFX Implementation Plan

## Goal

Render the source sword-poke transient effect and route it from the modeled
spike-trap sword-clink collision without changing ordinary enemy combat.

## Tasks

### 1. Specify the ROM transient effect

- Add a failing test for transient type `$05` and countdown `$0F`.
- Add a failing renderer test for both `Data_002_57DD` phases, raw tile
  attributes, vertical offsets, and OAM coordinate conversion.
- Add the `SWORD_POKE` type and render-layer dispatch.

### 2. Extend the combat event and consumer boundary

- Add a failing event/consumer test for an optional sword-poke request.
- Add jingle `$07` to the gameplay sound map.
- Spawn the request through an overload that preserves the existing consumer
  signature for callers without a VFX system.

### 3. Model the source spike-trap branch

- Add a failing runtime test for type `$27` proving no enemy damage, jingle
  `$07`, ignore-hits `$10`, cleared recoil, and exact VFX coordinates.
- Implement a source-backed `SWORD_CLINK_OFF` predicate for the spike trap.
- Keep the existing normal sword-hit branch unchanged for other supported
  enemy types and retain simultaneous Link contact damage.

### 4. Verify and document the visible increment

- Run focused tests, `git diff --check`, and a fresh `gradle clean test`.
- Add a roadmap entry describing the source-backed spike-trap sword-poke
  response and its explicit scope.
- Commit the implementation and documentation as a coherent increment.

## Source references

- `LADX-Disassembly/src/code/bank0.asm:919-940`
- `LADX-Disassembly/src/code/bank2.asm:5567-5590`
- `LADX-Disassembly/src/code/bank2.asm:~5780-5855`
- `LADX-Disassembly/src/code/entities/bank3.asm:5937-5990`
- `LADX-Disassembly/src/data/entities/options1.asm:43`
- `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
