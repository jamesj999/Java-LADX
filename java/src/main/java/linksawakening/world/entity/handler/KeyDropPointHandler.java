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
        if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            // decrementEnemyStatusCountdowns mirrors UpdateEntityTimers'
            // shared transition-byte decrement before bank-$03's handler
            // reads it.
            int slot = state.entity.slot();
            if (state.pickups().checkForKeyDropQuicksandHole(state.updated)) {
                state.emit(new KeyQuicksandEvent(slot));
                state.replaceEntity(withStatus(state.updated, EntityStatus.FALLING));
                state.slot().setTransitionCountdown(slot, 0x2F);
                state.background().initializeKeyQuicksandFall(slot);
                state.emit(new EntityCombatEvent(
                        slot, ENTITY_KEY_DROP_POINT, 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x18));
                return false;
            }
            boolean hookshotRoom = state.roomInspection().entityRoomId() == 0x80;
            if (hookshotRoom) {
                state.updated = withDefinition(
                        state.updated, state.slotProperties().spriteDefinitionFor(ENTITY_HOOKSHOT_CHAIN), 0);
            }
            KeyDropPointMotion.Update keyUpdate = KeyDropPointMotion.advance(
                    state.slot().transitionCountdown(slot), state.updated.spriteVariant(), hookshotRoom);
            state.slot().setTransitionCountdown(slot, keyUpdate.nextTransitionCountdown());
            if (keyUpdate.dialogLowId() >= 0) {
                state.emit(new DialogRequest(KeyDropPointMotion.DIALOG_TABLE, keyUpdate.dialogLowId()));
            }
            if (keyUpdate.rewardItemType() >= 0) {
                state.emit(new KeyRewardEvent(slot, keyUpdate.rewardItemType()));
            }
            state.keyDropTransitionActive = keyUpdate.holdAboveLink();
            if (state.keyDropTransitionActive) {
                state.updated = withPositionAndVariant(
                        state.updated, state.linkEntityX, state.linkEntityY - 0x0C, state.updated.spriteVariant());
                state.updated = withZ(state.updated, state.linkZ);
            }
        }
        return true;
    }
}
