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
