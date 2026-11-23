# Bottle Grotto First Key Implementation Plan

**Goal:** Continue ordered play through Bottle Grotto's opening torch gate and
collect the first Small Key.

**Architecture:** Extend the existing uninterrupted regression; use ROM room
objects/events and the live powder, combat, door, key-drop, and pickup paths.

**Tech Stack:** Java 21, JUnit 5, Gradle, LADX disassembly, ROM-backed tables.

### Task 1: Prove and implement the torch trigger

- [x] Cross north from `$36` to `$31` and assert event `$25`, torches `$34/$35`,
  and the closed east shutter.
- [x] Light both torches through `sprinkleMagicPowder` and live entity ticks.
- [x] Add a focused red regression for trigger `$05` and implement the missing
  `roomTriggerCount == 2` source condition in the common event dispatcher.
- [x] Assert event completion and the opened east shutter.

### Task 2: Collect room `$32`'s dropped key

- [x] Cross east into `$32`, assert event `$81` and the two source Stalfos.
- [x] Defeat both through `resolveEntityCombat` and normal recovery ticks.
- [x] Wait for `DropKeyEffectHandler`, collect entity `$30`, and assert one
  Small Key in map `$01`'s current dungeon flags.

### Task 3: Verify, review, and commit

- [x] Run focused regressions with `--rerun-tasks`.
- [x] Run `gradle -p java clean test` and `git diff --check`.
- [x] Request source-fidelity review and address Critical/Important findings.
- [x] Commit the coherent slice on `feature/entity-runtime`.
