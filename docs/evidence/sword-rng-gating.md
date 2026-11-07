# Sword RNG and the source interaction gate

The former Slime Eye evidence misidentified `GetRandomByte`'s return to
`00:1547` as a footstep. It is the call at `UseSword` (`00:1544`). It indexes
`SwordRandomSfxTable` with the result's low two bits: noise IDs 02, 14, 15, 18.
`Sword.onPress` already consumes the session's shared `RomRandomByteSource`
and uses the same four-way mapping. Adding another RNG call would be wrong.

The bounded input transcript is `tools/sword-rng/inputs.tsv`: the first 320
button frames of the original isolated D3 controls run. Both counterparts
start seed A2 and initial frame counter 1, with the same disclosed direct D3
setup and initial shake. They replay exactly these buttons; unlike the older
full-fight policies, they do not independently react to diverging positions.
The ROM boots and loads the room before setup; the engine uses production
`GameSession` initialization and its real equipment/entity update paths.
After setup only input/stepping and read-only observations occur.

The ROM hooks `GetRandomByte`'s return and the sword noise request instruction.
The engine's optional RNG observer records actual draws without replacing their
values. Caller identification uses the diagnostic Java stack. Its observation
includes prior seed, call-time frame counter and scanline; post-frame states
are recorded separately. Outputs are local `/tmp/ladx-sword-rng`.

## Identified discrepancy

Before the gate correction, at route frame 193 both counterparts start a
sword swing. The ROM uses
counter 194, LY 17, seed A2 and returns BA, selecting noise 15. The engine
uses counter 195, LY 0 and returns B2, also selecting noise 15. Both
post-tick frame counters equal 195. This is a call-time phase difference,
not a missing RNG consumption. Production deliberately still supplies LY 0;
no fake scanline or RNG value override was introduced.

Subsequent consumption differs for a source-backed reason. Bank2
`02:42B2–42C4` checks the exact byte `hLinkInteractiveMotionBlocked == 2`;
on this branch it skips both `CheckItemsToUse` and `UpdateLinkAnimation`.
The ROM holds the first sword in DRAW/timer 2 during the boss slow stun.
The former engine blocked Link's movement but continued equipment animation,
so its blade completed and accepted new swings at route frames 216, 240,
and 264. These extra draws shift the shared RNG seen by later entities.

The correction gates only that exact top-down source byte.
It preserves the former movement/presentation lock separately; this change
does not establish hardware-accurate movement semantics for every raw value. Ordinary sword
movement blocking, internal presentation locks, dialog/inventory gates and
side-view handling retain their separate policies. The raw request value is
preserved and overwritten in the existing application order, not combined
with OR or max. Full CPU/display scheduling and source call-time LY remain
outside this change.

## Current request producer audit

| Producer | Source write | Value |
|---|---|---|
| Slime Eye slow stun | 04:4E59 | 2 |
| Slime Eye split | 04:4A94 | Current splitting countdown |
| Moldorm/Slime Eye arena bounds, `label_394D` | 14:54E0 | 1 |
| Hinox hold/fallen helper | 06:51B2 | 1 |
| Warp pad | 19:4358 | 1 |
| Witch exchange wait | 05:486D | 2 |
| Witch mixing, music-timing value | 05:48AE | 1 |
| Tarin transformation | 05:4A19, 4B43, 4B9A, 4D02 | 2 |
| Armos Knight stun | 06:538E | 2 |
| Owl block helpers | 06:6939, 69BF | 2 |
| Kid kidnapping warning | 06:612E | 2 |
| Held item, heart piece, instrument, toadstool, sword and power-up poses | `HoldEntityAboveLink` / 03:5A4A | 2 |

This audits the current request producers; it does not claim all hardware
interaction writes or presentation helpers are represented by this event.

## Reproduction

With JDK 21 and PyBoy available, from the repository root:

```sh
java/gradlew -p java -I ../tools/sword-rng/capture.gradle captureSwordRng
python tools/sword-rng/capture.py
python tools/sword-rng/compare.py
```

The engine capture and original-ROM capture use the same checked-in input
file. The local setup JSON records the exact ROM SHA256, input hash and ROM
setup writes. Neither tool publishes ROM-derived graphics/audio. The older
controls-only fight, static region parity and this bounded call trace establish
different scopes; none establishes game-wide or full RNG fidelity.

## Final bounded results and verification

The final engine consumes sword RNG at route frames 288 and 312, returning
62 and 4E (hex). Before the gate correction it consumed at 193, 216, 240,
264, 288 and 312. The original consumes sword RNG at 193, 288 and 312 and
body-wander RNG twice at 257. Exact draws and selected states are preserved in
[sword-rng-gating-2026-11-07.json](sword-rng-gating-2026-11-07.json); input SHA256:
`d2a332dd8611ce7c936921599947a0ff7f6689f2ac88f940bc5f7b3ddf6fed36`.

This does **not** make the bounded call sequences equal. The engine's earlier
landing/slow-stun phase already leaves a pending raw-2 block before the A edge
at 193; the original has raw 0 before that edge, starts its swing, and writes
2 later in the same frame. At route 192 the engine slow countdown is 19,
reference 20. The corrected engine properly suppresses its blocked edge,
rather than invent an RNG consumption to mimic the original. That upstream
phase difference, body-wander call timing, call-time frame counter and LY
remain unresolved. No RNG override, arbitrary scanline, duplicate footstep
call, new clock policy or unrelated movement change was added.

The focused 62-test selection initially passed 61; one new isolation test
revealed that the test's retained boss issued its own block writes. Clearing
entities during that test's setup removed this unintended fixture interaction;
both new gate tests then passed. No implementation changed after that initial
selection. Checks include the 23 existing sword tests, RNG sequence test,
Slime Eye runtime and controls tests, Witch motion, and architecture guards.
The new regression verifies exact-2 freeze/resume of the existing sword timer,
dropping blocked button edges without buffering, raw-3 eligibility, overwrite
ordering, side-view exclusion and byte bounds. The connected split test checks
the actual emitted byte sequence 11 through 2.

The affected controls-only full encounter still defeats both halves and collects
the heart with normal buttons after setup. Its current run takes 1412 frames
and retains 24 health units before reward healing completes. This supersedes
the earlier 1302-frame/40-health engine run as current-code outcome, while the
older screenshot/checkpoint comparisons retain their historical scope. This
milestone ends at the source gate correction and measured remaining limits.
