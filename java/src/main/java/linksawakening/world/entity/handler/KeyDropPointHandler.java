package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_HOOKSHOT_CHAIN;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_KEY_DROP_POINT;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;
import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntitySnapshots.withZ;

import linksawakening.world.entity.EntityFrameExecution;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.KeyQuicksandEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.motion.KeyDropPointMotion;

/** ROM handler stages for KeyDropPoint. */
final class KeyDropPointHandler {
    static void install(EntityHandlerRegistration registry) {
        registry.registerTick(
                ENTITY_KEY_DROP_POINT,
                EntityTickStage.PHASE_2_BEFORE_RECOIL_APPLICATION,
                6,
                KeyDropPointHandler::phase2KeyDropPoint);
    }

    private static boolean phase2KeyDropPoint(EntityFrameExecution state) {
        var slotState = state.slotFrame();
        var frameInput = state.inputs();

        if (slotState.status() == EntityStatus.ACTIVE && !slotState.wasInitializing()) {
            // decrementEnemyStatusCountdowns mirrors UpdateEntityTimers'
            // shared transition-byte decrement before bank-$03's handler
            // reads it.
            int slot = slotState.entity().slot();
            if (state.pickups().checkForKeyDropQuicksandHole(slotState.updated())) {
                state.emit(new KeyQuicksandEvent(slot));
                state.replaceEntity(withStatus(slotState.updated(), EntityStatus.FALLING));
                state.slot().setTransitionCountdown(slot, 0x2F);
                state.background().initializeKeyQuicksandFall(slot);
                state.emit(new EntityCombatEvent(
                        slot, ENTITY_KEY_DROP_POINT, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x18));
                return false;
            }
            boolean hookshotRoom = state.roomInspection().entityRoomId() == 0x80;
            if (hookshotRoom) {
                slotState.updated(withDefinition(
                        slotState.updated(), state.slotProperties().spriteDefinitionFor(ENTITY_HOOKSHOT_CHAIN), 0));
            }
            KeyDropPointMotion.Update keyUpdate = KeyDropPointMotion.advance(
                    state.slot().transitionCountdown(slot), slotState.updated().spriteVariant(), hookshotRoom);
            state.slot().setTransitionCountdown(slot, keyUpdate.nextTransitionCountdown());
            if (keyUpdate.dialogLowId() >= 0) {
                state.emit(new DialogRequest(KeyDropPointMotion.DIALOG_TABLE, keyUpdate.dialogLowId()));
            }
            if (keyUpdate.rewardItemType() >= 0) {
                state.emit(new KeyRewardEvent(slot, keyUpdate.rewardItemType()));
            }
            slotState.keyDropTransitionActive(keyUpdate.holdAboveLink());
            if (slotState.keyDropTransitionActive()) {
                slotState.updated(withPositionAndVariant(
                        slotState.updated(),
                        frameInput.linkEntityX(),
                        frameInput.linkEntityY() - 0x0C,
                        slotState.updated().spriteVariant()));
                slotState.updated(withZ(slotState.updated(), frameInput.linkZ()));
            }
        }
        return true;
    }
}
