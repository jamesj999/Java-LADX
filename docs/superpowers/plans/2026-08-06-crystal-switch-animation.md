# Crystal-switch animation implementation plan

> For the implementation agent: follow the project source-of-truth rules and
> complete each test-first checkpoint before moving to production code.

## Objective

Implement the ROM-backed crystal switch (`ENTITY_CRYSTAL_SWITCH = $66`) path:
crystal sword hit -> flash -> stage `$01` request -> exact switch-block
animation stages -> ROM tile copies at `$9040`/`$9080` -> raw WAVE floor-switch
sound event.

## Source facts to preserve

- Crystal handler: `LADX-Disassembly/src/code/entities/15_crystal_switch.asm`.
- Crystal sprite pair: bank `$15`, address `$4320`, tiles `$58/$58`, attrs
  `$03/$23`.
- Crystal hitbox/health/options: `$66` uses the normal collision box and enemy
  hitbox, health group `$22`, initial health `$08`, and is excluded from the
  kill-all path.
- Switch-block update: `src/code/bank0.asm` `UpdateSwitchBlockTiles`.
- Stage table: `src/code/home/interrupts.asm` `BlockUpdateAnimationStageTable`
  maps update kinds A/B to `$07/$09`.
- Switch block source: bank `$0C` on DMG or adjusted bank `$2C` on GBC,
  address `$6800`.
- VRAM destinations: `$9040` and `$9080`, represented by Java tile slots
  `$104` and `$108`.
- `WAVE_SFX_FLOOR_SWITCH` is raw WAVE id `$0E`.

## Files expected to change

- `java/src/main/java/linksawakening/world/SwitchBlockAnimation.java`
- `java/src/main/java/linksawakening/world/RoomEntityRuntime.java`
- `java/src/main/java/linksawakening/world/RoomEntityCombatRules.java`
- `java/src/main/java/linksawakening/world/RoomSession.java`
- `java/src/main/java/linksawakening/gpu/GPU.java`
- `java/src/main/java/linksawakening/Main.java`
- `java/src/main/java/linksawakening/entities/EntitySpriteHandlerCatalog.java`
- `java/src/main/java/linksawakening/audio/GameplaySoundEvent.java`
- `java/src/main/java/linksawakening/audio/GameplaySoundEffectMap.java`
- `java/src/main/java/linksawakening/audio/EnemyCombatEventConsumer.java`

Focused tests should be added beside the existing tests for those classes,
with a new pure `SwitchBlockAnimationTest` if no suitable existing test class
exists.

## Checkpoint 1: write failing tests first

1. Test the pure state machine for stages `$01` through `$09`:
   - stage `$02` increments to `$03` and toggles state with XOR `$02`;
   - transition copies use source `$40` for both destinations;
   - final copies select `$00`/`$80` according to the toggled state;
   - the final update clears the stage.
2. Test GPU copies using a synthetic ROM buffer at bank `$2C`, address `$6800`:
   four tiles from each source offset must land at tile slots `$104`/`$108`.
3. Test the crystal sprite definition against the shipped ROM metadata.
4. Test a crystal sword hit:
   - it produces the standard hit flash/ignore timers;
   - it stays alive;
   - the next entity tick requests switch animation and sets the transition
     countdown;
   - an active switch animation suppresses a second request.
5. Test the gameplay sound mapping for WAVE id `$0D`.

Run the focused tests and record the expected RED result before production
changes.

## Checkpoint 2: implement the pure animation and GPU copy

1. Add a small immutable `SwitchBlockAnimation` model that encodes only the
   observed stage transitions, state toggle, copy source offsets, and VRAM
   destinations.
2. Add GPU methods that read four tiles from adjusted bank `$2C`, address
   `$6800 + sourceOffset`, and write them to tile slots `$104` or `$108`.
3. Initialize both final-state switch-block slots after indoor room-specific
   tiles are loaded.

Run the pure and GPU tests.

## Checkpoint 3: implement crystal interaction and sound

1. Add crystal `$66` to the ROM-backed sprite catalog and normal collision
   support.
2. Add crystal-specific combat handling so the entity cannot die, retains the
   standard hit timers, and emits the raw floor-switch wave event.
3. Let the crystal entity consume its flash on the following tick and expose a
   one-shot switch-animation request.
4. Add the WAVE `$0D` gameplay sound mapping/consumer path.

Run the focused runtime, sprite, and audio tests.

## Checkpoint 4: integrate room state and VBlank

1. Add `switchableObjectAnimationStage` to `RoomSession`.
2. Apply runtime switch requests only when the session stage is zero.
3. Add `tickGameplayVBlank()` to advance switch blocks with ROM priority and
   preserve the existing ordinary animated-tile tick while the switch stage is
   idle.
4. Replace the direct `GPU.tickAnimatedTiles` call in `Main` with the session
   VBlank method under the existing gameplay gates.

Run all focused tests and the complete Java test suite.

## Checkpoint 5: documentation and review

1. Record the implementation and the explicit scope boundary in the project
   roadmap/source notes.
2. Inspect the diff for guessed addresses, incorrect GBC bank handling,
   accidental main-worktree changes, and formatting errors.
3. Run:

   ```sh
   gradle -p java clean test
   git diff --check
   git status --short --branch
   ```

4. Commit the implementation in logical units only after the tests are green.
