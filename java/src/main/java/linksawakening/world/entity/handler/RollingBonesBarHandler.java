package linksawakening.world.entity.handler;

import static linksawakening.world.entity.EntityRomConstants.ENTITY_ROLLING_BONES;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_ROLLING_BONES_BAR;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;

import linksawakening.world.entity.EntityHandlerRegistration;
import linksawakening.world.entity.EntityStatus;
import linksawakening.world.entity.EntityTickStage;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.motion.BossIntroMotion;
import linksawakening.world.entity.motion.RollingBonesMotion;

/** ROM handler stages for RollingBonesBar. */
final class RollingBonesBarHandler {
    static void install(EntityHandlerRegistration registry) {
        for (int type = 0; type < 256; type++) {
            if (((type == ENTITY_ROLLING_BONES || type == ENTITY_ROLLING_BONES_BAR))) {
                registry.registerTick(type, EntityTickStage.PHASE_2_ROLLING_BONES_BAR, state -> {
                    if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                        // BossIntro runs before ReturnIfNonInteractive_06.
                        if (state.entity.type() == ENTITY_ROLLING_BONES
                                && !state.roomInspection().roomTransitionActive()
                                && !state.link().inventoryAppearing()) {
                            BossIntroMotion.Update intro = state.bossIntroMotion()
                                    .advance(
                                            state.slotProperties().options1(state.entity.slot()),
                                            state.entity.type(),
                                            state.roomInspection().entityMapId(),
                                            state.lifecycle().transitionSequenceCounter());
                            if (intro.musicTrack() >= 0) state.requestMusic(intro.musicTrack());
                        }
                        // The bar's room-clear check precedes its interaction guard.
                        if (state.entity.type() == ENTITY_ROLLING_BONES_BAR
                                && (state.roomInspection().entityRoomStatus() & 0x20) != 0) {
                            state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                            return false;
                        }
                        // ReturnIfNonInteractive_06 reads the pre-decrement wC1A8.
                        if (!state.handlerInteractive || state.countdownBlocksInteraction) return false;
                        if (state.entity.type() == ENTITY_ROLLING_BONES
                                && !state.rollingBonesHitThisFrame
                                && state.slot().ignoreHits(state.entity.slot()) != 0) {
                            // ApplyRecoilIfNeeded_06 pops the handler return address:
                            // no jump physics or AI movement occurs during recoil.
                            state.replaceEntity(state.background()
                                    .applyEnemyRecoilIfNeeded(state.entity, state.backgroundCollision)
                                    .entity());
                            return false;
                        }
                    }
                    return true;
                });
            }
        }
        registry.registerTick(ENTITY_ROLLING_BONES_BAR, EntityTickStage.PHASE_4_ROLLING_BONES_BAR, state -> {
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing) {
                if ((state.roomInspection().entityRoomStatus() & 0x20) != 0) {
                    state.lifecycle().disableEntityWithoutPersistence(state.entity.slot());
                    return false;
                }
                RollingBonesMotion.BarUpdate barUpdate =
                        state.rollingBonesMotion().advanceBar(state.entity, state.frame, state.backgroundCollision);
                state.updated = barUpdate.entity();
                if (state.sprites() != null) {
                    state.updated = withDefinition(
                            state.updated,
                            state.sprites().forRollingBonesBar(0, state.updated.y(), barUpdate.animationVariant()),
                            0);
                }
                state.preserveRollingBonesPresentation = true;
                if (barUpdate.rollingSound()) {
                    state.emit(new EntityCombatEvent(
                            state.entity.slot(),
                            state.entity.type(),
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.WAVE,
                            0x1A));
                }
                if (barUpdate.strongBump()) {
                    state.emit(new EntityCombatEvent(
                            state.entity.slot(),
                            state.entity.type(),
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.JINGLE,
                            0x0B));
                    state.emit(new ScreenShakeRequest(state.entity.slot(), barUpdate.screenShakeCountdown(), 0));
                }
            }
            return true;
        });
    }
}
