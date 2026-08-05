# ROM-Driven Water Tektite Entity Runtime

## Goal

Add the Water Tektite entity (`$99`) to the Java room-entity runtime by following
the shipped LADX disassembly for its display list, initialization, frame
animation, movement state machine, background-collision reset, and combat
metadata. The increment must improve runtime parity without inventing ROM data
or silently treating ordinary Tektite behavior as equivalent.

## Source of truth

- `LADX-Disassembly/src/code/entities/07_water_tektite.asm`
  - `WaterTektiteSpriteVariants` at bank `$07:$752D` has two pair variants:
    tile `$70` with attributes `$00/$20`, and tile `$72` with attributes
    `$00/$20`.
  - `WaterTektiteEntityHandler` selects `(hFrameCounter >> 4) & 1`, updates
    position with `UpdateEntityPosWithSpeed_07`, applies background interaction,
    resets to state 0 on a collision, and dispatches states 0, 1, and 2.
  - State 0 waits for transition countdown zero, loads `$20`, enters state 1,
    and stores two independent random values `(random & $02) - 1` in private
    state 1 and 2.
  - State 1 enters state 2 when its countdown expires; on countdown values with
    bit 0 clear it adds the private values to X/Y speed.
  - State 2 runs every other frame, converges both speeds toward zero, and when
    X speed reaches zero resets to state 0 with countdown `$10`.
- `LADX-Disassembly/src/code/entities/_handlers.asm`
  - entity `$99` uses `EntityInitNoop`.
- `LADX-Disassembly/src/code/entities/bank7.asm`
  - `UpdateEntityPosWithSpeed_07` and `AddEntitySpeedToPos_07` define the
    signed 4.4 fixed-point position update used by this handler.
  - `ReturnIfNonInteractive_07` and `ApplyRecoilIfNeeded_07` are shared bank-7
    gates. The current Java session handles its existing active/dying gates;
    recoil and full interaction flags are explicitly outside this slice.
- `LADX-Disassembly/src/data/entities/health_groups.asm`
  - entity `$99` selects health group `$00`.
- `LADX-Disassembly/src/data/entities/hitbox_flags.asm`
  - entity `$99` uses the normal collision box and enemy hitbox.
- `LADX-Disassembly/src/data/entities/options1.asm`
  - entity `$99` has `ENTITY_OPT1_NO_GROUND_INTERACTION`.
- `LADX-Disassembly/src/data/entities/physics_flags.asm`
  - entity `$99` uses physics type `2 | ENTITY_PHYSICS_SHADOW`.
- The health-group tables in `src/data/entities/health_groups.asm` resolve
  group `$00` to one health and four contact damage in the existing combat
  model.

## Design

### Dedicated motion state

Create `WaterTektiteMotion` next to the existing per-handler motion classes.
It owns one state, transition countdown, signed X/Y speeds, X/Y fractional
accumulators, private state 1/2, and initialization bit per entity slot.

`initialize(slot)` resets those fields to the ROM defaults. `advance` first
decrements the countdown, then computes the position update using the shared
bank-7 4.4 algorithm for X and Y. The state logic is applied after that update,
matching the handler's order. The class returns a new `RoomEntity` with the
ROM frame variant and updated position, and exposes package-private state and
speed accessors for focused tests. `clear` must reset and invalidate the slot
when the runtime disables an entity.

The background callback is queried with the moved position and current handler
direction inferred from the nonzero speed. A blocked movement is retained as a
collision reset: the entity remains at the pre-move position, both speeds are
cleared, the state is set to 0, and the countdown is `$10`. This is the Java
runtime's explicit representation of the handler's collision-table branch;
world-specific tile snapping remains a separate background-system concern.

### Runtime integration

Add `$99` to `RoomEntityRuntime`, initialize it on the normal INIT pass (no
random byte is consumed because the ROM init handler is `EntityInitNoop`),
advance it on subsequent ACTIVE passes, and clear it in both disable paths.
The entity's `variantFor` fallback must not overwrite the motion-selected
frame-bit-4 variant.

### ROM display and combat catalog

Add a catalog mapping for `$99` to bank `$07`, address `$752D`, pair shape,
two variants, and initial variant 0. The catalog continues to decode bytes
from the shipped ROM; no Java tile or attribute literal is added. Add a test
that checks both the mapping and the shipped `$70/$00/$70/$20/$72/$00/$72/$20`
bytes.

Add `$99` to the shared enemy collision predicate. Use the normal hitbox,
health group `$00` (one health), and its group contact damage (`$04`). Keep
the existing basic sword damage and dying lifecycle behavior.

The `NO_GROUND_INTERACTION` and shadow metadata are documented and validated
against the ROM tables, but broad ground/pit/water/conveyor and recoil effects
are not guessed into `RoomEntityRuntime` in this increment.

## Testing

Use red-green TDD:

1. Add display-catalog tests for the bank/address/shape/count and shipped ROM
   bytes; run the focused test and observe the missing mapping or mismatch.
2. Add runtime tests for noop initialization, frame-bit-4 variants, state-0
   random signed acceleration, state-1 alternating updates, countdown expiry,
   state-2 signed deceleration and `$10` restart timer, fixed-point motion,
   collision reset, and clear lifecycle; run them before implementation.
3. Add combat assertions for normal hitbox behavior, one health, four contact
   damage, and one sword damage before the existing dying transition.
4. Run the focused tests, then a fresh `gradle clean test`, `git diff --check`,
   and a clean-worktree check before committing.

## Explicit non-goals

- No emulator or CPU interpreter.
- No premade PNG runtime assets.
- No reuse of ordinary Tektite's Z-jump or random-direction initializer.
- No guessed recoil, sound effects, pit/water/conveyor interaction, drop
  behavior, or global projectile/damage changes.
