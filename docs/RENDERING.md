# Rendering

Runtime graphics come from the user's ROM. GPU is a final facade composed of
VRAM, named loaders and animation. Only OpenGlFramePresenter touches OpenGL.
Tests are headless and never open windows or real audio devices.

## One production pipeline

RenderStateAssembler owns a GameFrameState and GameFrameSceneBuilder. Each frame
it resets/refills inputs for the current RenderScreen and builds the borrowed
FrameScene, then draws once into caller-owned RGBA storage. There is no immutable
scene-building overload and no buildReusable alias. GameFrameState.empty creates
fresh mutable inputs, never shared global mutable state.

The builder retains WorldLayers, which switches once on RenderScreen.
Overworld uses room, entity, Link, ground, VFX, pickup and inventory layers.
Title/cutscene and file screens use BackgroundLayers. WarpLayers, StoryLayer and
PaletteLayer retain special-presentation state and scopes. These collaborators
are constructed once, retained and reused. No project-class inheritance or
wildcard imports are needed. Pure hardware geometry algorithms are shared;
owner-only layer slices are not used to enforce file-length targets.

## Screen sections and presence

GameFrameState has final, always-present OverworldInputs and BackgroundInputs.
The screen discriminator selects which section is drawn. Reusable presence cells
own intentionally omitted presentation values. Binding and reset update presence
once; layer assembly consults presence instead of rediscovering valid screen
shapes through combinations of null fields.

OverworldInputs groups room/scroll/transition data, actor presentation, VFX,
pickups, inventory and dialog. A room-only diagnostic frame may intentionally
omit actors/UI. BackgroundInputs groups raw BG planes, optional title controller,
intro snapshot and file snapshot. File snapshots take precedence over a retained
intro; intro takes precedence over raw planes, preserving the original rules.

Optional domain/hardware values are: absent room/actor/UI presentation for empty
or diagnostic frames; dialog and prompt; warp and heart-piece visuals; transient
VFX and pickups; previous scroll room; missing entity sprite definitions/overlays/
death effects; absent snapshot tile sources; optional line X/Y scroll and animated
uploads; transparent/missing hardware tile or palette; and optional test observers.
Private cache/storage initialization and public argument validation also retain
explicit null guards. Null is not a second screen discriminator.

RenderStateAssembler fills overworld room/actors, title/cutscene background, or
lets the active file screen publish its menu frame. An active non-world dialog
binds its room palette view explicitly. File screens publish through the same retained state and scene pipeline.

## Lifetimes and ownership

Inputs and scenes are borrowed, confined to one rendering thread, and valid only
until the next refill/build or simulation tick. Draw before advancing simulation.
RoomInputs consumes RoomRenderView through its public indexed interface into
retained storage; immutable room snapshots are extracted once per identity.
Fresh intro/file snapshots likewise need one defensive extraction, whereas
rebuilding a published snapshot reuses it. Upstream snapshot production is not
included in scene-construction allocation measurements.

Production has no snapshot copy API: no production caller needs owned render
inputs. Test-side FrameSnapshots copies room planes and fills the same production
state. Controller/Link references retain the former snapshot's reference semantics.
Legacy direct layer regression fixtures live in src/test, including background,
sprite-list, upload, warp and palette wrappers. They do not constitute a second
production scene assembler. Existing regression assertions and fixtures remain.

## Hardware composition

HardwareFrame retains indexed BG/window planes, transform IDs and pooled OAM.
Window pixels replace BG, including color zero. OBJ/BG composition respects CGB
priority flags and LCDC bit 0. OBJ painter order retains the existing emulator-
validated order; reproducing a general ROM OAM allocator remains separate work.
Each 8x16 entry keeps its original Y across both tile halves.

HideSpritesUnderDialog ($01:5F6A) skips reserved Link/prompt entries and removes
whole other entries by raw Y: < $3E for top boxes, >= $58 for bottom boxes.
Inventory hiding ($01:5F51) removes entries at raw Y >= WY+8. Fully open inventory
bypasses dialog filtering. Palette scopes evaluate inner-to-outer, and copied
scratch planes retain independent transform chains across scratch reuse.

Diagnostics and provenance implementations remain test-only. Package-local
observation hooks are optional and production performs no global observer lookup.
The scene-parity oracle compares 986 captured cases; these checks establish
bounded regression evidence, not game-wide emulator parity.

## Allocation and structural verification

With escape analysis disabled, unified assembly and construction measure 0 B;
composition measures 0 B. The representative room with ten entities, dialog and
active VFX measures 0 B/frame, guarded at zero; rendering consumes liveSlots()
without owned VFX snapshots. Intro and open
inventory build+draw measure 0 B, within <16. Whole-frame idle/inventory measure
0 B; walking measures about 139.5 B within its 175 B guard. Default-JVM and
EA-off runs use the same tests; temporary init scripts never alter build files.

Source ownership and historical unification metrics are recorded in the
[unification audit](archive/history/render-unification.md). Cohesion determines
class boundaries; there is no file-length target.

Run the required gate with the user ROM configured:

```sh
cd java
export LA_ROM=/path/to/user/azle.gbc
./gradlew spotlessApply test
```

[Unification audit](archive/history/render-unification.md)
