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

Hardware is the production default and FrameScene composes RGBA once. The
selectable legacy backend and duplicated layer rasterizers have been removed.
Direct `render(RenderContext)` calls and custom RGBA callbacks retain narrow
source-compatible adapters; production indexed scenes never use them.
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
Final full suite: 2,636 cases, 2,634 passed, 1 failed, 1 skipped (the opt-in
OpenAL live test). The sole failure remains `MainOpeningRouteTest`, blocked at
`left+a*+b x=72 4`, room `$0E`, ROM position `(84,91)`; it is left untouched.
Its route SHA-256 is
`7516e7d1d09a7d703ac6a87e99915cc5bed98153037db1f1b751f99e3abc1678`.

Production composition and diagnostics (2026-11-12):

`GameFrameSceneBuilder` owns one reusable `HardwareFrame` for its lifetime.
Scenes retain immutable layer lists; the builder reuses only its construction
lists and stable Link layer. Each draw resets the compositor before submitting
those layers. The pixel planes, transform-ID array, OAM entry pool and optional
background-copy scratch retain their capacity across frames. Palette transforms
are composed once per scope and referenced through a small table, preserving
inner-to-outer evaluation. OBJ painter order and original entry Y are unchanged.
A scene/builder and its scratch are confined to one rendering thread: do not
retain the package-private `hardwareFrame()` result across another draw.

Production frames have no pixel provenance arrays. Tests and local capture
code explicitly attach `HardwareFrameDiagnostics` when rebuilding a scene to
classify differences. It owns source strings, HUD evidence, hidden-inventory
history and legacy comparison methods. Production keeps only source identity
per OAM entry, which the whole-entry dialog visibility rules require.
`FrameDrawObserver` is an explicit optional completed-frame observer. Existing
session replay tests use a test-resources service provider whose scoped observer
storage lives entirely in test sources; production has no ThreadLocal observer.

`RgbaCallbackCompatibility` remains for the supported direct `RenderLayer.render`
and custom callback APIs, including palette overlays. Indexed production scenes
never enter it. Hardware capability is checked without streams; migration
inspection and legacy comparison run only in tests. Immutable `GameFrameState`
updates preserve referenced snapshot ownership and byte-masked frame counters.

`ComposeAllocationTest` uses synthetic full-screen BG tiles, 40 OBJ tiles and
sliding-window entry hiding, with 1,000 warm-up frames and 200 measured frames.
Its thread-allocation measurement excludes parity instrumentation and guards a
fixed budget below 4,096 bytes/frame. Baseline was 1,133,344 bytes/frame; the
reused compose path measured 64 bytes/frame. This is a composition benchmark,
not a whole-session allocation claim. The optional ROM benchmark was omitted
because `GameSession.headlessFrame()` also advances simulation/audio and does
not expose its private render assembler independently.

Verification for this refactor: the ROM-enabled default suite completed 2,681
cases with zero failures/errors and one skipped live-audio case. Scene parity
reported 30,708 identical scenes, zero unexplained mismatches and 33 scenes
containing the same 330 evidenced fidelity-fix pixels. One custom callback scene
is deliberately outside indexed parity. All 37 entity regression tests were
preserved while splitting their former large source file. No new emulator
capture was made; these checks do not establish game-wide parity.

The separately tagged `MainOpeningRouteTest` replay attempted the unchanged
627-checkpoint tail-cave route (SHA-256
`7516e7d1d09a7d703ac6a87e99915cc5bed98153037db1f1b751f99e3abc1678`).
It stopped at the alive assertion: map $00, room $F1, position (88,40), frame
counter 59, health 0. Recompiling the pre-refactor render/gpu sources into a
local temporary classpath reproduced the identical complete failure message
against the same application code. No route fixture or expected hash changed.
The known-gap task's classpath needed a temporary Gradle init script to include
the user-ROM test resource; repository build configuration was left unchanged.
