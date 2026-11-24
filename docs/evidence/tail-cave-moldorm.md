# Tail Cave Moldorm and the Cello

The original runs use English DX v1.0 and the 669-checkpoint source prefix with SHA-256 `16c78a13a8cc655a48bd5506e9ade8872731ae82a46293c274b934443540ab49`. The Java probe starts from the prepared checkpoint 497 described in [the Rolling Bones evidence](tail-cave-rolling-bones.md) and follows that probe's controller variant into room `$06`. The checkpoints and frame counts below are local to their respective recordings.

## Head clink and arena movement

The normal original route damages the tail at frame 62285. The head clink at frame 62381 instead reaches `$03:70FF` and jumps to the standard Link recoil code at `$03:6DDF`. CPU hooks at `$04:56B0` and `$03:7570` show Link raw (80,82), Z3 and the boss at (72,57), with the sword rectangle at (88,69). The recoil writes `(+7,+24)` and an ignore-collision timer of `$10`. The damage buffer stays zero and the boss retains three health. Java's head-clink event used to emit only the clink and hit lockout. The real enemy-combat path now applies the corresponding non-damaging recoil even during the Feather jump.

Native hooks immediately before and after the background pass at `$04:5AF1` and `$04:5AF4` resolve two movement cases.

- At natural frame 62409, Moldorm starts at (121,70). The first accelerated pass advances to (122,70), and the second integrates to (123,71) before the wall check. The right-side block restores X121, preserving Y71 and both integrated fractions. This is the frame-entry X from `$04:5791`, even on the second movement pass.
- A separate controlled ROM hook at `$04:5AEE` on frame 62607 places Moldorm at (123,44), velocity (+14,0) and X fraction zero. The first pass leaves the integer unchanged but advances the fraction to `$E0`. The right-side wall flag is still set. The background sampler tests every nonzero velocity and sees the integrated X and Y before restoring a blocked axis.

The Java Moldorm motion now follows that two-axis order, including the fraction-only probe and frame-entry rollback. The movement regression records the X and Y seen by each probe. The clink regression feeds the original combat frame into the room combat entry point.

## Normal controller continuation

With the corrected mechanics, reusing the old stationary pulse sequence from the controlled entry still falls into `$1A`. That entry has Link at (80,115), four health, facing up on the floor, no open dialogue, and frame counter 241. Moldorm is at (82,30) with four health. The original recording enters this fight on a different movement and dialog phase.

An external controller experiment aimed at the visible tail while staying on the central floor. Freezing those actions into 98 ordinary directional or A-button rows reproduces the win in Java from the same entry. The fight takes 310 frames, ending after the fourth tail hit with Link still at four health. The frozen controller rows have SHA-256 `f4b2d3328fc5d1380d6891dd183cb458b30e46b4b1f951c8b841f0e2b4b93ff7`. Playing those same rows after checkpoint 615 of the fresh original capture leaves the boss at four health and Link dies at original frame 62558. The original's own button sequence still wins the fight.

The frozen Java fight ends at (62,54). Waiting 335 frames spawns the Heart Container at (97,72). Right to X97 and Down to Y73 collect it, increasing maximum health from 24 to 32 and restoring all health. Left to X80 and Up enter the instrument room `$02`. Continuing Up to Y59 triggers the Full Moon Cello, and the native-style dialog inputs then start the exterior warp. The post-Cello route reaches the village alert and the return north into overworld room `$52`. This comparison uses a prepared dungeon entry and temporary controller data.

The longer fixed-input Java replay uses an immediate walking approach for the four return rows through `$52` (source checkpoints 737–740). It clears the hideout guards and reaches the King at room `$15:$E1`. After the King dialog, the prepared state has Link at (10,64) with 20 health and clock 98. The King is at (88,96) with eight health. The controlled King inputs alternately bait charges from the side lanes, step out of the charge path, and swing during the wall stun. They replay from a frozen 485 rows (2,672 frames, SHA-256 `eac29c1c491ed2e791d1eb903ef227249d96f99f16a55839726873727dcdd27b`) until the King dies. Continuing the rescue and exit inputs leaves BowWow state 1 and Link outside in room `$35` at (104,80), with 20 health. The complete prepared input file has 853 rows, numbered 498–1350 in its log, and SHA-256 `227cbd3e6375002fa3cb47d3e22d810d536078e06c146e7dad44648ae7e447fa`.

The isolated verification passed 22 tests across `EntityRuntimeMoldormTest`, `MoldormRuntimeTest`, `MiniMoldormMotionTest` and `GameplayRouteTest`, plus `spotlessJavaCheck`.

## Countdown ordering at the arena entry

The original runs its three shared countdowns at `$14:4DA8–4DC2` before the Moldorm handler's movement guard. Read-only native hooks record the opening draws as 20 and 86 at frame 62089, giving transition 36 and turn step +1. The first dispatch sets horizontal speed 16 and steering countdown 6. The arena doorway squeeze writes `wC111=2`. The key distinction is that `wC1A8` blocks motion during the squeeze but the shared countdown phase keeps ticking. On resumed frame 62105, the transition counter is 20, steering is zero, and Moldorm integrates to (57,48) before setting speeds (+14,+6). Java previously left the private steering counter at 6 during the pause.

On frame 62121 the original begins its intro with boss raw (67,57), transition countdown 4 and steering countdown 2. The dialog keeps those values through frame 62234. Normal dispatch resumes at frame 62235, when the countdowns are 3 and 1 and the boss moves to (68,58). Java previously depleted the transition countdown through the dialog. The corrected Moldorm path applies the common timer gate to transition, flash and private countdowns. The separate ignore-hits decrement at `$04:56B6` still runs when the shared clock is paused. A new tail hit retains the original acceleration countdown `$C8` on the hit frame.

The fixed-input rescue measurements above predate this countdown correction and document that earlier controlled build.

The room-runtime regression using the measured two RNG bytes matches (67,57) immediately before the dialog and (68,58) after it, with transition values 4 and 3. The post-countdown build also won another controlled normal-input fight from the prepared post-Rolling Bones route. That newer fight uses 123 rows and 413 frames (SHA-256 `c0a387f206661b8ffc1ed824b90f68525f3fb41d71196515f2fc4114c47d68ec`). This distinguishes the fixed room-entry clocks from remaining differences in a full replay's RNG state.

A separate controlled head-clink probe changes only the flash byte at `$04:56B0` on native frame 62381. At `$17` the source rejects the head weapon check at `$03:6E3B`, leaving Link's speeds and ignore timer zero. At `$18` it clinks and applies the original (+7,+24) recoil and timer 16. Java now samples the flash byte after the upcoming common timer decrement and applies the late-flash head gate. The tail remains gated until the same sampled byte reaches zero, as checked at `$04:59ED`. These experiments inject the flash byte and therefore do not serve as route results.

In a separate controlled body-contact probe on frame 62381, Link at the boss's raw (72,57) with airborne state 1 and Z0 still takes eight units while the boss has flash `$17` and an ignore-hits countdown of 12. The body check at `$03:6C72` uses Z rather than the airborne state. It precedes the enemy's flash and ignore-hit gates, which apply to the weapon check. This body difference remains outside the late-flash fix.
