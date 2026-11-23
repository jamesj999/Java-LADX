# Octorok/Moblin simultaneous body and sword contact, 2026-11-07

Plain Octorok/Moblin body contact now retains Link's recoil when the same
frame also damages the enemy with the sword. Previously Java applied health
damage to both participants but suppressed Link's recoil. The change keeps
existing damage, sword damage, collision parity and invincibility acceptance;
it corrects the missing speed/countdown response for these two enemy types.

## Source and controlled observation

`AnimateRoamingEnemy` calls the shared combat handler at `$03:585B` before
its ordinary movement/state dispatch. `DefaultEnemyDamageCollisionHandler`
(`$03:6E28`) calls the body contact path before checking the sword rectangle
at `$03:6E2B`. Link's recoil is written through `$03:7565`, before sword
damage at `$03:719D`. A subsequent ordinary sword hit does not cancel that
already-written Link response.

PyBoy 2.8.1 CPU hooks measured two **RAM-injected diagnostic cases**, using
the user-supplied English DX ROM SHA-256
`c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`.
Both cases place an Octorok in slot 2 at `(58,82)`, state 1, countdown 100,
velocity/fractions zero, health 8; Link is `(51,88)`, Z=0, global frame 135.
No input or random call occurs during the measured handler step.

For the simultaneous case, a CPU hook installs the sword rectangle
`(x=66,width=5,y=90,height=5)`, collision-enabled byte 1 and sword level 1
at entry to the shared combat routine, after ordinary equipment processing.
This controlled rectangle is an injected combat input, not a claim that a
normal replay reached this exact sword pose.

| Observation | Body only, ROM | Body + sword, ROM | Body + sword, Java before | Body + sword, Java after |
| --- | --- | --- | --- | --- |
| Link health buffer | 4 | 4 | damage event 4 | damage event 4 |
| Enemy health | 8 | 7 | 7 | 7 |
| Link velocity | (-20,+17) | (-20,+17) | response absent | (-20,+17) |
| Link collision-ignore countdown | 16 | 16 | response absent | 16 |
| ROM Link invincibility | 80 | 80 | existing app gate | existing app gate |

Both source CPU hooks see cached and current entity position `(58,82)`;
there is no pre/post-movement ambiguity in this stationary case. The Java
standalone runtime probe reproduced the missing response before the change
and the matching response afterward. The focused test also covers Moblin,
which uses the same plain shared handler, and verifies that frame 134's
opposite collision parity still applies sword damage without body recoil.
Iron Mask and custom boss paths are deliberately outside this change.

Reproduce source measurements with your ROM and `tools/requirements.txt`:

```sh
python tools/leever/capture_rom.py --case octorok-body --output /tmp/octorok-body.json
python tools/leever/capture_rom.py --case octorok-sword --output /tmp/octorok-sword.json
```

The existing probe's `free` and `wall` Leever cases were rechecked after
adding these modes. JSON records the RAM writes and CPU-hook observations;
no ROM, save, extracted graphic or audio asset is committed.

## Verification and limits

Six focused tests passed: `RoamingSimultaneousContactTest` (2 tests, each
covering both plain enemy types) and `LeeverContactRecoilTest` (4 tests).
Python probe syntax and all four source probe modes passed.

The earlier canonical replay, SHA-256
`bdb62bf6d714e77b97f1e670e849597f19e9e4e310c751194c6d525dda152e9d`,
had completed 73 checkpoints before dying at frame 10006 in room `$F1`,
Link `(82,28)`. Ordinary controlled body-contact geometry agrees with Java;
this simultaneous-sword defect is a separate proven mismatch. It has not
been established as the cause of that replay death. No full opening route
was rerun for this milestone while concurrent grass/render/audio fixes were
being completed. Rescue continuity and broader position/visual fidelity
remain unverified; this is a bounded combat response correction.
