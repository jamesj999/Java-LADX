# Bottle Grotto Room `$38` Source Route Design

## Goal

Continue the uninterrupted post-Stone-Beak trace through the source-authored
lower route, collect room `$38`'s Small Key, and reach room `$35` from the east
side of room `$34`'s permanent partition.

## Root-cause correction

`MapLayout1` places `$33` directly north of `$38` and `$34` directly north of
`$39`. Room `$2E` has no north opening, and room `$34`'s `$A6` column is neither
switch-controlled nor pushable. The two failed direct approaches were therefore
correctly rejected by live collision.

The valid route is `$2E -> $30 -> $31 -> $32 -> $33`, strike `$33`'s live
crystal switch so the shared state changes `$00 -> $02`, then cross south into
`$38`. After collecting `$38`'s Small Key, cross east to `$39`, north to `$34`
on the partition's east side, and east to `$35`. This uses only map adjacency,
room openings, and normal switch collision.

## Room `$38`

`IndoorsA38` contains chest `$A0` at `$43` and switch blocks `$DB`.
`IndoorsA38Entities` contains Moblin Sword `$14` at `$62` and crystal switch
`$66` at `$45`. Its event is `$00`, so combat is optional and cannot gate the
chest. The indoor-A chest table selects `CHEST_SMALL_KEY` `$1A`.

The ordered trace must use live collision and the complete chest lifecycle,
observe the ROM dialog/reward, persist the room chest bit, and finish in room
`$35` with exactly one Small Key. Room `$35`'s north key door and room `$2F`'s
push-block staircase are the next distinct slice.

## Verification

Require the extended ordered regression, focused switch/chest/transition
suites, `gradle clean test`, XML totals, `git diff --check`, and independent
source-fidelity then code-quality reviews. No direct switch-state assignment,
room relocation, object mutation, or room-id production exception is allowed.
