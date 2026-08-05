# Reconstruction roadmap

This document records verified ROM-driven implementation slices. It is a
progress log, not a claim of complete game parity.

## Verified entity foundation — 2026-08-04

- Room entity streams are read from bank `$16` through the four disassembly
  pointer tables at `$4000`, `$4200`, `$4400`, and `$4600`.
- The loader preserves source order, the first-eight cleared mask, sixteen
  entity slots, unsigned types, and the disassembly's location-to-pixel math.
- Room sprite groups and sheet selectors come from bank `$20`; standard sheets
  are copied to VRAM tiles `$40`, `$50`, `$60`, and `$70` using the GBC-adjusted
  `NpcTilesBankTable` sources.
- Object palettes come from bank `$21:$5518`, with the conditional Eagle's
  Tower palette read from the following entry.
- Crow, Dog, Marin, Kid 70/73, and Butterfly display lists are decoded from
  their handler tables in the disassembly. Unknown handlers remain explicit
  unsupported slots and are not given fabricated art.
- Pair and single OAM rendering covers 8x16 tile order, palette selection,
  transparent color zero, XORed flips, clipping, and both room-scroll halves.

The complete Java test suite passes for this slice with the shipped ROM test
resource.

## Verified entity runtime increments — 2026-08-04

- Static pickup and NPC handlers now advance through a mutable sixteen-slot
  runtime, including frame-driven Piece of Power, Butterfly, and kid variants.
- Room-defined indoor droppables mirror the `$80` slow-transition timer,
  four-frame decrement cadence, blink sentinel, and unload boundary.
- Pickup collision uses the ROM pickable table, hitbox `$1C`, frame/slot
  cadence, capacity-aware resource buffers, and first-eight room persistence.
- Butterfly movement mirrors bank `$06`'s signed fixed-point speed/accumulator
  math, phase-shifted random speed updates, and the two-pixel vector toward
  Link. `RomRandomByteSource` mirrors bank `$00`'s seed update while exposing
  the renderer's explicit `rLY` policy.
- Keese now use the bank `$06` display lists, Cave B variant, sleep/wake
  window, direction/speed tables, fixed-point flight, reverse entity-slot
  order, and ROM random cadence. The supported Keese collision path also
  mirrors the alternating Link-contact check, hitbox `$00`, basic sword
  rectangle, and a bounded dying transition.
- Entity movement is gated at the game-loop boundary during scroll, room
  transitions, inventory overlap, and dialog, matching
  `ReturnIfNonInteractive_06` for the currently wired gameplay path. Link
  handlers receive the ROM entity coordinates rather than Java sprite
  top-left coordinates.
- Rectangle OAM lists now decode signed `[Y offset, X offset, tile,
  attributes]` tuples, apply the ROM tile-offset register, and render each
  sprite at the hardware OAM origin. Grandpa Ulrira's two ROM variants are
  covered as the first concrete rectangle handler.
- Octorok now uses the bank-$03 roaming-enemy handler: its eight ROM display
  variants, `$30` tile offset, pause/walk countdowns, direction/speed tables,
  fixed-point movement, normal collision point, and one-hit basic-sword death
  path are covered. Its direction-gated rock projectile path is covered in the
  verified projectile increment below; recoil and the remaining damage states
  remain pending.
- Moblin now reads its bank-$03 eight-variant display list and runs through
  the same ordinary roaming-enemy countdown, direction, fixed-point movement,
  and collision path. Its normal hitbox, health-group `$01`/two-health
  sword path, `$04` contact damage, `$18` flash, and `$0A` ignore-hit window
  are also ROM-backed. Its direction-gated arrow path is covered in the
  verified projectile increment below; recoil and the remaining damage states
  remain pending.
- Armos Statue, Ghini, and Hardhat Beetle now decode their banked pair
  display lists, including the map-$0A Cave B Hardhat table. Their verified
  frame-driven display cadences are wired into the runtime; Armos activation
  and launch, Ghini hiding/flight, and Hardhat movement/collision states
  remain pending.
- Ordinary Ghini flight now mirrors bank `$04`'s shared-byte random target
  timers, signed speed approach to the `$0C/$F4` and `$08/$F8` tables,
  four-frame visual-Z correction, edge turnarounds, and direction flip. Its
  normal hitbox uses the ROM health-group `$13` values: eight health and
  `$08` contact damage; hiding and giant-Ghini branches remain pending.
- Hardhat Beetle now mirrors bank `$06`'s four-frame random target refresh,
  ROM infinity-norm vector calculation, signed speed approach, fixed-point
  movement, and axis-specific background stop. Its normal health-group
  `$0B` path (four health and `$08` contact damage) is wired; shield/
  sword-clink state, water behavior, and the full collision table remain
  pending. Its shared sword recoil is verified below.
- Armos Statue now mirrors the bank `$06` state-0 wake, state-1 `$30` charge
  countdown, state-2 random timer, contiguous ROM speed tables, and fixed-point
  movement. Its ROM normal hitbox drives wake-up; final-Link-position plumbing,
  flash/harmlessness transitions, background interaction, and active-state
  sword/contact damage remain pending.
- Tektite now decodes the bank `$06` pair display list and mirrors its state-0
  Z-gravity/landing transition, state-1 `$20` inertia animation, `$10` landing
  countdown, ROM direction tables, Z-aware `$14` vector toward Link, and
  fixed-point X/Y/Z updates. Its health-group `$01` path provides two health
  points and `$04` contact damage; wall-collision reversal, recoil, and full
  background/water interaction remain pending.
- Leever now decodes the four-entry bank `$04` display list and mirrors the
  hide/emerge/chase/burrow state loop, `$1F`/`$70`/`$30` ROM countdown bases,
  chase-only combat gate, `$08` Link-vector refresh, and fixed-point movement.
  Its health-group `$01` values provide two health points and `$04` contact
  damage; background interaction and recoil remain pending.
- Anti-Fairy now decodes the bank `$06` pair display list and mirrors
  `EntityInitWithRandomSpeed`, the four diagonal `$0C/$F4` fixed-point speed
  choices, horizontal-priority axis reversal, and `$08`-frame sprite cadence.
  Its health-group `$06` path provides four health points and `$04` contact
  damage; recoil and the exact collision-flag/object interaction remain
  pending.
- Sparks (`$16` counter-clockwise and `$17` clockwise) now share the bank
  `$06` pair display list and mirror their type-specific `$FD/$03` Y setup,
  eight-entry speed/mask tables, `$09` transition countdown, collision-driven
  direction index, and two-frame animation cadence. Their room collision
  probes use the ROM Spark collision-point row; health group `$2C` provides
  one health point and `$04` contact damage. Recoil and the remaining enemy
  interaction flags remain pending.
- Zol/Gel (`$1B/$1C`) now decode the bank `$06` red Zol, green Slime Eye Zol,
  and single-sprite Gel lists, mirror the shared Z-motion and state `0`-`4`
  inch/leap loop, use the ROM normal/small-enemy hitboxes and health-group
  damage values, and turn a damaged Zol into two ROM-backed Gel entities in
  the highest free slots. The split preserves source load order and ROM
  position/Z setup; Gel clinging suppresses the ordinary enemy collision path.
  Joypad-driven release from the clinging state, recoil, and the remaining
  damage-state branches remain pending.
- Hiding Zol (`$9B`) now decodes bank `$07`'s mixed hidden/pair/single display
  path, mirrors the signed `$20` proximity reveal, `$20` reveal countdown,
  random three-to-six bounce count, fixed-point Z gravity, horizontal inching,
  Link-vector leaps, repeated landing cycle, hide countdown, state-3 sword
  continuation, and full state-4/5 enemy-collision gate. Recoil, jump
  jingle/audio, background interaction, and the remaining damage-state
  branches remain pending.
- Spike Trap (`$27`) now decodes bank `$06:$74FA`, consumes the ROM random
  direction initializer, and mirrors the four-state alignment, launch,
  fixed-point travel, collision-gated return, and saved-coordinate loop using
  the source `$20/$E0`, `$F8/$08`, `$E0/$20`, and return-speed tables. Its big
  enemy hitbox and health-group `$09` values (four health, `$08` contact
  damage) are wired into the shared combat path. The handler's audio, recoil,
  and broader `hActiveEntityNoBGCollision` ground/pit/water/conveyor effects
  remain pending.
- Water Tektite (`$99`) now decodes bank `$07:$752D`, keeps its ROM noop
  initializer, selects the frame-bit-4 `$70/$72` pair animation, and mirrors
  the three-state `$20` acceleration, signed speed convergence, `$10` restart
  timer, and background-collision reset loop. Its normal enemy hitbox and
  health-group `$00` values (one health, `$04` contact damage) are wired into
  shared combat. Recoil, water/pit/conveyor side effects, and the remaining
  damage-state branches remain pending.
- Pairodd (`$57`) now decodes bank `$04:$5DD1`'s eight pair variants and
  mirrors the `$20` disappear, `$40` reappear, and `$30` resting countdowns,
  signed `$20` Link proximity windows, exact `($A0-x, $90-y)` teleport, and
  visible-phase enemy-collision gate. Its `$58` projectile uses the bank
  `$04:$5EF4` display list, reverse-slot dynamic spawn, source position/Z
  copy, length-`$18` ROM vector (including Z), and bank-$04 fixed-point travel.
  Pairodd's normal hitbox and health-group `$30` values (two health points,
  `$04` contact damage) are wired into shared combat, and variant `$03` renders
  the handler's two shifted pairs. Projectile Link/shield/object collision,
  sword-poke VFX, recoil, audio, and broader damage-state/background effects
  remain pending.
- PeaHat now decodes the bank `$07` pair display list and mirrors its resting,
  takeoff, and flying states, slow-countdown cadence, carry-aware animation,
  direct Z ascent/descent, and contiguous ROM phase-speed tables. Its grounded
  health-group `$00` contact/sword path is wired; hitbox-flag and sword-clink
  plumbing, recoil, background interaction, and water behavior remain pending.
- Aggressive Stalfos now decodes the bank `$06` three-variant display list and
  mirrors its slot-phased Link pursuit, proximity-triggered jump, fixed-point
  four-state Z arc, `$10`/`$20` landing countdowns, and health group `$2A`
  combat values. Landing dust/background collision flags, recoil, Color Dungeon
  special handling, and the full damage-state matrix remain pending.
- Gibdo now selects the regular bank `$06` pair list or Turtle Rock's alternate
  list, mirrors its init-state direction choice, `$00/$08/$F8/$00` and
  `$F8/$08` random-walk speed tables, fixed-point movement, and directional
  background bounce. Its normal health-group `$2F` path provides six health
  points and `$08` contact damage; exact collision-flag/object interaction,
  recoil, and the full damage-state matrix remain pending.
- `CreateFollowingNpcEntity` now runs on room load/state changes with the
  source indoor/map/room exclusions, source-order slot reuse, highest-free-slot
  `SpawnNewEntity` behavior, Link/wC13B spawn coordinates, Ghost trigger-state
  update, Bow-Wow's Mrs. Meow-Meow exclusion, and ROM-backed follower display
  lists for Bow-Wow, Marin, Ghost, and Rooster. Marin's sixteen-byte X/Y/Z/
  direction history is seeded through the same consecutive-byte fill helper.
- Ordinary overworld Ghost and Flying Rooster follower movement now mirrors the
  bank `$19` handlers' proximity gates, direction variants, vector refresh
  cadence, signed fixed-point position accumulators, background-collision
  rollback, and Ghost's eight-frame visual-Z bob table. Marin's history-driven
  per-frame handler and the followers' special interaction states remain
  pending.
- Bow-Wow's dynamic follower now mirrors bank `$05` setup offsets, the
  `hLinkPositionZModified` visual-Y target, ROM random speed tables, signed
  fixed-point X/Y movement, gravity/Z clamping, movement display variants, and
  the handler's `$20` target-correction window. Its edible-entity scan,
  kidnapping/retrieval, interaction, and scripted five-sprite OAM branches
  remain pending.
- Marin's ordinary `label_018_5C6A` follower branch now consumes the shared
  sixteen-entry X/Y/Z/direction history with separate position and Z indices,
  refreshes the current Link entry, carries the movement delay ring, and uses
  `Data_018_59E4` for directional display variants. The singing, lift,
  dialogue, transition, and special room-state branches remain pending.
- The Color Dungeon entity-tile path now reads the four bank-$20 room tables
  (`$46AA`, `$46D6`, `$4702`, `$472E`) as `[address high byte, bank]` pairs and
  copies each present 16-tile row into the fixed OAM slots `$40`, `$50`, `$60`,
  and `$70`. Zero entries preserve the existing slot, matching
  `LoadColorDungeonTiles` rather than treating the room as a standard sheet
  group.

The complete Java test suite passes after these runtime increments. Remaining
entity behavior—including the rest of the enemy damage matrix, recoil for
enemy families outside the shared Octorok/Moblin path, lifting/throwing
physics, entity-specific burning/death presentation and handlers, dynamic
display-list selection beyond the follower path, scripted spawns, and
history-driven follower handlers—is intentionally still unsupported rather
than represented by guessed shapes or generic movement.

## Verified enemy projectile runtime — 2026-08-05

- Octorok rock `$0A` reads its pair display list from bank `$03:$6A1E` (two
  variants); Moblin arrow `$0C` reads bank `$03:$6BC6` (four variants). The
  runtime keeps these ROM-decoded OAM bytes as the only projectile art source.
- The shared roaming handler emits a launch only in state `$01`, with
  transition countdown exactly `$0A`, private state 1 equal to zero, and the
  stored direction equal to `GetEntityDirectionToLink_03`. Octorok launches are
  suppressed during credits; Iron Mask never emits a projectile, and the
  eligibility check consumes no random byte.
- `SpawnOctorokRock`/`SpawnMoblinArrow` reverse-scan for the highest disabled
  slot. The projectile has source load order `-1`, one frame of ignore-hits,
  copied source Z, and the ROM direction tables. Octorok's direction-2/3
  offset/speed reads intentionally preserve the adjacent-label table behavior
  in the source ROM.
- `ArrowRenderAndMove` and `ArrowRockAfterHittingWall` use the shared fixed
  point movement path: a background collision starts countdown `$18`, sets Z
  speed `$10`, bounces Y before X after three arithmetic shifts, and unloads at
  countdown `$01`. Moblin transition frames select
  `[0, 3, 1, 2][countdown >> 3 & 3]`; Octorok rocks retain their raw two-entry
  display list without spin animation.
- `CheckLinkCollisionWithProjectile` (bank `$03:$6BDE`) is routed through the
  room session with the ROM byte-wide, half-open X/Y window, visual Y
  (`entity Y - entity Z`), motion-state and ground-Z gates, and the reversed
  shield-direction table. An opposite-facing shield produces jingle `$16`
  without damage; an unshielded hit produces raw wave `$03` and the existing
  player `$50` invincibility window. Moblin arrows unload on contact, while
  Octorok rocks enter the normal projectile transition. The main loop now
  supplies Link's equipped-slot/button shield state and consumes these events.
- The normal entity renderer consumes both projectile display definitions with
  the existing pair/OAM path. Tests cover all six variants, palette bytes,
  8x16 X/Y flips, tile offsets, Z subtraction, transition-selected arrow
  variants, room scrolling, and a real ROM room (`$2F`) publishing a spawned
  rock into the live render snapshot with its loaded entity tile sheet.
- Remaining projectile gaps are deliberate: laser/mirror-shield special
  handling, sword-poke transient VFX, object-intersection edge cases, full
  tunic/power-up damage modifiers and health-buffer timing, and player
  projectile interactions are not yet complete. The broader engine remains a
  staged reconstruction, not a complete entity-system claim.

## Verified enemy sword-hit response — 2026-08-05

- The shared bank-$03 roaming path now mirrors `ConfigureEntityRecoil` at
  `$6FCC`: it computes the integer dominant-axis vector toward Link with the
  ROM's `GetVectorTowardsLink` algorithm, negates it, and configures the
  default `$30` recoil speed for Octoroks (`$09`) and Moblins (`$0B`) before
  applying sword damage.
- `EnemyRecoilMotion` mirrors `UpdateEntityPosWithSpeed_03` at `$7F25` with
  signed sixteen-subpixel X/Y speeds, independent accumulators, unsigned
  coordinate wrapping, X-before-Y ordering, and background-block rollback.
  The runtime applies it before `RoamingEnemyMotion`, forces the handler's
  state-1 recoil path, consumes the existing `$0A` ignore-hits countdown, and
  clears recoil on `StopEntityRecoilOnCollision`-equivalent blocking and slot
  cleanup. The ROM `$18` flash window and existing collision gates remain in
  effect.
- Combat events preserve the final raw `hJingle` write from
  `EnemyCollidedWithSword`/`ApplySwordDamagesToEnemy`: JINGLE `$09` for a
  clink-only path and JINGLE `$03` for a normal damage hit. The gameplay
  consumer maps these to the ROM catalog's `JINGLE_BUMP` and
  `JINGLE_ENEMY_HIT` effects without guessing unknown IDs.
- Tests cover horizontal, vertical, diagonal, equal-axis, zero-distance, and
  Z-adjusted recoil vectors; fixed-point accumulation and blocking; runtime
  Moblin/Octorok health/flash/ignore/death behavior; raw sound routing; and
  explicit/dying slot cleanup. The slice intentionally does not claim the
  sword-poke VFX or recoil for other enemy handlers.

## Verified ROM enemy sword damage — 2026-08-05

- `RomEnemyCombatTables` now reads `HealthGroupForEntity` from bank
  `$03:$41F6`, `Data_003_43EC` from `$03:$43EC`, `Data_003_473C` from
  `$03:$473C`, `InitialHealthForGroup` from `$03:$47BC`, and
  `EntityDamagesForGroup` from `$03:$47F1` using the corrected banked-ROM
  offset formula. The active `RoomSession` supplies these tables to every
  `RoomEntityRuntime`; isolated no-ROM fixtures retain an explicit test-only
  compatibility path.
- Sword damage now mirrors `ApplySwordDamagesToEnemy` at bank `$03:$719D`:
  sword level, red tunic, Piece of Power, spin attack, and the explicit
  Pegasus-Boots-running state select the effective attack type before the
  health-group matrix and raw damage-value lookup. Numeric results subtract
  only their ROM amount; `$F0-$FF` results remain explicit special-action data
  rather than being treated as huge health damage.
- Initial health and Link contact damage in the ROM-backed runtime now use the
  entity's decoded health group instead of the former Java per-family
  constants. Combat events expose numeric enemy damage and raw special-action
  bytes while preserving the existing Link-damage and jingle compatibility
  constructors.
- The main loop supplies the live sword level, tunic, Piece-of-Power, spin,
  and boots-running state. The default tunic is green and boots-running is
  false until the movement subsystem sets that ROM state. Power hits use the
  ROM `$20` ignore window; the remaining power wave register and
  entity-specific clink/special-action handlers remain pending. The modeled
  spike-trap clink response is recorded below.
- Tests prove ROM bytes and lookup math for Octorok, Moblin, ignored results,
  and a raw `$FF` special result, plus runtime health progression and bump vs.
  enemy-hit feedback. This increment does not claim complete weapon coverage,
  all entity-specific collision branches, or equipment acquisition.

## Verified ROM enemy status response — 2026-08-05

- The shared bank-$03 damage path at `$7235-$7278` now routes raw `$FE` to
  BURNING with transition countdown `$60`, ignore-hits `$0A`, jingle `$03`,
  and bursting-flame noise `$12`; raw `$FF` routes to STUNNED with private
  countdown `$FF`, unchanged health, and ignore-hits `$0A`. Numeric damage
  remains on the existing health/flash/death path.
- The common status lifecycle follows `EntityBurningHandler` at
  `$4C4C-$4CA3` and `EntityStunnedHandler` at `$4E07-$4E9D`: burning
  non-Gibdos enter DYING with countdown `$1F`, while Gibdo `$1F` becomes
  Stalfos Evasive `$1E`; stunned entities return ACTIVE when their countdown
  expires. Timer decrements follow bank-$14 `$4D73-$4DDC`.
- Color Shells `$E9-$EB` are now admitted through the shared combat gate, so
  their shipped ROM `$FF` result is reachable without inventing movement or
  health data. New countdowns and recoil state are cleared on status transfer,
  slot cleanup, and dynamic replacement.
- Tests cover raw `$FE`, `$FF`, and `$FD` ROM outcomes, primary/secondary sound
  routing, collision suppression during non-active statuses, exact countdown
  boundaries, Gibdo conversion, Color Shell health, and countdown cleanup.
  Deferred work includes burn-expiry noise `$13`, poof VFX, lifting/thrown
  physics, and the full bank-$36 Color Shell handler.

## Verified ROM Color Shell runtime — 2026-08-05

- Color Shells `$E9-$EB` now select their bank-$20 rectangle display lists at
  runtime: the active eight-variant families are used while state is below
  `$06`, and the inactive four-variant families are used for the remaining
  states and non-active statuses.
- The bank-$36 handler states `0-$0D` are represented with the source
  direction, fixed-point movement, countdowns, Z arc, animation cadence,
  harmlessness, puzzle-object checks, and completion scan. The room's
  `$0C/$0D` puzzle states write the ROM-selected object IDs back into the live
  room object buffer rather than only changing an entity-local flag.
- A correct symbol match publishes the ROM noise `$04`, changes the room
  object to the color-specific solved value, and eventually emits the poof
  sprite and first-eight cleared-room persistence mask. A wrong match routes
  jingle `$1D` through the gameplay sound boundary and returns the shell to
  its movement path.
- The concrete visible checkpoint is Color Dungeon rooms containing Color
  Shells: their movement, active/inactive art, puzzle tilemap changes, sound
  feedback, and completion poof now have a live runtime path. Unrelated rooms
  are expected to look unchanged until their own source-backed handler slices
  are ported.
- Tests cover bank-$36 state transitions, ROM rectangle bytes and attributes,
  live room tilemap replacement, raw sound IDs, poof frames, and completion
  persistence. The complete Java test suite passes with the shipped ROM.

## Verified ROM burning status presentation — 2026-08-05

- The status path now decodes `FireSpriteVariants` from bank `$03:$4C44` and
  carries that pair as a room-selection overlay instead of replacing the
  entity's normal display definition.
- During `BURNING`, the renderer selects `(hFrameCounter >> 3) & 1`, applies
  the source `$02/$22` and `$14/$34` attributes, entity tile offset, flip
  attribute, visual Z, and room-scroll offset, then draws the fire over the
  normal entity body. The frame counter is passed from the live main loop into
  the render scene.
- This is the first general enemy status effect with a visible framebuffer
  result: a source `$FE` burn attack now produces animated fire feedback. The
  existing burn timers, Gibdo replacement, and death transition remain the
  separate runtime behavior already documented above; no unsupported status
  is given a guessed overlay.
- Tests cover the shipped ROM bytes, both animation phases, palette/flip
  selection, tile-offset and Z positioning, room selection installation, and
  the complete Java suite passes from a clean build.

## Verified ROM sword-poke presentation — 2026-08-05

- The transient VFX path now decodes type `$05`, countdown `$0F`, and both
  phases of bank `$02` `Data_002_57DD`: tiles `$3C` and `$3A`, raw attributes
  `$00/$20`, and the source Y-then-X OAM offsets. The renderer preserves the
  established transient OAM origin conversion and shared ROM VFX tile sheet.
- Combat events can carry the source `(wC140-$08, wC142-$08)` request. The
  gameplay boundary routes jingle `$07` to `JINGLE_SWORD_POKING` and spawns
  the transient effect into the live render layer.
- The currently modeled `ENTITY_SPIKE_TRAP` (`$27`) now follows the source
  `ENTITY_OPT1_SWORD_CLINK_OFF` branch: no enemy damage or normal recoil,
  ignore-hits countdown `$10`, cleared recoil state, and the exact sword-poke
  coordinates. Its simultaneous Link contact damage remains independent.
- The concrete visible checkpoint is a sword striking a spike trap: the
  engine now shows the two-sprite transient clink and plays the source jingle.
  Ordinary enemy sword hits retain their existing damage, recoil, status, and
  bump/hit sound behavior; other clink-off entity handlers remain pending.
- Tests cover both ROM VFX phases, transient lifetime, event routing, sound
  catalog mapping, spike-trap no-damage behavior, ordinary-enemy regression,
  and the complete Java suite passes from a clean build.

## Verified ROM Hard Hat recoil — 2026-08-05

- Hard Hat Beetle's bank-$06 handler now uses the shared `$30` sword-recoil
  vector before its ordinary `UpdateEntityPosWithSpeed_06` and target-seeking
  movement. The runtime preserves the signed sixteen-subpixel accumulators,
  normal health/flash/ignore-hit state, and the conditional roaming-state hook
  only for Octorok/Moblin.
- The bank-specific collision behavior is preserved: unlike bank-$03's
  `StopEntityRecoilOnCollision` path, `ApplyRecoilIfNeeded_06` does not clear
  Hard Hat's recoil/countdown when background interaction blocks a step.
- The visible checkpoint is a sword strike against a Hard Hat Beetle: its
  body now moves away from Link during the source ignore-hit window while its
  normal target movement remains active. Water behavior, shield/clink rules,
  recoil smoke, and the broader bank-$04/$06 recoil family remain pending.
- Focused recoil/runtime tests and the complete Java suite cover this
  increment; ordinary Octorok/Moblin recoil and spike-trap sword clinks remain
  regression-checked.

## Next entity increments

1. Port remaining simple enemy movement, collision, damage, lifting, and
   throwing behavior, plus entity-specific burning/death presentation, using
   the existing room collision model.
2. Extend rectangle and dynamically selected sprite handlers, complete entity
   tile-offset state, follower history and special states, and the remaining
   Color Dungeon symbol/animation path.
3. Port scripted spawns, followers, room events, drops, and boss/multi-entity
   state machines from the corresponding banked handlers.

## Broader parity gaps

The project still needs a systematic pass over the remaining entity handlers,
room interaction scripts, dungeon/boss phases, and hardware-visible ordering
details. These should continue to be implemented from the disassembly with
focused ROM-byte and framebuffer tests; a CPU or Game Boy emulator remains out
of scope.
