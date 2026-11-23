# Link's lower-body grass overlay, 2026-11-07

Link's grass overlay now renders through the production GameSession scene.
For the controlled right-entry experiment, the complete 160×144 frame matches
the emulator at RGB555 precision: 46 differing pixels before, 0 after. All 46
corrected pixels are within X4..19/Y88..95. This is one matched displayed frame,
not an assertion of scene parity at every time or whole-game fidelity.

## Source and implementation

Source of truth: English DX v1.0 user ROM and LADX-Disassembly. Workflow:
`skills/ladx-rom-tileset-debug/SKILL.md`.

`ApplyLinkGroundPhysics` calls `label_002_787D` for physics`$06` (grass):

- Reserved OAM 0/1 use tile pair`$1A/$1B`, 8×16 mode, at raw coordinates
  `(hLinkPositionX−1,hLinkPositionY+8)` and `(hLinkPositionX+7,hLinkPositionY+8)`.
  Java sprite coordinates are therefore Link pixel X−1/X+7 and Y+8.
- `$02:788A..788F` uses bit 3 of `wConsecutiveStepsCount` for the first
  horizontal flip; the second XORs`$20` at `$02:78CF`.
- Palette 0 is ordinary; overworld room`$32` uses CGB OBJ palette 6. The renderer
  receives that source room condition through an immutable room snapshot value.
- These reserved entries outrank Link's body OAM 2/3 and survive dialog sprite
  filtering. They are appended after the body in the current painter compositor.
  Full hardware OAM allocation remains outside this change.
- `ApplyLinkGroundPhysics` reuses the previous ground physics while dialog or
  scrolling blocks ordinary motion (`$02:75BD..75C4`, `$02:76C0`). The effect
  flag persists between Link updates and follows the same interpolated scroll
  position as his body. Airborne/unstucking Link does not emit grass columns.

A separate byte mirrors the source step counter for grass flipping:
`$02:4440` increments held ground input even if terrain/collision prevents
movement; `$02:445F` resets stationary motion to 7; running adds 2 at `$43E6`;
airborne ground-motion handling writes`$FF` at `$4503`. This change does not
replace the existing body-animation counter or claim all animation timing fixed.

GPU tiles`$1A/$1B` were already loaded from the user ROM through GBC-adjusted
`LinkCharacterTiles` into VRAM 0..3F. No image asset, hardcoded bitmap or new
ROM extraction is used at runtime. The test uses synthetic asymmetric tiles
only to verify flips, pixel placement and palette choice.

## Controlled capture

The setup is the same explicitly injected experiment recorded in
[overworld-scroll-entry.md](overworld-scroll-entry.md): normal house input
route, then Link at ROM X156/Y96 in A2 with fractional accumulators`$A0/$50`,
held right. Java scroll completes at CPU motion frame 210; the source upload/
screen snapshot distinction is retained. At the compared visible frame both
Link positions are 13,96 and the body uses state`$2D`. Java post1 is compared
with ROM displayed post2, after the source's delayed body upload becomes visible.
No image translation, masking or sprite replacement is applied.

Local files under `/tmp/ladx-entry-controlled`:

- Before: `java-grass-before-post1.png`.
- After: `java-grass-after-post1.png`.
- ROM reference: `rom-right-post2.png`.

The before capture compiles the actual pre-change `f611a9c`
`GameFrameSceneBuilder` into a temporary classpath. The same current GameSession,
input route, Link update and capture tool then run with that old scene builder;
it omits the grass layer. The after capture runs the production scene builder.
No source-file swapping or manually painted screenshots are involved.

ROM SHA256:
`c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`.
House route SHA256:
`e6065107a5ef0ed993855dc4e7acfabe8f4bfb5756106b5032bfede2e12b5142`.
PyBoy 2.8.1; JDK 21. Compare RGB555 by shifting each RGB channel right 3 bits,
as existing `tools/opening/compare.py` does. The different 8-bit expansion of
5-bit colors is not counted as a Game Boy palette mismatch. All 23040 pixels,
including HUD, entities and background, are compared. RNG is not synchronized;
no broader RNG/progression conclusion follows from this captured frame.

Reproduce ROM with `tools/entry/capture_rom.py --visual` and Java with
`EntryProbe grass-after 210 visual`, using the documented test runtime classpath.
Copy `java-right-post1.png` to the after filename before a later capture replaces
it. Captures remain local; no ROM, extracted tiles or screenshots are committed.

Focused verification: LinkGrassRenderLayerTest plus WorldDependencyTest,
MainArchitectureTest and GlBoundaryTest: 23 checks pass. The new single test covers
source step 8 flip versus stationary 7, complementary tile flips, 8×16 placement,
palette 6, reserved OAM dialog priority, warp-cache reset and airborne suppression.

Persistent body-VRAM/attribute staging remains open. Completion-frame Java still
resolves the new body pose before the ROM upload schedule allows it. The exact
source gates and adjacent-frame evidence remain in the prior entry document;
this grass milestone does not hide or claim to fix that scheduling gap. Body-VRAM
staging is pending manual direction; no staging implementation was started.
