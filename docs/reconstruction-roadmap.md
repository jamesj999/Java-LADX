# Reconstruction status

Open source Java 21 preservation/research engine; users supply their own ROM.
Never commit or distribute ROMs or extracted original assets.

## Implemented and verified scope

Current bounded evidence through 2026-11-07. Tests and isolated comparisons do
not establish complete parity or a restored normal progression route.

- Graphics: ROM-driven intro/menu scenes, overworld objects, GBC overlays,
  palettes, tilesets, animated tiles and entity/OAM rendering.
- Gameplay implementations include the opening house/dialogue, sword, forest
  and Witch exchange, Tail Key, Tail Cave, Rolling Bones, Moldorm, Heart
  Container, Full Moon Cello, hideout guards, king fight and BowWow rescue.
  The historical 843-checkpoint rescue replay is recorded in the dated log;
  it is not the current rebuilt-checkout result.
- BowWow chain OAM (`$05:4253`) and tether movement (`$05:43FE`) are implemented.
  Current fresh-game rescue continuity has **not** been restored. The unchanged
  canonical route currently reaches 73 checkpoints and dies at frame 10006,
  overworld `$F1`, Link `(82,28)`. Its SHA-256 remains
  `bdb62bf6d714e77b97f1e670e849597f19e9e4e310c751194c6d525dda152e9d`.
- [Leever contact recoil](evidence/leever-contact-recoil.md) now uses the source
  post-movement position for recoil while retaining the cached contact hitbox.
  Controlled free-sand/wall probes verify that narrow correction; the current
  rescue replay still fails as above.
- [Slime Eye](evidence/slime-eye-controls.md) is connected through the production
  engine. A directly initialized D3 encounter completes with normal buttons
  only after setup, including both halves and heart collection. Live ROM
  checkpoints exposed and verified specific sword/boots/recoil/death corrections.
  Live trajectories, damage and pictures still differ. Earlier
  [fixed-pose and injected-handler comparisons](evidence/slime-eye-half-motion.md)
  remain scoped; there is no normal-route D3 completion claim.
- [Scroll landing and grass movement](evidence/overworld-scroll-entry.md) have
  controlled source evidence for preserved entry coordinates/fractions and
  world-frame slowdown timing. Link body upload staging and grass overlay OAM
  remain open; these are not whole-scene pixel matches.
- [Intro audio measurements](intro-audio-validation.md) cover production PCM,
  source noise requests/writes, and isolated noise-envelope behavior. Measured
  noise corrections are implemented; sample-clock, mixer, waveform and other
  audio differences remain. See the [audio guide](audio-engine-guide.md) for
  current session wiring. No full audio parity claim is made.

## Pending

- Restore and compare current opening/rescue continuity before claiming the
  Goponga Swamp route or second-dungeon progression is verified.
- Source RNG consumption cadence and hardware LY inputs, live combat/position
  timing, follower behavior, and complete reward/presentation comparisons.
- Dynamic ROM OAM allocation; see [rendering](RENDERING.md).
- Normal-route D3 approach/entry/completion and the rest of the game remain
  unverified and are not production-ready.

## How verification works

Use [BUILDING.md](BUILDING.md) for tests and emulator capture/comparison tools.
ROM tests use `@RequiresRom` and `TestRom`; ROM-free tests run independently.
Opening routes replay normal buttons from a fresh game in Java and PyBoy.
Record route hashes, checkpoints, state and RGB555 comparison regions. Explicit
input overrides, setup injection, clock alignment and timing differences must
be disclosed. Region parity and engine-internal compositor parity do not imply
whole-scene or emulator parity. Failed captures retain observations and do not
produce a completion manifest.

The [dated log](archive/history/reconstruction-log.md) remains historical evidence,
including the earlier successful rescue and its then-current limitations.
Current evidence pages are indexed in [README.md](README.md); local `/tmp`
captures may no longer exist. This status page does not rewrite those records.
