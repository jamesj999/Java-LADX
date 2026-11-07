# Project guidance

## Purpose and data policy

This is an open source Java 21 engine reimplementation of Link’s Awakening DX
for preservation and research. Users supply their own ROM.
Never commit or distribute ROMs or extracted original assets, including in
releases, examples, fixtures, screenshots or generated atlases.
Local extraction is research tooling; runtime graphics, maps, attributes and
palettes must load from the user’s ROM, not exported PNGs.

## Sources and fidelity

The LADX-Disassembly and user-supplied ROM are technical sources of truth.
Resolve discrepancies against their actual loaders and tables; do not guess
macro shapes, bank adjustments, VRAM destinations or unsupported behavior.
Keep hardware-faithful decoded data and pixel-accurate placement, animation
and palettes; use hardware acceleration where practical.
See [ROM format notes](docs/ROM_FORMAT_NOTES.md) for the consolidated reference.
Use [ROM symbols](docs/ROM_SYMBOLS.md) for exact-label matching, typed reads,
explicit bank adjustment and symbol regeneration.
Use [the ROM graphics debugging skill](skills/ladx-rom-tileset-debug/SKILL.md)
when layout is right but graphics, palettes or addressing appear wrong.

## Build and verification

Follow [BUILDING.md](docs/BUILDING.md) for JDK 21, Gradle, ROM configuration,
platform distributions and emulator comparison tools.
Mark ROM integration tests with `@RequiresRom` and read through `TestRom.open()`;
ROM-free checks must remain usable without a ROM. Never add ROM fixtures.
Refactoring must preserve behavior: retain bounds checks, timing, ownership,
input ordering and intentional offsets. Run appropriate tests and opening-route
replays; compare relevant frames/state against the emulator when fidelity is
at issue. Record route hashes, checkpoints, comparison scope and remaining
mismatches. Passing tests or a region comparison does not prove game-wide parity.
See [status](docs/reconstruction-roadmap.md) for verified scope and open work.

## Scope and commits

Keep commits small and clear. Follow the user’s branch and commit instructions;
do not push or merge unless authorized. Preserve unrelated concurrent work.
Do not turn historical plans into current implementation requirements.
Do not write detailed class-level architecture guidance while package refactors
are in progress.
