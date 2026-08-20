# Bottle Grotto Genie Entry Design

## Goal

Continue the ordered Bottle Grotto route from the room `$25` return point to
room `$2B`, then implement only the source-authored Genie intro and initial jar
state. The full Genie fight remains a separate milestone.

## Route

Follow the authored passage from the existing room `$25` return point: re-enter
`$3B`, traverse horizontally into side-view room `$3A`, take `$3A`'s E1 exit to
room `$2D`, then use the already-earned Nightmare Key on the north boss door to
enter `$2B`. Room `$27` must retain its opened Nightmare Key chest, status bit
`$10`, and one Nightmare Key until that door consumes it.

Room `$2A` is the post-boss instrument room. It must not be used as a pre-boss
shortcut: its instrument entity, event `$21`, shutters, and E0 overworld warp
belong to dungeon completion after Genie.

## Genie Boundary

Room `$2B` loads one `ENTITY_GENIE` (`$5C`) at authored entity coordinates
`(2,4)`, yielding ROM position `$48/$30`. Integrate the existing `BossIntro`
timing for map `$01`: after its delay, emit boss music `$19` and dialog `$B4`
once.

Implement the initial private-state-0 jar behavior from
`04_genie.asm:$4000-$4068` only:

- normal-build jar threshold is `$03`;
- while below the threshold, entity health is held at `$20`, with harmless jar
  physics/hitbox state;
- reaching the threshold spawns the Genie body at the jar X and `Y-$18`, with
  private state `$02`, countdown `$27`, and health `$08`;
- the jar smash uses the source spawn/smash coordinates and emits break noise
  `$29`.

Do not route Genie through generic zero-health enemy teardown, and do not claim
fireballs, later body states, death, room completion, or instrument reward in
this milestone.

## Verification

Extend the ordered route through `$3B → $3A → $2D → $2B`; add focused Genie motion and
runtime tests for intro one-shot behavior, jar threshold, body spawn fields,
smash/noise, slot exhaustion, and state cleanup. Full suite and source review
must pass before commit.
