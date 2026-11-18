# Render unification audit

Merged baseline: 48 main Java files, 5,169 physical lines, 208 explicit
`== null` / `!= null` comparisons (including ternaries). The coordinator's 183
count predates the integration pass; this audit uses a reproducible source count.

| Owned path | Retained path | Owned callers |
| --- | --- | --- |
| GameFrameState | ReusableGameFrameState | RenderStateAssembler.currentGameFrameState, render/presentation tests and capture tools |
| BackgroundSceneLayers | ReusableBackgroundLayers | GameFrameSceneBuilder.build(GameFrameState), through those tests/captures |
| WorldSceneLayers | ReusableWorldLayers | Same builder overload; no live app rendering caller |
| PassOutPaletteRenderLayer / InventoryFadeRenderLayer | ReusablePaletteLayer | Owned builder and direct layer regression tests |
| InstrumentWarpRenderLayer / StorySprites factories | ReusableWarpLayers / ReusableStoryLayer | Owned world assembly and direct regression tests |

The live assembler uses the retained inputs and build overload. Both immutable
and mutable overloads of app.screen.Screen are migration seams. Layer-level
regression tests are independent consumers and must retain their assertions.

Unified pipeline verification (EA disabled): assembly/scene construction 0 B,
composition 0 B, representative active-VFX scene 264 B/frame, intro and inventory
0 B/frame. Whole-frame idle/inventory 0 B; walking 596.53 B/frame (existing guard).
No budgets or fixtures changed. FrameSnapshots is test-only; production returns
borrowed inputs. Section reset/capture ownership tests were added.

## Final structure

35 render-main files, 4,086 physical lines and 88 explicit null comparisons,
versus 48 / 5,169 / 208 at merge. No source wildcard imports were introduced.
The 88 remaining comparisons include validation, storage initialization and
optional hardware inputs, not just screen optionals; this is not a claim of zero
null checks. Screen-layer selection uses persistent sections and presence.

Removed production duplicates: the immutable frame record, both owned scene
assemblers and their builder overload, the unused read-only interface and
buildReusable alias. Eight direct legacy layer fixtures moved to test sources;
StorySprites test factories call production StoryLayer geometry. FrameSnapshots
is the sole test-side room-plane ownership helper; no production snapshot caller
needs or receives an owned frame.

Minimal edits outside render/presentation: app.screen.Screen, GameplayScreen,
FileMenuScreen and FileSaveScreen remove duplicate renderState overloads and the
ReusableGameFrameState import. No world, UI, entity or simulation implementation
was edited.

Remaining null guards by file (includes ternaries):

- HardwareBackgroundRenderer.java: 4
- IndexedRenderer.java: 3
- RgbaTileRasterizer.java: 2
- OpenGlFramePresenter.java: 2
- GameFrameState.java: 1
- HardwareFrame.java: 8
- DialogRenderLayer.java: 2
- EntityRenderLayer.java: 21
- EntitySpritePainter.java: 2
- FrameScene.java: 4
- EntitySelectionSprites.java: 2
- PaletteLayer.java: 1
- WorldLayers.java: 1
- RoomRenderLayer.java: 3
- HardwareRenderLayer.java: 3
- TransientVfxRenderLayer.java: 2
- WarpLayers.java: 4
- BackgroundLayers.java: 9
- RoomInputs.java: 14

Final default JVM: assembly/construction/compose/intro/inventory 0 B; representative
scene 240–264 B/frame; whole-frame idle/inventory 0 B, walking 548.53 B/frame.
EA-off: representative 264 B, walking 596.53 B; the other listed paths remain 0.
All existing allocation budgets are unchanged. Final full gate: 2,958 tests,
zero failures/errors, one existing skip; all 986 scene comparisons identical.
The merged baseline had 2,956 tests; two section/ownership tests were added.
