# Bottle Grotto Room `$2F` Staircase Route Design

## Goal

Continue the uninterrupted trace from room `$35`, spend room `$38`'s Small Key
on the north door, and solve room `$2F`'s two-block staircase event.

## Source behavior

`IndoorsA35` places `OBJECT_KEY_DOOR_TOP` `$EC` at `$07`. Opening it consumes
one key, runs the eight-frame door animation, and mirrors directional status to
room `$2F`'s south door.

`IndoorsA2F` contains `$A7` blocks at `$33/$36`, hidden stairs `$BF` at `$18`,
and event `$A7` (`TRIGGER_PUSH_BLOCKS | EFFECT_REVEAL_STAIRWAY`). The source
solution pushes both blocks inward once. The first settles as `$A6`; the second
settles horizontally adjacent and resolves trigger `$07`. The ordinary reveal
effect persists room completion and replaces `$BF` with active stairs `$BE`.

## Runtime constraints

Use the existing door, collision, pushed-block motion, event, and persistence
paths. A single block, vertical adjacency, or a block still in motion must not
resolve the event. Do not special-case room `$2F` or its coordinates in
production. End after the staircase is revealed; side-view room `$3F` is the
next milestone.

## Verification

Require focused RED/GREEN trigger coverage, the uninterrupted ordered trace,
door/push/event/transition suites, clean full tests, diff checks, and independent
source then quality reviews.
