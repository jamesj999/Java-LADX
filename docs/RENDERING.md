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

Production has no pixel provenance arrays. Tests attach `HardwareFrameDiagnostics`
explicitly when rebuilding scenes. Layer class names are resolved only for those
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
