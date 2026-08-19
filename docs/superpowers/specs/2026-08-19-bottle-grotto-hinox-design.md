# Bottle Grotto Hinox Ordered-Play Design

## Goal

Continue the uninterrupted Bottle Grotto trace north from the Stone Beak room
through room `$2C`, defeat Hinox in room `$28` through its ROM-authored handler,
and activate the miniboss warp only after the clear event resolves.

## Route and room source

The collision-valid next route from room `$2E` is north to `$2C` and north
again to `$28`; the attempted detour back through `$34` is blocked on both exits
by its solid `$A6` partition. Room `$2C` has event `$00`, a Keese, a
counter-clockwise Spark, and an excluded floating item. These are hazards, not
progression gates.

`IndoorsA28Entities` places Hinox `$89` at location `$25` and warp `$61` at
`$34`. `DungeonEventsTable[$28]` is `$C1`, the miniboss-clear event. The warp
must not function as an ordinary available shortcut while the live miniboss
event remains unresolved; after normal Hinox death and event persistence it
uses the existing dungeon miniboss-warp path toward room `$36`.

## Hinox runtime behavior

Mirror `src/code/entities/06_hinox.asm` as a reusable entity handler, not a
room-specific script. Hinox is a big miniboss using health group `$14`, normal
combat/recoil integration, and six private states:

1. Initial movement uses the ordinary damage path, moves with current speed,
   and changes to wandering after collision or its randomized timer expires.
2. Wandering selects cardinal movement and timers; alternating cycles may
   begin the charge wind-up.
3. Charge wind-up lasts `$30` ticks, then derives an `$18`-magnitude vector
   toward Link and emits bounce jingle `$20` at the source cadence.
4. Charge approach moves at that vector, creates dust, and enters grab state
   when Link is inside the source proximity box and in default motion.
5. Grab/throw blocks Link motion, holds Link at the source offsets, then at
   countdown `$20` applies horizontal throw speed `$E0` or `$20`, vertical
   speed `$20`, Z velocity `$10`, airborne state `$02`, jingle `$08`, and
   `$08` Link-health damage before returning to the initial state.
6. A damaging sword hit that reaches flash countdown `$03` while below grab
   state enters the bomb-throw state; at countdown `$10` it spawns bomb entity
   `$02` with the source offset and airborne throw values, then returns.

Use the runtime's existing deterministic RNG, entity slots, combat events,
motion-block requests, Link-effect events, bomb entity support, and generic
death/event paths. If a missing shared event type is required to carry the
throw effect to `Link`, add the smallest general value object/API; do not mutate
test-only Link state or special-case room `$28`.

## Scope boundaries

The slice includes route validation, focused deterministic coverage of all six
states, live sword damage and recovery, bomb spawning, grab/throw effects,
ordinary death, `$C1` persistence, and post-clear warp use. It does not enter
room `$29`, collect later dungeon items, or implement unrelated bosses.

## Verification

Require a RED/GREEN focused Hinox suite, the uninterrupted ordered trace,
focused combat/event/warp suites, `gradle clean test`, XML test totals,
`git diff --check`, and separate source-fidelity then code-quality reviews.
Every random-state assertion must control the existing RNG, and every wait must
have a deadline plus terminal assertion.
