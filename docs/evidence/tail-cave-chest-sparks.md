# Tail Cave chest and Sparks in room $0E

The fresh rescue route enters room $00:$0E from the east. At checkpoint
412 Link opens the chest at raw (72,58). Both Sparks keep flashing
through checkpoint 413. For its entire 130 frames their positions stay
at (83,56) for type $17 and (131,88) for type $16.

Read-only PyBoy 2.8.1 hooks at `$06:6643` (render), `$06:664F` (move) and
`$06:66CB` (return after updating speed) recorded the slot, position,
global frame counter, sprite variant, movement timer, collision flags
and interaction gates. The ROM was English DX v1.0, SHA-256
`c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60`.
The 415-row prefix including comments had SHA-256
`62dc1602e0c89e65b67c35b2d7df70dde4fb334818714bea8237e894400d5b8f`.

| ROM frame | Observation |
| --- | --- |
| 48551 | First ordinary movement dispatch, global frame $EB. The pair starts at (72,35) and (120,109). |
| 48859 | Both move on the chest initialization frame. The clockwise Spark sets its turn timer to nine. |
| 48860–48868 | The chest interaction countdown begins at 42. The Spark stays still and its turn timer falls to zero. |
| 48897–49156 | Chest dialogue holds the interaction countdown. Both Sparks still render both palette variants. |
| 49157–49160 | The interaction countdown reads four, three, two, one. Both positions remain fixed. |
| 49161 | Ordinary movement resumes at (83,57) and (131,87). |
| 49313 | The pair reaches (70,36) and (119,107). |

Chest initialization writes $2A at `$03:506D–506F`. Before entity dispatch,
`$00:399B–39AB` copies the interaction countdown to $C1A8 and decrements
the countdown while text is closed. The Spark renders at `$06:6643` before
the interaction or recoil checks. The ordinary entity timer uses a separate
gate at `$14:4D97`. It can run while the chest interaction countdown holds
movement. During recoil the Spark's no-background-rollback flag at
`$06:6637` preserves its integrated pixels through the wall probes.

A controlled Java comparison loaded the real room, opened its chest at
local frame 309, aligned the clock to $EB on the first movement dispatch
and supplied the measured dialogue interval. Both Sparks matched the ROM
on position and palette variant on all 763 consecutive frames through
49313, including 462 ordinary movement dispatches per Spark. The comparison
starts after the incoming scroll and covers the first visit to this chest.
