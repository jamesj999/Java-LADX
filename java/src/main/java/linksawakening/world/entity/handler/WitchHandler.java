package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_WITCH;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.WitchExchangeEvent;
import linksawakening.world.entity.event.WitchRewardEvent;
import linksawakening.world.entity.motion.WitchMotion;

/** ROM handler stages for Witch. */
final class WitchHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_WITCH, EntityTickStage.PHASE_4_BEFORE_FOLLOWER, 16, WitchHandler::phase4Witch);
    }

    private static boolean phase4Witch(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            state.story().advanceWitchGotItemPresentation(state.entity.slot());
            WitchMotion.Update witchUpdate = state.witchMotion()
                    .advance(
                            state.updated,
                            state.slot().transitionCountdown(state.entity.slot()),
                            new WitchMotion.Input(
                                    state.story().playerHasToadstool(),
                                    state.link().linkItemB(),
                                    state.link().linkItemA(),
                                    state.link().actionButtonAHeld(),
                                    state.link().actionButtonBHeld(),
                                    state.link().dialogActive(),
                                    state.linkZ != 0,
                                    state.linkEntityX,
                                    state.linkEntityY,
                                    state.romLinkDirection,
                                    state.lifecycle().transitionSequenceCounter(),
                                    state.story().booBuddyTriggerCount(),
                                    state.story().bgPaletteEffectAddress(),
                                    state.story().paletteDataFlags(),
                                    state.roomInspection().defaultMusicTrack()));
            state.updated = witchUpdate.entity();
            state.slot().setTransitionCountdown(state.entity.slot(), witchUpdate.transitionCountdown());
            state.preserveWitchPresentation = true;
            if (witchUpdate.blockLink()) {
                state.emit(new LinkMotionBlockRequest(state.entity.slot(), 0, 0, witchUpdate.motionBlockValue()));
            }
            if (witchUpdate.dialogGlobalId() >= 0) {
                state.emit(new DialogRequest(witchUpdate.dialogGlobalId() >>> 8, witchUpdate.dialogGlobalId() & 0xFF));
            }
            if (witchUpdate.musicTrack() >= 0) {
                state.requestMusic(witchUpdate.musicTrack());
            }
            if (witchUpdate.jingleId() >= 0) {
                state.emit(new EntityCombatEvent(
                        state.entity.slot(),
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        witchUpdate.jingleId()));
            }
            if (witchUpdate.exchangeStarted()) {
                int inventorySlot = witchUpdate.clearedSlot() == WitchMotion.InventorySlot.B ? 0 : 1;
                state.emit(new WitchExchangeEvent(state.entity.slot(), inventorySlot));
                state.story().consumeToadstool();
            }
            if (witchUpdate.grantMagicPowder()) {
                state.emit(new WitchRewardEvent(state.entity.slot()));
                state.story().startWitchGotItemCountdown(state.entity.slot());
                state.story().presentWitchGotItem(state.entity.slot());
            }
        }
        return true;
    }
}
