package linksawakening.app.gameplay;

import static linksawakening.app.gameplay.LinkGroundPhase.*;

import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.LinkDialogPosition;
import linksawakening.gameplay.SignpostDialogRef;

final class LinkEquipmentPhase {
    private final ActiveLinkPhase active;

    LinkEquipmentPhase() {
        this(LinkMotionDriver.DIRECT);
    }

    LinkEquipmentPhase(LinkMotionDriver motion) {
        this.active = new ActiveLinkPhase(motion);
    }

    void tick(GameplaySimulation context, boolean dialogBlocksGameplay, boolean inventoryBlockedAtStart) {
        prepare(context, dialogBlocksGameplay);
        if (context.passOutController().tick(context, dialogBlocksGameplay)) return;

        // LinkMotionMapFadeInHandler ($00:19DE-$00:1A21) restores normal
        // motion and returns; held movement starts on the following frame.
        boolean linkActive = !inventoryBlockedAtStart
                && !context.scrollController().isActive()
                && !context.transitionController().isInputBlocked()
                && !context.inventoryController().shouldBlockOverworldInput()
                && !dialogBlocksGameplay
                && (!context.link().animation().dialogGotItemActive())
                && (!context.roomSession().entityState().gotItemPresentationActive());
        boolean swordPickupSequenceActive = context.roomSession().entityState().swordPickupSequenceActive();
        linkActive = linkActive && !swordPickupSequenceActive;
        if (linkActive) {
            active.tick(context, swordPickupSequenceActive);
        } else {
            if (context.scrollController().isActive()) {
                // Keep Link's walk animation cycling during a room transition.
                context.link().animation().tickAnimation();
            }
            // $02:75C4 does not imply an unconditional pause: $76C0 can
            // return to Default via $7757 and reset wC1C9 during dialogs.
            if (!inventoryBlockedAtStart
                    && !context.inventoryController().shouldBlockOverworldInput()
                    && (dialogBlocksGameplay
                            || context.scrollController().isActive()
                            || context.transitionController().isInputBlocked()
                            || (context.roomSession().entityState().gotItemPresentationActive()))) {
                tickCrackedFloor(context, context.link().motion().romInteractiveMotionBlockValue() != 0);
            }
        }

        if (!dialogBlocksGameplay) {
            int rumbleHorizontal = context.roomSession().doors().tickTailCaveKeyholeSequence();
            context.scrollController()
                    .setScriptedScreenShakeHorizontal(
                            context.roomSession().doors().tailCaveKeyholeSequenceActive(), rumbleHorizontal);
        }
    }

    private static void prepare(GameplaySimulation context, boolean dialogBlocksGameplay) {
        if (!dialogBlocksGameplay) {
            {
                context.playerState().playerResources().tickResourceBuffers(context.frameCounter());
                var resourceSound = context.playerState()
                        .playerSounds()
                        .takeResourceSound(sound -> GameplaySoundEvent.valueOf(sound.name()));
                if (resourceSound != null) context.gameplaySoundSink().play(resourceSound);
            }
            {
                context.transientVfxSystem().tick();
            }
            {
                context.droppableRupeeSystem()
                        .tick(
                                context.link().position().pixelX(),
                                context.link().position().pixelY(),
                                context.playerState());
            }
        }

        // ApplyGotItem $00:0FC7 runs before normal Link/item work. A
        // toadstool requested by that work starts decrementing next frame.
        {
            int itemDialog = context.link().airborne().tickDialogGotItem(dialogBlocksGameplay);
            if (itemDialog == 2) {
                context.dialogController()
                        .openPreformattedForLinkY(
                                context.dialogTextLoader().load(new SignpostDialogRef(0, 0x0E)),
                                LinkDialogPosition.dialogYFromTopLeft(
                                        context.link().position().pixelY()));
            }
            if (context.link().animation().presentedDialogGotItem() == 2) {
                context.gpu().updatePowderInventoryTiles(context.romData(), true);
            }
        }
    }
}
