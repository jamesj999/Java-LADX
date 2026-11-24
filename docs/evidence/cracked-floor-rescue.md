# Rescue route through the cracked-floor cave, 2026-11-24

On the current rescue route, the engine crosses the Mysterious Forest cave with
the toadstool sequence reachable. Before the sword correction, Java completed
115 checkpoints, fell four times in map `$0A` room `$BD`, and died at frame
15954. Both runs used the same route and fresh-game bootstrap.

## Why the floor collapsed

At checkpoint 115 both games place Link at raw (24,112). The next input is
`up+a*`, which walks north while pulsing sword. At (24,111) the ROM samples
cell `$61`, object `$DF`, physics zero. The 80 playable room cells in the
Java load agree with the ROM. The cracked floor changes to a pit (`$AE`)
after 40 qualifying ground updates in the same cell.

PyBoy hooks on the normal-input route show the floor counter at 7 before
the swing, then 0 during the swing. The ROM tests the interactive-motion
block and resets this counter at `$02:7851..786C`. The sword animation sets
that block before advancing its state. Java used the scripted movement block
alone, so a sword swing allowed the floor timer to continue. The corrected
post-movement ground check includes the current sword animation's block.
The session regression starts a swing with the counter at 39.

| Checkpoint (one-based) | Java raw X,Y | PyBoy raw X,Y | Differing RGB555 pixels / 23,040 |
| --- | --- | --- | --- |
| 115, bottom of cracked floor | 24,112 | 24,112 | 157 |
| 116, north end | 24,28 | 24,28 | 173 |
| 117, turn toward exit | 72,28 | 73,28 | 380 |
| 118, next cave room | 72,128 | 73,127 | 175 |

These are unmasked full-screen comparisons at the end of each input segment.
Java enters and leaves this crossing with 16 health units, PyBoy with 24.
Later, Java obtains the toadstool and completes checkpoint 159. Segment 160
times out in overworld room `$42` at (72,90) targeting Y=88. PyBoy enters
this room at (89,127), reaches the target at (91,88), and completes all 843
checkpoints with BowWow state 1. The route contains 99 explicit ROM input or
timing overrides, including a Java-only wait just before segment 160.

## Reproduction

Use JDK 21, PyBoy 2.8.1 and the user-supplied English DX v1.0 ROM (SHA-256
`c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`).
The rescue TSV SHA-256 is
`62c1e95dd6d63f459a0cc541578591a90508daddf82d0b6612101d807bde3804`.
From the repository root, use a fresh output directory for each revision:

```sh
out=$(mktemp -d)
route="$out/rescue.tsv"
git show b5fd0ecf:tools/opening/bowwow-rescue-route.tsv > "$route"
python tools/opening/capture.py --route "$route" --output "$out"
(cd java && ./gradlew captureOpeningRoute -PopeningRoute="$route" -PopeningOutput="$out")
```

The Java timeout retains traces and completed checkpoint images. Compare
zero-based PNG indices 114–117 in `original/` and `ours/` after shifting
each RGB channel right three bits. The ROM route uses fresh in-memory save
RAM and button input with no gameplay memory injection.

## Beach enemy wall checks

A separate controlled probe verifies a fractional movement step in room
`$F1`. The roaming enemy handler probes each axis with nonzero speed
(`$03:7A36`, `$03:7A5D`). At (128,32) with X speed +8 and fraction zero,
the ROM Octorok sets right-wall bit 1 while X remains 128 and the fraction
becomes 128. On the next dispatch its state changes from 0 to 1 and its
speed becomes zero. Java now probes on that first step. With this fix alone
the rescue replay's Java state trace through the cave death was byte-for-byte
identical to the baseline.

Reproduce the two handler returns with RAM-injected positions and movement
state using `python tools/leever/capture_rom.py --case octorok-wall --output /tmp/octorok-wall.json`.
