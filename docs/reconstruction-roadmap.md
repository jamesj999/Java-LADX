# Reconstruction status

Open source Java 21 preservation/research engine; users supply their own ROM.
Never commit or distribute ROMs or extracted original assets.

## Implemented and verified scope

Verified through 2026-11-24. Fresh Java and PyBoy games both complete the
BowWow rescue. The first room mismatch on the onward swamp route is at
checkpoint 918.

- Graphics: ROM-driven intro/menu scenes, overworld objects, GBC overlays,
  palettes, tilesets, animated tiles and entity/OAM rendering.
- The [899-checkpoint rescue](evidence/rescue-forest-approach.md) runs from
  the opening house through the beach sword, forest, Tail Key, Tail Cave,
  Full Moon Cello and Moblin Hideout. Both engines finish in overworld
  `$35` at (104,80) with BowWow and four full hearts. The route records
  separate controller inputs where the fights and movement diverge.
- Focused Tail Cave comparisons cover [chest-room Sparks](evidence/tail-cave-chest-sparks.md),
  [side-view Goombas](evidence/tail-cave-goomba-patrol.md), [Spike Traps](evidence/tail-cave-spike-traps.md),
  [Rolling Bones](evidence/tail-cave-rolling-bones.md) and [Moldorm](evidence/tail-cave-moldorm.md).
  [Pit-edge movement](evidence/pit-edge-movement.md) covers escape, pull cadence
  and the centered fall.
- BowWow's chain OAM (`$05:4253`) and tether movement (`$05:43FE`) are
  implemented. A [fresh 1,190-checkpoint ROM run](evidence/bottle-grotto-entrance-torches.md)
  crosses Goponga Swamp, opens Bottle Grotto, defeats Hinox and both Boo
  Buddies, and collects the Power Bracelet. On the same route, fresh Java
  first misses the west exit from overworld `$44` and eventually dies in
  `$24`. With a controlled swamp start Java clears the flowers, opens the
  dungeon and reaches Hinox. A separate controlled Boo Buddy encounter
  awards the Bracelet. The [Feather crossing](evidence/room-44-feather-landing.md)
  and [BowWow spawn height](evidence/bowwow-follower-spawn-height.md) have
  separate comparisons.
- The [beach contact comparison](evidence/leever-contact-recoil.md) covers
  shield, sword and body responses. [Enemy recoil](evidence/beach-enemy-recoil.md)
  and [room allocation](evidence/room-entity-initialization.md) have
  frame-by-frame checks on the beach crossings.
- From a [controlled Key Cavern entrance](evidence/key-cavern-entrance-controls.md),
  the same 2,503 input frames earn and spend the first small key in both
  engines. With separate combat inputs, both earn and spend the second key
  and take the stairs to `$02:$4D` at (72,80).
- [Slime Eye](evidence/slime-eye-controls.md) is connected through the production
  engine. A directly initialized D3 encounter completes with normal buttons
  only after setup, including both halves and heart collection. Live ROM
  checkpoints exposed and verified specific sword/boots/recoil/death corrections.
  Live trajectories, damage and pictures still differ. Earlier
  [fixed-pose and injected-handler comparisons](evidence/slime-eye-half-motion.md)
  remain scoped; there is no normal-route D3 completion claim.
- [Scroll landing and grass movement](evidence/overworld-scroll-entry.md) have
  controlled source evidence for preserved entry coordinates/fractions and
  world-frame slowdown timing. The [grass overlay](evidence/link-grass-overlay.md)
  matches one aligned right-entry frame at RGB555 precision. Link body tile and
  attribute uploads now follow [gameplay VBlank transfer priority](evidence/link-body-vblank.md).
- [Intro audio measurements](intro-audio-validation.md) cover production PCM,
  source noise requests/writes, and isolated noise-envelope behavior. Measured
  noise corrections are implemented; sample-clock, mixer, waveform and other
  audio differences remain. See the [audio guide](audio-engine-guide.md) for
  current session wiring. No full audio parity claim is made.

## Pending

- Resume the fresh swamp approach at checkpoint 917 in overworld `$44`.
  Java is at (67,84) and PyBoy at (64,73), both at full health. On the next
  westward crossing PyBoy enters `$43` while Java stays in `$44`. Compare
  the approach and landing before extending fresh Java into Bottle Grotto.
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
