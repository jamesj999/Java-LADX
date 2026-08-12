# Witch Toadstool Exchange Design

## Scope

Implement `WitchEntityHandler` from `LADX-Disassembly/src/code/entities/05_witch.asm` for the opening progression path. The disassembly and ROM are authoritative. The implementation must preserve the existing entity-runtime architecture rather than emulate the Game Boy CPU.

## Runtime behavior

Entity type `$40` in indoor-B room `$A2` uses its ROM-backed four-sprite display list at bank `$05:$4780`. Its Y position is forced to `$40`, its inertia increments every frame, and `(inertia >> 4) & 3` selects the four cauldron animation variants.

State 0 uses the witch-specific talk rectangle, including conversation across the cauldron. Without `wHasToadstool`, talking opens `Dialog00C`. With the toadstool, the exchange starts only when Link activates the equipped Magic Powder slot as in the source: the item occupies A or B and the corresponding button is held. Starting the exchange clears that inventory slot and `wHasToadstool`, sets the transition countdown to `$08`, and advances state.

States 1-7 reproduce the source sequence: block Link until the first countdown expires; open `Dialog009`; run the `$C0` cauldron interval while restoring default room music at the source transition; open `Dialog0FE`; assign Magic Powder and add BCD `$20` uses; emit the treasure-found jingle and palette-effect request; wait for the effect to finish; then open `Dialog17E` and settle in the terminal state.

## Integration

`RoomEntityRuntime` owns the per-slot witch state and timing and emits typed requests for player inventory changes, reward application, dialogs, music, motion blocking, and palette presentation. `RoomSession` harvests durable reward requests. `Main` supplies the live toadstool, powder, inventory, dialog, and palette state and applies emitted player-state changes.

`PlayerState` receives explicit operations for starting the exchange and awarding `$20` powder uses. Save persistence continues through the existing inventory/count/toadstool fields.

## Verification

Tests exercise the unsupported-to-supported sprite transition, no-toadstool dialog, wrong-button rejection, correct A/B equipped-item activation, exact state/countdown/dialog order, Link blocking, music/jingle requests, reward state, and a live room-session exchange in indoor-B room `$A2`. Focused tests and the complete Gradle suite must pass, followed by a direct source comparison and `git diff --check`.
