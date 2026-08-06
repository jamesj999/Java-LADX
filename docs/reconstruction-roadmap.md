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
  and active combat are covered in the verified increment below, while Ghini
  hiding/flight and Hardhat movement/collision states remain pending.
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
  cases and player projectile interactions are not yet complete. The broader
  engine remains a staged reconstruction, not a complete entity-system claim.

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
- The first `$30` falling frames still defer each entity family's active
  handler and wall-collision rollback, and the renderer has not yet applied the
  separate phase-$02 visual-Y `+4` correction to its final OAM coordinate.

## Next entity increments

1. Port remaining simple enemy movement, collision exceptions, lifting, and
   throwing behavior, plus entity-specific burning/death presentation, using
   the existing room collision model.
2. Extend rectangle and dynamically selected sprite handlers, complete entity
   tile-offset state, follower history and special states, and the remaining
   Color Dungeon symbol/animation path.
3. Port scripted spawns, followers, room events, drops, and boss/multi-entity
   state machines from the corresponding banked handlers.

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
  `$86` toadstool-status unload branch, `DidKillEnemy`/drop persistence, and
  floating-item wave-audio delivery remain explicit deferrals. The existing
  event boundary carries only the modeled pickup state and does not invent
  those semantics.

## Broader parity gaps

The project still needs a systematic pass over the remaining entity handlers,
room interaction scripts, dungeon/boss phases, and hardware-visible ordering
details. These should continue to be implemented from the disassembly with
focused ROM-byte and framebuffer tests; a CPU or Game Boy emulator remains out
of scope.
