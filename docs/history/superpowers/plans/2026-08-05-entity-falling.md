# Entity Pit Falling Implementation Plan

> **For agentic workers:** Use the source-first and test-driven workflow for
> each task. Keep the deferred wall-collision and full active-handler branches
> explicit until their ROM inputs are ported.

**Goal:** Port the bank-$03 pit-entry and `EntityFallHandler` state machine.

**Architecture:** `RoomEntityGroundInteraction.Result` gains a pit-transition
payload. `RoomSession` supplies raw pit samples and exception policy;
`RoomEntityRuntime` owns falling targets, countdown/status writes, ROM vector
movement, events, and lifecycle cleanup.

## Task 1: Record the reviewed source design

- [x] Add the falling design and implementation plan.
- [x] Verify the docs with `git diff --check`.

## Task 2: Extend the callback contract and add failing tests

- [x] Add a source-shaped pit transition record to the ground callback result.
- [x] Add runtime tests for status, target, countdown, flash/ignore state, and
  event timing.
- [x] Add session tests for pit object/physics detection and BowWow/Rooster/
  Heart Container/Marin exceptions.
- [x] Run focused tests and confirm they fail before implementation.

## Task 3: Implement ROM pit entry and falling motion

- [x] Add per-slot falling target X/Y state and lifecycle reset.
- [x] Implement the ROM countdown phase, vector division/sign handling, and
  fixed-point position update.
- [x] Dispatch `FALLING` before ordinary active handlers and unload at zero.

## Task 4: Wire the falling event and renderer-facing state

- [x] Emit raw jingle `$18` exactly at the source countdown boundary.
- [x] Preserve the ROM phase/variant result for supported displays and keep the
  separate visual-Y correction explicitly deferred.
- [x] Add/verify gameplay sound catalog coverage for `JINGLE_ITEM_FALLING`.

## Task 5: Verify and record the slice

- [ ] Run focused tests, `gradle clean test`, and `git diff --check`.
- [x] Update `docs/reconstruction-roadmap.md` with verified behavior and
  remaining wall/full-handler gaps.
- [ ] Commit the isolated worktree slice.
