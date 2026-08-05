# Spark bank-$06 recoil design

## Context

Spark entities `$16` (counter-clockwise) and `$17` (clockwise) already have
ROM-backed display lists, initialization offsets, orbit direction tables,
background collision probes, movement, and shared combat metadata in the Java
room runtime. Their source handler in
`LADX-Disassembly/src/code/entities/06_spark.asm` calls
`ApplyRecoilIfNeeded_06` immediately after `ReturnIfNonInteractive_06` and
before the Spark movement/update sequence. The Java runtime currently has the
shared `EnemyRecoilMotion` and bank-$06 collision policy, but its type boundary
omits both Spark types.

## Source contract

- A normal sword collision configures the existing ROM `$30` vector-away recoil
  and the normal `$0A` ignore-hits countdown before applying sword damage.
- Each active Spark frame consumes one ignore-hit count and applies one shared
  fixed-point recoil step before `SparkMotion.advance` runs.
- Spark is a bank-$06, non-roaming handler. A blocked recoil step must therefore
  preserve the recoil state/countdown rather than use bank-$03's
  `StopEntityRecoilOnCollision` behavior.
- `hActiveEntityNoBGCollision` remains outside this slice's new boundary. The
  existing Spark-specific background/orbit behavior is not replaced or
  generalized here, and recoil smoke remains deferred.

## Options considered

1. Add `$16` and `$17` to `RoomEntityRuntime.usesBank6Recoil`, with focused
   runtime tests. This reuses the already verified shared vector, accumulators,
   countdown handling, and bank-specific block policy while changing only the
   missing source type boundary.
2. Refactor all entity background behavior around an explicit
   `hActiveEntityNoBGCollision` equivalent before adding Spark recoil. This is
   more comprehensive but mixes Spark recoil with unrelated collision-policy
   changes and risks changing existing Spark movement tests.
3. Create a Spark-specific recoil implementation. This duplicates the shared
   bank-$06 algorithm and would make the Java behavior less faithful to the
   common ROM helper.

Option 1 is selected because it closes the exact missing call-path boundary
without inventing a second recoil model or changing established Spark motion.

## Design

`RoomEntityRuntime.usesBank6Recoil` will include `ENTITY_SPARK_COUNTER_CLOCKWISE`
and `ENTITY_SPARK_CLOCKWISE`. No new state holder or `RoomEntity` fields are
needed. The existing combat resolver will configure recoil for a Spark sword
hit, `applyEnemyRecoilIfNeeded` will apply it before `SparkMotion.advance`, and
the existing non-roaming argument will preserve bank-$06's no-stop-on-block
behavior.

The type boundary remains separate from the `supportsEnemyCollision` and
Spark movement predicates. This prevents non-combat Spark updates from
creating recoil state and keeps future no-background and special damage work
isolated.

## Tests

Extend `RoomEntityRuntimeTest`'s bank-$06 recoil regression with both Spark
types. For each type, assert that an overlapping sword hit produces a normal
combat event, configures recoil speed `$D0/$D0` for the diagonal fixture,
starts the `$0A` ignore-hit window, and leaves recoil active. Retain the
existing Keese, Tektite, Anti-Fairy, and aggressive Stalfos assertions as
regressions for the shared predicate.

The focused runtime suite and a clean complete Java suite must pass. The
roadmap will state that Spark recoil is covered while Spark's no-background
collision details, recoil smoke, and remaining damage-state behavior remain
deferred.

## Scope boundary

This slice does not change Spark's orbit tables, background collision probes,
rendering, status lifecycle, or player collision. It does not claim complete
Spark parity; it only makes the source's existing bank-$06 sword-recoil call
reachable through the live runtime.
