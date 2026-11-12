package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityMotionMath.withinUnsignedWindow;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_BOW_WOW;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;
import static linksawakening.world.entity.EntitySnapshots.withVariant;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityRoomLoader;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;

/** ROM handler stages for BowWow. */
final class BowWowHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_BOW_WOW, EntityTickStage.PHASE_1_BEFORE_BOSS_INTRO, 1, BowWowHandler::phase1BowWow);
        registry.registerTick(
                ENTITY_BOW_WOW, EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE, 18, BowWowHandler::phase1BowWow2);
    }

    private static boolean phase1BowWow(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var timerGates = state.gates();

        if (slotState.initialStatus() == EntityStatus.ACTIVE) {
            int slot = slotState.entity().slot();
            if (timerGates.projectileTimersInteractive()) {
                state.motion(BowWowMotion.class, BowWowMotion::new).tickTimers(slot);
            }
            // $05:4054 selects the following tiles for every nonzero
            // quest state, including kidnapped ($80), before any gates.
            int spriteAddress = state.story().bowWowState() != 0 ? 0x401C : 0x4000;
            if (state.sprites() != null
                    && (slotState.entity().spriteDefinition().bank() != 0x05
                            || slotState.entity().spriteDefinition().address() != spriteAddress)) {
                slotState.entity(withDefinition(
                        slotState.entity(),
                        state.story().bowWowState() != 0
                                ? state.spriteViews().forFollowerEntityType(ENTITY_BOW_WOW)
                                : state.spriteViews()
                                        .forEntityType(ENTITY_BOW_WOW, EntityRoomLoader.RoomTable.OVERWORLD),
                        slotState.entity().spriteVariant()));
            }
            slotState.entity(withVariant(
                    slotState.entity(),
                    state.motion(BowWowMotion.class, BowWowMotion::new).selectedVariant(slot)));
            state.replaceEntity(slotState.entity());
            if (!timerGates.bowWowHandlerInteractive()) {
                return false;
            }
            if (state.slot().ignoreHits(slot) != 0) {
                state.slot().decrementIgnoreHits(slot);
            }
        }
        return true;
    }

    private static boolean phase1BowWow2(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();
        var timerGates = state.gates();

        if (slotState.status() == EntityStatus.ACTIVE
                && slotState.entity().sourceLoadOrder() >= 0
                && state.story().bowWowState() == 0x80
                && timerGates.handlerLinkCollisionEnabled()
                && frameInput.romCollisionType() != 0
                && withinUnsignedWindow(frameInput.linkEntityX(), 0x88, 0x10)
                && withinUnsignedWindow(frameInput.linkEntityY(), 0x40, 0x10)) {
            // BowWowEntityHandler's kidnapped branch checks the handler's
            // collision byte only after Link enters the $88/$40 pen.
            state.story().markBowWowRescued();
            if (state.sprites() != null) {
                slotState.entity(withDefinition(
                        slotState.entity(),
                        state.spriteViews().forFollowerEntityType(ENTITY_BOW_WOW),
                        slotState.entity().spriteVariant()));
                state.replaceEntity(slotState.entity());
            }
            state.story().enableBowWowFollowing();
            state.requestMusic(0x10);
            state.emit(new DialogRequest(1, 0x6C));
            state.emit(new EntityCombatEvent(
                    slotState.entity().slot(),
                    slotState.entity().type(),
                    0,
                    false,
                    EntityCombatEvent.SoundChannel.WAVE,
                    0x18));
        }
        return true;
    }
}
