# Crystal-switch animation design

Status: implemented and verified

## Goal

Carry the ROM's crystal-switch interaction through the Java runtime far enough
that a sword hit can start the switch-block animation, the animation toggles
the live switch state at the ROM-defined point, and each VBlank copies the
matching four-tile frame into the two gameplay VRAM slots.

This is a narrow runtime slice. It does not attempt to make every switchable
object producer complete.

## Source contract

The source of truth is the shipped ROM plus these disassembly paths:

- `src/code/entities/15_crystal_switch.asm`
  - entity `$66` renders its pair from `CrystalSwitchSpriteVariants` at bank
    `$15`, address `$4320`;
  - the handler writes health `$FF`, runs the normal enemy collision helper,
    and, after the helper creates the `$18`-frame flash, starts the switch
    animation only when `wSwitchableObjectAnimationStage` is zero;
  - starting the animation writes stage `$01`, resets the entity transition
    countdown to `$18`, and queues `WAVE_SFX_FLOOR_SWITCH`.
- `src/code/home/interrupts.asm` and `src/code/home/animated_tiles.asm`
  - VBlank gives switch-block updates priority over ordinary animated tiles;
  - when the animation stage is nonzero, `UpdateSwitchBlockTiles` is called
    once per VBlank.
- `src/code/bank0.asm`
  - `UpdateSwitchBlockTiles` increments the stage first;
  - the increment from stage `$02` to `$03` toggles `wSwitchBlocksState` with
    XOR `$02`;
  - stages `$03`, `$05`, `$07`, and `$09` copy transition/final four-tile
    frames, with stage `$09` clearing the animation stage after its final
    copy;
  - the source tile blob is `SwitchBlockTiles` in bank `$0C` (bank `$2C` after
    the GBC adjustment), at address `$6800`.
- `src/constants/memory/vram.asm`
  - `vTilesSwitchBlockA` is `$9040` and
    `vTilesSwitchBlockB` is `$9080`;
  - because the Java VRAM tile array begins at `$8000`, those are global tile
    slots `$104` and `$108` respectively.

The three source offsets used by the transition table are `$00`, `$40`, and
`$80`. The two final tables select `$00`/`$80` according to switch state; the
transition frame selects `$40` for both blocks.

## Runtime design

`RoomEntityRuntime` recognizes crystal switch `$66` as a normal enemy-hitbox
entity. A sword hit gives it the ROM collision flash/ignore-hit timers but it
cannot die. On the following entity tick, the crystal consumes its flash:

1. clear the flash countdown;
2. set its transition countdown to `$18`;
3. request stage `$01` if the room is not already animating switch blocks.

`RoomSession` owns the room-level `switchBlocksState` and
`switchableObjectAnimationStage`, so the request is applied at the same
runtime boundary that owns the collision state. The session's VBlank method
advances the stage and asks `GPU` to copy four ROM tiles for each stage that
has a copy. While the switch stage is active it returns immediately, matching
`AnimateTiles`; the existing ordinary animated-tile tick resumes once the
switch stage is idle.

The crystal's floor-switch wave sound is carried as an entity event with the
raw WAVE id `$0E`; the existing gameplay sound consumer maps that event to the
wave sound namespace.

When indoor tiles are loaded, the session initializes both switch-block VRAM
slots from the final state table after the room-specific tile load, because
the latter covers the surrounding `vTiles2` range and can overwrite those
slots.

## Explicit scope boundary

This increment does not implement:

- Link standing on or walking across switch blocks;
- hookshot-chain or other special switch-block producers;
- mobile-block update requests through `hSwitchBlockNeedingUpdate`;
- non-sword crystal-switch triggers;
- the complete `hLinkInteractiveMotionBlocked` movement gate;
- room-event scripting that consumes the new switch state.

Those paths remain separately tracked rather than being approximated by the
crystal-switch animation state machine.

## Verification target

Focused tests cover the pure stage sequence, ROM tile destinations and source
offsets, crystal sprite data, crystal sword-hit/flash/request behavior, and
WAVE sound mapping. The Java project test suite must also pass after the
integration hook is added.
