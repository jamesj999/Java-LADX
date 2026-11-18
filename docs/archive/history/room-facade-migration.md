# Room facade migration

Round-5 status: the app/render integration and all owned deprecated caller
migrations are complete. All former owned byte/enum/event delegates and
`Rom.unsafeBytes()` are deleted. Historical tables below describe the earlier
handoff; see [the integrated report](room-integration-round-5.md) for current
lifetimes, budgets, warning counts and remaining immutable/off-limits costs.

## Done

Rounds 3–4 migrated the former 104-delegate facade callers, including all
`world/entity` tests. All nine compatibility classes, the final 26 room bridges,
the positional session/combat/pickup adapters and the private reflection alias
are deleted. `RoomSession` has no superclass and no deprecated members; its only
constructor takes `RoomSessionConfig`. The four former nested boundary records
are top-level. Role views and frame inputs/results are the supported boundary.

Production `RoomSessionViews` no longer contains test-only setup or queries;
owned tests use `RoomDebugFixture` and `RoomFrameTestAccess`.

The completed member-by-member table is superseded by this note. The only
remaining work is outside the room facade: six ROM-byte entry points, twelve
integer-to-enum role/value adapters and four entity event constructors required
by external clients. The exact signatures, replacements and caller paths are
listed in [the round-4 report](room-seam-round-4.md#remaining-deprecated-boundaries).
Those entries remain deprecated until their owners migrate; they introduce no
legacy superclass or raw ROM storage.

See [the round-3 report](room-facade-round-3.md) for historical metrics and
[the architecture-test mapping](room-architecture-test-migration.md) for the
completed bytecode-to-behavior test migration.
