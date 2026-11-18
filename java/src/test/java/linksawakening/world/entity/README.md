# Entity test fixtures and contracts

Build a ROM-free runtime with `EntityRuntimeFixture` or
`new RoomEntityRuntimeBuilder(TestEntities.room(...))`. Configure ordinary state
through the runtime's named roles. `EntityTestFrames` supplies immutable frame
inputs, deterministic RNG and explicit open/no-interaction geometry policies.
Family inspection and debug fixtures expose focused setup for package-owned
state; production code needs no test hooks or reflection aliases.

Required collaborators are non-null. The builder selects explicit ROM-backed
or ROM-free sprite, combat and chest policies. Optional input normalization
happens when those policies are installed, rather than during every tick.
Missing parent entities, unspawned hookshot state, unavailable tail presentation,
optional spawn/effect requests and absent snapshot metadata are domain values;
preserve each owner's documented absence protocol.

Use `inspection().view()` only for immediate same-thread reads. Use
`inspection().snapshot()` when retaining a frame or comparing history. Scratch
input, slot, timer and presentation groups belong to the acquired execution;
normal ticks reuse them and reentrant execution owns separate groups.

ROM-backed integration tests use `@RequiresRom` and `TestRom.open()`. Keep
synthetic test data ROM-free. Allocation guards use retained inputs and stable
inspection views; temporary EA-off JVM settings must not enter the build.

The architecture and archived dependency reviews describe ownership and ROM
contracts. Historical file-size and null-count targets are not design rules.
