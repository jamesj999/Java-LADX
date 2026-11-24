# Enemy motion during beach recoil

PyBoy 2.8.1 ran the fresh-game beach prefix with no RAM injection. The
48-row prefix SHA256 is
`524092c7a4d4981988623f127390ac638c4a087f1ef8a7b981881ee800597593`.
The local hook script `/tmp/ladx-beach-enemy-recoil.py` records consecutive
CPU visits in room `$F1` for Leever and Octorok. The v1.0 ROM SHA256 is
`c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`.

| Enemy and source frame | Raw X,Y after recoil | X,Y fractions | Ignore timer | Transition timer |
| --- | --- | --- | --- | --- |
| Leever slot 2, CPU 33 | `32,2E` | `50,80` | `09` | `66` |
| Leever slot 2, CPU 42 | `3E,32` | `20,80` | `00` | `5D` |
| Octorok slot 1, CPU 69 | `49,18` | `80,60` | `09` | `40` |
| Octorok slot 1, CPU 78 | `5B,17` | `80,40` | `00` | `40` |

The source shield recoil is `15,20` for this Leever and `20,FE`
for this Octorok. Values in the table are hexadecimal, except CPU
frame numbers. Leever's subsequent vertical wall contact leaves
the recoil countdown running. Octorok and ordinary Moblin use
`$03:7FF1` to stop on collision along the greater absolute recoil
component, choosing vertical on a tie. Both axes update from the
ordinary fraction tables before the wall probes.

`$04:6DC8` discards the return to the Leever's ordinary motion and
state dispatch while recoil is active. `$03:7FF0` does the same
inside AnimateRoamingEnemy. Its transition countdown is set to
`$40` each recoil frame. The normal paused handler resumes with
`$3F` on CPU 79. It checks background with the restored ordinary
velocity at the current position. Leever resumes the ordinary XY
update at CPU 43, retaining both fractions from the final recoil step.

## Sword hit and the following frame

Use the controlled PyBoy tool to compare the attack frame and following
updates. The captured approach SHA256 is
`e31edb1a31f40eb290af4e61854430000802246695635f3fa506862e98d12638`
for Leever and
`176460aa2fd60b2b15e1c1b5dcb52648d04e651857900090c974405f2b154f35`
for Octorok and Moblin. The Moblin case substitutes type `$0B` in the Octorok setup
and shares its collision geometry. All three cases seed health 8 and
install a sword rectangle at the source combat call.

```sh
for enemy in leever octorok moblin; do
  python tools/leever/capture_rom.py --case "$enemy-sword" \
    --follow-recoil 11 --output "/tmp/ladx-$enemy-sword-followup.json"
done
```

| Enemy | Hit-frame position | First subsequent position | Hit-frame ignore | First subsequent ignore |
| --- | --- | --- | --- | --- |
| Leever | `109,40` | `109,43` | 10 | 9 |
| Octorok | `58,82` | `58,79` | 10 | 0 |
| Moblin | `58,82` | `58,79` | 10 | 0 |

All three remain active with health 7. Leever moves normally before
`$04:7F49` and uses the moved position to set the vector. The bank-three
handler assigns the vector before ordinary state dispatch at `$03:585B`;
the two controlled roaming cases start in the paused state. Their
first recoil hits a horizontal wall, so the X movement is restored
and the countdown clears while the Y movement remains. Tests compare
the recorded endpoints, the first nonlethal hit's countdown, and
the two shield trajectories. The controlled geometries and preliminary
fresh-game shield trace cover these specific motion paths.

## Recoil after a lethal sword hit

Repeat the controlled sword probes with health 1. Each lethal hit ends
with status 1, state 0, ignore-hits 10 and destruction countdown 47.
Use `--open-recoil-room` to replace the playable collision objects
with floor after recording the hit. Omitting the flag retains the
room's geometry. Java regression tests resolve the sword hit and
then advance the dying slot through the sampled wall and floor layouts.

```sh
for enemy in leever octorok moblin; do
  python tools/leever/capture_rom.py --case "$enemy-sword" --enemy-health 1 \
    --follow-recoil 12 --output "/tmp/ladx-$enemy-dying-wall.json"
  python tools/leever/capture_rom.py --case "$enemy-sword" --enemy-health 1 \
    --follow-recoil 12 --open-recoil-room --output "/tmp/ladx-$enemy-dying-open.json"
done
```

| Enemy | Room | Step after hit | Position | X/Y fractions | Transition | Ignore-hits | Collision |
| --- | --- | ---: | --- | --- | ---: | ---: | --- |
| Leever | Wall | 9 | `109,64` | `$80,$00` | 99 | 0 | Down |
| Leever | Floor | 10 | `109,70` | `$80,$00` | 98 | 0 | Clear |
| Octorok and Moblin | Wall | 1 | `58,79` | `$00,$70` | 98 | 0 | Right |
| Octorok and Moblin | Floor | 10 | `88,56` | `$00,$60` | 89 | 0 | Clear |

`EntityDeathHandler` uses the bank-three recoil routine at `$03:5599`.
The dying Leever drops ignore-hits from 2 to 0 on the blocked
downward step, keeping the incoming X fraction from its last ordinary
move. The two roaming enemies restore the blocked X while keeping
the Y step and its fraction. Their transition countdown starts
at 99 and falls once per step, with state remaining 0. In all six
probes the destruction countdown changes to 31 on the first update
after ignore-hits reaches zero.

Run the roaming floor cases again with `--roaming-walk` to seed the
normal leftward walk. The lethal hit computes recoil from `58,82` at
`$03:585B`, then the cached state at `$03:585E` dispatches ordinary
movement, ending at `57,82` with X fraction `$80`. The death handler's
first update reaches `60,79`, with fractions `$80,$70`. The paused
cases retain their position on the hit frame and use the state-table
reset from `$03:7341` on the following frame.
