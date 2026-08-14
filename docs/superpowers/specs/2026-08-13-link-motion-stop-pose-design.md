# Link Motion-Stop Pose Semantics

## Goal

Make Link's animation state match the LADX disassembly when an entity or
script stops his movement. In particular, the Marin/Tarin house doorway stop
and Owl events must not acquire the fallen-on-his-butt pose merely because
their handlers freeze Link. ROM handlers that explicitly write animation
state `$6A` must continue to show that pose.

## ROM evidence

- `LADX-Disassembly/src/code/bank2.asm:$42B2` handles
  `hLinkInteractiveMotionBlocked == $02` by clearing Link's speed and
  velocity and returning. It does not write `hLinkAnimationState`.
- `LADX-Disassembly/src/code/entities/06_owl_event.asm:$6937-$6939` and
  `$69BD-$69BF` write only the `$02` interactive-motion block. The Owl path
  does not request animation `$6A`.
- `LADX-Disassembly/src/code/entities/05_tarin.asm:$4C79-$4C83` handles
  the unshielded house doorway by moving Link back two pixels and opening
  `Dialog000`. It does not write either `$02` or animation `$6A`.
- `LADX-Disassembly/src/code/entities/06_armos_knight.asm:$538E-$5392`
  writes `$02` and then explicitly writes animation `$6A`. Armos therefore
  remains an intentional fallen-pose case.

## Current Java mismatch

`Link.blockNextRomMotionFrame()` currently sets both the motion lock and
`romAnimationStateOverride = 0x6A`. That method is shared by Owl requests,
the new-game Tarin doorway script, room/entity stops, and explicit ROM paths.
The shared animation write makes the first two categories show the wrong
pose and happens to hide the distinction present in the disassembly.

## Design

1. Make `Link.blockNextRomMotionFrame()` model only the ROM motion freeze:
   set the one-frame interactive-motion lock, stop movement for that update,
   and leave the current animation state untouched.
2. Add a separate Link operation for an explicit ROM `$6A` write. It will set
   the `$6A` animation override together with the motion lock, preserving the
   current rendering and one-frame timing behavior of explicit fallen-pose
   handlers.
3. Add a separate runtime request channel for the explicit fallen pose. Emit
   it from the Armos Knight path, whose disassembly writes `$6A`; do not emit
   it from Owl, Tarin, witch, transition, or generic interaction blocks.
4. Apply the explicit fallen-pose request before held-item and sword-spin
   requests in `Main`, so the existing later ROM-specific presentation writes
   remain authoritative if two scripted systems overlap in a frame.
5. Keep the Tarin house state machine's existing position correction, dialog,
   and script timing. The Java frame gate remains as a scheduling detail for
   the scripted sequence; its call into Link will be freeze-only and will not
   invent an animation write that the Tarin handler does not make.

## Verification

- A Link unit test must prove a generic `$02` freeze preserves the prior
  animation state while still stopping movement and vertical velocity.
- A Link unit test must prove the explicit fallen-pose operation resolves to
  `$6A`.
- Owl runtime coverage must prove its block requests do not produce a fallen
  pose request.
- Armos runtime coverage must prove its block requests still produce the
  explicit fallen-pose request.
- Existing Tarin house, sword pickup, held-item, and full Gradle test suites
  must remain green.

## Non-goals

- Do not change the ROM's `$6A` animation data or Link sprite assets.
- Do not reinterpret handlers that write `$6C` (got-item), `$6B` (sword
  acquisition final pose), or the sword spin states.
- Do not broaden this change into a rewrite of unimplemented entity handlers;
  only currently modeled Java paths are changed.
