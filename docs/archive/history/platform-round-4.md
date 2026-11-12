# Platform structural review, round 4

## Implementation inheritance

The source/class-file audit found five project-class edges:
`GPU -> RomTileLoader -> Vram`, `SfxCpu -> SfxCpuState`,
`EntitySpritePainter -> EntityOamPainter`, and the nested `FrozenTile -> Tile`.
All five were removed. Framework inheritance is limited to Swing's JFrame and
DefaultListCellRenderer. Interface extension expresses actual public contracts.
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
