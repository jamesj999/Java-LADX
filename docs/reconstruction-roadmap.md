# Reconstruction status

Open source Java 21 preservation/research engine; users supply their own ROM.
Never commit or distribute ROMs or extracted original assets.

## Implemented and verified scope

This is a bounded status snapshot of the recorded verification through
2026-11-06, not a new test run or a claim of complete parity.

- Graphics: ROM-driven intro/menu scenes, overworld object decoding, GBC
  overlays, palettes, tilesets, animated tiles and entity/OAM rendering.
- Gameplay: opening house/dialogue, sword acquisition, forest and Witch
  exchange, Tail Key, Tail Cave progression, Rolling Bones, Moldorm,
  Heart Container, Full Moon Cello and return to the overworld.
- Post-dungeon progression: Heart Piece, hideout guards, king fight and BowWow
  rescue are covered by the continuous fresh-game replay.
- Latest rescue verification: 843 checkpoints in Java and PyBoy, ending in
  overworld `$35`, Link `(104,80)`, health `32`, BowWow following. Route SHA-256
  `bdb62bf6d714e77b97f1e670e849597f19e9e4e310c751194c6d525dda152e9d`.
  Latest recorded full suite: 2,379 tests, zero failures/errors.
- Audio: ROM-backed sequencing/output is documented in the
  [audio guide](audio-engine-guide.md); this does not establish full audio parity.

## Pending

- BowWow chain OAM (`$05:4253`) and tether movement (`$05:43FE`); verify
  follower continuity before proceeding to Goponga Swamp and the second dungeon.
- Whole-scene, follower, combat/power-up and entry-motion phase differences.
- Dynamic ROM OAM allocation; see [ROM symbols](ROM_SYMBOLS.md) rendering TODO.
- The rest of the game remains unverified and is not production-ready.
- Detailed architecture documentation awaits the concurrent package refactors.

## How verification works

Use [BUILDING.md](BUILDING.md) for tests and emulator capture/comparison tools.
ROM tests use `@RequiresRom` and `TestRom`; ROM-free tests run independently.
Opening routes replay normal button inputs from a fresh game in Java and PyBoy.
Record route hashes, checkpoints, state and RGB555 comparison regions. Explicit
input overrides and timing differences must be disclosed; region parity does
not imply whole-scene parity. The latest rescue used no emulator memory injection.

The [full dated log](history/reconstruction-log.md) is preserved byte-for-byte,
including detailed technical findings and historical limitations. Its original
repository-relative paths remain historical references. Current evidence pages
are indexed in [README.md](README.md); local `/tmp` captures may no longer exist.
