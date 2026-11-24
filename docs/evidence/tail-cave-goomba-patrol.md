# Goomba patrol in Tail Cave's side-view passage

On the rescue route, Link takes the stairs from $00:$03 to side-view
room $00:$19. The room loads two Goombas at (40,112) and (88,112). PyBoy
2.8.1 used the English v1.0 ROM and the first 477 checkpoints of rescue
route `de3e447d…`. The prefix SHA-256 is
`69feb03957ef52fb32576f647cd09fb6003e73c3a967f3ca0da0a79e84828628`.

The first active handlers start at frame 52259, global counter 86. Both
set horizontal speed to eight sixteenths of a pixel. During patrol,
`$07:66B8–66C0` integrates both speeds, adds two to vertical speed and
checks background collisions. Any nonzero speed probes its axis, even
when only the fraction moved. The handler animates on floor contact and
uses variant zero in the air. A downward contact first restores Y, then
snaps the restored pixel to its tile row plus three. A horizontal
contact reverses direction. At frame 52329 slot 1 hits the right wall
at (122,115) with collision mask nine and changes speed from +8 to −8.

A Java comparison loaded the real room, placed Link at the native
ladder entrance and aligned the first dispatch clock. Position and
variant matched on all 130 consecutive pre-recoil samples for slot 0
and 133 consecutive samples for slot 1. Both Goombas reached floor
height 115. The natural ROM trace shows slot 0 starting ten ticks of
recoil at (104,115), velocity (−32,+6), fractions ($80,$80).
During frames 52389–52398 each dispatch returns at `$07:7E08`.
X decreases two pixels per tick to 84. The floor holds Y at 115 while
the Y fraction advances to $40. Ordinary patrol velocity stays (+8,0).

A separate controlled ROM probe set the side-view flag to zero during
one state-one dispatch and started velocity at (+8,0). It reached the
ordinary-room motion and wall routines at `$07:665A` and `$07:665D`
and returned with (+8,0). That walker uses the selected direction
until the timer expires. At `$07:6656–6659` it returns to state zero
for a 32-tick pause. The top-down and side-view handlers previously
shared Java's gravity and floor response.

Checkpoint 473 observes different phases of the stair warp. The ROM
has selected the destination room and Link is still at (136,36).
By checkpoint 474 the ROM has spent 27 active Goomba dispatches in the
new room, ending at (53,115) and (101,115), with Link at (120,16).
The Java runner waits for the warp to finish before applying the
recorded 64-frame idle. These observations bound the motion comparison
independently of when a particular controller replay meets an enemy.

Supplying the measured recoil vector and ten ticks at the first native
contact extended the controlled comparison through the end of the
recording for both slots. All 149 ordinary pre-motion samples and ten
recoil returns for slot 0 matched on position and sprite variant. Slot 1
matched on all 133 ordinary samples. The trigger was supplied in Java
to isolate how motion uses the existing velocity and fractions.

The revised rescue route gives Java 24 idle frames at checkpoint 474
and preserves the ROM's 64. From the fresh Java start the Goombas now
end that checkpoint at the native coordinates (53,115) and (101,115).
The next shared Down+B row reaches (120,112) with four health in both
engines. The two Goombas are at (102,115) and (95,115) on both replays.

## Contact and bounce

A separate controlled PyBoy probe started from checkpoint 474, aligned
Link with slot 1's hitbox and ran one handler per case. The live hitbox
bytes were (8,5,8,5). The side-view handler enters the shared collision
routine at $03:6C77 on both frame parities. At clocks 113 and 114 an
airborne Link with Y speed +8 stomped the Goomba. It entered state 2
with transition timer 48. Link kept X speed +12, acquired Y speed −16
and set the two-tick bounce countdown. With Y speed −8 and countdown
zero he took the damage branch instead. With countdown one the ascending
Link stomped, while a grounded Link still took the damage branch.

With the top-down flag supplied during the same dispatch, Link stomped
at Z 7 and Z velocity −8. At Z 8 the handler returned before contact.
At Z 7 and velocity +8 he took the damage branch unless the countdown
was already active. The top-down bounce wrote +16 to Z velocity and
preserved X speed. These single-dispatch probes supplied the player
state and do not measure the frequency of natural stomps on the route.
