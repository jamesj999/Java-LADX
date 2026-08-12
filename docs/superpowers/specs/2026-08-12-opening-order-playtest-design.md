# Opening Order Playtest Design

## Goal

Make a normal engine run enter the source-shaped opening gameplay path so the
Marin/Tarin house sequence, shield acquisition, route to Toronbo Shores, beach
owl event, and sword acquisition can be tested in order.

## Startup behavior

The checked-in development configuration will use the existing `NEW_GAME`
item profile with the title and cinematic intro disabled. A configured launch
with that profile will delegate to `Main.startNewGame()`, the same bootstrap
used by creating an empty file. It must not maintain a second approximation of
new-game inventory, room, Link position, Marin, Tarin, palettes, or music.

Title, cinematic intro, file selection, saved-game loading, and arbitrary
configured-room launches remain available through configuration. Only the
checked-in playtest defaults change.

## Order-of-play verification

Tests will establish that the configured `NEW_GAME` path selects the dedicated
new-game bootstrap and that the shipped beach room `$F2` carries both opening
entities until their independent room-status events complete. Existing focused
tests remain authoritative for Marin's wake timing, Tarin's shield sequence,
the beach owl, and the sword fanfare/reward sequence.

The integration test will use the live `RoomSession` and durable `PlayerState`
boundaries so it verifies event harvesting and persistence, not only isolated
motion classes.

