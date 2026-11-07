# Ghini family runtime design

## Goal

Port the remaining bank-$04 Ghini family into the Java room-entity path:

- `ENTITY_HIDING_GHINI` (`$10`),
- `ENTITY_GIANT_GHINI` (`$11`), and
- the already-supported ordinary `ENTITY_GHINI` (`$12`).

The slice must preserve the source handler's shared hidden/visible state gate,
ROM display-list bytes, visible flight motion, collision availability, and
Link collision trigger. It must remain a room-entity reconstruction rather
than an emulator or a fabricated generic enemy behavior.

## Source of truth

The implementation follows:

- `LADX-Disassembly/src/code/entities/04_ghini.asm`,
  `GhiniEntityHandler` at bank `$04:$5C0D`,
  `GhiniVisibleHandler` at bank `$04:$5C43`, and
  `RenderGiantGhini` at bank `$04:$5DA6`;
- `GhiniSpriteVariants` at bank `$04:$5BFC`;
- `GiantGhiniSpriteRectVariants` at bank `$04:$5D26`;
- `GhiniTargetXSpeeds`/`GhiniTargetYSpeeds` at bank `$04:$5C04`/`$5C06`;
- `GhiniUpdateFlipAttribute` in `bank4.asm` at `$7F90`;
- the entity hitbox, options, physics, and health tables in
  `src/data/entities/`; and
- `AddedCollisionType` in `bank2.asm`, which is the source bit mapping for
  the `wCollisionType` value consumed by the hidden branch.

## Behavior

### Display data

`EntitySpriteHandlerCatalog` will decode the pair list at bank `$04:$5BFC`
for `$10` and `$12`. `$11` will decode the eight-sprite rectangle list at
bank `$04:$5D26`, preserving all signed offsets, tile bytes, palettes, and
flips. The source pair attributes are `$02`; rectangle entries use `$02` for
unflipped sprites and `$22` for XFLIP sprites. The giant list's four groups are selected as two animation frames and
two source-specific horizontal orientations; the Java renderer must not
apply a second generic flip to the giant list.

### Shared Ghini state

`GhiniMotion` will retain the source's per-slot state and private countdown:

- `$10` and `$11` start hidden (`state != 0`); `$12` starts visible;
- `EntityInitGhini` still performs the source initialization split: `$12`
  receives private state 3 `$01` and Z `$10`, while `$10`/`$11` enter state 1;
- a hidden entity renders with sprite variant `$FF` and does not run the
  visible flight update;
- a hidden entity reveals only when its unsigned X and Y distances from Link
  satisfy the source `$10` proximity windows and the current ROM collision
  type is nonzero;
- the reveal writes state zero and private countdown `$30`, but the entity
  remains visually hidden for that handler tick, matching the source's
  render-before-state-update order;
- while the private countdown is nonzero, position/Z correction continues but
  target refresh and acceleration are skipped, matching the source early
  return; and
- the visible branch uses the existing ROM fixed-point speed, target,
  boundary-turn, Z-correction, and animation cadence. Its X flip follows
  `GhiniUpdateFlipAttribute` (negative X speed is unflipped; nonnegative X
  speed selects X flip, XORed with the base attribute).

The runtime will pass Link's current `wCollisionType` equivalent into the
entity tick. `Link` will reset that byte at the start of each update and set
the source direction bit when a leading-edge terrain probe blocks movement.
This is the smallest live integration that supplies the source signal; it
does not invent a separate entity-to-Link collision system.

### Combat and background gates

The three types will use the source normal/large hitbox selection, health
group `$13`, and `$08` contact damage. Hidden Ghinis are excluded from the
ordinary enemy collision pass until the shared state is visible. The source
`ENTITY_OPT1_NO_GROUND_INTERACTION | ENTITY_OPT1_NO_WALL_COLLISION` flags
will be honored for all three types. Their bank-$04 recoil path will use the
existing shared recoil boundary once a sword hit is accepted.

## Runtime integration

- Extend `EntityRoomLoader` indirectly through the ROM-backed sprite catalog;
  no entity type is hardcoded with guessed art.
- Carry the Link collision-type byte through `Main` and `RoomSession` into
  `RoomEntityRuntime` without changing existing callers that do not provide
  it; their default remains zero.
- Keep ordinary Ghini behavior byte-compatible with its existing tests.
- Do not add bomb placement, bomb explosion effects, giant-Ghini boss logic,
  or unrelated room scripts in this slice.

## Verification

Focused tests will cover:

1. `loadRom()`-backed shipped-ROM pair/rectangle bytes and exact bank/address
   metadata for all three Ghini definitions, including the complete 4x8 giant
   rectangle matrix with raw `$02`/`$22` attributes;
2. hidden startup, no-reveal proximity/collision gates, reveal-frame hidden
   presentation, `$30` countdown early return, visible target/motion update,
   and source X-flip behavior;
3. Link collision-type bit production for blocked leading-edge movement;
4. hidden collision exclusion and giant normal-hitbox combat; and
5. a shipped-ROM room load containing the new entity definitions.

The complete Gradle test suite and `git diff --check` must pass before the
slice is documented or committed. The roadmap will state explicitly that
bomb interaction and the remaining giant/hiding special branches are not
claimed by this slice if any source behavior remains outside this boundary.
