# November 2026 quality effort

Historical reports describe their checkpoint, not current requirements. Current
contracts live in ARCHITECTURE.md, RENDERING.md, BUILDING.md and TESTING.md.
No file-length targets or implementation inheritance are part of the design.

| Report | Scope |
|---|---|
| [Composition redesign](composition-redesign.md) | Link/player, GPU/audio and other implementation chains replaced by composed state owners; inheritance guard |
| [Simplification round](simplification-round.md) | Non-world cohesion, explicit imports and slice consolidation |
| [Room simplification](room-simplification.md) | Room responsibilities, descriptive tests and single-representation boundary records |
| [Render unification](render-unification.md) | One retained scene pipeline, screen sections, indexed parity and allocation evidence |
| [Non-null collaborators](non-null-collaborators.md) | Constructor/no-op policies and the original app wiring hand-offs |
| [App collaborator lifecycle](app-collaborator-lifecycle.md) | Final per-class/site null classification; screen/death states, eager debug tools and boundary normalization |
| [Room lifecycle](room-lifecycle.md) | RoomRuntime publication, generation ownership and the no-room operation matrix |
| [Room null audit](room-null-audit.md) | Earlier room lifecycle classification and intentional optional data |
| [Room facade migration](room-facade-migration.md) | Former delegates and their role-view replacements |
| [Room facade round 3](room-facade-round-3.md) | Compatibility-chain deletion and external caller consolidation |
| [Architecture-test migration](room-architecture-test-migration.md) | Old bytecode assertions mapped to dependency and behavioral tests |
| [Entity API migration](entity-api-migration.md) | Entity builders, typed ROM and reusable inspection/frame APIs |
| [Link/player API migration](link-player-api-migration.md) | Component/role replacements for former legacy bridges |
| [ROM migration inventory](rom-migration-inventory.md) | Typed ROM entry-point and table migration inventory |
| [Room seam round 4](room-seam-round-4.md) | Room/entity caller cleanup, enums, public surface and allocation attribution |
| [Room render/ROM hand-off](room-render-rom-handoff.md) | Live room/entity/dungeon rendering views and five typed wiring switches |
| [Platform round 4](platform-round-4.md) | GPU/audio composition, reusable presentation and resource lifetimes |
| [Room integration round 5](room-integration-round-5.md) | Production fast-path adoption, whole-frame baselines and frame pacing |
| [Frame input allocation](frame-input-allocation.md) | Borrowed phase/boundary registers versus retained immutable values; walking 580.3 → 139.5 B/frame EA off |
| [Room hub removal](room-hub-removal.md) | Direct role owners, lifecycle-only facade, construction inventory isolation and compiled-reference acceptance guards |
| [Build hardening](build-hardening.md) | Per-check main/test baseline, risk and array-record audits, scoped suppressions and final strict compiler policy |

This final integration closed the explicit room-projection and production
non-null wiring hand-offs. Test-used constructor adapters remain source-compatible.
A4 was committed as `84f65c4a`; B4 as `cbc23580`; B5 as `0b7427bb`.
The required ROM-enabled gate passes 2,988 tests (zero failures/errors, one
skipped); ROM-free CI passes 1,293. Python checks pass (7 tests, one skipped).
Both Java source sets have zero enabled lint warnings, down from 1,313/154.
Recording behavior is unchanged; its disabled path adds zero steady allocation.

Older project chronology and design proposals remain in
[reconstruction-log.md](reconstruction-log.md), [implementation_plan.md](implementation_plan.md)
and [superpowers/](superpowers/README.md). They are not additional quality-round requirements.
