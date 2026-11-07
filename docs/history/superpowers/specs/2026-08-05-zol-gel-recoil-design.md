# Zol/Gel bank-$06 recoil design

## Context

The live Java room runtime already models Zol (`$1B`) and Gel (`$1C`)
movement, the Zol-to-two-Gel split, and Gel's clinging state. Their shared
combat resolver configures the ROM `$30` sword-recoil vector for the supported
families, but the Zol/Gel types are not admitted to that policy. As a result,
the Java runtime can register a sword hit while leaving these entities in
place during the source ignore-hits window.

The disassembly's `AnimateZolGel` path in
`LADX-Disassembly/src/code/entities/06_zol.asm` calls
`ApplyRecoilIfNeeded_06` at `$7CB7`, after the split block and before the
private countdown, collision, and state-handler dispatch. This is the exact
shared bank-$06 recoil contract to expose in Java.

## Source behavior

- A normal sword collision configures the existing ROM `$30` vector-away
  recoil and the normal `$0A` ignore-hits countdown before the next entity
  tick.
- Every active Zol/Gel tick applies one fixed-point recoil step before
  `ZolGelMotion.advance` runs. The existing Java tick order already provides
  this position in the handler pipeline.
- Zol/Gel use the bank-$06 policy: a blocked recoil step does not call the
  bank-$03 `StopEntityRecoilOnCollision` behavior or clear the recoil state.
- If the existing split path converts a Zol to Gel, its current state reset
  clears transient recoil state just as the source reconfigures the original
  entity before the post-split recoil call. The increment must not introduce
  stale recoil into either Gel slot.
- This increment does not change Zol/Gel movement, split timing, clinging,
  background interaction, recoil smoke, or other damage-state branches.

## Selected approach

Extend `RoomEntityRuntime.usesBank6Recoil` to include `$1B` and `$1C`, reusing
`EnemyRecoilMotion` and the existing combat/tick ordering. Add regression
coverage in `RoomEntityRuntimeTest` for both entity types, the bank-$06
blocked-step policy, and the split reset. No new recoil helper or
Zol-specific duplicate state machine is needed.

This keeps the policy boundary explicit and makes the smallest source-faithful
change: the entity handler already calls the shared routine, and the Java
runtime already invokes its shared equivalent before `ZolGelMotion.advance`.

## Verification

The focused runtime tests must first fail when they admit Zol/Gel to the
expected bank-$06 recoil behavior, then pass after the predicate is extended.
The complete Java suite and `git diff --check` must pass before the roadmap is
updated. The roadmap will record that Zol/Gel shared recoil is covered while
retaining their separately pending background and damage-state gaps.
