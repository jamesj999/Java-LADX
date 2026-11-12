package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.DIALOG_MOBLIN;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_MOBLIN_SWORD;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityBackgroundInteraction;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.motion.*;
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
        var slotState = state.slotFrame();

        state.motion(MoblinSwordMotion.class, MoblinSwordMotion::new)
                .initialize(slotState.entity().slot(), slotState.entity().x());

        return true;
    }

    private static boolean phase3MoblinSword(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE
                && !slotState.wasInitializing()
                && !state.link().dialogActive()
                && !state.link().inventoryAppearing()
                && !state.roomInspection().roomTransitionActive()
                && state.roomInspection().gameplayWorld()
                && state.lifecycle().transitionSequenceCounter() == 0x04
                && !state.roomInspection().witchGotItemPresentationActive()) {
            RoomEntityBackgroundInteraction swordBackgroundInteraction =
                    state.background().interaction();
            if (swordBackgroundInteraction == null && frameInput.backgroundCollision() != null) {
                swordBackgroundInteraction =
                        RoomEntityBackgroundInteraction.fromBoolean(frameInput.backgroundCollision());
            }
            MoblinSwordMotion.Update moblinSwordUpdate = state.motion(MoblinSwordMotion.class, MoblinSwordMotion::new)
                    .advance(
                            slotState.entity(),
                            frameInput.linkEntityX(),
                            frameInput.linkEntityY(),
                            swordBackgroundInteraction,
                            state.slot().ignoreHits(slotState.entity().slot()),
                            state.projectiles().swordMoblinAlertingSoundCounter(),
                            state.roomInspection().entityMapId(),
                            state.lifecycle().transitionSequenceCounter(),
                            frameInput.frame());
            slotState.updated(moblinSwordUpdate.entity());
            if (moblinSwordUpdate.dialogRequested()) {
                state.emit(new DialogRequest(1, DIALOG_MOBLIN));
            }
        }
        return true;
    }
}
