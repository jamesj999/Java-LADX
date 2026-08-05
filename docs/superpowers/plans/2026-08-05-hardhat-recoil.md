# ROM Hard Hat Beetle Recoil Implementation Plan

## Goal

Port the source Hard Hat Beetle sword-recoil response through the existing
ROM-shaped combat and runtime state without disturbing its normal movement or
the already verified Octorok, Moblin, and spike-trap paths.

## Tasks

### 1. Lock the source behavior with a failing runtime test

- Add a Hard Hat sword-hit test asserting the default `$30` recoil vector,
  normal health reduction, `$18` flash, and `$0A` ignore-hits countdown.
- Add a tick assertion proving recoil moves the entity before the normal
  Hard Hat speed update and consumes one countdown step.
- Add a blocking assertion using the existing room background collision hook.

Run the focused test before implementation and confirm it fails because Hard
Hat currently has no recoil state.

### 2. Reuse the existing shared recoil state

- Treat Hard Hat as a shared-recoil entity in the runtime pre-handler phase.
- Configure the `$30` vector for Hard Hat's normal sword-hit branch.
- Restrict `RoamingEnemyMotion.beginRecoil` to the Octorok/Moblin path.
- Keep the source clink-off spike-trap exception free of recoil.

### 3. Verify and document the visible increment

- Run focused tests, `git diff --check`, and a fresh `gradle clean test`.
- Add the verified Hard Hat recoil behavior to the reconstruction roadmap,
  including its explicit scope boundary.
- Commit the implementation and documentation as one coherent increment.

## Source references

- `LADX-Disassembly/src/code/entities/06_hard_hat_beetle.asm:33-88`
- `LADX-Disassembly/src/code/entities/bank6.asm:64F7-6540`
- `LADX-Disassembly/src/code/home/entities.asm:3E8E-3EDE`
- `java/src/main/java/linksawakening/world/EnemyRecoilMotion.java`
- `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
