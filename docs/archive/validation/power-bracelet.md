# Power bracelet lifting verification

Verified 2026-11-07 against the user-supplied English DX v1.0 ROM.

Implemented paths:

- Hold the equipped bracelet button to grab; pull opposite the facing direction.
  Grabbing preserves facing and position and uses the original grab/pull poses.
- Both levels lift outdoor rocks/bushes and indoor pots, including switch pots.
  Normal pulls take eight frames; Piece of Power takes three.
- Statues require level two. Their pull counter also uses three frames for
  Guardian Acorn, matching $19:40EC's nonzero-power-up test.
- Carry transitions use the original slow/fast tables. Level two, red tunic,
  Piece of Power, and bombs select fast timing. Stored phase and display phase
  remain separate, including the phase-zero local-E special case.
- Throw requires a fresh press, respects B-slot priority, and retains the
  original fallback to either action button when the item is unequipped.
- Outdoor reveals update the live ROM-backed GBC cell. Statues use the ROM
  rectangle lists, including Bottle Grotto's alternate list. Thrown rocks/bushes
  smash; statues emit six ROM rubble sprites and the two poof requests.

Source locations: `bank0.asm` $1F49-$21A7; `bank3.asm`
`EntityLiftedHandler` $5732-$57E6; `bank14.asm` $537D-$53A3 and
`RevealObjectUnderObject`; `19_liftable_statue.asm` $4000-$41FE;
`19_side_view_pot.asm`. Side-view pots retain their initial pickup delay;
subsequent carry presentation is not suppressed during transition timers.

## Checks and comparison scope

`tools/lifting/probe.py` runs a controlled, RAM-injected carry-handler probe in
PyBoy. It needs the local ROM and `LADX-Disassembly/azle.sym`; it writes only
numeric traces under `java/build/bracelet-verification`. For direction right,
level one reaches carry state 1 on frame 20, level two on frame 12. The Java
runtime regression asserts those absolute frame counts. This probe verifies
carry timing, not full-room pixel parity or an uninterrupted gameplay route.

Full ROM suite: 2,492 tests, four baseline failures listed below. ROM-free
suite passes. Focused lifting and physical traversal tests pass.

Tests cover grab/pull position and facing, interrupted pulls, outdoor object
replacement, bracelet-level statue eligibility, strength timing, fresh-press
throwing, B-slot priority, statue rectangle decoding, six-piece rubble expiry,
side-view pots, and throw damage. ROM-free tests remain available.

The physical gameplay route in `RoomTransitionCoordinatorTest` passes after
correcting the side-view-pot presentation gate. The separate new-game opening
route is still blocked by an existing failure. The following four failures
were individually reproduced on baseline commit `066db24`, before either
bracelet commit:

- `RoomSessionTest.openingBeachEventsCompleteInPlayOrderAndStayGoneAfterReload`
- `BeachOpeningRuntimeTest.swordPickupUsesTheRomFanfareSequenceBeforeGrantingTheSword`
- `DialogEmulatorParityTest.bedExitMatchesEveryFrameOfHopLandingAndNpcOverlap`
- `MainOpeningRouteTest.newGameCanDefeatRollingBonesAndWarpBackToTheEntrance`

The shorter `house-route.tsv` was captured in Java and PyBoy and compared at
22 checkpoints (dialogue, movement in Marin's house, and exit). Route SHA-256:
`e6065107a5ef0ed993855dc4e7acfabe8f4bfb5756106b5032bfede2e12b5142`.
All 22 checkpoints have pixel differences (worst: 20,298 pixels); Java remains
in the house at the last checkpoint while the ROM exits to A2. This route
contains no bracelet use. No game-wide or full-room lifting parity is claimed.
Generated screenshots, ROM data, and extracted assets are not committed.
