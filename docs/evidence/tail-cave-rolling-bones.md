# Tail Cave after the Feather

The source capture used the first 669 checkpoints from the rescue recording whose SHA-256 begins `2ab4fefc`. The truncated input file has SHA-256 `16c78a13a8cc655a48bd5506e9ade8872731ae82a46293c274b934443540ab49`. The route uses the English DX v1.0 ROM and the normal buttons recorded in `tools/opening/bowwow-rescue-route.tsv`. To reproduce this particular prefix, count data rows starting at one and preserve the original ROM override columns.

A controlled Java replay starts after checkpoint 497 in Tail Cave `$1D` at raw (72,58). It restores the observed dungeon flags and room statuses before loading, equips the same sword and Feather, and uses 24 health to isolate these rooms from cumulative damage. The native run starts from a fresh file and has 4 health at checkpoint 497. Java opens the Nightmare Key chest in room `$08` at checkpoint 556, records the key by checkpoint 560, and reaches Rolling Bones in `$11` at checkpoint 587. This exercise starts from a prepared dungeon state and compares normal controller progression from there.

## Rolling Bones approach

The near-bar branch at `$06:6D5A–6D65` sets a 24-frame timer, advances the stored state, and returns with the existing sprite variant. The farther branch at `$06:6D68` integrates the X speed immediately through `$06:654E` and sets the movement variant. The jump branch instead uses the background-collision pass at `$06:6E07`.

In a separate controlled run in room `$11`, PyBoy CPU hooks positioned the boss and bar before the state-zero calculation at `$06:6D41`. Boss X149 with fraction `$80`, bar X181, and boss Y64 yielded X150 with fraction zero after the source step. Boss X103 with bar X104 instead left X and the prior sprite variant 2 intact while setting timer 24. Java now follows both branches directly.

## Airborne sword collision

The normal replay agrees on the boss position and opening player poses, and the first two accepted sword contacts occur at native frames 61127 and 61151. At the third contact, native frame 61175 and Java controlled frame 6527 both have boss raw (136,38), Z0 and sword X138. The original takes a hit with sword Y58 and height 8. Java previously used Y59, which is one pixel outside the strict vertical overlap given the target center Y46 and half-height 5.

Read-only hooks at `$00:1033`, `$00:103C`, `$02:44ED`, `$02:4563`, `$02:489D`, and `$06:6CEE` distinguish the two pose samples. At frame 61175, `ApplyGotItem` stores `wC145=66` using the frame-entry Y71 and Z5. By sword rectangle generation, Link has moved to (130,71) and Z3. The source uses the new X but adds the vertical sword offset to the sampled 66. The previous Java implementation computed both axes from the moved pose, where the Java raw Y was 70 and Z3. A separate RAM-injection run wrote only sword Y59 instead of 58 at the boss dispatch hook. Native boss health stayed at 6 on that frame. The normal run dropped it to 5.

The Java enemy sword rectangle now receives the before-motion Y−Z from the gameplay phase. Its X, direction, animation, and collision eligibility still use the active equipment/combat phase. This measurement covers the enemy sword path. The static sword routine at `$00:15D6–15ED` indexes the room grid with live raw positions. The shield rectangle at `$20:4B65` also reads `wC145` in the ROM and needs its own comparison against Java's shield geometry.

## Replay results

With the phase correction, the controlled Java replay lands seven hits at frames 6479, 6503, 6527, 6551, 6575, 6599 and 6623. The native run lands these hits at the same relative offsets and its eighth at frame 61298. The existing Java input sequence misses the last hit. By the last strike, the boss is at Y47 in Java and Y39 in the ROM. The seventh hit explains the growing difference. At native `$03:6FCC`, Link (134,31) and the boss (138,32) produce recoil (+48,+12) at `$03:6FE1`. With Java's Link (133,30) and boss (137,32), the same ratio calculation yields (+48,+24). The controlled replay entered the uphill stretch with different position fractions.

An external controller-only variant changes checkpoints 599 and 600 to Down+A for six frames and Down+A+B for two frames. Checkpoint 604 holds Up until room `$0B`, then idles 30 frames. The variant wins at 600, leaves the boss room at 604 and reaches Moldorm in `$06` at 613. Java lands two Moldorm hits before recoil and a pit send Link to `$1A` at 630. The rescue TSV was kept as recorded for this comparison. A fresh Java A/B using the later canonical hash `de3e447dbf39a75bfe8de8c5b9f58ab9d15cc45d8d08d3a0f1523b079331fc6e` yields the same checkpoint and damage log with either sword-anchor policy up to a death in overworld room `$52` at frame 20280.
