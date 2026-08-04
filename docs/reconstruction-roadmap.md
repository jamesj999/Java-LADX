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

The complete Java test suite passes after these runtime increments. Remaining
entity behavior—including the rest of the enemy damage matrix, recoil,
stun/lift/throw/burning/death handlers, dynamic display rectangles, scripted
spawns, and followers—is intentionally still unsupported rather than
represented by guessed shapes or generic movement.

## Next entity increments

1. Port simple enemy movement, collision, damage, stun, lift, throw, burning,
   and death transitions using the existing room collision model.
2. Add rectangle and dynamically selected sprite helpers, entity tile offsets,
   follower overrides, and the Color Dungeon's separate entity-tile loader.
3. Port scripted spawns, followers, room events, drops, and boss/multi-entity
   state machines from the corresponding banked handlers.

## Broader parity gaps

The project still needs a systematic pass over the remaining entity handlers,
room interaction scripts, dungeon/boss phases, and hardware-visible ordering
details. These should continue to be implemented from the disassembly with
focused ROM-byte and framebuffer tests; a CPU or Game Boy emulator remains out
of scope.
