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
  path are covered. The shared rock projectile spawn remains unsupported.
- Moblin now reads its bank-$03 eight-variant display list and runs through
  the same ordinary roaming-enemy countdown, direction, fixed-point movement,
  and collision path. Its normal hitbox, health-group `$01`/two-health
  sword path, `$04` contact damage, `$18` flash, and `$0A` ignore-hit window
  are also ROM-backed. Its direction-gated arrow spawn and recoil/damage
  branches remain pending.
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
  `$0B` path (four health and `$08` contact damage) is wired; recoil, shield/
  sword-clink state, water behavior, and the full collision table remain
  pending.
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
entity behavior—including roaming-enemy projectiles, the rest of the enemy
damage matrix, recoil, stun/lift/throw/burning/death handlers, dynamic
display-list selection beyond the follower path, scripted spawns, and
history-driven follower handlers—is intentionally still unsupported rather than
represented by guessed shapes or generic movement.

## Next entity increments

1. Port simple enemy movement, collision, damage, stun, lift, throw, burning,
   and death transitions using the existing room collision model.
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
