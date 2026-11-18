# Link Motion-Stop Pose Implementation Plan

> Execute this plan in the repository root: `/Users/jamesjohnson/Documents/Personal/LinksAwakening`.

## Objective

Separate the ROM's one-frame interactive-motion freeze from its explicit
`hLinkAnimationState = $6A` fallen pose. This removes the accidental pose from
the new-game Tarin doorway stop and Owl events while preserving the explicit
Armos Knight `$6A` path.

## Source-of-truth checkpoints

- `LADX-Disassembly/src/code/bank2.asm:$42B2-$42C4`: `$02` clears Link
  speed/velocity and returns without writing animation state.
- `LADX-Disassembly/src/code/entities/06_owl_event.asm:$6937-$6939` and
  `$69BD-$69BF`: Owl writes only `$02`.
- `LADX-Disassembly/src/code/entities/05_tarin.asm:$4C79-$4C83`: the
  unshielded Tarin doorway branch moves Link back and opens `Dialog000`, with
  no `$6A` write.
- `LADX-Disassembly/src/code/entities/06_armos_knight.asm:$538E-$5392`:
  Armos writes `$02` and then `$6A`.

## TDD sequence

### 1. Add failing Link behavior tests

Edit `java/src/test/java/linksawakening/entity/LinkTest.java`:

- Change the generic block test so it captures the normal standing animation,
  calls `blockNextRomMotionFrame()`, and expects motion/vertical velocity to
  stop while the captured animation remains unchanged.
- Add a test for the new explicit fallen-pose method that expects animation
  state `$6A` and the same motion lock.

Run the focused Link test before production changes and confirm the generic
test fails because the current method forces `$6A`.

### 2. Add failing runtime request tests

Add coverage to the existing runtime suites:

- `BeachOpeningRuntimeTest`: Owl motion-block requests must not emit a fallen
  pose request.
- `RoomEntityRuntimeTest`: the Armos motion-block frame must emit both its
  existing motion-block request and a separate fallen-pose request.

Run those focused tests before implementation and confirm the Armos request
test fails because no separate pose channel exists.

### 3. Implement the Link semantic split

Edit `java/src/main/java/linksawakening/entity/Link.java`:

- Remove the `$6A` assignment from `blockNextRomMotionFrame()`.
- Add an explicit method, named consistently with the existing ROM-pose
  methods, that sets the `$6A` override and motion lock.
- Leave animation overrides untouched for generic blocks so the last ROM pose
  persists through the blocked update.

### 4. Route only explicit fallen-pose writes

Edit `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`:

- Add `LinkFallenPoseRequest`, its pending list, reset/consume plumbing, and
  emit it alongside the Armos block request only.
- Keep Owl, Tarin, witch, item, sword-dialog, and transition paths on the
  generic block request channel.

Edit `java/src/main/java/linksawakening/world/RoomSession.java` to expose the
new request list.

Edit `java/src/main/java/linksawakening/Main.java` to consume fallen-pose
requests after generic motion blocks but before held-item and sword-spin
presentation requests. This preserves the existing precedence of later ROM
specific presentation writes.

### 5. Run focused tests and adjust existing assertions

Run the Link, Owl, Armos, Tarin, and room-session tests. Update only assertions
whose expected request stream now has the new separate Armos pose request.

### 6. Verify the complete change

Run:

```text
gradle test
git diff --check
```

Inspect the final diff to confirm no ROM data, sword spin behavior, held-item
pose, or unrelated working-tree changes were altered.

## Files expected to change

- `java/src/main/java/linksawakening/entity/Link.java`
- `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- `java/src/main/java/linksawakening/world/RoomSession.java`
- `java/src/main/java/linksawakening/Main.java`
- `java/src/test/java/linksawakening/entity/LinkTest.java`
- `java/src/test/java/linksawakening/world/BeachOpeningRuntimeTest.java`
- `java/src/test/java/linksawakening/world/RoomEntityRuntimeTest.java`
- Potentially related existing tests if request-consumption assertions require
  an explicit update.

## Completion criteria

- Tarin's no-shield doorway stop and Owl stops preserve Link's prior pose.
- Armos still resolves to the ROM's `$6A` fallen pose.
- Sword acquisition remains ROM-table-driven and unaffected.
- Focused tests and the full Gradle suite pass.
