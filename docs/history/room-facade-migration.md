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
