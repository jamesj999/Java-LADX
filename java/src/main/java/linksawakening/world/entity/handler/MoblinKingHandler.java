package linksawakening.world.entity.handler;

import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.RoomEntityCombatRules;
import linksawakening.world.entity.event.EntityCombatEvent;







import linksawakening.world.entity.motion.MoblinKingMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;

/** ROM handler stages for MoblinKing. */
final class MoblinKingHandler {
  static void install(EntityHandlerRegistration registry) {
    for (int type = 0; type < 256; type++) {
      if (((type == RoomEntityRuntime.ENTITY_MOBLIN_KING))) {
        registry.registerTick(
            type,
            EntityTickStage.PHASE_2_MOBLIN_KING,
            state -> {
              {
                state.runtime.bossRewards.advanceMoblinKingDestruction(
                    state.entity, state.frame, state.randomByteSupplier);
                return false;
              }
            });
      }
    }
    registry.registerTick(
        RoomEntityRuntime.ENTITY_MOBLIN_KING,
        EntityTickStage.PHASE_4_MOBLIN_KING,
        state -> {
          if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
            if (state.runtime.bowWowState != 0x80) {
              state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
              return false;
            }
            if (!state.handlerInteractive) {
              state.preserveMoblinKingPresentation = true;
            } else {
              // ApplyRecoilIfNeeded_15 pops the handler return address:
              // movement/Z physics and AI wait until the recoil ends.
              if (state.runtime.enemyRecoilMotion.isActive(state.entity.slot())
                  && state.runtime.enemyIgnoreHitsCountdown[state.entity.slot()] != 0) {
                state.replaceEntity(state
                        .runtime
                        .backgroundMechanics
                        .applyEnemyRecoilIfNeeded(state.entity, state.backgroundCollision)
                        .entity());
                return false;
              }
              MoblinKingMotion.Update kingUpdate =
                  state.runtime.moblinKingMotion.advance(
                      state.entity,
                      state.linkEntityX,
                      state.linkEntityY,
                      state.runtime.enemyTransitionCountdown[state.entity.slot()],
                      state.runtime.slowTransitionCountdown[state.entity.slot()],
                      state.frame,
                      state.randomByteSupplier,
                      state.backgroundCollision,
                      state.handlerLinkCollisionEnabled
                          && RoomEntityCombatRules.collisionCadenceMatches(
                              state.frame, state.entity.slot())
                          && RoomEntityCombatRules.overlapsLink(
                              state.entity, state.linkEntityX, state.linkEntityY));
              state.updated = kingUpdate.entity();
              state.runtime.enemyTransitionCountdown[state.entity.slot()] =
                  kingUpdate.transitionCountdown();
              state.runtime.slowTransitionCountdown[state.entity.slot()] =
                  kingUpdate.slowTransitionCountdown();
              state.runtime.slowTimerInitialized[state.entity.slot()] = true;
              state.runtime.entityOptions1Override[state.entity.slot()] =
                  state.runtime.moblinKingMotion.state(state.entity.slot()) == 5
                      ? 0x84
                      : state.runtime.moblinKingMotion.state(state.entity.slot()) >= 2
                          ? 0xC4
                          : 0x40;
              if (state.runtime.moblinKingMotion.state(state.entity.slot()) == 5) {
                state.runtime.enemyPhysicsFlags[state.entity.slot()] |=
                    state.runtime.ENTITY_PHYSICS_HARMLESS;
              } else {
                state.runtime.enemyPhysicsFlags[state.entity.slot()] &=
                    ~state.runtime.ENTITY_PHYSICS_HARMLESS;
              }
              if (state.runtime.spriteHandlers != null) {
                state.updated =
                    withDefinition(
                        state.updated,
                        state.runtime.spriteHandlers.forMoblinKingState(
                            state.runtime.moblinKingMotion.bodyVariant(state.entity.slot()),
                            state.runtime.moblinKingMotion.weaponVariant(state.entity.slot()),
                            state.updated.z() != 0),
                        0);
              }
              state.preserveMoblinKingPresentation = true;
              if (kingUpdate.openIntroDialog()) {
                // $15:475C clears DIALOG_BOX_BOTTOM_BIT after opening Dialog191.
                state.emit(new DialogRequest(1, 0x91, true));
              }
              if (kingUpdate.arrowRequest() != null) {
                state.runtime.projectileLaunchRequests.add(
                    new RoamingEnemyMotion.LaunchRequest(
                        state.entity.slot(),
                        state.entity.type(),
                        state.runtime.ENTITY_MOBLIN_ARROW));
                state.enemySpawns().spawnEnemyProjectile(
                    state.entity,
                    new RoamingEnemyMotion.LaunchRequest(
                        state.entity.slot(),
                        state.entity.type(),
                        state.runtime.ENTITY_MOBLIN_ARROW),
                    kingUpdate.arrowRequest().direction());
              }
              if (kingUpdate.jingleId() >= 0) {
                state.emit(
                    new EntityCombatEvent(
                        state.entity.slot(),
                        state.entity.type(),
                        0,
                        false,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        kingUpdate.jingleId()));
              }
              if (kingUpdate.screenShakeCountdown() != 0) {
                state.emit(
                    new ScreenShakeRequest(
                        state.entity.slot(), kingUpdate.screenShakeCountdown(), 0));
              }
            }
          }
          return true;
        });
  }
}
