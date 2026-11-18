# Evasive Stalfos clone implementation plan

## 1. Source audit and red tests

Inspect the exact `SpawnNewEntity`, `ConfigureNewEntity`, vector, options, and
physics definitions and record the constants in the design spec. Add failing
tests to `RoomEntityRuntimeTest` for clone gating, clone state/attributes,
slot exhaustion, and fleeing side effects. Add a failing consumer test for the
new raw whoosh mapping. Run the focused tests and confirm they fail for the
missing behavior.

## 2. Motion result and source-backed clone request

Update `StalfosEvasiveMotion` so its update can report a clone request when the
post-decrement private countdown reaches the source branch, while preserving
the source entity's same-frame movement. Keep the existing vector and
fixed-point math. Add test-only state setup/accessors only where the runtime
tests need to reach a ROM state deterministically.

## 3. Runtime clone allocation and attributes

Update `RoomEntityRuntime` to consume the request only when the map gate allows
it, allocate the highest free slot, and initialize the clone with the ROM
catalog definition and exact source fields. Preserve the handler's final
ignore-hits write of zero after the generic spawn default. Track dynamic physics/options
overrides, reset them on clear, skip generic ground interaction for the clone,
and correct the ordinary Evasive/Gibdo-conversion physics byte to `$12`.
Emit the raw whoosh only after successful allocation and preserve the existing
fleeing sword-poke event/VFX behavior.

## 4. Audio boundary and integration tests

Reuse the existing `NOISE_SFX_WHOOSH` (`$0A`) mapping already consumed by
`EnemyCombatEventConsumer`; add a type-$1E regression proving that the clone's
raw write reaches that shared shipped effect. Add a RoomSession integration
assertion if the live Angler's Tunnel entity path can exercise the branch
without inventing room state.

## 5. Verification and handoff

Run focused tests, `./gradlew clean test`, `git diff --check`, and inspect the
diff for unrelated changes. Update `docs/reconstruction-roadmap.md` with the
verified source-backed boundary, commit the increment on
`feature/entity-runtime`, and report the exact verification results without
claiming full engine parity.
