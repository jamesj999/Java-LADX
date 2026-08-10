# New-Game House ROM-Path Repair Design

## Goal

Match the shipped LADX disassembly during the first new-game scene in indoor room `$A3`: render both Marin and Tarin, animate Link in the correct bed display lists, play the correct swordless/new-game music, keep the inventory window backed by the correct indoor VRAM state, and require the Tarin shield interaction before Link can leave.

## Source behavior

- `LADX-Disassembly/src/data/entities/indoors_b.asm:IndoorsBA3Entities` loads Marin (`$3E`) at `$35` and Tarin (`$3F`) at `$47`.
- `LADX-Disassembly/src/code/entities/05_marin.asm:MarinEntityHandler_Indoor` owns the new-game wake sequence, including the 127-frame slow countdown, dialogue `$01`, bed display lists, and the transition out of bed. Its bed tile references are `$40/$42`, `$44/$46`, `$48/$4A`, and mattress `$4E`.
- `LADX-Disassembly/src/code/entities/05_tarin.asm:TarinIndoorsHandler` owns the indoor Tarin display, door-blocking `Dialog000`, shield offer `Dialog054`, item acquisition, shield level write, and item jingle. Tarin's indoor normal display uses `Tarin3SpriteVariants`; the shield sequence uses the source states and item display data.
- `LADX-Disassembly/src/code/audio/select_music_track.asm:SelectMusicTrackAfterTransition` selects `MUSIC_OVERWORLD_SWORDLESS` while `wSwordLevel == 0`. The house table independently maps map `$10` to `MUSIC_INSIDE_BUILDING`; the startup path must not inject `MUSIC_OVERWORLD_INTRO`.
- `LADX-Disassembly/src/code/bank20.asm` and the inventory loader define the menu tile/attribute data and object palettes. The inventory must render from the same GPU tile state and tile addressing used by gameplay after the indoor load, rather than a stale or mismatched tile snapshot.

## Architecture

Keep the existing room/entity runtime and split the repair into source-shaped components:

1. Extend ROM entity decoding and sprite catalog support for entity `$3F`, including its indoor display variants and palette attributes. Room `$A3` must retain both entities as loaded, render them in source load order, and expose Tarin to the interaction path.
2. Replace the current Marin-only startup shortcut with a new-game house controller that mirrors the source entity state machines. It will use the ROM-derived entity positions, display-list tile indices, frame countdowns, dialog ids, and `PlayerState` inventory/shield writes. It will block movement and door exit until the source shield sequence completes.
3. Keep wake-bed rendering as a renderer concern, but feed it the exact ROM display-list data and the indoor entity tile snapshot. No guessed tile offsets, hand-painted bed tiles, or alternate sprite substitutions are allowed.
4. Route startup music through `GameplayMusicController`/`AreaMusicResolver` using the source swordless rule and preserve room-transition semantics. Add a regression for map `$10`, room `$A3`, with sword level zero.
5. Audit the indoor-to-inventory GPU transition. Make the menu consume the authoritative tile/attr/palette snapshot after indoor room loading, and test representative bed/menu tile slots so the fix does not alter the already-correct outside menu.

## Data flow

`NewGameStartProfile` starts map `$10`, room `$A3` with the ROM entry position and zero shield/sword state. `RoomLoader` decodes the indoor room and both entity records, selects the indoor entity sheets, and loads the room tile/palette state. The house controller ticks before ordinary Link input, opens the ROM dialogs, updates the entity snapshots and Link bed display, grants the shield at the same transition point as Tarin, and releases input only after the source state reaches its post-shield state. Room music selection runs from the same active-room/player-state context. Inventory rendering snapshots the GPU after this indoor setup and uses the menu's ROM tilemap/attrmap unchanged except for source-defined HUD/item writes.

## Error handling and compatibility

Malformed entity display-list addresses, missing `$3F` sprite data, invalid room/entity positions, and unavailable GPU tile slots remain descriptive failures. Existing overworld, saved-game, title, and outside-menu paths remain unchanged. New-game-only state is cleared when leaving the new-game flow or starting a saved game.

## Tests

- A real-ROM room-loader test proves indoor map `$10`, room `$A3` contains loaded entity types `$3E` and `$3F` at the source positions and both have supported indoor sprite selections.
- A Tarin state test proves the door guard, forced shield dialog, item grant, shield level write, and release of movement follow the source state transitions.
- A wake renderer test proves each bed variant uses `$40/$42`, `$44/$46`, `$48/$4A`, and mattress `$4E` with the source palettes/flip flags.
- A music resolver/controller test proves swordless room `$A3` selects `MUSIC_OVERWORLD_SWORDLESS` and never `MUSIC_OVERWORLD_INTRO`.
- An inventory/GPU regression test proves the initial indoor menu uses the authoritative loaded tile state and remains correct after leaving the house.
- Run the complete Gradle test suite and the project’s normal application/build verification before claiming completion.

