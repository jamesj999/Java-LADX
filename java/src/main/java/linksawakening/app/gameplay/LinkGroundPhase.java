package linksawakening.app.gameplay;

import linksawakening.world.room.LinkGroundFrame;

/** Ground sampling at the pre- and post-motion ROM positions. */
final class LinkGroundPhase {
    private LinkGroundPhase() {}

    static void tickCrackedFloor(GameplaySimulation context, boolean interactionBlocked) {
        context.roomSession()
                .terrain()
                .tickCrackedFloor(
                        groundFrame(
                                context,
                                context.link().position().pixelX(),
                                context.link().position().pixelY(),
                                interactionBlocked),
                        dialogGotItemActive(context));
    }

    /** wDialogGotItem != 0 ($02:7854). Ported writers: Link's ApplyGotItem
     * sequence (UseMagicPowder $00:149E) and the witch presentation
     * ($05:4834, $05:48DB). */
    static boolean dialogGotItemActive(GameplaySimulation context) {
        return (context.link().animation().dialogGotItemActive())
                || (context.roomSession().entityState().gotItemPresentationActive());
    }

    static LinkGroundFrame groundFrame(GameplaySimulation context, int pixelX, int pixelY, boolean interactionBlocked) {
        return new LinkGroundFrame(
                pixelX,
                pixelY,
                context.link().airborne().isAirborne(),
                interactionBlocked,
                context.dialogController().isActive(),
                context.scrollController().isActive()
                        || context.transitionController().isInputBlocked(),
                context.link().airborne().romMotionState(),
                context.link().motion().romSlowWalkingSpeed(),
                context.link().motion().romItemUsageContext());
    }
}
