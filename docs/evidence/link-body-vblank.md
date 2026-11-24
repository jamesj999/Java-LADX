# Link body uploads during gameplay, 2026-11-24

The four body tiles at `$8000..803F` and the two column flip attributes can
retain the preceding pose for a frame after Link turns or begins a sword swing.
The OAM writer still updates the position. Java now retains the uploaded pose
index, which addresses both immutable ROM tables, and uses it in the direct
renderer and hardware OAM producer. Equipment, body visibility, screen
coordinates and damage flashing continue to read their current state.

## Transfer order

The world handler queues an inventory-slot draw on every fourth ordinary
frame when the draw queue is free and Link's motion state is below 2
(`$00:0FEB..100A`). PyBoy observed the alternating slot destinations
`$9C01` and `$9C06`. During VBlank the draw queue, scroll and switch-block
paths can preempt both ordinary transfers. A BG animation copy also occupies
the body upload opportunity. When a group branches to
`SkipTilesGroupAnimation` at `$00:1D1E`, the handler at `$20:54F5` updates
the two 32-byte tile pairs and their flip bits. Hidden pose `$FF` returns
without changing them. The OAM writer at `$00:1D2E` independently tests
current visibility.

Opening-house ROM traces also show dialog row writes on five consecutive
VBlanks and alternating letter-tile clears and glyph writes. Idle page waits
allow uploads between periodic HUD transfers. The Java simulation schedules
VBlank on story and pass-out frames and models the dialog tile writes.
Other special-purpose draw commands, requested graphics loads and exact
CPU-to-display timing still have incomplete coverage.

## Beach walk, swing and turn

Append the following three rows to `tools/opening/sword-route.tsv` without
editing the seed. Use the capture commands in [the scroll-entry evidence](overworld-scroll-entry.md).
The resulting route SHA-256 is
`b275ff19aafe09a9a0c659a71ef9ff1c0685da1a1280e49a03303e88b651a648`.
Both engines start a fresh game and complete 72 checkpoints using the same
inputs. The user ROM SHA-256 is
`c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`.

```text
right 12 8
a 1 20
left 12 8
```

The 61 comparison frames are the frames of those three inputs. For each
frame, compare a CRC32 of the four tiles in Game Boy bitplane order plus
both horizontal/vertical flip attributes. The ROM capture reads bank-0
VRAM and OAM slots 2 and 3. Java samples the first four tile submissions
of the visible body in this room. PyBoy 2.8.1 entered the body upload
handler 42 times, deferred it for 16 HUD transfers and deferred it for
three tide tile copies.

| Segment | Matching tiles and flips on identical inputs | Samples |
| --- | ---: | ---: |
| Right walk and idle | 18 | 20 |
| Sword swing and idle | 19 | 21 |
| Left walk and idle | 20 | 20 |

Ordinary ground movement now takes its walk pose from bit 3 of
`wConsecutiveStepsCount` at `$00:1A50`. Idle sets this byte to 7, so the
first directional frame selects pose 1. This fixes the wrong walk pose
after turning left. In the identical-input swing the Java HUD clock has a
different phase. A second trial added three Java idle frames before the
swing while preserving all ROM inputs. At the A press both engines then
retain pose `$2C`'s CRC `efcce262` and attributes `$00/$20` while the live
pose is `$18`. At the next VBlank both submit `90774ad0` with
`$20/$20`. The two trials have different BG counters, and one of the
21 swing frames still differs in the adjusted trial. Java starts the
swing at (100,81), PyBoy at (100,82).

## Controlled scroll into A3

After the [house route and injected right-edge setup](overworld-scroll-entry.md),
the ROM clears the scroll and sets the BG animation frame count to `$FF` at
`$02:7989`. At CPU frame 210 the eligible VBlank wraps the count to zero,
copies the destination BG frame at `$00:1C51` and retains Link's old body
tiles. Frame 211 uploads pose `$2D`. Full world loads have a separate
`$FF` write at `$01:4460`. Both sites preserve the graphics data offset.

With the Java clock aligned to 210 at scroll completion, Java and PyBoy
have raw position (12,96). Comparing Java completion with ROM displayed
post1 gives zero differences in the 16×16 Link rectangle and 68 elsewhere
in the 160×144 RGB555 image. Before the scroll-counter correction that
comparison contained 113 differing pixels in the same Link rectangle.
Java post1 and ROM displayed post2 match across all 23,040 pixels. The
unadjusted same-index completion images represent different points in the
display pipeline.

Run the existing ROM probe with `--visual`. Compile `tools/entry/EntryProbe.java`
using `javac -d` and the Gradle test runtime classpath, then run
`linksawakening.render.EntryProbe room-reset 210 visual` from the repo root.
Both probes write images under `/tmp/ladx-entry-controlled`.
