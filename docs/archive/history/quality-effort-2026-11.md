# November 2026 quality effort

## Integration and hardening

- A1: render assembly now checks `hasRoom()` and reads required room metadata; all room role callers use explicitly nullable projection names. The four old nullable role methods are deleted. Full ROM gate: 2,985 tests, zero failures/errors; scene parity remains identical.
- A2: PlayerWiring uses LinkDependencies; EquipmentWiring uses Sword.Assets; SubsystemWiring passes PhysicsTables/CollisionTables; UiWiring passes InventoryGraphics. LinkWriteApplier and RoomNavigationFixture use non-null registry items, and the fixture supplies invisible sword assets explicitly. Existing constructor adapters and nullable lookup still have dedicated/other test callers; their original caller inventory is in non-null-collaborators.md. Full ROM gate: 2,985 tests, zero failures/errors.
