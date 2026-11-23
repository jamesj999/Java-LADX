# Leever contact recoil: bounded source comparison, 2026-11-07

A moving Leever used its previous position to select Link's recoil vector.
The corrected path keeps the original contact hitbox, accepts damage through
the existing player gate, and selects recoil from the Leever's actual position
after movement and background collision. It does not change RNG or route inputs.

## Source timing

`LeeverEntityHandler` moves at `$04:7F01` and resolves background collision at
`$04:7F04`, before the chasing handler calls the shared combat routine at
`$04:7F49`. These operations do **not** make every collision input current:
`CheckLinkCollisionWithEnemy` (`$03:6C72`) reads cached
`hActiveEntityPosX`/`hActiveEntityVisualPosY`, whereas recoil at `$03:7565`
uses `GetVectorTowardsLink` (`$03:7E45`), which reads the updated entity
position tables (`$03:7ED9`/`$03:7EE9`). Moving the whole combat pass would
change the source hitbox timing.

The app marks only the slot whose damage was accepted. That slot emits a
silent, zero-damage response after its Leever movement. The response captures
Link's recoil speed for the next Link step. Damage, invincibility and the
initial collision-ignore gate retain their existing acceptance path. Pending
responses clear at every new combat pass and at tick completion, including
skipped handlers. Other entity families retain their existing order.

## Controlled measurements

These are **RAM-injected diagnostic setups**, not normal-input progression or
whole-scene parity. PyBoy 2.8.1 runs the user-supplied English DX ROM, SHA-256
`c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`.
Both cases use slot 2, Leever state 2, global frame 35, countdown 109,
velocity `(-8,0)` in sixteenths of a pixel, zero fractions and Link Z=0.
No input or RNG call occurs in the measured handler step. CPU hooks observe
the cached collision pose and the immediate recoil write.

| Case | Link | Cached Leever | Leever after background collision | ROM recoil | Java before | Java after |
| --- | --- | --- | --- | --- | --- | --- |
| Free sand | (109,39) | (110,40) | (109,40) | (0,-20) | (-20,-20) | (0,-20) |
| Background restores X | (63,64) | (64,65) | (64,65) | (-20,-20) | (-20,-20) | (-20,-20) |

Both ROM contacts buffer 4 health units, set invincibility to 80 and the
collision-ignore countdown to 16. The deferred Java event adds no damage,
invincibility or sound. Synthetic Java coverage also checks a contact rejected
by the player gate and a skipped handler followed by another frame. A Link
adapter check verifies that the next actual Link step has no erroneous left
movement in the free-sand case.

Reproduce local source observations with the dependencies from
`tools/requirements.txt` and your own ROM:

```sh
python tools/leever/capture_rom.py --case free --output /tmp/leever-free.json
python tools/leever/capture_rom.py --case wall --output /tmp/leever-wall.json
```

The tool writes diagnostic JSON only; it does not write a save or distribute
ROM data or graphics. Local observations used `/tmp/ladx-leever-contact/`.

## Verification and remaining scope

Focused checks: `LeeverContactRecoilTest`, `LinkCommandAdapterTest`,
`WorldDependencyTest` and `MainArchitectureTest`. The first two cover the
state response and observable next movement; the latter two guard boundaries.

The unchanged historical rescue route has SHA-256
`bdb62bf6d714e77b97f1e670e849597f19e9e4e310c751194c6d525dda152e9d`.
A current rebuilt replay still completes only 73 checkpoints and dies at
route frame 10006, overworld room `$F1`, Link `(82,28)`. Its 10006 preceding
trace rows remain local in `/tmp/ladx-leever-fixed-canonical/`; it has no
completion manifest. This fix does **not** establish the cause of that death,
restore rescue continuity, or establish all combat/position/visual fidelity.
The session's fixed `rLY=0` RNG policy and unmatched startup frame origins
remain separate comparison limitations. Independent-control route experiments
are not input-identical fidelity evidence and were not committed as a route.
