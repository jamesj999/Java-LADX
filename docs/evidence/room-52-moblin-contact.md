# Moblin contact in overworld room $52

The ordinary Moblin uses the shared shield rectangle and recoil path at
`$03:6EA5–6FE1`. On contact it writes Link recoil of magnitude 18 and 12
collision-ignore frames. Its own recoil starts on the next dispatch.
For Sword Moblin, `$07:7969` invokes the separate blade test at `$03:73EB`
before the ordinary body and weapon tests. The blade can clash with either
the shield or sword rectangle. A clash writes Link and enemy recoil,
eight collision-ignore frames each, a 12-frame Link clash cooldown and
an alert counter of four. An active sword holds its middle pose for 11
following ticks and clears when the 12-frame animation countdown expires.

Sword Moblin recoil temporarily swaps speeds and uses the ordinary
position accumulators. The return at `$07:7E08` ends that dispatch
whenever incoming recoil is active, including zero velocity. The pursuit
countdown refresh occurs on the first incoming recoil dispatch, after
the frame that wrote the new recoil. The Java motion now preserves those
fractions, clocks and ordering.

## Controlled source observations

PyBoy 2.8.1 used the English DX ROM with SHA-256
`c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`.
The observations mix read-only hooks during the button route and
RAM-injected diagnostics using matching poses in the same loaded room.

| Case | Link | Moblin | Link recoil | Ignore frames |
| --- | --- | --- | --- | --- |
| Plain Moblin, raised shield at global frame 171 | (72,94) | (63,80) | (+11,+18) | 12 |
| Sword Moblin, shield or active sword, frame 148 | (63,102) | (44,87), variant 7 | (+18,+14) | 8 |
| Sword Moblin, unguarded blade, frame 148 | (56,91) | (44,87), variant 7 | (+20,+6) | 16 |

The unguarded blade buffers four health units and starts invincibility at
80. Existing invincibility blocks blade damage and preserves shield or
sword clashes. In an injected simultaneous plain body/shield contact at
Link (70,88), body damage and (+17,+20) occur first, followed by the
no-damage shield recoil (+15,+18).

Three additional ROM diagnostics used actual Java contact poses and
matched the observed Link recoil (+8,+18), (+18,+14), and (+18,+12).
Link pixels matched across the first four following steps. With shared
enemy fractions and the early handler return, Java also matched the
first 3–4 recoil steps on enemy position, fractions, facing and pursuit
countdown. The shorter comparison stopped before a native wall contact.
An injected long-fall check verified left facing, falling timers
$6E/$6D/$6C and variants 4/4/5 after zero-speed incoming recoil.

## Route and test scope

Focused tests cover plain and sword Moblin, simultaneous body and weapon
contact, the swing hold, falling and shared combat boundaries. The
Key Cavern entrance fixture uses a small body-centered sword box so it
targets the body without also intercepting the blade.

The checked-in rescue route SHA-256 was
`62c1e95dd6d63f459a0cc541578591a90508daddf82d0b6612101d807bde3804`.
The read-only ROM route entered room $42 at (89,127). A Java prefix on
the integrated snapshot after the motion fixes reached 155 checkpoints
and died in $52 at frame 20508, (78,86). Java-only overrides to match the
ROM's room-$52 controls advanced farther along the climb and still died
in $52. The routes use different inputs in places, enter with different
health and frame counters, and retain differing enemy state. The
controlled matches establish the cited contact behavior and leave rescue
continuity unresolved.

## Sword-hit target and slot order

`$03:719F` and the shared clink branch at `$03:70E9` write `wC1AC` as the
contacted slot plus one. The ordinary rectangle at `$03:6E40` rejects the
same slot. The Sword Moblin blade gate at `$03:73EB` rejects any nonzero
latch. Starting and finishing the swing clear it at `$00:1541` and
`$02:48B1`. Starting a held-sword pose or starting a dash also clears it.

Controlled descending-slot probes used Link (67,120), weapon rectangle
(75,8,120,3), and a Sword Moblin in slot 0 at (47,104), variant 6.
Its isolated clash writes (+18,+14) and ignore 8. A sword hit against a
plain Moblin in slot 1 first writes latch 2 and suppresses the clash.
A previously latched 2 has the same effect. An accepted body hit in
slot 1 instead writes ignore 16 and buffers 4 health units, which also
suppresses the lower clash. Initial invincibility 5 suppresses the
higher body hit and preserves the lower clash.

When a normal swing and a shield are active together, `$02:4878` leaves
the shield box on odd frames. Native CPU hooks at Link (63,102) observed
shield rectangle (71,8,102,3) on frame 137 and sword rectangle
(71,8,94,8) on frame 136. An isolated Java comparison on working route
SHA-256 `d21b8ba1c6f702689be10cda86e58b564959f14427431ac5c02be56e693ad208`
reached return room $52, then died at frame 27582 (67,56), with only
the priority change disabled. Including the priority change shifted the
first failure back to room $F1, frame 8990 (68,15), on that build.

## Projectile damage on the return

At the return-route Java pose, the native injected left-moving Moblin
arrow at (80,67) contacted Link (75,63), facing down with shield raised.
With collision-ignore zero the arrow unloaded and Link received four
buffered damage, invincibility 80, and recoil (−20,−16) with ignore 16.
With ignore 12 the arrow still unloaded and left Link's damage and motion
unchanged. Facing right instead preserved shield deflection, collision
$FF and the arrow's 24-frame bounce timer.

`HealthGroupForEntity` gives the Moblin arrow and Octorok rock group $0D,
whose `EntityDamagesForGroup` entry is four. The shared projectile path
now uses the ROM slot combat table. It checks Link's collision-ignore
countdown in the damage branch, after shield selection, following
`$03:6C5B` and `$03:6D56`.

The later parity-on Java replay of working route `d21b8ba…` included the
ordinary shield and projectile fixes. It completed 73 checkpoints and
ended in $F1 at frame 8928, Link (68,12). It produced no completion
manifest and did not reach the return approach. The isolated focused
suite ran 219 tests, including projectile collision, sword and Moblin
contact, Key Cavern, falling, allocation and architecture checks.

## First shot after a room load

The initializer shared by the Octorok ($09) and ordinary Moblin ($0B)
writes 160 to private countdown 1 at `$03:4FAF–4FB4`. The central timer
pass at `$14:4D97–4DB9` decrements it before the active handler. When the
roaming transition timer reaches ten, `$03:586C` checks that private
countdown before checking facing or spawning a projectile.

A fresh 112-checkpoint PyBoy prefix (SHA-256
`00b2e5ad0993e0038a8ff350a7e8e761a632b64ce986bd76550849a24fea9c84`)
produced 21 initializations at 160. Of 81 shooting opportunities, the
private timer rejected 50. The remaining 31 reached the facing check and
produced seven arrows and one rock. In forest room $62, the Moblin
initialized on frame 14659. Its first active dispatch read 159 on frame
14701, its first shooting opportunity read 151 on frame 14709, and it
reached the facing check on frame 14902. The Java regression forces a
valid facing and firing opportunity on every active tick and checks
the first shot on tick 160, including a 40-frame room scroll.
