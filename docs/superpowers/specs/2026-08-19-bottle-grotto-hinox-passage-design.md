# Bottle Grotto Hinox Passage Design

## Goal

Continue from room `$2F`'s revealed stairs through the side-view passage and
enter Hinox room `$28` through the source-authored one-way route.

## Source route

`IndoorsA2F` warp `E2 01 3F 88 10` enters side-view room `$3F`. `MapLayout1`
places `$3E` immediately west of `$3F`; the passage crosses that live horizontal
boundary. `IndoorsA3E` warp `E1 01 2C 78 70` returns to indoor room `$2C`.
Room `$2C` contains Keese `$19`, counter-clockwise Spark `$16`, and excluded
floating item `$6E`; its north one-way door leads to room `$28`.

Room `$28` must load event `$C1`, Hinox `$89` at `$25`, and warp `$61` at
`$34`. This slice ends on entry before combat so side-view traversal and the
new miniboss handler remain independently diagnosable.

## Runtime constraints

Use the active stairs warp, side-view scrolling/collision, ROM warp records,
and ordinary indoor boundary handling. Do not assign room/category or Link
coordinates. Every transition has a deadline and asserts category, room, and
ROM destination coordinates.

## Verification

Require the ordered regression, focused side-view/warp/transition suites, clean
full tests, diff checks, and source then quality reviews.
