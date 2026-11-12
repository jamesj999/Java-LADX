package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.DIALOG_MOBLIN;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_MOBLIN_SWORD;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.motion.MoblinSwordMotion;

/** ROM handler stages for MoblinSword. */
final class MoblinSwordHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_MOBLIN_SWORD,
                EntityTickStage.PHASE_2_AFTER_INITIALIZE,
                1,
                MoblinSwordHandler::phase2MoblinSword);
        registry.registerTick(
                ENTITY_MOBLIN_SWORD, EntityTickStage.PHASE_3_BEFORE_LASER, 3, MoblinSwordHandler::phase3MoblinSword);
    }

    private static boolean phase2MoblinSword(EntityFrameExecution state) {
        state.moblinSwordMotion().initialize(state.entity.slot(), state.entity.x());

        return true;
    }

    private static boolean phase3MoblinSword(EntityFrameExecution state) {
        if (state.status == EntityStatus.ACTIVE
                && !state.wasInitializing
                && !state.link().dialogActive()
                && !state.link().inventoryAppearing()
                && !state.roomInspection().roomTransitionActive()
                && state.roomInspection().gameplayWorld()
                && state.lifecycle().transitionSequenceCounter() == 0x04
                && !state.roomInspection().witchGotItemPresentationActive()) {
            RoomEntityBackgroundInteraction swordBackgroundInteraction =
                    state.background().interaction();
            if (swordBackgroundInteraction == null && state.backgroundCollision != null) {
                swordBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(state.backgroundCollision);
            }
            MoblinSwordMotion.Update moblinSwordUpdate = state.moblinSwordMotion()
                    .advance(
                            state.entity,
                            state.linkEntityX,
                            state.linkEntityY,
                            swordBackgroundInteraction,
                            state.slot().ignoreHits(state.entity.slot()),
                            state.projectiles().swordMoblinAlertingSoundCounter(),
                            state.roomInspection().entityMapId(),
                            state.lifecycle().transitionSequenceCounter(),
                            state.frame);
            state.updated = moblinSwordUpdate.entity();
            if (moblinSwordUpdate.dialogRequested()) {
                state.emit(new DialogRequest(1, DIALOG_MOBLIN));
            }
        }
        return true;
    }
}
