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
  and active combat are covered in the verified increment below. At this
  checkpoint, Ghini hiding/flight and Hardhat movement/collision states
  remained pending.
- Ordinary Ghini flight now mirrors bank `$04`'s shared-byte random target
  timers, signed speed approach to the `$0C/$F4` and `$08/$F8` tables,
  four-frame visual-Z correction, edge turnarounds, and direction flip. Its
  normal hitbox uses the ROM health-group `$13` values: eight health and
  `$08` contact damage. At this checkpoint, hiding and giant-Ghini branches
  remained pending.
- Hardhat Beetle now mirrors bank `$06`'s four-frame random target refresh,
  ROM infinity-norm vector calculation, signed speed approach, fixed-point
  movement, and axis-specific background stop. Its normal health-group
  `$0B` path (four health and `$08` contact damage) is wired; shield/
  sword-clink state, water behavior, and the full collision table remain
  pending. Its shared sword recoil is verified below.
- Armos Statue now mirrors the bank `$06` state-0 wake, state-1 `$30` charge
  countdown, state-2 random timer, contiguous ROM speed tables, and fixed-point
  movement. Its ROM normal hitbox drives wake-up; activation, active combat,
  and bank-$06 recoil are covered in the verified increment below. Final-Link-
  position plumbing and background interaction remain pending.
- Tektite now decodes the bank `$06` pair display list and mirrors its state-0
  Z-gravity/landing transition, state-1 `$20` inertia animation, `$10` landing
  countdown, ROM direction tables, Z-aware `$14` vector toward Link, and
  fixed-point X/Y/Z updates. Its health-group `$01` path provides two health
  points and `$04` contact damage; wall-collision reversal and shared bank-$06
  recoil are verified below, while full water/pit/ground-status interaction
  remains pending.
- Leever now decodes the four-entry bank `$04` display list and mirrors the
  hide/emerge/chase/burrow state loop, `$1F`/`$70`/`$30` ROM countdown bases,
  chase-only combat gate, `$08` Link-vector refresh, and fixed-point movement.
  Its health-group `$01` values provide two health points and `$04` contact
  damage; shared bank-$04 recoil and wall rollback are verified below, while
  generic ground/water/pit/conveyor interaction and remaining damage states
  remain pending.
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
  one health point and `$04` contact damage. Their shared bank-$06 sword recoil
  is covered in the verified increment below; no-background interaction,
  recoil smoke, and the remaining damage-state branches remain pending.
- Zol/Gel (`$1B/$1C`) now decode the bank `$06` red Zol, green Slime Eye Zol,
  and single-sprite Gel lists, mirror the shared Z-motion and state `0`-`4`
  inch/leap loop, use the ROM normal/small-enemy hitboxes and health-group
  damage values, and turn a damaged Zol into two ROM-backed Gel entities in
  the highest free slots. The split preserves source load order and ROM
  position/Z setup; Gel clinging suppresses the ordinary enemy collision path.
  Shared bank-$06 sword recoil is covered in the verified increment below;
  joypad-driven release from the clinging state, background interaction, and
  the remaining damage-state branches remain pending.
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
  shared combat, and its bank-$07 shared recoil and shallow/deep-water
  collision exception are verified below. Pit/ground-status side effects and
  the remaining damage-state branches remain pending.
- Pairodd (`$57`) now decodes bank `$04:$5DD1`'s eight pair variants and
  mirrors the `$20` disappear, `$40` reappear, and `$30` resting countdowns,
  signed `$20` Link proximity windows, exact `($A0-x, $90-y)` teleport, and
  visible-phase enemy-collision gate. Its `$58` projectile uses the bank
  `$04:$5EF4` display list, reverse-slot dynamic spawn, source position/Z
  copy, length-`$18` ROM vector (including Z), and bank-$04 fixed-point travel.
  Pairodd's normal hitbox and health-group `$30` values (two health points,
  `$04` contact damage) are wired into shared combat, and variant `$03` renders
  the handler's two shifted pairs. Its projectile Link/shield collision,
  shared sword-poke VFX, and bank-$03 sword-hit branch are verified below;
  projectile sword/object intersection, recoil, audio, and broader
  damage-state/background effects remain pending.
- PeaHat now decodes the bank `$07` pair display list and mirrors its resting,
  takeoff, and flying states, slow-countdown cadence, carry-aware animation,
  direct Z ascent/descent, and contiguous ROM phase-speed tables. Its grounded
  health-group `$00` contact/sword path is wired, and its grounded shared
  bank-$07 recoil and wall rollback plus airborne contact/clink behavior are
  verified below; generic ground-status/water/pit interaction and remaining
  damage-state behavior remain pending.
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
enemy families outside the now-covered bank-$03/bank-$06 paths,
lifting/throwing physics, entity-specific burning/death presentation and
handlers, dynamic display-list selection beyond the follower path, scripted
spawns, and history-driven follower handlers—is intentionally still
unsupported rather than represented by guessed shapes or generic movement.

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
- Remaining projectile gaps are deliberate: exact object-intersection edge
  cases, additional player-projectile producers, and their target-specific
  interactions are not yet complete. The broader
  engine remains a staged reconstruction, not a complete entity-system claim.

## Verified ROM ordinary player-arrow producer — 2026-08-07

- `ShootArrow` now follows the source item ordering: the `$10` shooting gate
  and two-active-projectile cap are checked before the arrow count, zero count
  routes to the wrong-answer jingle, and a successful request spends the arrow
  before asking the room to allocate entity `$00`.
- `SpawnPlayerProjectile` and `label_140F` are mirrored for ordinary arrows:
  Link's current single-axis D-pad input updates facing through `func_157C`,
  the entity starts at Link X/Y and Z+1, and the initial `$20/$E0` table is
  replaced by the normal `{ $40, $C0, 0, 0 }`/`{ 0, 0, $C0, $40 }` speeds or
  the Piece-of-Power `{ $30, $D0, 0, 0 }`/`{ 0, 0, $D0, $30 }` speeds.
- Player arrows read the shared bank-$03 display list at `$6BC6`, use options
  `$12` and physics flags `$42`, bounce both axes at quarter speed on a wall,
  enter the shared `$18` wall-rock countdown, spin through `[0,3,1,2]`, and
  feed `AlertSwordMoblins`'s global `$04` counter.
- The live Main/RoomSession equipment bridge now spawns and renders arrows and
  routes the ROM `$0A` whoosh through the gameplay sound catalog. Focused
  motion, item, catalog, runtime, sound-map, and full-suite tests cover this
  producer/runtime seam.
- The bank-$03 `func_003_75A2` target pass and the live Piece-of-Power speed
  selector are wired in the follow-up arrow-collision increment below. Other
  projectile producers and target-specific collision exceptions still require
  their own source-shaped slices.

## Verified ROM bomb-arrow conversion and detonation handoff — 2026-08-07

- The shared `wBombArrowCooldown` window now follows the source `$06` timing:
  bomb-first then arrow removes `wLatestDroppedBombEntityIndex` and marks the
  new arrow state `$01`, while arrow-first then bomb marks the latest arrow
  and leaves the ordinary bomb active. The cooldown decrements once per entity
  frame and the normal whoosh is suppressed for a converted shot.
- Ordinary bomb placement now preserves `ConvertToBombArrowIfNeeded`'s source
  private countdown `$10`, directional X/Y offsets, zeroed candidate motion,
  top-view Z behavior, and conditional top-view `JINGLE_BUMP` emission. This
  also fixes the earlier mistaken assumption that a freshly placed bomb
  continues using `SpawnPlayerProjectile` speeds.
- Bomb-arrow presentation composes the shipped bank-$03 bomb single sprite
  and arrow pair with the handler's direction-specific offsets. When its wall
  transition begins, the arrow unloads into a same-slot-independent bomb
  entity at transition `$17` and emits the source explosion noise `$0C`.
- The common `func_003_75A2` scan is now shared by the runtime's arrow path:
  an active bomb arrow selects attack type `$0C`, leaves target health and
  recoil untouched, writes target transition `$03`, and remains loaded; the
  ordinary arrow selects attack type `$05` and unloads after an accepted hit.
- Focused conversion, ROM catalog, equipment, session, and forced clean Java
  suite tests pass with 935 test cases.

## Verified ROM player-arrow collision and ShootArrow speed handoff — 2026-08-07

- `RoomEntityRuntime` now runs the bank-$03 descending-slot collision cadence
  before `ArrowRenderAndMove`, including the active/status, projectile-no-clip,
  ignore-hit, sprite-variant, and unsigned `$0C` X/Y gates.
- Ordinary arrow hits use ROM damage type `$05`, copy the arrow's signed speed
  directly into the target recoil velocity, apply the shared numeric/burn/stun
  damage state transitions, flash/ignore windows, death presentation, and
  JINGLE `$03`/secondary SFX events. The runtime currently applies this to the
  enemy families already covered by `RoomEntityCombatRules`.
- Bomb-arrow hits follow `BombArrowHandler`'s active-state exception: damage
  table lookup is bypassed, target health/recoil/events are unchanged, and the
  target transition countdown becomes `$03` while the arrow continues moving.
- `ShootArrow` now carries the active Piece-of-Power bit from `Main` through
  `RoomSession` to the two ROM speed tables. Tests cover normal and powered
  speeds, target damage/recoil, bomb-arrow harmless hits, and the full Java
  suite (935 cases, zero failures).
- Remaining target-specific branches in `func_003_75A2` (grabbable objects,
  bombites, Iron Masks, boss/fairy transformations) and the other player
  projectile producers remain open work.

## Verified ROM Link damage buffering — 2026-08-05

- Accepted generic enemy contact and projectile hits now pass through the
  bank-$03 `ApplyLinkCollisionWithEnemy` modifier order. Blue Tunic halves
  nominal damage; Guardian Acorn nullifies nominal `$04` and halves other
  values; effective damage is accumulated in an unsigned-byte
  `wSubtractHealthBuffer` equivalent.
- The live Main entity-contact path and projectile event consumer preserve the
  source `$50` invincibility window and hurt sound while deferring health loss
  to the existing odd-frame resource tick. Pending healing takes precedence,
  and a full-health healing buffer falls through to the source damage-reduce
  branch on the same tick.
- Piece of Power and Guardian Acorn hits now increment the source power-up hit
  counter and clear the active power-up on the third accepted hit. Pickup
  boundaries reset that counter. Pit damage remains a separate immediate path,
  and medicine/low-health presentation are still pending.
- Tests cover the source modifier precedence, zero-effective Guardian Acorn
  acceptance, byte wrapping, delayed health progression, healing precedence,
  power-up expiry, projectile consumption, and the complete Java suite.

## Verified ROM laser runtime — 2026-08-05

- Beamos parent `$2A` now decodes the eight two-sprite rotation pairs from
  bank `$04:$6C2D`, advances its direction every eight active frames, and
  spawns the source-backed invisible sensor and bank-$15 beam at the ROM
  countdown boundary.
- The dynamic sensor mirrors `LaserLinkSensorHandler` in bank `$04`: direct
  signed-byte movement, the half-open `$20` Link window, invincibility gate,
  parent `$20` transition and `$10` flash write, and the source
  `ApplyVectorTowardsLink` length `$40` calculation are all live.
- Beam `$2B` has the bank-$15 fixed-point `$1/16` movement and background
  boundary path. Its collision event preserves the ROM mirror-shield window
  (`Data_003_6BDA`), jingle `$07`, sword-poke VFX, selected-axis reflection,
  `$10` Link collision-ignore countdown, and direction-indexed Link response
  speeds. The normal beam hit still unloads and reports eight Link damage.
- Transient VFX `$06` now renders the ROM tile `$24` at the bank-$02 OAM
  offsets with the frame/slot alternating attribute bit. `RoomSession` routes
  the parent, sensor, beam, mirror-shield, VFX, shield-level, and Link response
  state through the live `Main` frame boundary; Link consumes the reflected
  speed on its next update and the sword reset mirrors `ResetSpinAttack` at
  bank `$00:$0CAF`.
- Tests cover the real parent display bytes, sensor cadence/window/vector,
  beam fixed-point motion, mirror-shield direction filtering and reflection
  side effects, transient rendering, and the live event boundary. The clean
  complete Java test suite passes with the shipped ROM.
- Successful beam creation at the parent countdown `$10` now emits the raw
  `NOISE_SFX_BEAMOS_LASER` `$08` event through the same live audio boundary;
  failed beam-slot allocation emits neither the beam nor the noise, matching
  the bank-$04 `SpawnNewEntity` carry branch.

Deferred laser details remain the parent’s generic background/contact path,
the exact bank-$15 `ApplySwordIntersectionWithObjects` edge cases beyond the
runtime collision callback.

## Verified ROM Spike Trap audio — 2026-08-05

- Spike Trap `$27` now carries the bank-$06 `$753F-$75C1` sound writes through
  the live entity-event boundary: a clear launch emits
  `NOISE_SFX_WHOOSH` `$0A`, while the state-2 forward endpoint or background
  collision emits `JINGLE_SWORD_POKING` `$07` exactly once.
- Blocked launches remain silent, matching the source branch that clears the
  transition countdown and returns before `$759A`. Existing movement,
  countdown, and state transitions are unchanged.
- Noise `$0A` resolves to the shipped ROM `NOISE_SFX_WHOOSH` catalog entry;
  jingle `$07` reuses the existing `SWORD_POKE` mapping. The live
  `RoomSession`/`Main` consumer therefore reaches the same audio sink without
  a parallel sound path.
- Focused Spike Trap/runtime/audio tests cover both launch outcomes and both
  jingle triggers. Remaining entity audio writes outside the already-covered
  handlers remain deferred.

## Verified ROM Pairodd projectile collision — 2026-08-05

- Pairodd projectile `$58` follows the bank-$04 handler order at
  `$5EFC-$5F28`: bank-$04 fixed-point movement occurs before the shared
  bank-$03 projectile collision check, and the active slot is cleared when
  the collision byte is nonzero.
- The generic shield path uses the projectile's ROM direction byte (random
  for room-loaded entities and reset-zero for the direct `SpawnNewEntity`
  path) against `ReversedDirectionsTable` at bank `$03:$6BD6`. A reverse-facing
  shield emits jingle `$16`, collision `$FF`, and removes `$58`.
- A normal contact reports the health-group `$0E` `$08` Link damage with wave
  `$03`, collision `$FF`, and the same removal behavior. Both outcomes publish
  the bank-$04 `func_004_6BE1.createSwordPokeVfx` request at post-movement X
  and visual Y (`Y-Z`), rendering transient VFX `$05` through the existing
  runtime boundary.
- The optional live sword rectangle now reaches the entity tick after generic
  combat has run. The `$58` branch uses the normal `[8,5,8,5]` hitbox and
  post-movement visual Y, then mirrors bank-$03 `$6F20-$6F27`: bump jingle
  `$09`, Link collision-ignore countdown `$0C`, collision `$FF`, slot removal,
  and one shared sword-poke VFX request. It is represented as a separate
  `SWORD_HIT` projectile event and remains excluded from generic enemy combat.
- The post-movement `ApplySwordIntersectionWithObjects` path at bank-$03
  `$7CAB-$7E0B` now samples the padded room buffer at the ROM `$11` base and
  `$10` row stride, uses the active ROM physics table, and removes `$58` on
  the source `$01`/range/ledge/`$FF` outcomes. The existing final sword-poke
  VFX request is preserved without inventing audio or bounce behavior.
- Focused collision/runtime tests and the existing entity suite pass. The
  remaining `$58` gaps are shared thrown-object state beyond this ordinary
  Pairodd path, projectile recoil/other damage-state handling, and any
  additional audio side effects.

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
  Deferred work includes death/poof presentation beyond the shared death
  countdown, lifting/thrown physics, and the full bank-$36 Color Shell handler.

## Verified ROM burn expiry — 2026-08-05

- `EntityBurningHandler` bank `$03:$4C4C-$4CA3` now reaches the Java status
  boundary with the same non-Gibdo side effects: DYING countdown `$1F`,
  physics flags `$04`, and `NOISE_SFX_ENEMY_DESTROYED` `$13`.
- The runtime emits that raw noise as a pending status event, and the live
  `Main`/`RoomSession` boundary maps it to the explicit gameplay sound
  `ENEMY_DESTROYED` using the shipped ROM effect. Gibdo `$1F` conversion to
  Stalfos Evasive `$1E` remains silent, matching the source branch.
- The source branch does not call `AddTranscientVfx`; poof/death presentation
  beyond the existing DYING countdown remains intentionally deferred.
- Focused burn/audio tests and the clean complete Java test suite pass.

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

## Verified ROM bank-$06 enemy recoil — 2026-08-05

- Keese (`$19`), Tektite (`$0D`), Anti-Fairy (`$15`), and aggressive Stalfos
  (`$1A`) now configure the shared `$30` sword-recoil vector from the ROM
  combat path. Their handlers apply `ApplyRecoilIfNeeded_06` before their
  existing family movement, preserving the current state machines and
  fixed-point accumulators.
- Bank-$06 retains its source distinction from bank-$03: a blocked temporary
  recoil movement does not invoke `StopEntityRecoilOnCollision` or clear the
  ignore countdown. The Octorok/Moblin collision-stop behavior, Hard Hat path,
  and spike-trap clink exception remain regression-covered.
- Spark (`$16/$17`) is explicitly deferred because its handler sets
  `hActiveEntityNoBGCollision`; Zol/Gel (`$1B/$1C`) is deferred because its
  recoil call follows split handling. Bank-$04/$07 families, recoil smoke,
  and remaining damage-state branches remain separate gaps.
- Focused bank-$06 runtime tests and the full Java suite verify this
  increment; the visible checkpoint is a sword strike against any of the four
  newly covered enemy families.

## Verified ROM Armos activation and combat — 2026-08-05

- Armos Statue (`$0F`) now projects the source physics transition through the
  live room runtime: dormant/waking slots begin at `$92` (shadow plus harmless),
  wake starts the `$18` flash, and state 0/1 remain outside the shared enemy
  combat pass. State 2 clears the harmless bit to expose active flags `$12` and
  admits the normal sword and Link-contact paths.
- The active path uses the ROM health-group data when a room has the shipped
  combat tables, with the isolated runtime fixture retaining the source-derived
  four-health and `$10` contact-damage fallback. Normal sword hits use the
  existing `$0A` ignore-hit window and damage event path.
- Armos is included in `ApplyRecoilIfNeeded_06`'s shared `$30` sword recoil;
  bank-$06 background-block behavior remains distinct from bank-$03's
  stop-on-collision path. The runtime still needs final-Link-position plumbing
  and Armos-specific background interaction before this handler is complete.
- Focused Armos activation/combat tests and the complete Java suite pass from a
  clean build.

## Verified ROM Spark recoil — 2026-08-05

- Spark entities `$16` and `$17` now reach the source bank-$06
  `ApplyRecoilIfNeeded_06` path after a normal sword hit. The existing shared
  `$30` vector-away calculation, `$0A` ignore-hit countdown, fixed-point
  accumulators, and pre-movement ordering are reused without duplicating the
  recoil state machine.
- Spark remains a non-roaming bank-$06 handler, so the runtime preserves its
  no-stop-on-background-block recoil policy rather than applying bank-$03's
  `StopEntityRecoilOnCollision` behavior. `hActiveEntityNoBGCollision`, recoil
  smoke, and the remaining Spark damage-state work remain separate gaps.
- The six-type bank-$06 recoil regression and the complete Java suite cover
  this increment.

## Verified ROM Zol/Gel recoil — 2026-08-05

- Zol (`$1B`) and Gel (`$1C`) now configure the shared ROM `$30` sword-recoil
  vector and use the normal `$0A` ignore-hits window.
- The live runtime applies one fixed-point recoil step before
  `ZolGelMotion.advance`, matching `AnimateZolGel`'s
  `ApplyRecoilIfNeeded_06` call after split handling and before the state
  dispatch. A blocked step preserves recoil state and the countdown because
  these are non-roaming bank-$06 handlers.
- The existing split reset clears recoil for both resulting Gel slots, and
  the focused Zol/Gel regressions plus the complete Java suite cover this
  increment. Clinging-input release, background interaction, recoil smoke,
  and remaining damage-state branches remain pending.

## Verified ROM Tektite wall reversal — 2026-08-05

- Tektite now restores a blocked X or Y coordinate after its fixed-point
  movement update, then applies the source `negate`/arithmetic-half helper to
  speed X for horizontal and vertical collision flags in ROM order.
- The vertical path intentionally updates speed X as written by
  `TektiteVerticalCollision`; speed Y is unchanged. Direction mapping uses
  the disassembly's right/left/up/down values exposed by the room collision
  callback.
- Focused Tektite regressions and the complete Java suite cover this
  increment. Recoil, water/pit/conveyor behavior, and remaining damage-state
  branches remain pending.

## Verified ROM Leever recoil — 2026-08-05

- Leever (`$0E`) now enters the shared ROM `$30` recoil path after a normal
  sword hit, before its bank-$04 hide/emerge/chase/burrow state movement.
- The runtime decrements the `$0A` ignore-hits countdown and preserves recoil
  after a blocked step, matching `ApplyRecoilIfNeeded_04` rather than the
  bank-$03 roaming stop-on-collision helper.
- Focused Leever regressions and the complete Java suite cover this increment.
  Generic wall/ground/water/pit/conveyor interaction, recoil smoke, and
  remaining damage-state branches remain pending.

## Verified ROM Leever wall rollback — 2026-08-05

- Leever's bank-$04 post-movement path now queries the room background in
  right/left/up/down order and restores blocked X/Y coordinates without
  reversing its ordinary speed, matching the shared ROM helper.
- The chase-state blocked and unblocked regressions plus the complete Java
  suite cover this increment. Ground status, water/pit/conveyor effects,
  recoil smoke, and remaining damage-state branches remain pending.

## Verified ROM PeaHat recoil — 2026-08-05

- PeaHat (`$A0`) now enters the shared ROM `$30` recoil path after a grounded
  sword hit, before its bank-$07 rest/takeoff/flying state movement.
- The runtime decrements the `$0A` ignore-hits countdown and preserves recoil
  after a blocked step, matching `ApplyRecoilIfNeeded_07` rather than the
  bank-$03 roaming stop-on-collision helper.
- Focused PeaHat regressions and the complete Java suite cover this increment.
  Hitbox-flag and sword-clink plumbing, generic background/water behavior,
  recoil smoke, and remaining damage-state branches remain pending.

## Verified ROM PeaHat wall rollback — 2026-08-05

- PeaHat's bank-$07 post-movement path now queries the room background in
  right/left/up/down order and restores blocked X/Y coordinates without
  reversing ordinary speed or changing its rest/takeoff/flying state.
- The flying-state blocked/control regression and the complete Java suite
  cover this increment. Ground status, water/pit/conveyor effects, collision
  flags, sword-clink behavior, and remaining damage-state branches remain
  pending.

## Verified ROM PeaHat airborne sword clink — 2026-08-05

- Airborne PeaHat now suppresses Link contact while retaining sword-rectangle
  eligibility, matching its dynamic hitbox and
  `ENTITY_OPT1_SWORD_CLINK_OFF` flags.
- A colliding sword uses the shared ROM clink branch: no enemy damage or
  recoil, jingle `$07`, sword-poke VFX, and a `$10` ignore-hits window.
- Grounded PeaHat damage/recoil remains unchanged. Ground/water/pit/conveyor
  interaction and other clink-off entity handlers remain pending.

## Verified ROM Water Tektite shared recoil — 2026-08-05

- Water Tektite (`$99`) now enters the bank-$07 shared `$30` recoil path after
  a sword hit, before its frame selection and three-state movement handler.
- The runtime applies one fixed-point `$D0/$D0` recoil step, consumes the
  `$0A` ignore-hits countdown, and preserves active recoil when the room
  background blocks the left/up step.
- Focused Water Tektite regressions and the complete Java suite cover this
  increment. Water/pit/conveyor interaction and remaining damage-state
  branches remain pending.

## Verified ROM Water Tektite water collision — 2026-08-05

- The live room collision boundary now follows bank-$03's
  `ApplyEntityCollisionWithObject` exception for Water Tektite (`$99`):
  shallow-water `$05` and deep-water `$07` are passable, while all other
  physics flags continue through ordinary background blocking.
- A real ROM indoor room containing Water Tektite now advances through a
  deep-water field instead of repeatedly resetting against it. Pit/conveyor
  interaction and the remaining damage-state branches remain pending.

## Verified ROM entity conveyor interaction — 2026-08-05

- The live entity runtime now applies bank-$03's conveyor movement tables
  after an active handler update, every fourth frame. The room physics lookup
  uses the exact `entityX - 1`, `entityY - 7` sample in the padded room-object
  buffer and the selected overworld/indoor ROM physics table.
- `RomTables` now loads `Options1ForEntity` from bank `$03:$42F1`; entities
  with `ENTITY_OPT1_NO_GROUND_INTERACTION` are excluded before terrain
  movement. The source Z gate and one-byte coordinate wrapping are preserved.
- Real ROM regressions cover an Octorok on overworld conveyor `$CF` (physics
  `$F4`, diagonal `+1,+1`) and a Spark `$17` on an indoor conveyor, which
  remains unchanged because its no-ground option is set.
- Ground-status updates, water splash effects, pit transitions, and the
  remaining entity-specific background behavior remain separate pending
  increments.

## Verified ROM Evasive Stalfos runtime — 2026-08-05

- Stalfos Evasive (`$1E`) now decodes the bank-$15 normal pair at `$4E7D`
  and its fleeing pair at `$4E8E`; the normal handler mirrors the ROM's
  `$00/$06/$FA/$FA/$06` random walk, fixed-point movement, animation cadence,
  held A/B jump window, length-`$12` jump-away vector, airborne Z arc, and
  `$08/$08/$10` landing writes.
- The live `RoomSession`/`Main` boundary now carries the held A/B condition
  into the entity tick. A real indoor room `$00:$0F` regression confirms the
  loaded Evasive Stalfos enters its airborne state and uses the ROM sprite
  definition. Gibdo burn expiry now leaves the converted entity with Evasive
  physics flags and a supported display definition when the ROM catalog is
  active.
- Angler's Tunnel cloning now follows the bank-$15 `$4F02-$4F50` branch: the
  map gate, highest-free-slot allocation, copied position/Z, `$52` clone
  physics, `$1A` options, fleeing display pair, fixed-point length-$18 vector,
  and the handler's final zero ignore-hits write are covered by runtime tests.
  The live `$03` room path creates the `$15:$4E8E` clone and emits raw noise
  `$0A`, which the existing shipped sound boundary consumes as the whoosh.
- Fleeing collision/ignore handling now emits jingle `$07` and sword-poke VFX
  or clears at the ROM screen edge. Generic ground-status/water/pit behavior
  remains an explicit follow-up slice.

## Verified ROM entity ground status and water splash — 2026-08-05

- The post-handler terrain boundary now samples the padded room-object buffer
  at the ROM's `entityX - 1`, `entityY - 7` coordinates and selects the active
  overworld or indoor physics table. Per-slot ground status is reset and
  retained with the same lifecycle as the entity slot.
- Positive nonzero Z and `ENTITY_OPT1_NO_GROUND_INTERACTION` skip terrain
  sampling. Deep-water/lava unloads ordinary entities and emits the source
  water splash directly; Fish, PeaHat, Rooster, BowWow, and Marin retain the
  source deep-water status. Shallow water, grass, side-scroll water, and the
  unclassified physics fallback map to the ROM status bytes. The existing
  eight-entry conveyor nudge remains every fourth frame.
- Normal top-down status transitions use the ROM options and downward-Z gate.
  Water splash VFX `$01` now renders the ROM `$18` Link tile through both
  `Data_002_57FD` two-sprite phases, and jingle `$0E` reaches the gameplay
  sound catalog as `JINGLE_WATER_SPLASH`.
- Focused collision, runtime, room-session, VFX, and audio tests plus a clean
  Java suite pass with the shipped ROM. Pit falling, wall rollback, and the
  side-scroll speed-X/speed-Y damping branch remain pending rather than being
  approximated here.

## Verified ROM entity pit falling — 2026-08-05

- The live room ground boundary now recognizes the raw well object `$61` and
  the ROM pit physics bytes `$50/$51`, then enters entity status `$02` only
  when the source ignore-hits countdown is nonzero. Falling targets use the
  sampled padded-room cell's `left + $08` and `top + $10` coordinates.
- The runtime preserves the source `$6F` countdown for Octorok, Moblin, and
  Moblin Sword, `$48` for other entities, the `$2F`/jingle `$18` expiry path,
  flash clearing, phase variants, `[0,0,4,0]` visual-Y state, repeated-remainder
  vector math, signed fixed-point movement, and unload at countdown zero.
- The Color Dungeon shell exception returns color shells to active state `$06`
  rather than running the generic falling handler. BowWow, Rooster, and Heart
  Containers remain on the ground; Marin only follows Link into a well while
  Link is in motion state `$06`.
- Raw jingle `$18` now reaches the gameplay sound map as the ROM item-falling
  effect. Focused runtime/session/audio tests and the complete Java suite cover
  the slice with the shipped ROM.

## Verified ROM falling presentation handoff — 2026-08-07

- During the long `$6F` Octorok/Moblin/Moblin Sword falling interval, the
  runtime now mirrors `EntityFallHandler`'s three
  `SetEntityVariantForDirection_03` calls before presentation. Moblin Sword's
  init direction and post-init XOR are preserved, while the entity remains
  status `$02`, so the source `ReturnIfNonInteractive` path still prevents
  movement and wall collision.
- The falling phase visual-Y table `[0,0,4,0]` now crosses the immutable room
  snapshot into the final OAM Y coordinate. The phase-$02 `+4` is display-only;
  physics Y and Z remain unchanged.
- Moblin Sword's bank-$07 inline sword pair, bank-$20 generated pair, warning
  sprite, OAM priority, tile-source split, and eight handler variants now
  decode and render from the shipped ROM.
- The ordinary bank-$07 active states now mirror the ROM's `$06/$16` speed
  tables, `$80/$30/$18` timers, alert window, ignore-hits alert handoff,
  direction inertia, background collision reversal, and Link-facing/vector
  updates. The bank-$07 health/contact/recoil family is also wired through
  the runtime combat boundary.
- The global `$04` alerting-sound counter now decrements per frame and is fed
  by modeled bank-$03 projectile wall impacts and the bomb explosion `$12`
  interaction tick in source slot order.
- The BowWow Hideout state-2 branch now emits a one-shot typed request for
  `Dialog190` when `wTransitionSequenceCounter == $04`; the live gameplay
  boundary resolves that request through the ROM dialog pointer/bank tables
  and the existing preformatted dialog renderer. The raw persistent
  `wIsBowWowFollowingLink` clear gate remains part of the follower/save-state
  pass, while the player-arrow damage pass and remaining bomb-arrow target-hit
  branches remain separate.
- Focused motion/runtime/dialog/renderer regressions and the complete Java
  suite (928 test cases) pass.

## Next entity increments

1. Port remaining simple enemy movement, collision exceptions, lifting, and
   throwing behavior, plus entity-specific burning/death presentation, using
   the existing room collision model.
2. Extend rectangle and dynamically selected sprite handlers, complete entity
   tile-offset state, follower history and special states, and the remaining
   Color Dungeon symbol/animation path.
3. Port scripted spawns, followers, room events, handler-specific drops, and
   boss/multi-entity state machines from the corresponding banked handlers.

## Verified ROM entity collision-point selection — 2026-08-05

- The shared room collision probe now loads `HitboxFlagsForEntity` from bank
  `$03:$40FB` and selects the source's normal, small, big, or Spark collision
  row from the low two bits of the entity hitbox flags.
- `EntityCollisionPointsX/Y` are loaded from bank `$03:$785F/$786F` as signed
  ROM offsets, and the live callback uses the source `posX - $08` and
  `posY - $10` sample math in right/left/up/down order. Existing handler-local
  movement and rollback behavior remains unchanged.
- ROM-table and all-four-box probe tests pass alongside the live room suites.
  Directional collision flags, collided-object reporting, and the larger shared
  post-handler rollback state machine remain separate follow-up work.

## Verified ROM entity collision result boundary — 2026-08-06

- Entity background probes now return the sampled object ID, ROM physics byte,
  sample coordinates, and the source `$01/$02/$04/$08` directional collision
  bit while preserving the existing boolean callback for unmigrated handlers.
- The live room session wires that rich boundary through the ROM-selected
  collision-point tables. The bank-$03 Octorok/Moblin/Iron Mask roaming path
  records a blocked axis and consumes the collision byte on the following
  state-0 dispatch, then enters state 1 and clears speed as in
  `RoamingEnemyState0Handler`.
- Focused result/roaming tests and the complete Java suite pass. Fine-shape,
  ledge, switch-block, hookshot-chain, entity-specific physics exceptions, and
  the remaining handler migrations remain explicit follow-up work.

## Verified ROM entity collision resolution — 2026-08-06

- `FineCollisionShapes` at bank `$03:$7A85` is ROM-loaded as 18 four-byte
  rows. The entity resolver now follows the shared
  `ApplyEntityCollisionWithObject` boundary for fine/open-door shapes,
  water-only Fish/Water Tektite behavior, grounded/airborne pit/lava handling,
  bomb/wrecking-ball fine and switch exceptions, generic entity physics, and
  rich collision result reporting.
- The existing roaming enemy path receives the resolver through `RoomSession`.
  The shipped-ROM room `$2F` Octorok regression covers deep-water passability
  versus solid blocking.
- The resolver now receives the live `wEntitiesIgnoreHitsCountdownTable`
  equivalent through a state-aware rich probe. Grounded entities pass lava,
  normal pits, and pit-warps while the countdown is nonzero, with Moldorm's
  source exception preserved; airborne entities remain passable.
- A Moblin recoil regression proves the state-aware probe is adapted into the
  legacy movement callback used by the live runtime. The roaming movement
  path now receives the same value through its rich-probe overload, and the
  session boundary verifies that its four-argument helper observes the active
  countdown.
- Remaining gaps are switch-block producers beyond the crystal-switch path,
  hookshot-chain transitions, and remaining handler-specific migrations.

## Verified ROM entity ledge collision state — 2026-08-06

- The `$D0-$D3` ledge branch now compares the ROM physics direction with each
  entity's thrown direction, blocks grounded matching-direction contacts, and
  starts the ledge transition timer for airborne matching contacts.
- Nonmatching ledges now use the source zero-timer and wrecking-ball rules. A
  live per-entity timer decrements on the ROM's indoor three-of-four-frame
  cadence and overworld odd-frame cadence, with unsigned-byte wrap behavior.
- The frame-aware rich probe feeds the same current frame into both legacy
  movement adapters and the direct roaming handler. Negative-Z samples clear
  the timer before resolution, matching the source's negative-Z path.
- Resolver, runtime lifecycle, roaming, and shipped-room indoor/overworld
  cadence tests pass with the complete Java suite. Switch-block producers
  beyond the crystal-switch path, hookshot-chain transitions, and remaining
  handler-specific migrations remain explicit follow-up work.

## Verified ROM entity switch-block collision state — 2026-08-06

- Entity physics `$04` now follows the bank-$03 `ApplyEntityCollisionWithObject`
  branch: bombs `$02` and wrecking balls `$A8` pass, non-switch `$04` objects
  remain solid ocean, and object IDs `$DB`/`$DC` select the ROM table bytes
  `[0x00, 0x02]`.
- The live room collision boundary carries the session-wide unsigned
  `wSwitchBlocksState` equivalent. State `$00` makes `$DB` passable and `$DC`
  solid; state `$02` reverses those results. Valid mismatches retain the ROM
  no-wall exception, while invalid ocean objects remain blocked regardless.
- Resolver and shipped-room session tests verify object IDs, physics `$04`,
  both states, and the bomb/wrecking-ball exceptions. The crystal-switch/VBlank
  toggle path is recorded below; `wLinkStandingOnSwitchBlock`, hookshot-chain
  transitions, and room-event producers remain separate source-backed
  increments.

## Verified ROM crystal-switch animation — 2026-08-06

- Entity `$66` now uses `CrystalSwitchSpriteVariants` at bank `$15:$4320`,
  including the shipped pair bytes `$58/$03` and `$58/$23`. Its normal sword
  hit path preserves the ROM `$18` flash and `$0A` ignore-hit timers, but the
  preceding `$FF` health write prevents the crystal from entering the death
  path.
- On the following entity tick, the live runtime consumes the flash, sets the
  crystal transition countdown to `$18`, and requests stage `$01` only when
  `wSwitchableObjectAnimationStage` is zero. The raw floor-switch sound is
  `WAVE_SFX_FLOOR_SWITCH = $0E` and is routed through the gameplay sound
  boundary.
- `RoomSession.tickGameplayVBlank()` follows `UpdateSwitchBlockTiles`: stage
  `$02 -> $03` toggles `wSwitchBlocksState` with XOR `$02`; transition frames
  read `SwitchBlockTiles` from bank `$2C:$6800` in the GBC path and copy four
  tiles to global GPU slots `$104/$108` (`$9040/$9080`). Ordinary animated BG
  tiles resume after the switch stage returns to zero.
- Indoor loads initialize both switch-block slots from the ROM final-state
  tables after room-specific tiles. Focused state-machine, GPU, sprite,
  runtime, audio, room-session, and complete-suite tests pass. Standing on a
  switch block, hookshot-chain transitions, mobile-block requests, non-sword
  producers, and complete Link motion blocking remain explicit follow-up work.

## Verified ROM Link switch-block footing state — 2026-08-06

- Link's room collision now follows `ApplyCollisionWithOceanOrSwitchBlock` in
  bank `$02`: object `$DB` expects `wSwitchBlocksState == $00`, object `$DC`
  expects `$02`, and a mismatched block is solid unless the transient
  `wLinkStandingOnSwitchBlock` override is active.
- The live `OverworldCollision` boundary applies the rule only to physics byte
  `$04` and the two source object IDs. Link refreshes the standing override
  from the ROM foot sample before its leading-edge movement probe, allowing
  Link to move off a block during a state transition.
- `RoomSession` synchronizes its room-owned switch state after room loads,
  test state changes, and the existing crystal-switch VBlank toggle. Focused
  decision, collision, Link movement, and room-session tests plus the complete
  Java suite pass with the shipped ROM.
- The source's footstep sound and `Data_002_786F` `wC13B` presentation offset,
  switch-button/mobile-block producers, and hookshot-chain transitions remain
  separate source-backed increments.

## Verified ROM Link tunic palette — 2026-08-06

- `LinkTunicPalette` now loads the six object palettes from `ObjectPalettes` at
  bank `$21:$5518` through the existing ROM RGB555 decoder. The live `Main`
  construction supplies that source to Link instead of relying on the former
  hardcoded green colors.
- `Link.render` selects palette 0 for green, palette 2 for red, and palette 3
  for blue from the live `PlayerState.tunicType()`. Tile selection, geometry,
  flips, transparency, and existing no-ROM test constructors are unchanged.
- Synthetic ROM palette tests and a shipped-ROM framebuffer regression verify
  the mapping and that tunic-colored pixels change at stable body locations.
  Sword/equipment palette effects and Color Dungeon dynamic palette writes
  remain separate source-backed work.

## Verified ROM sword palettes — 2026-08-06

- `SwordPalette` now decodes the six object palettes from `ObjectPalettes` at
  bank `$21:$5518` and selects row `3` (`BlueTunicPalette`) for the normal
  blade and row `4` for the fully charged red/orange flash.
- The live `Main` startup path supplies that ROM-backed palette to `Sword`.
  Existing no-ROM constructors retain an explicit fixture-only compatibility
  palette; they do not affect the live path.
- The equipment controller forwards the global frame counter and `Sword` uses
  its bit 2 (`$04`) phase for the charged palette, matching the source
  `hFrameCounter` branch. A shipped-ROM framebuffer test verifies that normal
  and charged colors change while the sword's alpha mask and tile geometry
  remain identical. The broader GBC palette upload/VRAM state machine, Color
  Dungeon transitions, and other equipped-item palette effects remain separate
  work.

## Verified ROM Color Dungeon room render path — 2026-08-06

- Map `$FF` now loads its room palettes from `Data_021_67D0`, its 2x2 object
  tile bytes from `ColorDungeonObjectsTilemap` at `$08:$4760`, and its object
  attributes from `ColorDungeonBGAttributes` at `$23:$6000`.
- The GPU room-load path now selects the `$20:$45EA` room BG source row,
  Color Dungeon floor/items blocks, shared dungeon tiles, and the dedicated
  wall pointer for the gameplay VRAM slots. The existing Color Dungeon entity
  row loader remains ROM-backed as well.
- Synthetic ROM boundary tests and the shipped-ROM Color Dungeon room-session
  regression pass. Switch-block state, symbol animation, dynamic palette
  effects, and Color Dungeon event scripts remain separate follow-up work.

## Verified ROM-backed opening intro runtime — 2026-08-06

- The default startup path now passes the shipped ROM into the intro sequence,
  loads the intro tile data through the existing GPU path, and forwards each
  ROM-derived frame snapshot into the renderer. The ordinary room-render path
  remains unchanged.
- Intro ship OAM, heave timing, lightning entities, rain cadence, title draw
  rows, sea color modifiers, vertical wave offsets, inert Link/Marin variants,
  sparkles, post-beach data, and DX fade palette rows are read from their
  disassembly-backed ROM locations rather than approximated Java tables.
- Shipped-ROM tests cover the initial frame, lightning palette/frame changes,
  title DX palette changes, and a framebuffer regression proving that an
  animated intro snapshot changes the rendered opening frame while its static
  background map remains stable.
- Exact beach palette interpolation, complete DX logo OAM/timing, and the
  file-select/save/new-game flow remain separate follow-up slices; a CPU/PPU
  emulator is still out of scope.

## Verified ROM-backed file selection and New Game boundary — 2026-08-06

- Title-screen Enter now loads the menu tile path from LoadMenuTiles, switches
  to a distinct file-menu render screen, and selects the ROM-encoded
  no-save or command-row background map/attrmap with the file-selection music
  track $11.
- The controller reads the name-entry character table, codepoint-to-tile
  table, cursor positions, and save-name rendering locations from the
  disassembly. Selection wrapping, the copy/erase arrow position, New Game
  character movement, A/B editing, and Start action are covered by focused
  controller tests.
- Empty-slot name entry emits START_NEW_GAME and now enters the dedicated
  ROM-backed new-game bootstrap. The file menu now hydrates its names and
  initialized-slot mask from the source-shaped SRAM image; copy/erase
  execution, fades, and exact menu jingle/audio sequencing remain explicit
  follow-up work.

## Verified ROM-backed New Game bootstrap — 2026-08-06

- The `START_NEW_GAME` boundary now follows `LoadSavedFile.initNewGame` from
  bank `$01:$5394`: it loads indoor map `$10`, room `$A3` (Marin's House),
  and places Link at the source map-entry position `(X=$50,Y=$60)`.
- The fresh player state clears the Java model's debug inventory and mutable
  resources, starts with three full hearts (`$18`), no sword/shield or
  equipped items, and applies the source capacities of `$30` arrows, `$30`
  bombs, and `$20` magic powder. The ROM direction and standing animation
  values are recorded in the immutable startup profile.
- A shipped-ROM room-session regression verifies the indoor map/room path.

## Verified ROM-shaped save-slot persistence — 2026-08-06

- The host save image mirrors the disassembly's skipped `$100` SRAM prefix,
  three `$3AD` slots, valid prefix sequence `[1,3,5,7,9]`, `$380` main block,
  DX1/DX2/DX3 regions, and source-derived modeled field offsets. Unknown raw
  bytes remain available for later decoders.
- New-file creation writes the selected name, health `$18`, max hearts `$03`,
  and zero death count at the ROM offsets, then flushes the exact image before
  entering the existing `initNewGame` runtime profile. Missing host images and
  invalid slot prefixes initialize like `InitSaveFiles`.
- Initialized slots decode the currently modeled player state and saved
  indoor/overworld spawn. Indoor loads restore up-facing standing entry;
  zero saved X uses the ROM new-game bootstrap sentinel path.
- Exact in-game `SaveGameToFile` triggers, death-count mutation, copy/erase
  screens, unmodeled WRAM/DX fields, and exact file-menu transition effects
  remain separate source-backed increments.

## Verified ROM entity lifting and generic throwing — 2026-08-06

- Status `$07` now follows `EntityLiftedHandler`'s phase/countdown tables,
  including the contiguous-ROM phase-$04 table reads, Link-relative X/Y/Z
  offsets, side-scroll Y adjustment, and the non-boolean carry states used by
  the source animation path.
- Stunned grabbable rocks and the other modeled liftable families enter the
  lifted state only through the held A/B Power Bracelet slot. Link restores the
  source direction while carrying, selects animation states `$3E..$45` at
  carry state `$01`, and item dispatch is gated during the carry state.
- Generic status `$08` now selects the bank-$14 top-view/side-scroll and
  bomb/non-bomb velocity windows, uses the ROM fixed-point accumulators and
  gravity, and applies the negate-and-quarter wall response. The room session
  and Main frame boundary forward input and synchronize the carry state.
- Focused table/runtime/Link tests and the complete Java suite pass. Bomb
  explosion/destroyable-object effects, entity-specific thrown handlers and
  triggers, power-bracelet level/tunic fast-transition sources, and story-item
  pickup presentation remain separate follow-up work.

## Verified ROM common entity death presentation — 2026-08-06

- The shared non-boss death rectangle lists now decode directly from bank `$03`:
  normal death frames use `Data_003_5488` at `$5488`, while the power-recoil
  variant uses the source groups at `$54C8`.
- `RoomEntity` keeps death-frame selection and power-recoil selection separate
  from the ordinary active sprite variant. The runtime starts lethal deaths at
  countdown `$40`, selects the ROM body phase through `$20`, then advances the
  four rectangle frames from the countdown bits through `$1F`; burning expiry
  enters the source `$1F` rectangle phase.
- The renderer consumes the death definition and frame independently of the
  normal entity display definition, preserving ROM signed offsets, hidden
  `$FF` pieces, tile attributes, palette selection, and the existing OAM
  clipping/scroll behavior.
- Runtime tests cover normal and power-recoil lethal transitions, the `$20` to
  `$1F` boundary, burning expiry, initial-state hydration, DYING-state display
  reconstruction, cleanup, slot reuse, and the full rectangle renderer.
- This slice intentionally does not claim `DidKillEnemy`, drops, room
  persistence, bosses, or entity-specific death handlers/poof effects; those
  remain source-backed follow-up work.

## Verified ROM floating-item runtime — 2026-08-06

- `ENTITY_FLOATING_ITEM` `$86` and `ENTITY_FLOATING_ITEM_2` `$E5` now decode
  their bank-$06 main display data from `$7ADD`, preserving the seven single
  entries and the `$E5` variant-$05 source quirk at `$7AD3`. The shared
  two-frame, two-entry rectangle overlay comes from `$7AEB` and is carried by
  immutable `EntitySpriteSelection` metadata into the renderer.
- Room initialization mirrors the bank-$03 position-derived variants and
  initial Z `$13`. Active ticks select the exact bank-$06 top-down or
  side-scroll Z table using `(frameCounter >> 3) & 7` while leaving X/Y
  unchanged.
- Floating pickup events preserve the source variant. Their collection path
  uses visual Y (`entity.y - entity.z`) and the source collision boundary:
  top-down Link collection requires ROM Z `>= $0C`, while side-scroll bypasses
  that Z gate. Airborne floating items therefore remain collectible through
  the collision-even-in-air path; ordinary static pickables retain their
  existing airborne rejection.
- Main/RoomSession now forward Link's ROM Z. The implemented resource writes
  are ten rupees, ten arrows, capacity-limited ten bombs, magic-powder
  inventory/count, and the `$18` health buffer.
- Color Dungeon `$86` `func_036_4F9B`, indoor `$E5` room `$1C` `wDE00`, the
  `$86` toadstool-status unload branch, `hReplaceTiles`/
  `REPLACE_TILES_MAGIC_POWDER`, `DidKillEnemy`/drop persistence, and
  floating-item wave-audio delivery remain explicit deferrals. The existing
  event boundary carries only the modeled pickup state and does not invent
  those semantics.

## Verified ROM common enemy death/drop lifecycle — 2026-08-06

- Terminal ordinary enemy deaths now follow the shared `DidKillEnemy` boundary:
  static entities update kill order/count and first-eight room persistence,
  while dynamic entities retain source load order `$FF` and skip those writes.
- `SpawnEnemyDrop` now decodes its common drop tables directly from the shipped
  ROM, including Guardian Acorn and Piece of Power counters, low-health chance
  masks, indexed drops, and the eight-entry fallback table. The counters are
  carried by `RoomSession` across room loads, and `Main` supplies live player
  health, max hearts, active-power-up, and ROM-derived boss context.
- Resolved items use the existing entity sprite and pickup paths. The highest
  free slot receives source position/Z, despawn `$80`, private countdowns
  `$18/$03`, and top-down/side-scroll initial speed state `$18/$EC`. The
  private `$18` collection delay is enforced at the live pickup boundary.
- Common drops now execute the ROM `BouncingEntityPhysics` consumer after
  spawning: top-down Z flight uses the source fixed-point accumulator and
  gravity, while side-scroll Y motion uses the four ground-status gravity/cap
  entries and the down-direction collision alignment/bounce threshold.
- Focused resolver/runtime tests and the complete Java suite pass. Handler-
  specific dropped-item writes (Like-Like shield recovery, key points, Color
  Dungeon scripts, boss/multi-entity drops), bounce audio/effects, and the
  remaining item-specific collision exceptions remain explicit follow-up work.

## Verified ROM hookshot launch and open-room return — 2026-08-06

- The equipped hookshot now follows the ROM `UseHookshot`/`FireHookshot`
  boundary: airborne and pushing launches are rejected, a second active
  chain is rejected, and the room runtime owns the dynamically allocated
  entity-$03 slot.
- `HookshotChainMotion` uses the ROM direction tables (`$30`, `-$30`, and
  zero), `$2A` transition countdown, and signed four-bit fixed-point position
  updates. Once the outbound countdown expires, it applies the ROM-shaped
  `$30` vector toward Link in an open room.
- The chain's inline `$36/$36+XFLIP` OAM pair is represented directly from
  `HookshotChainSpriteVariants`. Link's equipped-item path remains motion- and
  facing-locked for the lifetime of the active chain.
- Return unload uses entity `$03`'s normal `HitboxPositions._00` geometry and
  visual-Y (`entity Y - Z`) collision semantics, with a launch grace period so
  the spawn-on-Link position does not immediately unload.
- The complete Java test suite covers the launch contract, slot exhaustion,
  fixed-point outbound movement, return boundary, hitbox predicate, runtime
  cleanup, and sprite bytes. Hookshotable-object interaction, `$01` pulling,
  and Link's forced return movement remain separate runtime follow-up work;
  bridge spawning and the dynamic chain OAM are recorded below.

## Verified ROM hookshot background interaction and pull — 2026-08-06

- The chain now consumes the existing rich entity/background probe rather than
  reducing its sample to a boolean. Hookshotable detection is the ROM physics
  byte `$60`; object IDs remain diagnostic data and are not used as a guessed
  interaction list.
- Entity `$03` preserves source state `$01` for a non-point-blank
  hookshotable collision, unloads at the exact effective countdown boundary
  `$26`, and passes switch-block samples when Link's synchronized
  `wLinkStandingOnSwitchBlock` flag is set.
- Generic wall contact rolls the chain back, defers the collision-table wall
  response by one handler tick, then emits jingle `$07` and the existing
  sword-poke transient VFX at visual Y (`entity Y - entity Z`) before the
  normal return path.
- Pulling emits the ROM-shaped inverse `$30` Link vector through the existing
  post-entity event boundary. The `$0B` hookshot noise is emitted every fourth
  active handler frame and routed through the ROM sound-effect catalog.
- Focused motion, resolver, runtime, gameplay-audio, Main-boundary, and live
  RoomSession probe tests cover the increment. Bridge entity `$68` spawning
  and the three dynamic chain-link OAM entries are recorded below.

## Verified ROM hookshot chain and indoor bridge rendering — 2026-08-06

- `RenderHookshotChain` now emits three source-shaped dynamic OAM entries with
  wrapped signed quarter-deltas, raw X offset `$04`, tile `$24`, zero
  attributes, and the ROM's alternating visibility cadence. The renderer
  resolves tile `$24` from the live Link-character GPU load and preserves room
  scroll offsets.
- Indoor hookshot movement samples the exact padded room-object cell after a
  successful outbound step. Negative Y speed selects object `$9E` and bridge
  direction 0; positive Y speed selects `$9F` and bridge direction 1, matching
  the source's deliberately opposite pull-direction naming.
- Bridge entity `$68` uses the source `$30/$D0` fixed-point vertical motion,
  pre-move object-cell tile targeting, object `$9D` replacement, no-physics
  clear condition, and direction/status-dependent two-column tile patterns.
- `RoomSession` applies the bridge request to the padded object buffer, lets
  the ROM-backed `$9D` tilemap rebuild provide IDs/attributes, and reapplies
  the source `$81` tile-column writes with bounds guards. Room changes clear
  bridge override state.
- Focused OAM, bridge-motion, runtime, render, and real-ROM RoomSession tests
  pass, as does the complete Java suite. A generic OAM-buffer emulator and
  broader hookshot return/pulling behavior remain intentionally separate.

## Verified immediate hookshot pull motion — 2026-08-06

- Hookshot entity state `$01` now emits its inverse vector through the live
  event boundary.
- `Main` applies that vector immediately via Link's final-position operation
  after the entity pass, matching `UpdateFinalLinkPosition` ordering (vertical
  then horizontal) and bypassing normal input collision.
- Active Hookshot remains input/facing locked, and non-hookshot response speeds
  still use the queued `applyRomSpeed` path with their existing collision-ignore
  and sword-reset effects.
- Focused tests pass, and `gradle -p java test` passed.

## Verified ROM Ghini family runtime — 2026-08-06

- Hiding Ghini `$10`, Giant Ghini `$11`, and ordinary Ghini `$12` now decode
  their shipped-ROM bank-$04 pair/rectangle display lists, including the
  giant's source palettes and pre-oriented X-flip variants.
- The shared Ghini handler now mirrors hidden startup, the unsigned `$10`
  proximity window, the live Link `wCollisionType` wake signal, hidden
  reveal-frame presentation, private countdown `$30`, visible flight motion,
  ROM fixed-point timing, Z correction, and source X-flip/orientation rules.
- All three ROM types use health `$08` and contact damage `$08`; Giant Ghini
  uses the source large hitbox. Hidden Ghinis are excluded from Link and
  sword combat until visible, while visible Ghinis use the existing death and
  shared bank-$04 recoil paths. The source no-ground/no-wall options are
  honored.
- Link resets and reports the source-shaped collision bits per update. Main
  and RoomSession carry that byte directly into the runtime tick while legacy
  callers retain a zero-collision default.
- Shipped-ROM catalog, loader, Link, motion, runtime, RoomSession boundary,
  and Main architecture tests pass; the fresh clean Java suite passes with
  843 tests and `git diff --check` is clean.
- Bomb entity `$02` destroyable-object/puzzle effects, and
  remaining Ghini-specific branches outside the shared hiding/flight/combat
  path remain separate follow-up work. This slice does not add an emulator.

## Verified ROM bomb explosion interaction seam — 2026-08-06

- Ordinary Link bombs now expose the source `BombExplosionHandler` interaction
  window after the shared countdown decrement: deferred room-object/puzzle
  requests are published for countdowns `$16..$0E`, while the entity pass is
  gated to exactly `$12`.
- Entity candidates follow `CheckExplosionInteractionWithEntities`: statuses
  below ACTIVE, projectile-noclip/grabbable physics, and the separate
  `HITFLAGS_IGNORE_HITS` bit are excluded; eligible targets are scanned in
  slots `$0F` down to `$00` using the bomb visual Y (`Y-Z`) and the source
  unsigned `$30` window. Requests carry damage type `$07` and remain typed
  bomb events rather than sword/projectile combat results.
- `RoomSession.consumeBombExplosionEvents()` carries the seam to gameplay
  without fabricating room mutations or generic enemy health/recoil effects.
  Enemy bombs, destroyable-object/puzzle state changes, and the remaining
  source-specific recoil/damage application remain explicit follow-up work.
- Focused runtime/session tests and the forced clean Java suite pass with 892
  tests; this increment does not add an emulator.

## Verified ROM Link-bomb damage and recoil — 2026-08-07

- The exact `$12` Link-bomb interaction now resolves damage type `$07` through
  the shipped ROM health-group, damage-matrix, and damage-value tables before
  publishing the existing typed explosion event.
- Numeric, burn (`$FE`), stun (`$FF`), and special (`$F0..$FD`) results retain
  the source generic-damage branches. Normal lethal hits enter the `$2F`
  dying countdown; nonlethal hits use the source `$18` flash and `$0A`
  ignore-hits window.
- `GetVectorTowardsOtherEntity` is mirrored for the final length-`$30`
  recoil write, including the active bomb's source Z in the Y-distance math.
  Slot-order countdown handling is covered for both lethal Octorok and
  nonlethal Ghini targets.
- The clean Java suite passes with 938 tests and zero failures, errors, or
  skipped tests. Enemy-bomb Link collision, destroyable-object/puzzle state
  mutation, and remaining entity-specific special branches remain follow-up
  work; this increment does not add an emulator.

## Verified ROM Link-bomb basic object mutation — 2026-08-07

- The room session now consumes the existing `$0E..$16` bomb object-window
  events and mirrors `BombObjectBasicDestroyingX/Y` as a source-indexed 3x3
  sweep, preserving the padded YX room-object addressing.
- Overworld tall grass `$0A`, bush-covered stairs `$D3`, and bush `$5C` now
  use the existing ROM-backed `RevealObjectUnderObject` resolver. The active
  object, GBC render overlay value, 2x2 tile cell, and collision room view are
  updated together; the bomb path does not reuse the sword cut-grass sound.
- The room path hands bombed leaves and grass to the source type-$05 smash
  entity path at the source cell's center/bottom anchor. Indoor bombable
  blocks, puzzle-only skulls and walls retain their own interaction branches.
- The clean Java suite passes with 942 tests and zero failures, errors, or
  skipped tests. This increment does not add an emulator.

## Verified ROM bombed giant-skull persistence — 2026-08-07

- The puzzle candidate uses the raw bomb Y position, as distinct from the
  basic-object visual Y, and recognizes giant-skull cells `$BB..$BE` in the
  overworld puzzle pass.
- A hit snaps to the enclosing 2x2 block, replaces all four cells with rocky
  ground `$09`, refreshes their ROM-backed tile/attribute cells and collision
  view, and sets overworld status bit `$04` (`OW_ROOM_STATUS_OPENED`). The
  room loader now feeds that status byte back into the ROM macro parser, so a
  reload reconstructs the blasted skull as rocky ground.
- The later bomb-puzzle presentation slice now covers the source puzzle jingle
  and rubble entities; indoor bombable-wall status/adjacent-room writes remain
  a separate branch below.
- The clean Java suite passes with 944 tests and zero failures, errors, or
  skipped tests. This increment does not add an emulator.

## Verified ROM indoor bombable-wall mutation — 2026-08-07

- Indoor room parsing now consumes the selected A/B/Color Dungeon room-status
  table, replacing normal and hidden bombable wall objects `$3F..$42`/`$47..$4A`
  with the ROM's vertical `$3D` or horizontal `$3E` passage objects.
- The live bomb puzzle pass resolves wall orientation through the shipped
  indoor physics table, writes both door-status bits, and resolves the adjacent
  room through the ROM bank-$14 map-layout table where one exists.
- The immediate active-room draw applies the source `BombedWallTilesIndexes`
  row order (`$72,$72,$73,$73` or `$69,$79,$69,$79`); a room reload clears that
  transient draw override while status-driven object replacement persists.
- Focused vertical/horizontal ROM-room tests and the clean Java suite pass with
  946 tests and zero failures, errors, or skipped tests. The puzzle jingle is
  covered by the later bomb-puzzle presentation slice; remaining bomb-specific
  presentation branches remain follow-up work.

## Verified ROM indoor bombable-block mutation — 2026-08-07

- Indoor room loading now applies status bit `$40` (`ROOM_STATUS_EVENT_3`) to
  the source `ConfigureRoomObjects` map gate, replacing bombable block `$A9`
  with floor `$0D` on reload for maps at or beyond `MAP_CAVE_B` (`$0A`).
- The live basic bomb path recognizes `$A9`, writes the same floor object and
  status byte, refreshes the collision view, and applies the source immediate
  draw-command order (`$10,$12,$11,$13`) before a reload rebuilds the normal
  floor order (`$10,$11,$12,$13`).
- A shipped-ROM room test covers the live mutation, status persistence, and
  immediate-versus-reloaded tile order. Remaining bomb-specific presentation
  branches remain separate follow-up work.
- The clean Java suite passes with 947 tests and zero failures, errors, or
  skipped tests. This increment does not add an emulator.

## Verified ROM bombed liftable-rock smash presentation — 2026-08-07

- Bombed overworld leaves/grass and indoor bombable blocks now spawn the
  source entity `$05` at the exact interaction origin, with the source raw
  variant selecting rock, bush, or tall-grass presentation. The post-puzzle
  room snapshot is refreshed so the newly allocated entity is visible on the
  same gameplay frame.
- Entity `$05` decodes its intact pairs from bank `$03:$5398/$53A0` and its
  smash rectangles from bank `$19:$7B10` (`SmashedRockSpriteRect`),
  `$7B50` (`CutLeavesSpriteRect`), and `$7BD0`
  (`CutLeavesSpriteRectSwamp`). The GBC swamp rectangle is selected only for
  overworld room `$32`, matching the source branch.
- The smash countdowns are `$0F` for rock/pot/skull and `$1F` for
  bush/grass. Countdown masks select the ROM display-list frames, the source
  `$01` terminal frame unloads the entity, and raw `$FF` grass preserves the
  alternating visibility gate. The emitted noise is `$09` for rock and `$05`
  for leaves/grass.
- Focused catalog, runtime, and shipped-ROM room-session tests cover the
  display-list bytes, attributes, spawn position, flags, countdown frame
  boundaries, sounds, and cleanup. The clean Java suite passes with 950 tests
  and zero failures, errors, or skipped tests. Remaining bomb-specific
  presentation branches remain separate follow-up work.

## Verified ROM bomb puzzle jingle and giant-skull rubble — 2026-08-07

- Successful giant-skull and indoor bombable-wall mutations now publish the
  source `JINGLE_PUZZLE_SOLVED` `$02` write through the gameplay sound boundary,
  backed by the shipped-ROM sound catalog. Basic bombed leaves and indoor
  bombable blocks do not receive that puzzle-only jingle.
- Giant-skull destruction mirrors `Spawn2x2RubbleEntities` from bank `$36`: it
  allocates up to four dynamic type-$05 entities in descending slot order,
  applies `Data_036_7052`/`Data_036_7056` offsets, subtracts the live bomb Z,
  initializes countdown `$0F`, and uses physics `$C4` without emitting a
  second smash sound.
- The rubble entities render the bank-$19 smashed-rock frame from the first
  visible handler frame and unload at the source countdown boundary. The room
  session test covers all four ROM positions, dynamic display-list source,
  exact puzzle jingle ID, and the clean Java suite passes with 953 tests and
  zero failures, errors, or skipped tests. Remaining bomb-specific
  presentation branches remain separate follow-up work.

## Verified ROM outdoor bombable cave-door mutation — 2026-08-07

- The overworld puzzle pass now resolves the shipped cave-door physics window,
  publishes puzzle jingle `$02`, replaces bombable door `$BA` with rocky cave
  door `$E1`, sets `OW_ROOM_STATUS_OPENED` `$04`, refreshes collision, and
  preserves the existing warp tile location.
- Overworld reload parsing applies the source status-driven `$BA` to `$E1`
  replacement and updates the live GBC render-overlay value to match the
  source WRAM2 backup path.
- The immediate GBC redraw reads `BombedCaveDoorTilesIndexesGBC` from bank
  `$03:$6751` (`$64,$66,$64,$66`). A later room load intentionally uses E1's
  ordinary object-table order (`$64,$64,$66,$66`), matching the source's
  transient draw versus `LoadRoomTilemap` paths.
- The shipped `Overworld13` test covers live mutation, jingle, warp location,
  immediate tile order, status/render persistence, and reload. The clean Java
  suite passes with 954 tests and zero failures, errors, or skipped tests. This
  increment does not add an emulator.

## Verified ROM enemy-bomb Link collision — 2026-08-07

- Type-$02 bombs with nonzero `wEntitiesPrivateState4Table` now follow the
  separate `$12` enemy-bomb branch from `BombExplosionHandler`; the collision
  window uses the raw active-entity Y rather than the visual `Y-Z` position.
- The branch resolves the bomb's shipped-ROM contact damage group `$02`
  (`$08` nominal damage), preserves the generic Link damage boundary, resets
  Pegasus Boots through the gameplay consumer, and emits the source WAVE hurt
  request only when Link is not already invincible.
- The current `hLinkSpeedX/Y` bytes cross the Link → RoomSession → entity
  runtime boundary. A colliding enemy bomb publishes the source post-collision
  `SLA` writes, including the protected-hit case where damage is suppressed but
  recoil still occurs; the `$04` sword/Moblin alert write is retained.
- Enemy bombs retain the shared `$0E..$16` destroyable-object/puzzle pass before
  the Link branch, while private state `$4C` keeps the source early return.
  Shipped-ROM tests cover nominal collision, raw-Y separation, invincibility,
  ROM speed exposure, and shared object-event ordering.
- The clean Java suite passes with 959 tests and zero failures, errors, or
  skipped tests. Source-specific Mad Bomber/Bombite throw and spawn wiring,
  plus remaining enemy-bomb special branches, remain follow-up work; this
  increment does not add an emulator.

## Verified ROM Bomber enemy-bomb producer — 2026-08-07

- Entity `$BA` now decodes `Data_018_77ED` from bank `$18` as the source's
  four three-sprite rectangle variants, with the handler's `$04` frame cadence.
- `BomberEntityHandler` state, inertia, Z bob, random direction/speed tables,
  fixed-point movement, Link-facing sword window, and the `$7F` inertia spawn
  cadence are represented by a dedicated runtime motion holder.
- Bomber-spawned type-$02 bombs copy the source position/Z, receive the
  `$10` vector toward Link, Z speed `$08`, countdown `$40`, private state `$01`,
  and `SpawnNewEntity`'s initial ignore-hits frame. Their subsequent movement
  uses the existing bomb physics path without changing player-bomb behavior.
- The source JINGLE_FALL_DOWN `$08` write is carried through the entity event
  boundary and mapped to the gameplay sound sink. Bomber's ROM physics/options,
  health group `$2B`, contact damage `$08`, and normal collision registration
  are also wired.
- Shipped-ROM tests cover the rectangle bytes, first bomb spawn, source vector
  movement, and sound mapping. The clean Java suite passes with 963 tests and
  zero failures, errors, or skipped tests; Mad Bomber/Bombite producers and
  remaining enemy-bomb special branches were follow-up work at this increment.

## Verified ROM Mad Bomber enemy-bomb producer — 2026-08-07

- Entity `$93` now decodes `MadBomberSpriteVariants` from bank `$06:$4126`,
  including the hidden `$FF` variant and the five source animation pairs.
- The eight source hole positions (`$28/$38/$58/$78/$88` X values and
  `$20/$40/$50/$70` Y values) and the state `$00..$04` wait, pop-out, throw,
  and hide cycle are represented directly. A hole is accepted only when Link
  is at least `$20` units away on one axis, matching the source comparisons.
- Mad Bomber's flash guard suppresses the throw, while a successful throw
  creates type `$02` with the source position, `z=$04`, `speedZ=$18`, a
  length-`$10` vector toward Link, countdown `$40`, private state `$01`, and
  the `SpawnNewEntity` initial ignore-hits frame. JINGLE_FALL_DOWN `$08`
  crosses the existing gameplay sound boundary.
- ROM options/physics, health group `$25`, contact damage `$04`, no-ground
  interaction, and handler-owned dropped-item byte `$3C` are wired. A
  shipped-ROM end-to-end test covers the hole selection and bomb payload; the
  clean Java suite passes with 964 tests and zero failures, errors, or skipped
  tests. Bombite producers and remaining enemy-bomb special branches were
  follow-up work at this increment.

## Verified ROM Bombite enemy-bomb producers — 2026-08-07

- Entity `$55` (Bouncing Bombite) and `$56` (Timer Bombite) now decode their
  bank-$04 sprite-pair lists, including the Turtle Rock alternatives at
  `$04:$7E0D` and `$04:$7D07`; the standard lists are `$04:$7DF5` and
  `$04:$7CEF`.
- Timer Bombite's source `$00/$01` walk/lit state machine is wired with its
  `$6F` slow fuse, `$12` Link-distance chase threshold, `$0E` vector, countdown
  animation table, Pegasus Boots reset, and private-countdown palette flip.
  Bouncing Bombite's random turn, fixed-point motion, lit collision bounce,
  and JINGLE_BUMP `$09` path are also represented.
- The bank-$03 Bouncing Bombite sword special enters state `$02`, reverses the
  ROM vector, starts transition `$40` and private countdown `$08`, and avoids
  ordinary sword damage. Both Bombites use health `$04`, contact damage `$08`,
  physics `$02`, and splash-in-water options from the shipped ROM tables.
- `BombiteExplode` now creates the source type-$02 enemy bomb at the moved
  Bombite position with `z=$00`, countdown `$17`, private state `$01`, the
  initial ignore-hits frame, and `PlayBombExplosionSfx` noise `$0C`. Shipped-ROM
  tests cover the timer fuse, sprite addresses, explosion payload, and special
  Bouncing Bombite sword path. The clean Java suite passes with 966 tests and
  zero failures, errors, or skipped tests; Bombite entity-vs-entity collision
  table branches and remaining enemy-bomb special branches remain follow-up
  work.

## Verified ROM Bouncing Bombite entity collision — 2026-08-07

- Lit Bouncing Bombites now run the Bouncing Bombite-specific portion of the
  bank-$03 `func_003_75A2` scan after movement: descending target slots,
  alternating frame cadence, active-status threshold, projectile-noclip filter,
  strict unsigned twelve-pixel X/visual-Y windows, and the `$FF` sprite-variant
  exclusion all match the ROM.
- A valid collision zeroes the active Bombite's transition byte. When the target
  is another Bouncing Bombite, its current speed bytes are copied directly and
  its state, transition, and private countdown become `$02`, `$40`, and `$08`;
  the source then follows the existing `BombiteExplode` payload path.
- A shipped-ROM regression covers sword-lit source setup, the frame-selected
  target, copied speed, target state, source disappearance, type-$02 bomb
  payload, and noise `$0C`. The clean Java suite passes with 967 tests and zero
  failures, errors, or skipped tests. Remaining target-specific branches in
  `func_003_75A2` and other enemy-bomb special branches remain follow-up work.

## Verified ROM Iron Mask entity runtime — 2026-08-07

- Entity `$24` now decodes the masked bank-$03 display list at `$4FCB`, uses
  the shared roaming state machine with the source `$0C/$F4` speed tables,
  initializes physics `$12`, and participates in ROM-backed health/contact
  collision (`$02` health, `$04` contact damage).
- Masked sword collisions compare Link's ROM direction against the active mask
  direction. Rear hits preserve health, configure the source length-$10 recoil,
  start the `$10` ignore window, and publish sword-poke VFX/jingle `$07`;
  front hits continue through the normal sword-damage path.
- Hookshot entity collision now follows the Iron Mask branch of
  `func_003_75A2`: matching approach direction sets private state `$01`, spawns
  type `$32` at the hook position with the source variant, ROM display list
  `$03:$5B80`, physics `$B2`, and the source item options. The unmasked target
  then uses its separate transition/direction/speed handler and display list
  `$03:$4FEB` rather than the masked roaming animation.
- Focused ROM display-list, movement, rear-hit, hookshot-unmask, and spawned-mask
  tests are covered. The clean Java suite passes with 970 tests and zero
  failures, errors, or skipped tests. Other hookshot projectile damage branches,
  item-grab behavior for the dropped mask, and the remaining entity handlers
  remain follow-up work; this increment does not add an emulator.

## Verified ROM Goomba entity runtime — 2026-08-07

- Entity `$9F` now decodes `GoombaSpriteVariants` from bank `$07:$65CE`,
  including the exact three pair frames, palette attributes, and X-flipped
  walking frame. Its shipped-ROM physics, hitbox/health group `$00`, and
  contact damage `$04` are wired into the shared entity runtime.
- The bank-$07 normal-room state machine now uses the source `$08/$F8`
  direction speeds, `$30..$6F` random walk windows, every-fourth
  Link-directed turn, `$20` state transition, fixed-point movement, gravity,
  wall reversal, floor snap, and frame-bit `$04` walking animation. The
  side-scroll branch uses the source horizontal Link-facing speed and the same
  gravity/collision helper.
- Airborne Link collision now carries the source vertical-velocity byte through
  `RoomSession` to the entity runtime. Descending Goomba contact enters state
  `$02`, sets transition `$30`, emits the floor-switch WAVE `$0E`, and returns a
  typed Link-bounce action; the main loop applies the top-down `$10` Z bounce or
  side-view `$F0` Y speed. Ascending contact remains non-colliding.
- State `$02` now writes the source droppable-heart byte `$2D`, private death
  window `$0C`, physics `$04`, and shared DYING status, allowing the existing
  enemy-drop/death handler to finish the ROM path. Shipped-ROM tests cover the
  display list, random walk, animation, health/contact/sword values, stomp
  direction gate, side-scroll bounce, and death transition. The clean Java suite
  passes with 976
  tests and zero failures, errors, or skipped tests; other entity handlers and
  remaining Goomba presentation edge cases remain follow-up work.

## Verified ROM Snake entity runtime — 2026-08-07

- Entity `$A1` now decodes `SnakeSpriteVariants` from bank `$07:$683E`, with
  the exact four pair frames, palette bytes, and horizontal-flip attributes
  read from the shipped ROM. Its physics `$12`, normal enemy hitbox, health
  group `$00`, and contact damage `$04` are wired into the shared runtime.
- The bank-$07 handler now follows the source ordering: render the persistent
  direction offset, move with the ROM fixed-point speed tables, apply room
  background interaction, then dispatch states `$00..$02`. It covers the `$30`
  init private countdown, `$30..$4F` random walk windows, direction table,
  `$18` crawl-to-dash transition, axis-proximity dash with doubled speed, and
  `$20/$40` dash recovery timing.
- Collision reset writes state `$00`, transition `$08`, and private countdown
  `$20`, while the frame-bit `$03` crawl and bit-$02 dash animation cadence
  remains distinct. Shipped-ROM tests cover exact OAM bytes, initialization,
  fixed-point motion, dash selection, collision reset, and health/contact/sword
  behavior. The clean Java suite passes with 981 tests and zero failures,
  errors, or skipped tests; remaining entity handlers and broader room-script
  parity remain follow-up work.

## Verified ROM Wizrobe entity runtime — 2026-08-07

- Entities `$21` and `$22` now decode the exact bank-$06 Wizrobe and projectile
  display lists at `$7604` and `$65E1`, including the projectile's four-way
  palette pulse and the source direction-selected projectile frames.
- The parent follows the source reveal/vanish state machine: transition `$80`,
  private countdown `$20`, direction-to-Link selection, `$40` firing window,
  projectile launch at the ROM offset tables, and the `$28` launch cadence.
  Projectile slots copy the source position/Z, direction, fixed-point speeds,
  physics `$42`, options `$12`, and `SpawnNewEntity`'s initial ignore-hits
  frame.
- Wizrobe projectile Link collision uses the shared shield/hurt event boundary,
  clears on contact, and retains the source object-intersection clear path.
  Parent health group `$0C` and projectile group `$0E` are loaded from the ROM
  combat tables; Wizrobe contact damage is `$08`, while the normal sword rows
  are zero as defined by `Data_003_43EC`.
- Shipped-ROM tests cover both display lists, reveal timing, direction/launch
  payloads, fixed-point projectile motion, palette flipping, projectile
  collision lifecycle, and combat-table behavior. The clean Java suite passes
  with 986 tests and zero failures, errors, or skipped tests; remaining entity
  handlers and broader room-script parity remain follow-up work.

## Verified ROM Like Like entity runtime — 2026-08-07

- Entity `$23` now decodes `LikeLikeSpriteVariants` from bank `$06:$7DD4`,
  including both exact pair frames and their GBC attributes. The shared
  bank-$06 Gibdo walk is kept as an independent per-slot motion state and uses
  the source init direction choice, fixed-point speeds, and background bounce.
- Like Like's state-0 collision follows the alternating frame cadence and
  transitions into the swallowed state without applying the harmless entity's
  `$04` contact damage. The swallowed state scans the B slot before A, steals
  only a normal shield, refuses a level-two shield, animates the source
  private-state cadence, and releases after eight held A/B frames with slow
  timer `$15`.
- Capture/release requests now cross `RoomEntityRuntime` and `RoomSession` as
  typed events. The main loop clears the stolen inventory slot, positions Link
  from the ROM entity coordinates, hides and freezes Link while swallowed, and
  restores control on release. Sword collisions are disabled during the
  swallowed handler state, matching the source dispatch.
- A swallowed shield forces the source `$31` sword/shield pickup on Like Like's
  terminal death path. Its bank-$03 single-sprite definition at `$5B95`,
  physics/options, and existing pickup collection path are wired as well.
  Focused ROM-byte, movement, capture, inventory, release, drop-art, and Link
  integration tests are covered. The clean Java suite passes with 993 tests
  and zero failures, errors, or skipped tests; remaining entity handlers and
  broader room-script parity remain follow-up work.

## Verified ROM Spiked Beetle entity runtime — 2026-08-07

- Entity `$2C` now decodes the normal-room and Angler's Tunnel sprite lists
  from bank `$07:$7784` and `$07:$7794`, respectively, using the exact shipped
  ROM bytes. Its physics `$12`, contact damage `$04`, health group `$02`, and
  normal/flipped handler options and hitbox flags are wired into the shared
  runtime.
- The bank-$07 state machine now covers the source rest-to-walk countdown,
  Link/random direction selection, `$06` walking speeds, axis-proximity dash,
  gradual `$18/$E8` dash acceleration, collision reset, fixed-point movement,
  landing bounce, and frame-bit walking animation. The flipped state uses the
  source variant pair, disabled hitbox, and post-sword landing transition.
- The shared sword special now follows the source boundary: the initial
  static `$08` options path flips the beetle with the ROM direction speeds and
  jingle `$09`, while the normal handler's `$48` options path produces the
  sword-poke/clink event without damage. Shipped-ROM tests cover both paths,
  state transitions, animation, display lists, health, and contact damage.
  The clean Java suite passes with 999 tests and zero failures, errors, or
  skipped tests; remaining entity handlers and broader room-script parity
  remain follow-up work.

## Verified ROM Pols Voice and Ocarina playback seam — 2026-08-07

- Entity `$18` now decodes `PolsVoiceSpriteVariants` from bank `$06:$7373`
  with the exact two pair frames from the shipped ROM. Its physics `$12`,
  splash-only options `$08`, health group `$0C` (four health), and contact
  damage `$08` are wired into the shared runtime.
- The bank-$06 jump handler follows the source state loop: the six random
  horizontal/vertical speed pairs, the Link-directed vector branch, fixed-
  point Z motion, `$18 + (rand & $0F)` landing transition, horizontal-speed
  clear, and delayed standing/jumping presentation variants are covered by
  ROM-backed tests. The temporary ignore-hits byte `$01` is visible only to
  the background probe and is cleared before ordinary collision resolution.
- The Ballad preamble now consumes the live Ocarina playback state at
  countdown `$01`, sets the source `$1F` death timer and physics `$04`, and
  emits the bank-$06 noise `$13` event. The Ocarina item selects the ROM
  Ballad/Mambo/Frog countdowns (`$DC`, `$D0`, `$BB`) and sound events, blocks
  Link motion while playing, and forwards playback through `RoomSession` to
  a live Pols Voice room entity.
- Shipped-ROM tests cover the display list, jump/vector behavior, animation,
  background ignore window, combat values, death preamble, item selection,
  and live room integration. The clean Java suite passes with 1,009 tests
  and zero failures, errors, or skipped tests.
- The remaining Ocarina boundary is explicit: the modeled player state is now
  written through the in-game save command, while the rest of the source
  `SaveGameToFile` field copy remains outside the currently modeled save
  state. Remaining entity handlers, room scripts, and broader hardware-visible
  ordering remain follow-up work.

## Verified ROM Ocarina state in save slots — 2026-08-07

- The SRAM model now decodes `wOcarinaSongFlags` and `wSelectedSongIndex` at
  main offsets `$349` and `$34A` (`DB49`/`DB4A` relative to `D800`), preserving
  the source's byte layout rather than adding a parallel host-only store.
- `PlayerState.applySavedGame` restores both bytes with the same three-song
  flag mask and zero-based selector bounds used by live Ocarina playback.
  New-game SRAM creation continues to leave both values zero, matching the
  ordinary ROM new-game path.
- Focused save-image and player-state tests cover raw decoding, live-state
  application, and the source save-health normalization. The complete modeled
  write path is recorded in the following save-command increment.

## Verified ROM Ocarina popup navigation and rendering — 2026-08-07

- The inventory cursor now opens the Ocarina popup when it lands on a learned
  Ocarina, and horizontal input is handled by the popup while it is visible.
  Selection wraps across the three source entries and skips unavailable songs
  using the exact `$04/$02/$01` masks from `Data_020_610E`.
- Start first closes the popup through the source 16-frame close animation,
  then closes the inventory bar. The popup's opening/closing state and selected
  song continue to use `PlayerState`, so the existing Ocarina playback path
  consumes the changed selection directly.
- `Data_020_604B` is decoded from ROM bank `$20:$604B`; the four animation
  frames, unavailable-song attribute masking, selection marker, and inventory
  object palettes are rendered through the existing indexed sprite path.
  Ocarina symbol tiles follow the VBlank copies to `vTiles0+$200/$240/$260`
  from bank `$2C:$6960`, with the shared VFX rows restored after the popup.
- Focused popup, GPU, integration, and framebuffer tests are included. The
  clean Java suite passes with 1,018 tests and zero failures, errors, or
  skipped tests. Remaining entity handlers, room scripts, and broader
  hardware-visible ordering remain follow-up work.

## Verified ROM Ocarina save-command path — 2026-08-07

- `SaveRamImage.writePlayerState` writes the currently modeled inventory,
  resource, health, heart, rupee, Ocarina, and tunic fields at their source
  SRAM offsets. Its zero-health path uses the ROM
  `MaxHeartsToStartingHealthTable`; rupees use the source high/low BCD bytes.
  The lower-level Ocarina writer remains available for exact two-byte updates.
- Unknown fields, including death-count and photo bytes, are preserved rather
  than guessed. The source spawn checkpoint is handled by the dedicated
  source-offset writer below. Dungeon item flags now have their own
  source-shaped runtime owner and save writer. `SaveRamStore` delegates the
  raw image operations.
- The main loop now records the active save slot when a file is created or
  loaded. The source A+B+Start+Select chord is exposed through a configurable
  Select key (default `Tab`) and opens a dedicated two-option save screen only
  when gameplay input is otherwise available.
- The save screen uses `MenuFileSaveTilemap` at bank `$20:$6A6D`,
  `MenuFileSaveAttrmap` at `$24:$6262`, the shared file-menu palette block,
  and the source arrow OAM entry (`$BE`, X `$24`, Y `$48/$58`). Its tile sheet
  follows `LoadSaveMenuTiles` from bank `$0F:$4400` to `vTiles1`, rather than
  reusing the broader file-selection loader. A confirms the ROM-selected
  Return to Game / Save and Quit option; Save and Quit flushes the modeled
  live player fields and returns to file selection.
- Save-image, controller, ROM-scene, configuration, input-chord, and main-flow
  tests are included. The clean Java suite passes with 1,026 tests and zero
  failures, errors, or skipped tests. The remaining save parity work is the
  unmodeled gameplay fields within the broader `$380`-byte `SaveGameToFile`
  copy; those bytes are currently preserved rather than overwritten by guessed
  state.

## Verified ROM room-status save/load path — 2026-08-07

- `RoomSession` now exposes defensive snapshots of the WRAM status tables that
  its room loader and interaction handlers already mutate: the contiguous
  overworld, indoor-A, and indoor-B `$100`-byte tables, plus the Color Dungeon
  `$20`-byte DX2 table. Restoring them occurs before a saved room is loaded, so
  source status-gated objects and entities see the loaded flags immediately.
- `SaveRamImage.writeRoomStatuses` mirrors the exact `SaveGameToFile` layout:
  main offsets `$000..$2FF` receive the three room tables, and the DX2 region
  receives the Color Dungeon table. DX1 Color Dungeon item flags and other
  unmodeled fields remain untouched by this method. Save and Quit writes these
  status tables alongside the currently modeled player fields.
- Focused SRAM, RoomSession, store-delegation, and main-flow tests cover the
  offsets, defensive copies, restore boundary, and preservation behavior. The
  clean Java suite passes with 1,029 tests and zero failures, errors, or skipped
  tests. Remaining save work is the source state not yet modeled, including
  death-count updates and photo persistence.

## Verified ROM dungeon-item flag state — 2026-08-07

- `DungeonItemState` now keeps the source's five-byte
  `wCurrentDungeonItemFlags` buffer separate from the nine-entry
  `wDungeonItemFlags` table at `$DB16-$DB42` and the five-byte
  `wColorDungeonItemFlags` DX1 extension at `$DDDA`.
- Loading an ordinary dungeon copies its five-byte entry into the current
  buffer. Color Dungeon loads DX1; Wind Fish's Egg and non-dungeon indoor maps
  expose zeroes, matching `GameplayWorldLoad0Handler`'s inventory setup.
  Source chest-type increments synchronize back to the selected persistent
  table through the same map split used by `SynchronizeDungeonsItemFlags`.
- `RoomSession` owns this state and restores it before loading a saved indoor
  room. `SaveRamImage.writeDungeonItemFlags` writes main offset `$316` for the
  `$2D`-byte dungeon table and the five-byte DX1 block without disturbing
  adjacent save fields; the main save flow now reads and writes both regions.
- Focused source-layout, state, RoomSession, SRAM, and main-flow tests cover
  ordinary dungeons, Color Dungeon, non-dungeon maps, synchronization, exact
  offsets, and defensive copies. The clean Java suite passes with 1,038 tests
  and zero failures, errors, or skipped tests.
- Chest entity spawning, presentation/dialog sequencing, and the inventory
  effects that consume these flags remain a separate parity slice; this change
  establishes their source-faithful state boundary without fabricating a chest
  handler.

## Verified ROM chest contents and collected-chest room state — 2026-08-07

- `ChestContentsTable` now reads the shared room chest table at bank
  `$14:$4560` and the Color Dungeon table at `$14:$4860`, preserving the
  source's raw item ids. The sword-level replacement for a secret-shell chest
  (`$20` to `$1C` once the sword is upgraded) follows `func_014_5900`.
- Room-object loading now applies the source chest-status bit `$10`: every
  closed chest object `$A0` in the active padded room buffer becomes the open
  chest object `$A1` when the room status is set. This makes the persisted flag
  visible in the room representation before rendering.
- Focused shipped-ROM tests cover both tables, Color Dungeon selection, the
  sword fallback, byte/range validation, and the status-gated object change.
  The clean Java suite passes with 1,043 tests and zero failures, errors, or
  skipped tests.
- The remaining chest boundary after this table/status slice is the dynamic
  entity `$07` integration described in the following checkpoint.

## Verified ROM chest entity runtime — 2026-08-07

- Closed `$A0` chest interaction now follows the bank-$00 sword-area path:
  Link must face up and press an action button, the ROM chest table selects
  the item (including the upgraded-sword seashell replacement), and entity
  `$07` spawns at the masked object cell with the source `$C2` physics and
  `$02` options flags.
- `EntityInitChestWithItem` is mirrored through the live room runtime. It
  emits door-unlock noise `$04`, writes the open-object state, applies the
  immediate reward boundary, advances the `$FC` vertical launch through
  inertia `$10`, emits the ROM presentation sound at `$08`, opens the exact
  dialog table/low-byte pair at `$26`, and unloads at `$28`.
- Chest rewards now cross the live session boundary: ordinary inventory,
  shield/bracelet levels, medicine, rupee/seashell buffers, and dungeon-item
  flag synchronization are applied to modeled player state. The source
  Face Shrine/Eagle's Tower alternate chest sprite lists are selected from
  the map/room/item triple.
- The special chest Zol path now initializes the spawned Zol with the exact
  source state `$03`, X speed `$08`, Z speed `$18`, Z position `$06`, and
  private countdown `$50`, and emits wrong-answer jingle `$1D`.
- Bracelet and medicine save bytes are read and written at the source main
  SRAM offsets `$343` and `$30D`; transient chest counters are cleared when a
  save is applied. Focused ROM tests and the clean Java suite pass with 1,052
  tests and zero failures, errors, or skipped tests. The remaining chest
  boundary is the source-specific magnifying-lens/trade-item edge case and
  broader room-script/input ordering outside this interaction seam.

## Verified ROM chest-progress save fields — 2026-08-07

- The Java player/save boundary now mirrors the contiguous source bytes
  `$DB0C-$DB15`: flippers, medicine, trade-sequence item, seashells, the
  source medicine-count byte, four dungeon keys, and golden-leaf count.
- Chest rewards for flippers, medicine, Tail/Angler/Face/Bird keys, and golden
  leaves update those owners with the source increment semantics. Dungeon map,
  compass, stone-beak, Nightmare-key, and small-key rewards continue through
  the separate `DungeonItemState` current/persistent flag bridge.
- `SaveRamImage` reads and writes the fields at main offsets `$30C-$315`, and
  `PlayerState.applySavedGame` restores them before clearing transient runtime
  state. Save-image, player-state, and source-offset tests cover round trips;
  the clean Java suite remains at 1,052 tests with zero failures, errors, or
  skipped tests.

## Verified ROM flipper swimming motion — 2026-08-07

- Link's leading-edge collision now keeps deep-water physics `$07` solid until
  the saved `wHasFlippers` state is present. With flippers, the source entry
  speeds from `Data_002_750A/750E` are written and motion state `$01` takes
  over; leaving deep water returns to the default motion state.
- Swimming acceleration reads the two ROM rows at
  `Data_002_4EF0/4F00` and `Data_002_4F10/4F20`. The source even-frame
  cadence, newly-pressed A fast-swim window, newly-pressed B diving toggle,
  diving timeout, and animation states `$46-$4F` are represented without
  introducing a general emulator loop.
- Focused shipped-ROM tests cover the no-flippers barrier, entry/exit state,
  ROM speed-table loading, swimming animation, and diving animation. The
  broader entity, room-script, and hardware-ordering gaps remain follow-up
  work.

## Verified ROM save spawn checkpoint — 2026-08-07

- `SaveGameToFile`'s six-byte `wSpawnLocationData` copy is now represented at
  the exact main-block offsets `$35F-$364`: indoor flag, map id, room id, X/Y,
  and `wIndoorRoom`. The raw writer validates byte ranges and leaves adjacent
  progress bytes untouched.
- Room entry transitions retain both host top-left coordinates and the source
  OAM convention (`hLinkPositionX = top-left + $08`,
  `hLinkPositionY = top-left + $10`). New-game and loaded-game startup now
  convert the source fields back through that same boundary instead of treating
  them as renderer coordinates.
- Save and Quit records the active ROM room and Link's current room-entry
  checkpoint. Indoor maps with no ROM layout table preserve the prior
  `wIndoorRoom` byte, matching the source's unmodeled fallback behavior.
  Focused save-image, Link-coordinate, and main-flow tests cover the boundary;
  death-count and photo bytes remain explicit follow-up save fields.

## Verified ROM boomerang producer and runtime — 2026-08-07

- `UseBoomerang` now registers the inventory item against the live room and
  uses the source shared player-projectile gate. Entity `$01` is spawned with
  the ROM coordinate/Z convention, `$28` outbound countdown, direction and
  diagonal pressed-button speed tables, thrown direction, and fresh-projectile
  `unknownTableJ` state.
- The bank-$19 handler now mirrors the four-frame `$4451` OAM list, `$2D`
  cadence-limited noise, `$08` boomerang damage type, outbound-to-return state
  transition, `$08/$20` Link-vector updates, and the alternating-frame Link
  return collision window. The shared modeled projectile count also keeps
  arrows and boomerangs from bypassing each other's ROM item gates.
- Object intersection uses the padded room buffer and the source
  `ApplySwordIntersectionWithObjects` physics ranges, including directional
  ledges and the initial `unknownTableJ` cadence. Outdoors, bush objects
  (`$5C/$D3`) request the source reveal, smoke at the intersected object
  coordinates, and noise `$13`; target hits use the ROM combat table and
  projectile recoil seam. Boomerang display, sound, smoke, launch/return, bush,
  and projectile-gate tests pass against the shipped ROM.
- The clean Java suite passes with 1,075 tests and zero failures, errors, or
  skipped tests. Remaining entity handlers, room scripts, and broader
  hardware-visible ordering remain follow-up work.

## Verified ROM sword-beam producer and runtime — 2026-08-07

- `UseSword` now offers the source's optional projectile branch only when the
  sword is level two, Link's health is full, and the shared active-projectile
  count is zero. Entity `$DF` is spawned with the common player-projectile
  coordinate/Z convention, ROM direction, `$8800`-style ROM rectangle list
  `$19:$44FC`, and the source physics/options flags.
- The bank-$19 handler now applies the direction-specific `$451C/$4520`
  launch offset, doubles the shared `$20/$E0` speed bytes, writes flash `$FF`,
  and emits jingle `$3B` on its initial state pass. Visible state uses the ROM
  rectangle variants and the sixteen-subpixel movement accumulator; common
  entity and `ApplySwordIntersectionWithObjects` paths unload the beam on a
  hit.
- Transient VFX type `$0D` now carries the handler-selected direction through
  the room/session boundary and renders the source two-sprite flicker tables
  `$559C/$55BC`, including the frame/slot parity gate and palette attributes.
  The clean Java suite passes with 1,088 tests and zero failures, errors, or
  skipped tests. Remaining entity handlers, room scripts, and broader
  hardware-visible ordering remain follow-up work.

## Verified ROM Magic Rod producer and fireball runtime — 2026-08-07

- `UseMagicRod` now arms the high-bit `$8E` attack-step window, preserves the
  source side/forward Link OAM timing, applies `func_157C`'s direction update
  at the delayed launch boundary, and spawns entity `$04` only when the shared
  two-projectile gate allows it. The Link auxiliary sprite uses the ROM
  bank-$02 offset, tile, and attribute tables at `$52E0-$5307` and the shipped
  Link character tiles/object palettes.
- The bank-$03 fireball handler now uses the `$69AA` pair, the shared
  `$20/$E0` projectile speed tables, the frame-counter variant cadence, damage
  type `$0A`, common target collision path, `ApplySwordIntersectionWithObjects`
  wall rules, and the `$30` private fire-state countdown with
  `FireSpriteVariants` at `$4C44`.
- Burnable overworld bushes (`$5C/$D3`) and side-scrolling frozen blocks (`$8A`)
  cross the live room boundary through explicit mutation requests. The session
  reveals the ROM object, refreshes tilemap/collision data, emits smoke at the
  source `$08/$10` offset, and queues enemy-destroyed noise `$13`; the runtime
  keeps the projectile active after a burn as in the original handler.
- Focused ROM-byte, countdown, display-list, motion, item-gate, sound, and
  object-request tests cover the slice. A clean Java suite passes with 1,105
  tests and zero failures, errors, or skipped tests. Remaining entity handlers,
  room scripts, and broader hardware-visible ordering remain follow-up work.

## Verified ROM Magic Powder producer and sprinkle runtime — 2026-08-07

- `UseMagicPowder` now has a ROM-backed inventory boundary: it rejects an
  active attack window, allocates entity `$08` before spending the count, plays
  jingle `$05`, clears an empty A/B powder slot, and starts the source `$0E`
  item attack-step countdown. Entity `$08` uses the bank-$18 rectangle list at
  `$7ABA` and the source direction offsets at bank-$20 `$4C3F/$4C43`.
- `MagicPowderSprinkleEntityHandler` now follows the `$17` transition timer and
  `((countdown >> 2) & $07)` animation selection. Its exact computed room cell
  handles outdoor `$5C/$D3` reveals with poof VFX/jingle `$2F`, applies damage
  type `$09` during the final collision window, and handles indoor unlit torch
  `$AB -> $AC -> $AB` transitions through the source `$80` slow timer and
  bursting-flame noise `$12`.
- The room/session boundary mutates the padded room-object buffer and rebuilds
  the ROM tilemap/collision view. The bank-$18 torch pairs at `$795E` and
  `$7962`, sound mappings, source cell math, runtime timers, live overworld
  reveal, and live indoor torch path are covered by focused tests. A clean
  Java suite passes with 1,119 tests and zero failures, errors, or skipped
  tests. The source's already-held-toadstool got-item-dialog branch remains a
  separate UI-state boundary.

## Verified ROM shovel producer and dig/drop runtime — 2026-08-07

- `UseShovel` and `func_002_4B49` now drive a ROM-shaped 24-frame Link item
  window. The animation states come from bank `$02:$4B41`; the poke-vs-dig
  sound uses jingle `$07` or noise `$0E`, and the active item blocks motion and
  facing for the same timer interval.
- `func_002_4D20` is ported with the source direction offsets, padded `$11`
  room-object addressing, physics-table probe, overworld blocker list, and
  indoor-only `$05` dig rule. A successful timer-$10 pass writes hole `$CC`
  to both the live object table and the mutable GBC render overlay, then
  refreshes the affected 2x2 tile/attribute cell and collision view.
- The shovel's post-dig branch consumes the source random bytes, suppresses
  rewards in Eagle's Tower, selects heart `$2D` or rupee `$2E`, starts the
  reward at object `(+8,+16)`, uses the source `$80/$18/$20` timers/speed, and
  applies the ROM infinity-norm vector away from Link. A completed successful
  dig queues Dialog279 when Marin is following.
- Main/RoomSession registration, raw sound routing, ROM table loading, live
  room mutation, reward timing, vector setup, and dialog handoff are covered
  by focused tests. The clean Java suite passes with 1,129 tests and zero
  failures, errors, or skipped tests. Remaining item work includes the other
  unported `UseItem` producers and their room-specific branches.

## Broader parity gaps

The project still needs a systematic pass over the remaining entity handlers,
room interaction scripts, dungeon/boss phases, and hardware-visible ordering
details. These should continue to be implemented from the disassembly with
focused ROM-byte and framebuffer tests; a CPU or Game Boy emulator remains out
of scope.
