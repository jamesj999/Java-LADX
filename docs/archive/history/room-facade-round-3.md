# Room facade round 3

Branch: `t3/refactor-room-entity-facade`. Started by fast-forward merging
`develop` at `b6ab0d60`. The final documentation commit contains this report;
the implementing allocation commit is `1cd18ca8`.

## Checklist status

1. **Complete:** 263 owned references migrated to roles and parameter objects.
   The final compiler-symbol audit finds deprecated member references only in
   four `world/entity` test files. No off-limits main-source edits were needed.
2. **Complete:** compatibility inheritance is deleted. Only the explicitly
   allowed entity-test bridges remain directly on the facade in one section.
3. **Complete:** all four formerly nested records moved to top-level files;
   app and owned tests migrated.
4. **Complete with the coordinator's entity-test exception:** owned constructors
   use one `RoomSessionConfig`; long app combat/pickup overloads are deleted.
   The 11-parameter entity-test combat bridge remains. A seven-argument constructor
   also remains for `SlimeEyeRuntimeTest` and `TarinRaccoonRuntimeTest`.
5. **Audited and deliberately retained:** the private reflection alias is used
   only by the off-limits `SlimeEyeControlsTest`.
6. **Complete:** `MainArchitectureTest` and `CompiledCode` are deleted. Constant-pool
   boundaries and behavioral recording/real-state tests replace all 24 disassembly
   tests, including the 11 in `MainFileMenuFlowTest`. The count increased.
   The [old-to-new mapping](room-architecture-test-migration.md) is also in commit
   `d1334e32`'s message.
7. **Complete:** cached live views; immutable input components reused inside the
   tick boundary; indexed list loops; retained callbacks; allocation-free Link
   merge visitors and reusable read-only reward/combat batches; first-dialog
   selection without a merged list. Empty object channels reuse the first entity
   snapshot. Public snapshot-returning consume APIs preserve immutable ownership.
   Budgets tightened with approximately 25% headroom.
8. **Complete:** architecture, testing guidance and migration inventory updated.

## Metrics

Counts cover room/transition/object main sources unless specified. Public-method
counts exclude constructors and `Object` methods; both declared and effective
public surfaces are shown. Signature counts include explicit constructors and
methods, not implicit canonical constructors of records.

| Metric | Before | After |
| --- | ---: | ---: |
| RoomSession lines | 373 | 333 |
| RoomSession declared public methods | 25 | 39 |
| RoomSession effective public methods including inherited members | 142 | 39 |
| RoomSession inherited public methods | 117 | 0 |
| Deprecated public bridge methods | 128 | 26 |
| RoomSession superclass depth | 1 | 0 |
| Compatibility interfaces/classes | 9 | 0 |
| Nested boundary records | 4 | 0 |
| RoomSession constructors | 4 | 2 |
| Explicit signatures >8 parameters | 4 | 1 |
| Context field reads outside wiring | 0 | 0 |
| Main files >400 lines | 0 | 0 |
| Methods >80 lines | 0 | 0 |
| EntityPhase lines | 166 | 186 |
| Full-suite tests | 2,785 | 2,802 |
| Failures / errors | 0 / 0 | 0 / 0 |
| Existing skipped tests | 1 | 1 |

The declared facade surface is larger because the remaining bridges are now
honestly counted on `RoomSession`. Its 39 methods include all 26 deprecated
bridges. There is no compatibility shelf. `EntityPhase` remains below 300 lines;
new recording ports and batch application make its guarantees executable.

## Allocation

Measured on JDK 21 by the unchanged two headless `SessionAllocationTest` scenarios
using `ThreadMXBean`: 600 warm-up and 600 measured update-only frames, without
framebuffer composition. Baseline is the initial green full-suite run; final
figures are the step-7 full-suite run. Normal JIT/run-to-run variation remains.

| Scenario | Before bytes/frame | After bytes/frame | Old budget | New budget |
| --- | ---: | ---: | ---: | ---: |
| Opening-dialog idle | 4,237 | 4,205 | 8,192 | 5,248 |
| Active gameplay after leaving bed | 18,871 | 15,154 | 24,576 | 18,944 |

Active allocation falls about 20%. Final JFR samples filtered to update/probe
stacks identify audio PCM arrays (`FrameAudioPlayer.update`), entity snapshot
arrays (`RoomEntitySnapshot`), channel wrappers/iterators/captures (`EntityOutbox`),
shared `EntitySpriteDefinition` display data and physics ground samples among
remaining sources. Sampling weights are not precise per-frame attribution.
Those main packages are off-limits this round. Input records remain immutable;
no reused mutable input holder was introduced. Snapshot-returning consumption
still allocates on nonempty retained-result calls; production drains use scoped,
read-only reusable batches instead. Recordings and generated data stay local.

## Remaining exceptions and verification

The exact 26 bridges, constructor, replacements and caller files are in the
[current migration inventory](room-facade-migration.md). The only caller files are:

- `java/src/test/java/linksawakening/world/entity/SlimeEyeControlsTest.java`
- `java/src/test/java/linksawakening/world/entity/SlimeEyeRuntimeTest.java`
- `java/src/test/java/linksawakening/world/entity/SlimeEyeSessionTest.java`
- `java/src/test/java/linksawakening/world/entity/TarinRaccoonRuntimeTest.java`

All required commit gates used `LA_ROM` and `./gradlew spotlessApply test`.
Last full-suite tally: **2,802 tests, zero failures/errors, one existing skip**.
No off-limits main/test sources, opening fixtures, ROMs or assets were edited;
no native window was launched, screen recorded, push made or other branch changed.
