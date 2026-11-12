# Testing

Run Gradle from `java/` with JDK 21. The local pre-merge gate is
`LA_ROM=/absolute/path/azle.gbc ./gradlew spotlessApply test`.
Public CI cannot distribute a ROM; it runs `spotlessCheck` and `romFreeTest`.
Passing tests bound verified behavior; they do not prove game-wide parity.

## Test taxonomy and adding coverage

- **ROM-free unit tests:** use synthetic bytes, tiles, palettes and collaborators
  in `java/src/test/java/linksawakening`, beside the package under test. Add a
  focused JUnit `@Test` for observable behavior and boundaries; run it with
  `./gradlew romFreeTest --tests fully.qualified.ClassName`. Avoid native UI,
  original assets and fixture copies from the ROM.
- **ROM integration:** annotate the entire test class with `@RequiresRom` and
  read using `TestRom.open()` in a closed stream. This supplies the `rom` tag
  and missing-ROM skip; indirect ROM consumers need the annotation too. Run
  `./gradlew test --tests fully.qualified.ClassName` with `LA_ROM` configured.
  Never check in a ROM or extracted fixture to satisfy CI.
- **Opening-route replays:** `tools/opening/*.tsv` are shared button routes;
  `MainOpeningRouteTest` is separately excluded from the default suite.
  Other route tests and capture harnesses exercise their documented scopes.
  Add a separate route for new coverage; preserve canonical fixtures and hashes.
  `captureOpeningRoute` pairs with `tools/opening/capture.py` and `compare.py`.
  Record the route SHA-256, completed checkpoints, terminal state and comparison
  scope. A failed route must not be reported as a complete capture.
- **Architecture and boundary guards:** tests under `architecture/` use the
  shared `ClassFileReferences` reader for symbolic type/field dependencies,
  descriptors and generic signatures. `WorldDependencyTest` enforces room/entity
  boundaries and the explicit app/world allow-list; `AppBoundaryTest` checks
  session ownership, native restrictions and public role contracts. They do
  not invoke `javap` or match instruction text. ROM constant guards remain
  separate. Extend the guard closest to the boundary.
- **Behavioral ordering:** `GameplayFrameOrderTest`, `SimulationOrderTest` and
  `EntityFrameOrderTest` record calls through the same ordered runners used by
  production. `EntityFrameSessionTest` records real Link motion and dialog/music
  ports; command and bomb tests assert observable effects. `RoomLinkWritesTest`
  covers descending slot order, ties and stable stream order. Headless
  `FileMenuBehaviorTest` covers file/menu persistence and opening-story gates.
  The [replacement inventory](archive/history/room-architecture-test-migration.md)
  maps each former disassembly assertion to its boundary/behavioral coverage.
  These checks do not establish game-wide semantic ROM fidelity.
- **Allocation guards:** the [performance budgets](#performance-budget) below
  list full-frame, simulation, renderer, world, audio and typed-ROM scopes.
  Add representative work and measure after warm-up using thread allocation
  counters, outside parity instrumentation. Preserve budgets; report JVM and
  scope rather than attributing whole-session cost to one package.
  JFR samples complement allocation counters. Filter recording stacks to the
  update/measurement path to separate startup allocation. Keep recordings local.
  Room drain tests verify read-only callback batches, queue clearing before
  effects, exception cleanup and retained immutable snapshot ownership.
- **Emulator comparison tools:** the [tool catalog](../tools/README.md) lists
  headless PyBoy scripts and matching Java capture tasks. Add comparisons using
  the original loaders/tables, with explicit regions, input overrides, injected
  state and clock alignment disclosed. Keep all generated ROM-derived images,
  atlases, audio and reports local. Do not launch the windowed app for these tests.

## Performance budget

`AllocationProbe` measures calling-thread heap bytes after warm-up and reports
the quietest of five windows. Whole-session probes use the supplied ROM and
ordinary button input. Positive updated budgets add approximately 25% to the
EA-off achieved result; zero-cost producers have exact-zero or existing probe-floor
guards. Use a temporary local test JVM argument `-XX:-DoEscapeAnalysis` to check
EA-off behavior; do not commit runner/build changes. Stage probes are narrower
than full frames and do not replace pixel/replay parity.

| Guard (test class) | Scope | Budget |
| --- | --- | --- |
| `app.headless.WholeFrameAllocationTest` | Full update + render assembly + scene + composition + copy into retained capture storage; idle/walking/inventory | <=1,340 / 1,287 / 900 B/frame |
| `app.SessionAllocationTest` | Update only, opening dialog / active house | <=80 / 842 B/frame |
| `app.presentation.RenderStateAllocationTest` | Reusable assembly / assembly+scene, no composition | exactly 0 / 0 B/frame |
| Same class, owned reference/capture path | Owned assembly+scene and frame-state overhead over room snapshot | <=21,200 B/frame; <=100 B state overhead (existing tighter caps retained) |
| `app.gameplay.entity.EntityAllocationTest` | Immutable entity input / output application without world ticking | <=50 / 350 B/frame |
| `render.SceneConstructionAllocationTest` | Representative reusable build+draw / construction only | <410 / <16 B/frame |
| `render.SpecialSceneAllocationTest` | Published intro and fully open inventory reusable build+draw | <16 B/frame each |
| `render.ComposeAllocationTest` | Indexed composition of a retained room | <4,096 B/frame |
| `gpu.GpuAllocationTest` | Animated tile tick / stable entity-tile snapshot | <16 B/call each |
| `audio.AudioAllocationTest` | Music+APU tick, frame mixer, frame audio player | <16 B/call each |
| `audio.sfx.SfxAllocationTest` | Frame SFX CPU with active channels | <16 B/tick |
| `rom.RomSliceAllocationTest` | Bounded typed byte/word reads | exactly 0 B/iteration |
| `world.entity.EntityTickAllocationTest` | Six-entity ROM-free mix plus three live inspections | <=1,100 B/tick |
| `world.entity.EntityCombatAllocationTest` | Sword overlaps two enemies; immutable events/drain included | <=570 B/frame |
| `world.entity.EntitySpriteAllocationTest` | Warm recurring falling display-list cache | exactly 0 B/query |
| `world.entity.MarinPresentationAllocationTest` | Repeated immutable poses | exactly 0 B/iteration |
| `world.room.RoomLiveBoundaryTest` | Empty outbox drains and ordered merges | exactly 0 B/iteration |
| `world.room.RoomRenderAllocationTest` | Populated room/entity/BG+OBJ/inventory read-only views and buffer copies | <256 B/frame |
| `dialog.DialogAllocationTest` (external owner) | Dialog printing/update and event drain, not full window/font painting | <=8 B/frame |
| `entity.LinkAllocationTest` (external owner) | Link walking/sword update, not sprite submission | <=8 B/frame |
| `ui.InventoryAllocationTest` (external owner) | Synthetic HUD/inventory submission fixture | <=8 B/frame |

`app.gameplay.PhaseAllocationTest` reports per-phase nonnegative measurements;
it has no upper performance budget. The owned capture API allocates its returned
framebuffer by design; whole-frame budgets use `headlessFrame(retainedBuffer)`
while testing that its pixels match `headlessFrame()`.

EA-off whole-frame results are approximately 1,072 idle, 1,029 walking and 720
inventory B/frame. Idle exceeds 256 only because off-limits dialog font/text,
inventory setter clones and startup pose values total 1,072 B/frame; attributed
owned idle allocation is zero. Inventory's 720 bytes are also off-limits UI.
Walking meets the 2 KB target including its changed immutable input values.
JFR allocation-site scope and exact sources are recorded in the
[round-5 report](archive/history/room-integration-round-5.md). Keep JFR files local.

## Opening route format

Despite the `.tsv` extension, readers split non-comment rows on whitespace.
Each row has `buttons held_or_target idle_frames`, optionally followed by
`rom_buttons rom_held_or_target rom_idle_frames` (three through six columns).
Lines beginning with `#` and empty lines are ignored. Buttons join with `+`;
`-`/`none` represent no input as supported by the route reader. A trailing `*`
pulses a button during a segment (eight frames held in each 24-frame period).
Numeric held values are frame counts; conditions such as `x=72`, `y=88` and
`room=0xA2` wait for the reader's directional/room goal with its timeout.
A `-` duration override retains the shared target. Optional ROM columns explicitly
account for differences between Java and original-ROM trajectories; report them.
Consult the actual Java route reader and Python capture parser before adding a
new condition: accepting a token in one runtime does not establish support in both.

## Render parity and independent evidence

`RenderPipelineParityTest` compares indexed composition with a test-only RGBA
reference. `SceneParityExtension` attaches explicitly to test-held session
builders, while standalone scenes pass the observer overload. Sessions created
inside a test method must opt in themselves. Local reports live in
`java/build/scene-parity`; `-PsceneParityProbe=true` records mismatch groups.
Expected fixes require narrow source/pixel evidence. Two engine paths can share
a mistake, so internal parity does not replace independent emulator evidence.

## Python checks

With Python 3.11+ and `tools/requirements.txt` installed, run from the root:

```sh
python -m ruff check tools
python -m ruff format --check tools
python -m pytest tools
```

Symbol-generation tests and shared helper tests run without a ROM. The optional
local symbol/ROM readability test skips when no local ROM is present. Tests must
not import capture entry points that execute a ROM at module import time.
