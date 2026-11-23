# ROM-Backed Color Shell Runtime Design

## Goal

Port the Color Shell family (`$E9` red, `$EA` green, `$EB` blue) from
`bank36.asm` into the active Java room runtime. The increment covers the
source handler's state machine, fixed-point movement, state-dependent ROM
rectangle display lists, room-object puzzle checks, and the observable room
side effects needed by the puzzle. It must preserve the existing shared
combat/status path: a stunned shell is still non-interactive until its ROM
countdown expires, and the bank-$36 state handler is not called while the
entity has a non-active status.

This is an entity-family increment, not a generic Game Boy interpreter. The
Java runtime owns explicit per-slot state shaped like the bank-$36 WRAM
tables, while ROM bytes remain the source for display lists, direction data,
object IDs, and raw sound/VFX IDs.

## Alternatives considered

1. **Dedicated source-shaped handler (selected).** Add a `ColorShellMotion`
   helper with per-slot state and fixed-point accumulators, decode its six ROM
   rectangle list families in `EntitySpriteHandlerCatalog`, and pass a small
   room-world side-effect boundary from `RoomSession`.
2. **Generic bank handler interpreter.** Model the assembly dispatch and WRAM
   table accesses as a bytecode/interpreter layer. This would cover future
   banks uniformly, but it would obscure the state-specific contracts and add
   an emulator-shaped abstraction to a renderer/gameplay runtime that is meant
   to remain source-driven.
3. **Behavior-only patch.** Add a generic wandering movement helper and leave
   Color Shell art and puzzle states unsupported. This would change ordinary
   motion but would not reproduce the ROM handler's state transitions or make
   the color puzzle playable.

The selected approach keeps the handler's state boundaries visible and makes
unsupported external systems explicit through events/callbacks rather than
silently dropping ROM writes.

## ROM source of truth

- `LADX-Disassembly/src/code/entities/bank36.asm:6130-6157` dispatches the
  three Color Shell types through `ColorShellState0Handler` through
  `ColorShellStateDHandler` after the dynamic renderer and common combat
  gate.
- `bank36.asm:6159-6341` implements the random-direction idle state, bounded
  walking state, Link-proximity charge, two-state jump/landing loop, and the
  frame-cadenced sprite variants. The direction speed tables are
  `Data_036_6735 = [$03,$FD,$00,$00]` and
  `Data_036_6739 = [$00,$00,$FD,$03]`; the charge direction remap is
  `Data_036_67B1 = [$02,$03,$01,$00]`.
- `bank36.asm:6188-6224` clamps shell position to X `$16..$89` and Y
  `$1E..$72` after background interaction.
- `bank36.asm:6479-6524` places a failed or completed puzzle shell at one of
  the four fixed coordinates derived from X `< $50` and Y `< $48`, then uses
  Z speed `$10` and the common fixed-point Z update.
- `bank36.asm:6526-6737` performs the three color-specific room-object
  comparisons, writes solved/failed/final object IDs, emits door-unlock or
  wrong-answer sound IDs, spawns transient poof `$02`, and unloads the solved
  shell through `DidKillEnemy`.
- `bank36.asm:7099-7180` defines unsigned wrapped X/Y proximity differences
  and the padded-room object lookup:
  `((y - $07) & $F0) | ((x - $01) & $F0)` relative to `wRoomObjects + $11`.
- `bank36.asm:7220-7230` toggles the active sprite variant every eight frames;
  the jump/landing states use the handler's every-other-frame toggle and
  `Data_036_67B1` remap.
- `bank36.asm:6739-6819` selects the active eight-variant lists for states
  below `$06` while status is active, and the inactive four-variant lists for
  all other states/statuses. The list pointers are bank-$20 data:
  `Data_020_6688`, `$66E8`, `$6748`, `$67A8`, `$67D8`, and `$6808`, each with
  three rectangle sprites per variant.

## Runtime state model

`ColorShellMotion` owns one entry per entity slot for the bank-$36 values:

- state, transition countdown, direction;
- signed-byte X/Y/Z speeds and their 4-bit fixed-point accumulators;
- current sprite variant;
- physics flags needed for the ROM harmless/mask writes;
- an initialized bit so room-load `INIT` dispatch does not consume a random
  byte before the first active handler frame.

The existing runtime loop continues to decrement the shared combat/status
countdowns before dispatch. On the first `INIT` frame it initializes the
Color Shell helper. On later active frames it calls the helper and stores the
returned position/Z/type/variant in the room entity. `DYING`, `BURNING`, and
`STUNNED` remain handled by the existing status lifecycle before family motion
is considered.

The helper will use the same fixed-point add and infinity-norm Link-vector
division already used by the ported entity helpers, with the bank-$36 signed
byte behavior retained explicitly. Background collision uses the existing
`RoomEntityBackgroundCollision` contract and then applies the exact bank-$36
position clamp.

## State behavior

- **State 0:** if its transition countdown is zero, consume one ROM random
  byte, select direction `(random & $06) >> 1`, set countdown `$40`, and enter
  state 1. While outside `$30` in either wrapped axis, it remains state 0;
  otherwise it enters state 1.
- **State 1:** load the direction speed tables, move/collide/clamp, and when
  its transition countdown expires restart state 0 with countdown `$10`.
  If both wrapped Link distances are below `$20`, apply a length-$0E vector
  toward Link, set countdown `$20`, and enter state 2. Toggle the active
  eight-frame visual variant on `frame & 7 == 0`.
- **States 2/3:** wait through the `$18` charge/landing countdown, toggle
  variants every even frame, and use the ROM remap when variant returns to
  zero. State 3 restores state 1 with zero XY speed when its countdown ends;
  both states perform movement/collision/clamp. A nonzero ignore-hits timer
  sets the harmless flag and moves to state 4.
- **States 4/5:** clear the ignore-hits timer, then keep the harmless flag
  while a hit-ignore timer remains. State 5 returns to state 1 and clears the
  harmless bit only when the entity status is below stunned (`$06`).
- **States 6/7:** relocate to the ROM-selected corner, set Z speed `$10`,
  decrement Z speed each frame, and advance after the signed-Z crossing.
- **States 8/9/A/B:** compare/write the padded-room puzzle object, apply the
  failed-answer horizontal launch and Z arc, then return to walking with the
  harmless bit cleared after the ROM `$20` delay.
- **States C/D:** wait until every active Color Shell has reached state C or
  later, write the color-specific solved object, wait `$18`, write the final
  object, emit poof `$02`, and unload the entity. The scan uses all sixteen
  runtime slots and preserves the ROM status/state predicates.

## Room-world side effects

The runtime will expose a narrow `ColorShellWorld` callback boundary rather
than directly coupling entity motion to `ActiveRoom` or audio/render classes.
The boundary provides:

- the current padded room-object value at a shell position;
- replacement of that room-object value, with the padded `$11` offset and
  row stride preserved;
- raw jingle/noise requests (`$1D` wrong answer and `$04` unlock);
- a transient poof request with ROM world coordinates; and
- an unload request for the solved entity.

`RoomSession` supplies this boundary from the active room and existing
sound/VFX integration points. Tests can provide a deterministic fixture
without constructing OpenGL or a full room renderer. Room-object writes are
also reflected in the room collision backing array; no fabricated tilemap or
palette data is introduced. If the current renderer cannot yet refresh a
live BG map for a write, the write still remains authoritative for subsequent
collision/room loads and is recorded as an explicit integration item rather
than being silently ignored.

## Dynamic ROM display lists

Add a catalog method that selects one of the six bank-$20 rectangle sources by
color and by `(status == ACTIVE && state < $06)`. Active sources decode eight
variants × three sprites; inactive sources decode four variants × three
sprites. `EntityRoomLoader` uses the inactive definition for the initial
`INIT` snapshot so a Color Shell is renderable before its first runtime tick.
`RoomEntityRuntime` refreshes the definition when the status/state crosses
the active/inactive rendering boundary. The existing rectangle renderer
already handles signed offsets, tile `$FF` hidden entries, palette/flip
attributes, and entity tile offsets, so no second rendering primitive is
needed.

## Compatibility boundaries

- Existing constructors and no-ROM runtime fixtures remain valid; when no ROM
  catalog or world callback is available, Color Shell motion still advances
  with unsupported art and no external side effect.
- The shared combat pass remains responsible for `$FE` burning and `$FF`
  stunned statuses. Color Shell state motion is skipped while those statuses
  are non-active, matching `ReturnIfNonInteractive_36` and the Java status
  gate.
- This increment does not add Power Bracelet lifting/thrown physics, fire
  sprite animation during burning, or a generic poof sprite-sheet renderer
  beyond emitting the ROM event to the existing transient-VFX boundary.
- It does not replace `OverworldTilesetTable`, room object parsing, or the
  GBC overlay path. The shell puzzle reads the already-decoded padded object
  grid exactly as the disassembly does.

## Verification

Tests will cover:

1. All three color definitions select the correct bank-$20 active and
   inactive rectangle lists, including variant counts and hidden OAM tiles.
2. State 0 random direction/countdown, state 1 table-driven movement and
   clamp, proximity charge, variant cadence, and the state 2/3 landing loop.
3. Ignore-hit/harmless state 4/5 behavior and the fixed state 6/7 Z arc.
4. Correct/incorrect state-8 object checks, color-specific writes, raw sound
   events, all-shell state-C gate, final object writes, poof request, and
   unload request.
5. Runtime definition refresh across active and inactive state/status paths,
   slot cleanup/reuse, existing combat/status/projectile behavior, and the
   complete `gradle clean test` suite.

