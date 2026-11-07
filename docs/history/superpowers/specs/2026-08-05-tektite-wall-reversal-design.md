# Tektite wall-reversal design

## Context

The Java Tektite runtime already mirrors the bank-$06 jump, landing, inertia,
random direction, and Link-vector behavior. It computes the proposed X/Y
position but currently leaves the supplied background-collision callback
unused. The source handler calls `ApplyEntityInteractionWithBackground` after
`UpdateEntityPosWithSpeed_06`, reads the resulting collision flags, and then
dispatches the horizontal and vertical Tektite collision helpers.

The authoritative source is
`LADX-Disassembly/src/code/entities/06_tektite.asm:$78D6-$7988`:

- flags `$01/$02` select the horizontal helper;
- flags `$04/$08` select the vertical helper;
- each helper complements and increments the byte in
  `wEntitiesSpeedXTable`, then performs an arithmetic right shift by one;
- the source vertical helper also targets `wEntitiesSpeedXTable`, so that
  unusual X-speed effect is preserved rather than corrected by the Java port.

## Selected approach

Use the existing `RoomEntityBackgroundCollision` callback as the Java model of
the source directional collision flags. After fixed-point movement, query the
proposed X and Y positions independently using the existing right/left/up/down
direction values. Restore a blocked coordinate, then apply the source
negate-and-arithmetic-half helper once for a horizontal collision and once for
a vertical collision, in the same order as the ROM. Leave Tektite state,
Z-motion, random launch logic, and the generic collision interface unchanged.

This is preferable to introducing a generic collision-flag buffer because the
runtime currently has no persistent WRAM collision table; it also avoids
duplicating a second Tektite movement state machine.

## Verification and boundaries

Focused tests will launch a Tektite with ROM speeds `$10/$10`, block only the
right direction, and assert that X remains fixed while speed X becomes `$F8`.
A second test will block only down, assert Y remains fixed, and assert the
literal source vertical helper also changes speed X to `$F8` while speed Y is
unchanged. The complete Java suite and `git diff --check` must pass.

This increment does not claim to implement Tektite recoil, water/pit/
conveyor interaction, or the remaining damage-state matrix.
