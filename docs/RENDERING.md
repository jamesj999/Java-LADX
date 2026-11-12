# Rendering

Runtime graphics come from the user's ROM. GPU is a compatibility facade over
VRAM storage, animation, and named ROM loaders. Software layers submit indexed
BG/window pixels and OBJ entries; only OpenGlFramePresenter touches GL.

OBJ-to-OBJ composition preserves the emulator-validated legacy painter order.
TODO: a future RomOamAllocator must reproduce the ROM's dynamic allocation before
using actual CGB lower-OAM-index priority. Static Link/sword slot assignments are
not a replacement. Each 8x16 entry retains its original Y across both tile halves.

OBJ-to-BG/window uses CGB color-zero transparency, both priority flags, and LCDC
bit 0. Original-ROM PyBoy captures establish OBJ visibility over the HUD. Dialog
captures establish attribute $01 and prompt visibility, but do not reproduce the
engine's Link-under-dialog geometry. Dialog expected fixes admit only Link/prompt
sources, exact legacy BG RGB, and an actual hardware-visible OBJ.

HideSpritesUnderDialog (bank1.asm $5F6A-$5FB2) skips reserved entries 0-8,
including Link/prompt, and hides whole other OAM entries by raw Y: below $3E for
top boxes, at least $58 for bottom boxes. Fully open inventory bypasses this
filter. Entity-layer hiding is retained and the compositor applies the same
filter to submitted nonreserved sources. No BG priority bit is invented.

Hardware is the production compositor and FrameScene composes RGBA once. The
selectable legacy backend and duplicated layer rasterizers have been removed.
Direct `render(RenderContext)` calls submit an isolated hardware frame.
Custom RGBA callback composition exists only in the test-only `CallbackScene`;
`FrameScene` rejects non-hardware layers before drawing.
The ignored `RenderPipeline` selector has been removed.
The test-only reference preserves old RGBA layer composition through those
compatibility APIs; independent emulator fixtures still verify raster output.
Strict parity fails on unexplained pixels; `-PsceneParityProbe=true` records all mismatch groups. Reports live in
java/build/scene-parity. Expected-fix source reports distinguish HUD and dialog.
Sliding inventory uses HideSprites ($01:5F51-$5F67): raw entry Y >= WY+8
hides both halves. Above-window differences are classified only with a traced
removed OBJ, exact legacy OBJ RGB, and exact revealed hardware BG RGB. Inventory
pixels retaining OBJ follow the same CGB priority rule as the closed HUD.
The default switch and removal gate passed both suites and the 613-checkpoint
tail-cave-warp capture (route SHA256
79c179a1bce0e23d63c84238a6da404b9ddc8f2786b8751550873dcfffb4bf80).
Closed inventory uses the same submission preparation as open inventory, including
the mushroom/powder VRAM uploads from InventoryMenu.render. A direct hardware-only
test checks these uploads with reference rendering disabled, preventing the oracle
from accidentally supplying rendering-cache side effects. Generated screenshots/ROM data are local only.

Room scrolling uses `SkipDisabledEntityDuringRoomTransition` (`00:3D57–3D7E`),
called by the pair, single and rectangle OAM helpers. It rejects a whole draw
list when unsigned `(active X - 1) >= $C0`, unsigned `(visual Y - 1) >= $88`,
or either position sign byte is nonzero. Java's unbounded room offsets retain
those sign bytes; the guard uses X before horizontal screen shake and visual Y
after Z/handler offsets. `UpdateEntityPositionForRoomTransition`
(`19:798E–79B5`) maintains the original position/sign pairs.
`DrawLinkSprite` has its own stricter body check (`00:1D4F–1D59`): raw visual
Y must be less than `$88`. Equipped-item OAM writers remain independent.
These are whole-list submission rules, not a rectangular HUD pixel clip.
The window still follows the existing CGB BG/OBJ priority rules; allowed
entries may overlap its upper pixels. `HideSprites` (`01:5F4B`) only enters
its window-Y hiding loop while inventory is appearing; the LCD interrupt
(`00:0388–0407`) does not disable gameplay OBJ at the HUD boundary.

Shared death clouds use `EntityDeathHandler` (`03:5518`, rectangle calls
`03:5557/555E`) and `Data_003_5488` / `Data_003_54C8`. Before dispatch,
`UpdateEntityTimers` clears the active tile offset (`14:4D73–4D76`) and supplies
only the flash attribute (`14:4DE4–4DF0`). The cloud must not inherit Octorok's
body tile offset (`+$30`, `03:57F3–57F5`) or another handler's facing flip.
The original rectangle entry order and per-entry flips remain unchanged.

Verification (2026-11-11): `EntityRenderLayerTest` covers ROM-backed normal and
power cloud lists with asymmetric synthetic tiles for Octorok and Moblin.
`EntityScrollVisibilityTest` covers enemy/heart/rupee lists, all three OAM
helper shapes, exact boundaries, Z, sign bytes, shake and RenderBomb's visual
adjustment. `LinkScrollVisibilityTest` covers the interpolated Link body.
Independent headless PyBoy 2.8.1 execution of the original routines checked
8 transition-guard inputs, 8 death-cloud OAM frames and 3 Link Y inputs;
all matched their source rules. This used isolated RAM inputs, not a gameplay
route or a full-frame emulator comparison. Outputs remain in `/tmp`.
The headless `house-route.tsv` replay completed all 22 checkpoints
(SHA-256 `e6065107a5ef0ed993855dc4e7acfabe8f4bfb5756106b5032bfede2e12b5142`),
ending in overworld room `$A2`, Link ROM position `(88,82)`, shield 1, health 24.
No frame-hash expectations were changed. These checks establish bounded
rendering behavior, not game-wide parity.
## Composition ownership and diagnostics

`GameFrameSceneBuilder` owns one reusable `HardwareFrame`. Scenes retain immutable
layer lists; each draw resets the compositor before submission. Pixel planes,
transform IDs, OAM entries and background scratch retain capacity across frames.
Palette scopes preserve inner-to-outer evaluation. A builder and its scenes are
confined to one rendering thread; do not retain `hardwareFrame()` across draws.

Production has no pixel provenance arrays or evidence implementation.
`HardwareFrameDiagnostics` and `TestHardwareFrames` live exclusively in src/test.
Tests attach them through the nullable `HardwareFrameObserver` submission interface
when rebuilding scenes. A compiled-code guard rejects main-source references to
the evidence implementation. Layer class names are resolved only for those
frames; reserved OBJ source identity remains available for dialog hiding.
`FrameScene.drawTo(buffer, gpu, observer)` accepts explicit completed-frame inspection.
For session captures, `GameFrameSceneBuilder.setDrawObserver` attaches an optional
observer to subsequently built scenes. The two-argument draw performs no service
lookup. There is no global production observer or service provider.

`SceneParityExtension` explicitly attaches to builders reachable from test-instance
fields before test execution and detaches afterwards. Tests creating a session
inside their method must attach their own builder observer; standalone scenes
pass an observer to the explicit draw overload. This bounds automatic parity
coverage; dedicated `RenderPipelineParityTest` checks remain independent.

`ComposeAllocationTest` measures synthetic full-screen BG, 40 OBJ tiles and
sliding-window hiding after 1,000 warm-up frames, over 200 measured frames.
The 2026-11-12 cleanup measured **64 bytes/frame**, below **4,096 bytes/frame**.
This protects composition reuse, not whole-session allocation.
The ROM-enabled default suite passed 2,745 cases (zero failures/errors, one skipped
live-audio case) after the cleanup. No route fixtures or expected hashes changed.
No new emulator capture was run; these checks bound behavior and do not prove parity.

The render/gpu size audit covered production and test sources: the largest file
is 296 lines and the largest method is 71 lines after helper extraction.
Explicit standalone observation measured 986 identical scenes with no unexplained
mismatches. This is a narrower observation scope than the former implicit service;
method-local sessions outside render tests require their owner to attach the
builder observer. The full default regression suite remains the behavioral gate.

## ROM uploads and tile snapshot ownership

Animation uploads copy directly from ROM into VRAM through range-checked
`Rom.copyInto`; they no longer allocate a temporary byte array or address record
on animation frames. The synthetic fast-animation guard measured 26 bytes/tick
before this change and 0.0 afterward, including with escape analysis disabled.
It asserts less than 16 bytes/tick.

`Vram.snapshotEntityTiles` reuses an immutable snapshot while its actual pixels
remain unchanged, including detecting direct mutable Tile writes. Room sheet
loads invalidate identity even when pixels match. The stable-snapshot guard went
from 18,992 bytes/call to 0.0. `tile()` still returns an independent mutable copy;
`readOnlyTile()` returns a stable, mutation-rejecting tile for render submission.
Old snapshots remain unchanged after subsequent VRAM or returned-copy mutations.

OpenGL startup registers shader, VAO, VBO and texture ownership immediately.
Any later initialization failure releases earlier resources; cleanup attempts all
releases and is idempotent. Shader compile/link errors retain their driver log.
Pure fake shader and ownership tests exercise these paths without a native context.
These checks supplement the default regression gate, not independent emulator parity.

## Reusable scene construction

`GameFrameSceneBuilder.build(ReusableGameFrameState)` refills a builder-owned
scene and configurable room/entity/Link/VFX/HUD/dialog layers. The returned scene
is borrowed until the next build on that builder and must be drawn on the same
thread. `GameFrameState` and `build(GameFrameState)` retain their existing owned
container/layer-list behavior for tests and captures. `snapshot()` explicitly
copies mutable room planes into that form; Link/controller/live-view references
retain the original frame-state API's reference semantics.

Reusable setters mirror the immutable `withX` names and return the same object.
`reset()` restores empty-input defaults. `withRoomData(tiles, attributes, palettes,
entities, warp, heart, grassPalette)` borrows caller-owned planes/metadata;
`withRoomControllers(scroll, transition)` binds the controllers. The compatibility
`withRoom(snapshot, ...)`/`copyFrom(snapshot)` paths cache immutable room metadata
by identity, but new immutable room snapshots still incur their defensive getters.
They must not be used as a substitute for the world's pending borrowed room view.

Normal overworld scenes retain layers and indexed contexts. Entity metadata is
packed into primitive integers, snapshot tiles are read through immutable views,
and hardware OAM structs retain storage across frames. No iterator, captured
palette callback, sprite record or copied tile is created per ordinary entity
submission. Bounded origin-relative VFX poses are sampled once from the existing
VFX renderer, so its ROM geometry and ordering remain the only implementation.
The VFX runtime's current `activeSlots()` API still creates its immutable list/slot
snapshot; the render layer consumes that boundary once per draw.

The ROM-free representative guard uses an injected engine-authored dialog glyph/window
(`setDialogWindowPainter`); production keeps the default ROM-backed dialog painter.
The fixture includes a room, HUD/window, visible Link,
ten entities including a three-entry rectangle, three active VFX and an open
bottom dialog. With escape analysis disabled, build+draw measured **25,184 bytes
before** and **328 bytes after**; construction alone measures **0**. With escape analysis enabled, the four
variants measured **272–311.8 bytes/frame** (construction still **0**). The budget is
**410 bytes/frame**, adding 25% headroom to the achieved total and remaining below
512. The measured reusable loops cover both immutable-input copying and complete
`reset()`/borrowed-plane refills, low/high entity type ids, varying frame phase
and Link position. Pixel tests compare against owned scenes across changing room tiles,
attributes, Link positions and dialog presence; cached VFX poses are checked
against the existing renderer across all types/phases. Scene parity remains the
wider oracle.

Special presentation paths (menu/intro snapshots, instrument warp, heart-piece
presentation, pass-out/inventory fades and scripted story poses) currently use
the existing owned pipeline through the reusable entry point. This preserves
their exact behavior; the allocation budget above covers ordinary overworld
presentation, not these fallback paths.

### App owner handoff

No app file is changed by this rendering refactor.

- `app/presentation/RenderStateAssembler.java:composeFrameBuffer` can immediately
  change `frameSceneBuilder.build(currentGameFrameState())` to
  `frameSceneBuilder.buildReusable(currentGameFrameState())`. This preserves the
  current `Screen.renderState` overrides (including gameplay's game-over menu).
  It removes repeated layer/scene construction while retaining immutable room
  extraction costs until the world owner exposes borrowed planes.
- For the complete allocation-bounded path, retain one `ReusableGameFrameState`
  on that assembler, refill it using the same current fields with `reset()` and
  `withX`, feed borrowed room arrays/entities using `withRoomData`, then call
  `frameSceneBuilder.build(reusableState).drawTo(...)`. Keep
  `currentGameFrameState()` as the explicit snapshot/capture API.
- If using mutable states through screen dispatch, add the corresponding
  `Screen.renderState(ReusableGameFrameState)` overload. In
  `GameplayScreen.renderState`, preserve the existing game-over override by
  returning `state.reset().withScreen(FILE_SAVE).withFileMenuFrame(snapshot)`
  when `driver.gameOver()` is non-null; other screen overrides use the matching
  mutable setters. The one-line bridge above needs no screen changes.

The world's immutable `RoomRenderSnapshot` getters currently copy their arrays.
The world owner must supply borrowed planes/a live entity view to realize the
full end-to-end bound; this renderer deliberately does not bypass that ownership
boundary or access private world fields.
