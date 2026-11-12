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

/** ROM handler stages for BowWow. */
final class BowWowHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(ENTITY_BOW_WOW, EntityTickStage.PHASE_1_BOW_WOW, BowWowHandler::phase1BowWow);
        registry.registerTick(ENTITY_BOW_WOW, EntityTickStage.PHASE_1_BOW_WOW_2, BowWowHandler::phase1BowWow2);
    }

    private static boolean phase1BowWow(EntityFrameExecution state) {
        if (state.initialStatus == EntityStatus.ACTIVE) {
            int slot = state.entity.slot();
            if (state.projectileTimersInteractive) {
                state.bowWowMotion().tickTimers(slot);
            }
            // $05:4054 selects the following tiles for every nonzero
            // quest state, including kidnapped ($80), before any gates.
            int spriteAddress = state.story().bowWowState() != 0 ? 0x401C : 0x4000;
            if (state.sprites() != null
                    && (state.entity.spriteDefinition().bank() != 0x05
                            || state.entity.spriteDefinition().address() != spriteAddress)) {
                state.entity = withDefinition(
                        state.entity,
                        state.story().bowWowState() != 0
                                ? state.sprites().forFollowerEntityType(ENTITY_BOW_WOW)
                                : state.sprites().forEntityType(ENTITY_BOW_WOW, EntityRoomLoader.RoomTable.OVERWORLD),
                        state.entity.spriteVariant());
            }
            state.entity = withVariant(state.entity, state.bowWowMotion().selectedVariant(slot));
            state.replaceEntity(state.entity);
            if (!state.bowWowHandlerInteractive) {
                return false;
            }
            if (state.slot().ignoreHits(slot) != 0) {
                state.slot().decrementIgnoreHits(slot);
            }
        }
        return true;
    }

    private static boolean phase1BowWow2(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE
                && state.entity.sourceLoadOrder() >= 0
                && state.story().bowWowState() == 0x80
                && state.handlerLinkCollisionEnabled
                && state.romCollisionType != 0
                && withinUnsignedWindow(state.linkEntityX, 0x88, 0x10)
                && withinUnsignedWindow(state.linkEntityY, 0x40, 0x10)) {
            // BowWowEntityHandler's kidnapped branch checks the handler's
            // collision byte only after Link enters the $88/$40 pen.
            state.story().markBowWowRescued();
            if (state.sprites() != null) {
                state.entity = withDefinition(
                        state.entity,
                        state.sprites().forFollowerEntityType(ENTITY_BOW_WOW),
                        state.entity.spriteVariant());
                state.replaceEntity(state.entity);
            }
            state.story().enableBowWowFollowing();
            state.requestMusic(0x10);
            state.emit(new DialogRequest(1, 0x6C));
            state.emit(new EntityCombatEvent(
                    state.entity.slot(), state.entity.type(), 0, false, EntityCombatEvent.SoundChannel.WAVE, 0x18));
        }
        return true;
    }
}
