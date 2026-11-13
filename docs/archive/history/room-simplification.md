# Room simplification

## Baseline

Scope: world/room, world/transition, world/object main and tests. Counts use
physical lines; null checks are equality/inequality comparisons with null.
Small classes use nonblank, non-import/package lines after stripping comments
(including braces). Numbered tests match `[12]Test` or `Support2`.

```text
main {"files": 155, "lines": 16361, "wildcards": 95, "null_checks": 328, "under40": 67, "numbered": 0}
test {"files": 81, "lines": 18327, "wildcards": 84, "null_checks": 140, "under40": 19, "numbered": 8}
```

## Main consolidation

The parser now owns stream walking, macro expansion and persistent status patches;
these all mutate the same decode state. Object mutations own ground, bomb-wall,
projectile and torch writes to the same room map. Puzzle progression owns block
motion, event triggers, reveals and shutters. Door interactions own indoor key
blocks and dungeon entrances. Story state owns player flag writes and restoration.
Entity simulation owns configuration, ticking and ordered post-tick effects as
one sequence. The former helpers had no independently owned mutable state.

Kept boundaries: loader/pointer tables (ROM acquisition); tilemap/palette decoding
(renderer data); collision queries (live probes and caches); output queues and
ordered drains (ownership); follower synchronization (entity respawning/history);
NPC presentation (sprite overrides); item actions (distinct lift/chest/shovel
state machines); transitions/scrolling (timed state); dungeon/progress/story
state (different persistence); public role views (consumer capabilities); boundary
values (immutable ownership). No class inheritance was introduced.
