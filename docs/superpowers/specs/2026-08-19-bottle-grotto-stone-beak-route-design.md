# Bottle Grotto Stone Beak Route Design

## Goal

Continue uninterrupted play after the room `$37` Compass by returning to
cleared room `$31`, unlocking its west door, clearing room `$30`, and obtaining
the Stone Beak from room `$2E`.

## Route evidence

`MapLayout1` adjacency does not imply a usable door. Runtime collision proves
that room `$37` has neither east nor west access, room `$31`'s west edge is a
locked door, and room `$34`'s settled `$A6` partition blocks its southern route
from the current side. The valid route is `$37 -> $32 -> $31`, followed by the
source key door into `$30` and the northern exit into `$2E`.

`IndoorsA31` places locked-left-door macro `$EE` at `$30`. It expands to the
left-door pair `$31/$32`, uses physics `$92`, collision bit `$04`, and consumes
the one remaining Small Key. Its eight-frame animation writes `$09/$0A`, sets
room `$31`'s left status `$02`, and mirrors room `$30`'s right status `$01`.
Room `$31`'s torch event was already completed earlier in the same trace.

`IndoorsA30Entities` contains four excluded spike traps and two Keese. Its
event `$21` is kill-all plus open locked doors, so only the Keese participate.
Their ordinary combat/death completion opens the north shutter without
persisting a chest or key reward. `MapLayout1` then leads north to room `$2E`.

`IndoorsA2E` contains one Hardhat Beetle, holes/trench objects, and static chest
`$A0` at `$24`. Its indoor-A chest entry is `CHEST_STONE_BEAK`. Link must use
collision-valid movement and the existing Roc's Feather traversal where the
trench requires it. The Hardhat uses its generic shield/knockback and pit-death
behavior; no direct health mutation or room-specific shortcut is allowed.

## Chosen scope

The selected slice covers the complete Stone Beak route: key-door persistence,
room `$30` kill-all shutter behavior, room `$2E` traversal, Hardhat interaction,
and the full Stone Beak chest lifecycle. Stopping at the west door would not
prove the key unlock leads anywhere; extending through the Hinox route would
combine a distinct miniboss milestone with this item route.

## Runtime and testing

Extend the existing fresh-game ordered regression. Every semantic interaction
starts from a path found against the live collision map, followed by one actual
contact step when a door or chest requires it. All animation, recovery, death,
event, and chest waits are bounded and include useful diagnostics.

Existing generic APIs are expected to cover the route. If the ordered test
produces a source-backed RED, retain the failing assertion and make the smallest
shared runtime correction under TDD. Do not add room-id conditionals.

Verification includes the ordered test, focused key-door/Keese/Hardhat/chest/
transition suites, the clean Java suite with XML totals, `git diff --check`, and
separate source-fidelity and code-quality reviews. The Hinox room `$28` route is
the next milestone after this slice.
