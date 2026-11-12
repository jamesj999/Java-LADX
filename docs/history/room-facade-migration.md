# Room facade migration

The legacy room API remains available through role-specific compatibility
interfaces inherited by `RoomSession`. Internal forwarding calls now reach
the focused collaborators directly. Boundary values and public role views
are top-level types in `world.room`.

## Types retained until the app merge

These four records must **move to top-level after app merge**. They remain
nested and deprecated to avoid conflicts in files being rewritten by the app
engineer:

| Nested record | External source |
| --- | --- |
| `RoomSession.LinkGroundFrame` | `app/gameplay/LinkEquipmentPhase.java` |
| `RoomSession.ShovelStartResult` | `app/SessionBootstrap.java` |
| `RoomSession.ArrowShotResult` | `app/SessionBootstrap.java` |
| `RoomSession.ChestOpenResult` | `app/gameplay/DialogInteractions.java` |

The delegate replacement inventory will be added after the dependency and
parameter-object migrations are complete.

## Reflection compatibility

`RoomSession.context` is a private, deprecated alias of `RoomSessionComponents`
solely for `world/entity/SlimeEyeControlsTest` reflection. It does not retain
a `RoomSessionContext`. Remove this alias after that test migrates to the
entity test-access API. Runtime room collaborators receive typed dependency
records and do not retain the facade, wiring result, or wiring class.

## Long signatures retained for external callers

Owned callers use parameter objects. These four explicit long signatures remain
`@Deprecated(forRemoval = true)` until the listed files migrate:

| Legacy signature | Replacement input | External caller |
| --- | --- | --- |
| `RoomSession` constructor (9 parameters) | `RoomSessionConfig` | `app/SessionBootstrap.java` |
| `resolveEntityCombat` (11 parameters) | `EntityCombatInputs` | `world/entity/SlimeEyeRuntimeTest.java` |
| `resolveEntityCombat` (12 parameters) | `EntityCombatInputs` | `app/gameplay/CombatPhase.java` |
| `collectEntityIfNeeded` (12 parameters) | `EntityPickupInputs` | `app/gameplay/LinkEquipmentPhase.java` |

The separately approved nested `LinkGroundFrame` record still has nine
components; it moves after the app merge with the other three nested records.
`LoadedRoom` and `RoomBoundaryState` now group constructor inputs while retaining
flat accessors, equality, hash values, text and defensive-copy behavior.
