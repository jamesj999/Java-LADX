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

## Beach shield and sword contacts, 2026-11-24

On checkpoint 47 of the fresh rescue route, the ROM raises the shield while
walking east across room `$F1`. Four Leever bumps and three Octorok bumps
reach `$03:6F93`. Each plays jingle 9, sets Link's collision-ignore timer
to 12 and the enemy's timer to 10. Three bumps occur while Link's previous
collision-ignore timer is still running.

| Route frame | Enemy | Cached XY | XY used for recoil | Link XY | Link recoil | Enemy recoil |
| --- | --- | --- | --- | --- | --- | --- |
| 4619 | Leever 2 | 49,44 | 49,44 | 41,32 | −12,−18 | +21,+32 |
| 4655 | Octorok 1 | 71,25 | 71,25 | 56,26 | −18,+1 | +32,−2 |
| 4710 | Leever 5 | 65,32 | 66,32 | 64,30 | −18,−18 | +32,+32 |

The weapon rectangle is compared against the cached coordinates. Both
recoil vectors use the position tables. A later slot in descending order
can replace Link's response while the earlier enemy retains its own recoil.
At `$03:6FC5` the ROM loads `$08` before an OR instruction, which makes
the zero branch unreachable. A separate injection at `$03:6FC1` cleared
both the input byte and the saved boots byte. All seven bumps still entered
`ConfigureEntityRecoil` with magnitude 32.

Capture the read-only measurements from the repository root. The input
prefix has SHA-256 `524092c7a4d4981988623f127390ac638c4a087f1ef8a7b981881ee800597593`
when written as the 48 gameplay rows with a trailing newline.

```sh
out=$(mktemp -d)
awk 'NF && !/^#/ { print; if (++n == 48) exit }' tools/opening/bowwow-rescue-route.tsv > "$out/beach.tsv"
python tools/opening/capture.py --route "$out/beach.tsv" --shield-trace-room 0xf1 --output "$out"
```

In a second, controlled comparison, a sword and Link's body both touch a
chasing Leever. With initial enemy health 1 or 8, the native Leever moves
from (110,40) to (109,40), then buffers 4 damage to Link and writes recoil
(0,−20) and collision-ignore 16. The enemy takes one damage and receives
recoil (0,+48), collision-ignore 10 and flash 24. The lethal case enters
the dying state at (109,40) with its death countdown still at 47. Java
regressions cover both health values and a lower-slot Octorok overriding
a higher-slot Leever's shield response. The [subsequent recoil trace](beach-enemy-recoil.md)
covers positions and timers on later dispatches.

```sh
python tools/leever/capture_rom.py --case leever-sword --enemy-health 1 --follow-recoil 2 --output /tmp/leever-lethal.json
python tools/leever/capture_rom.py --case leever-sword --enemy-health 8 --follow-recoil 2 --output /tmp/leever-survivor.json
```

The full rescue TSV has SHA-256
`d21b8ba1c6f702689be10cda86e58b564959f14427431ac5c02be56e693ad208`.
On the integrated sword/arrow/recoil revision Java ended checkpoint 47 at
(120,25) with 20 health, against (120,32) with 24 in PyBoy. An earlier
revision with only the shield changes reached the Java checkpoint with 24.
After collecting the sword, the integrated Java replay finished checkpoint
73 and died on the return through `$F1` at frame 8928, (68,12).

## Final recoil step at the north edge

The last step of a shield shove can carry Link across the north scroll
threshold while Right remains held. In a controlled native probe, the
candidate raw coordinates were (96, 10), the saved coordinates were
(97, 12), and the velocity was (−7, −18). With zero ignore frames left,
neutral or Right took the rejection path at `$02:6E12`, restored (97, 12)
and cleared both speeds. Up or Up+Right reached the scroll branch at
`$02:6DCC` with both speeds intact. Up with an active ignore timer still
rejected the scroll. Java's final recoil step now tests the held
direction and preserves the current movement fractions when restoring
both coordinate bytes.
