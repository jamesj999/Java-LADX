# Platform structural review, round 4

## Implementation inheritance

The source/class-file audit found five project-class edges:
`GPU -> RomTileLoader -> Vram`, `SfxCpu -> SfxCpuState`,
`EntitySpritePainter -> EntityOamPainter`, and the nested `FrozenTile -> Tile`.
All five were removed. Framework inheritance is limited to JFrame, DefaultListCellRenderer,
WindowAdapter and GLFWKeyCallback. Interface extension expresses actual public contracts.
`PlatformInheritanceTest` loads class metadata without initialization and guards
zero project-class inheritance across all eight owned package roots.

GPU is a final facade with retained VRAM, animation and named tile loaders.
VRAM owns storage and decoded-cache updates; inventory loading owns powder-icon
state, and animated tiles own animation state. No loader reaches through a facade
into storage. The SFX CPU's decoder, registers, ALU and stack are one 506-line
interpreter, replacing the artificial superclass split. Entity shape expansion
and OAM emission likewise form one 390-line painter. Tiles encode read-only
ownership directly; snapshot mutation still throws and copies remain mutable.

## Slice audit

The audit examined all owned main sources, including names matching Access,
Support, Helper, Part, Values and Lifecycle, owner back-references, and default
interface implementations. No such suffix-named production types remain.
Three actual slices were merged: GameFrameSceneUpdates and GameFrameStateUpdates
are now the record's own public withX methods; BrowserPanels is now private Swing
view assembly in AudioBrowserFrame. Their only state was the owner's state.

Retained collaborators and pure functions have these responsibilities:

| Type(s) | Responsibility |
| --- | --- |
| MusicVoiceEmitter | Owns waveform/NOISE_FF articulation state and converts sequencer notes into ordered APU register writes. |
| MusicChannelState | Stores one independently advancing music channel's command position and articulation state. |
| ParsePositions | Owns reusable exact-tuple cycle detection for one zero-time music parse. |
| FrameAudioMixer / FrameAudioPlayer | Own their distinct sample-clock/browser and video-frame/gameplay transport lifetimes. |
| EnvelopeStep / LengthCounter | Implement the shared saturating volume rule and an independently clocked APU length timer. |
| MusicBrowserModel / SoundEffectBrowserModel / BrowserTransportState | Own selection/search and transport presentation state without Swing. |
| HostResources / PendingOpenAlBuffer | Own rollback ordering and one partially created native buffer respectively. |
| PcmStreamBuffering / DialogPcmBufferQueue / IntBufferRing | Own prefill/underrun policy, dialog PCM scheduling, and primitive buffer-id queue storage respectively. |
| RomSoundEffectTables / SoundEffectPcmRenderer | Decode ROM handler pointers and render owned offline PCM respectively. |
| SaveSlotCodec | Owns the SRAM backing-array view and translates modeled player fields at labeled offsets. |
| SaveSlotStateBuilder / SaveSlotComponents | Assemble cohesive slot groups and express the SRAM's resource/inventory/progression/spawn values. |
| MiniJson | Owns strict JSON cursor/grammar parsing independently of config semantics. |
| TitleIntroTileLoader | Uploads title/intro/menu ROM sheets into its retained VRAM target. |
| InventoryTileLoader | Owns dynamic powder-icon state and inventory/VFX-sheet patch uploads. |
| RoomTileLoader / ColorDungeonTileLoader | Decode room tileset selection and Color Dungeon's special overlay addressing respectively. |
| EntitySheetTileLoader / AnimatedTiles | Decode entity sheet selectors and own animation counters/staging respectively. |
| EntityRenderInputs / EntitySelectionSprites | Express palette/offset rules and own indexed immutable selection metadata respectively. |
| OamDrawing / RgbaTileRasterizer | Submit hardware tile pairs and rasterize indexed tile pixels respectively. |
| BackgroundSceneLayers / WorldSceneLayers | Assemble snapshot background scenes and own world-layer ordering/cached Link layers respectively. |
| StorySprites | Implements the ROM's distinct story/shadow/grass sprite placement rules. |
| ReusableWorldLayers / ReusableRoomInputs / VfxPoseCache | Own retained layers, borrowed/cached room inputs, and bounded sampled VFX poses respectively. |
| HardwareBackgroundRenderer / IndexedRenderer | Translate tilemaps to hardware background planes and compose hardware planes/OAM respectively. |
| RomCursor / RomSlice / themed *Tables | Own checked parsing position, bounded ROM views, and immutable domain tables respectively. |

These are responsibilities or explicit value/algorithm boundaries, rather than
mechanical chunks receiving every field of a different owner on each call.

## Deprecated API sweep

Removed all eleven GPU byte-array delegates: loadAnimatedTilesGroup,
tickAnimatedTiles, loadBaseOverworldTiles, loadBaseTiles, loadEntitySpriteSheets,
loadColorDungeonEntitySheets, loadRoomSpecificTiles, both loadIndoorTiles overloads,
copySwitchBlockTiles and copyPressedFloorSwitchTiles. No remaining owned main
member carries @Deprecated. World callers already use Rom.

Render test/capture callers now use IntroFrameSnapshot.Builder,
FileMenuFrameSnapshot.Builder, DialogTextLoader.loadFromRom(Rom), and typed Rom
constructors for RoomLoader, OverworldTilesetTable and RoomSessionConfig. A clean
compile with temporary -Xlint:deprecation,removal reports zero warnings in owned
main and test roots; remaining warnings originate only in off-limits app/gameplay
callers. No build configuration was changed.

## Reusable special presentation

The former fallback list was title/cutscene/intro and file-menu backgrounds,
inventory entry/open/exit fades, pass-out palette frames, instrument warp/swirl/
fullscreen, heart-piece presentation, Marin's bed, Tarin's shield and got-item
poses. Scrolling and ordinary room transitions already used the retained room
layer. Every screen now stays on the reusable scene entry point; there is no
owned-scene fallback. Owned build(GameFrameState) remains explicitly available.

ReusableBackgroundLayers owns immutable snapshot extraction and retained plane/
upload/sprite nodes, consuming IntroSprite values directly without conversion.
ReusablePaletteLayer owns cached synchronous callbacks and nested fade scopes.
HardwareFrame pools transform composition, retaining copied scratch transform
chains so scratch reuse cannot alter submitted colors. ReusableWarpLayers and
ReusableStoryLayer own their independent special-presentation metadata/poses.
All original OAM order, palette scope nesting and whole-entry hiding rules remain.

ROM-free EA-off baselines: intro build+draw 11,400 bytes/frame; fully open inventory
12,952. Both now measure 0.0; construction and draw are included. A zero baseline
uses the probe's <16-byte noise floor (positive baselines use achieved +25%).
The ordinary representative guard remains 328 bytes/frame with EA off. Eleven
special-path parity cases cover 80 changing phases each, including inventory
entry/exit, pass-out, room scroll, warp/fullscreen, story and heart poses.

New immutable IntroFrameSnapshot/FileMenuFrameSnapshot instances still require
one defensive extraction on first use. Their own construction and ROM/world
snapshot production remain upstream ownership costs; repeated draw/build of a
published snapshot reuses that extraction. The raw CutsceneManager compatibility
path must still obtain its public live sprites/line-scroll values; snapshot-based
presentation avoids that boundary's repeated production costs. No private fields
of off-limits packages are accessed.

## Honest package shape and visibility

Scope is main sources in audio/save/config/input/gpu/render/rom/app/host,
including nested and compiler-generated class files where stated. Buckets use
physical source lines: 1–100, 101–200, 201–300, 301–400, 401–500 and >500.

| Metric | Merged baseline | After structural review |
| --- | ---: | ---: |
| Main Java files / top-level types | 138 | 136 |
| 1–100 lines | 85 | 84 |
| 101–200 | 29 | 27 |
| 201–300 | 18 | 17 |
| 301–400 | 6 | 6 |
| 401–500 | 0 | 1 |
| >500 | 0 | 1 |
| Public top-level types | 85 | 63 |
| Project-class inheritance edges | 5 | 0 |

Class-file audit after review: 172 types, comprising 157 classes (including 18
records and six enums) and 15 interfaces. The two largest cohesive files are
SfxCpu (506 lines) and AudioBrowserFrame (448); neither is split into slices to
meet a numerical target. EntitySpritePainter is 390, GameFrameState 342.

Twenty-two top-level types became package-private: MusicOpcode,
RomSoundEffectTables, SoundEffectBrowserModel, AudioBrowserFrame, MiniJson, Vram,
and sixteen render implementation/value/callback types (BackgroundRenderLayer,
DialogRenderLayer, DialogWindowPainter, DroppableRupeeRenderLayer,
EntityRenderLayer, FrameDrawObserver, GameFrameInputs, HardwareRenderLayer,
InventoryRenderLayer, LinkRenderLayer, PaletteOverlayRenderLayer, RenderContext,
RenderLayer, RoomRenderLayer, SpriteListRenderLayer, TransientVfxRenderLayer).
The synthetic fixture uses GPU's public TILE_COUNT instead of naming Vram.
A clean compile of every main/test caller verified these visibility changes.
Package-local observation/glyph hooks and layer-list scene factories likewise
no longer export signatures containing these internal types.

Public boundary contracts remain accessible even when callers infer their types:
RegisterObserver is a lambda parameter, MusicTrackPlayer a transport port,
SaveSlotStateBuilder/SaveSlotComponents define the save value-construction API,
and RomCursor/RomSlice are returned by the public ROM reader. AudioBrowserMain
is the Gradle/Java launcher entry point; ReusableGameFrameState is the explicitly
requested app integration API. These are public contracts, not internal slices.
