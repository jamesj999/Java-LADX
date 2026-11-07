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
`RenderPipeline.LEGACY_RGBA` is a deprecated alias, not a second backend.
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
