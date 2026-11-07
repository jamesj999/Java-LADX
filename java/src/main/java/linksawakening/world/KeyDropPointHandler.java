package linksawakening.world;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.KeyQuicksandEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import static linksawakening.world.EntitySnapshots.withDefinition;
import static linksawakening.world.EntitySnapshots.withPositionAndVariant;
import static linksawakening.world.EntitySnapshots.withStatus;
import static linksawakening.world.EntitySnapshots.withZ;

/** ROM handler stages for KeyDropPoint. */
final class KeyDropPointHandler {
  static void installPhase2(EntityHandlerRegistry registry) {
    registry.registerTick(
        RoomEntityRuntime.ENTITY_KEY_DROP_POINT,
        EntityTickStage.PHASE_2_KEY_DROP_POINT,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            // decrementEnemyStatusCountdowns mirrors UpdateEntityTimers'
            // shared transition-byte decrement before bank-$03's handler
            // reads it.
            int slot = state.entity.slot();
            if (state.runtime.pickups.checkForKeyDropQuicksandHole(state.updated)) {
              state.emit(new KeyQuicksandEvent(slot));
              state.replaceEntity(withStatus(state.updated, EntityStatus.FALLING));
              state.runtime.enemyTransitionCountdown[slot] = 0x2F;
              state.runtime.fallingTargetX[slot] = 0x50;
              state.runtime.fallingTargetY[slot] = 0x48;
              state.runtime.fallingSpeedX[slot] = 0;
              state.runtime.fallingSpeedY[slot] = 0;
              state.runtime.fallingSpeedXAccumulator[slot] = 0;
              state.runtime.fallingSpeedYAccumulator[slot] = 0;
              state.runtime.fallingVisualYOffset[slot] = 0;
              state.emit(
                  new EntityCombatEvent(
                      slot,
                      state.runtime.ENTITY_KEY_DROP_POINT,
                      0,
                      false,
                      EntityCombatEvent.SoundChannel.JINGLE,
                      0x18));
              return false;
            }
            boolean hookshotRoom = state.runtime.entityRoomId == 0x80;
            if (hookshotRoom) {
              state.updated =
                  withDefinition(
                      state.updated,
                      state.runtime.slotProperties.spriteDefinitionFor(
                          state.runtime.ENTITY_HOOKSHOT_CHAIN),
                      0);
            }
            KeyDropPointMotion.Update keyUpdate =
                KeyDropPointMotion.advance(
                    state.runtime.enemyTransitionCountdown[slot],
                    state.updated.spriteVariant(),
                    hookshotRoom);
            state.runtime.enemyTransitionCountdown[slot] = keyUpdate.nextTransitionCountdown();
            if (keyUpdate.dialogLowId() >= 0) {
              state.emit(
                  new DialogRequest(KeyDropPointMotion.DIALOG_TABLE, keyUpdate.dialogLowId()));
            }
            if (keyUpdate.rewardItemType() >= 0) {
              state.emit(
                  new KeyRewardEvent(slot, keyUpdate.rewardItemType()));
            }
            state.keyDropTransitionActive = keyUpdate.holdAboveLink();
            if (state.keyDropTransitionActive) {
              state.updated =
                  withPositionAndVariant(
                      state.updated,
                      state.linkEntityX,
                      state.linkEntityY - 0x0C,
                      state.updated.spriteVariant());
              state.updated = withZ(state.updated, state.linkZ);
            }
          }
          return true;
        });
  }
}
