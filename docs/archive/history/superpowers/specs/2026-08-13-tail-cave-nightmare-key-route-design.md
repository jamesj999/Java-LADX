# Tail Cave Nightmare Key Route Design

## Goal

Continue the fresh-game ordered runtime sequence after Roc's Feather to the
Tail Cave Nightmare Key, then prove that the acquired key opens the boss door.
The route and interactions must come from the indoor layout, room object data,
physics table, chest table, and door logic in the disassembly.

## Source route

`MapLayout0` places the post-Feather route at `$1D -> $1C -> $01`, through
room `$01`'s staircase warp to side-view `$18`, then `$19 -> $03`. From `$03`,
the route is east to `$04`, south to `$07`, south to `$0D`, east through
`$0E/$0F`, and north to `$09`. Room `$09` contains keyhole block `$DE` at
location `$52`; bank 2 requires `$20` sustained contact frames and one Small
Key, then reveals floor `$0D` and persists room-status event bit 3. Link can
then leave west into `$08`. `IndoorsA08` places the closed chest object `$A0`
at location `$14`, and the room-$08 chest-table entry is
`CHEST_NIGHTMARE_KEY`.

The Small Key needed at `$09` comes from room `$16` immediately west of the
entrance. Its event `$81` is a kill-all/drop-key event. The ordered test knocks
both Hardhat Beetles into the north pit with live sword recoil, lets the shared
fall handler finish, waits for the ROM-positioned key drop to land, and collects
it through normal pickup cadence.

Room `$04`'s south route is separated by the ROM-authored pit field. The ordered
test proves the south edge is not ground-reachable, then invokes the real Roc's
Feather behavior and ticks real Link movement/jump/collision state until Link
lands safely beyond the mandatory barrier. Room `$07` itself is traversed south
on ordinary ground; the apparent direct east route is not the source route.

After opening the chest, the test will tick the chest entity and consume the
same reward event consumed by `Main`. That event updates the live dungeon-item
state; the test will not restore or edit Nightmare Key flags directly. It then
leaves east through `$09`, uses the Feather to cross the `$B0` pit on the
return route, and goes south to `$0F`. The already-opened `$0F/$10`
key-door pair leads east to miniboss room `$11`. The live
Rolling Bones combat/destruction result must clear that room before Link can
move north to `$0B` and open the boss door using the acquired key.

## Test-harness correction

The generic indoor connectivity helper treats normal pits as traversable. It
remains useful for locating geometric exits because it intentionally abstracts
several fine-collision shapes, but it cannot prove a Feather gate. A dedicated
strict path check treats pits as unavailable ground in room `$04`, and the
required pit traversal there is covered by actual `Link.update()` movement
while airborne. Room `$09`'s keyhole-block approach is ordinary ground once the
block is removed, but returning from its west entrance to the south boundary
requires a second live Feather jump across object `$B0`.

## Scope

This slice ends after live Rolling Bones completion and after the room-$0B boss
door has opened with synchronized room status. Moldorm combat and the instrument
sequence already have focused runtime coverage and remain the next ordered-play
integration slice.
