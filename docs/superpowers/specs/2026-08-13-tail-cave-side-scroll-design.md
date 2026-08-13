# Tail Cave Staircase and Side-Scroll Design

## Goal

Extend the ordered Tail Cave playtest from room `$03` through its revealed staircase into side-scrolling room `$19`, then back to room `$03`, using the transition rules in the disassembly rather than invented warp anchors.

## Source behavior

Indoor objects `$BE`, `$BF`, and `$CB` run `.configureStairs` in `bank0.asm`. This stores the staircase center and initializes `hStaircase` to `STAIRCASE_INACTIVE`. The type-`$04` transient VFX uses `func_002_5F5C` to do the same when room `$03`'s stairs appear at location `$18`.

`renderTranscientVFXs` in `bank2.asm` advances an inactive staircase to active only after Link leaves a 12-by-12 area around its center. An active staircase triggers warp 0 when Link enters the inner 10-by-10 area while grounded and not carrying an object. This is separate from `wWarpPositions`; the stairs must not be assigned a guessed door tile.

Room `$03` warp 0 is `E2 00 19 78 10`: category 2, map `$00`, room `$19`, landing at Java top-left `(0x70, 0x00)`. Room `$19` warp 0 is `E1 00 03 88 20`: category 1, map `$00`, room `$03`, landing at `(0x80, 0x10)`.

`CheckPositionForMapTransition` sends an ordinary side-scrolling room through `ApplyMapFadeOutTransitionWithNoise` when Link reaches a vertical edge. Therefore room `$19` returns through warp 0 at the top edge; it does not scroll to an adjacent indoor room.

## Runtime design

`RoomObjectParser` will record the final staircase location while it processes the object stream, just as repeated `.configureStairs` calls overwrite the single HRAM staircase position in the source. `RoomObjectParseResult`, `LoadedRoom`, and `ActiveRoom` carry that location without conflating it with a door warp tile. `RoomSession` owns the live `NONE/INACTIVE/ACTIVE` state because it already owns room loading and room `$03`'s reveal mutation. Revealing `$BE` configures location `$18` immediately.

The session exposes one transition query that accepts Link's ROM entity coordinates, Z, and carrying state. It first performs the inactive-to-active leave check and then, on later calls, returns warp 0 only when the active staircase's trigger conditions match. Loading another room clears and reconstructs the state.

`RoomBoundaryController` will distinguish vertical category-2 exits from ordinary indoor scrolling with a dedicated decision. `RoomTransitionCoordinator` will apply the room's first warp for that decision. Horizontal category-2 boundaries remain ordinary room transitions because that is the generic source path; this slice only needs the vertical warp behavior used by room `$19`.

## Ordered verification

The existing fresh-game runtime sequence will continue after the beetles reveal the stairs. It will prove:

- standing on newly appeared stairs does not immediately warp;
- stepping outside the 12-pixel activation box arms the staircase;
- returning inside starts a fade and lands in category-2 room `$19` at the ROM-derived coordinates;
- reaching room `$19`'s top edge starts a fade through warp 0, rather than indoor scrolling;
- the return lands in room `$03` at the ROM-derived coordinates with the hidden staircase restored from room status.

Focused controller/session tests will cover the state boundaries so the long ordered regression remains readable.

## Non-goals

This slice does not yet implement complete side-scrolling Link gravity, ladders, every exceptional room in `CheckPositionForMapTransition`, or the next Tail Cave combat/reward room. It establishes the ROM-authored transition graph required to continue ordered playtesting.
