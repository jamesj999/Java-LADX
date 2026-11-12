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
