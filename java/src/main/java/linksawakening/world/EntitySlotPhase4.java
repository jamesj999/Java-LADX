package linksawakening.world;

import static linksawakening.world.EntityMotionMath.*;
import static linksawakening.world.EntitySnapshots.*;
import static linksawakening.world.EntityTypeRules.*;
import linksawakening.entity.EntitySpriteSelection;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.gpu.EntitySpriteTileSnapshot;
import linksawakening.rom.RomTables;
import linksawakening.vfx.TransientVfxType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.function.IntSupplier;
import java.util.function.IntUnaryOperator;
import static linksawakening.world.RoomEntityRuntime.*;

/** Ordered slot dispatch phase 4; false preserves the ROM handler early return. */
final class EntitySlotPhase4 {
    static boolean run(EntityFrameExecution state) {

            if (!state.runtime.entityHandlers.tick(72, state)) return false;
            if (!state.runtime.entityHandlers.tick(73, state)) return false;
            if (!state.runtime.entityHandlers.tick(74, state)) return false;
            if (!state.runtime.entityHandlers.tick(75, state)) return false;
            if (!state.runtime.entityHandlers.tick(76, state)) return false;
            if (!state.runtime.entityHandlers.tick(77, state)) return false;
            if (!state.runtime.entityHandlers.tick(78, state)) return false;
            if (!state.runtime.entityHandlers.tick(79, state)) return false;
            if (!state.runtime.entityHandlers.tick(80, state)) return false;
            if (!state.runtime.entityHandlers.tick(81, state)) return false;
            if (!state.runtime.entityHandlers.tick(82, state)) return false;
            if (!state.runtime.entityHandlers.tick(83, state)) return false;
            if (!state.runtime.entityHandlers.tick(84, state)) return false;
            if (!state.runtime.entityHandlers.tick(85, state)) return false;
            if (!state.runtime.entityHandlers.tick(86, state)) return false;
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && !state.runtime.indoorRoom && state.entity.type() == state.runtime.ENTITY_TARIN
                && (state.runtime.tarinRaccoonMotion.state(state.entity.slot()) == 0
                    || state.runtime.tarinTransformationHandlerInteractive())) {
                RoomEntityBackgroundInteraction tarinBackgroundInteraction = state.runtime.backgroundInteraction;
                if (tarinBackgroundInteraction == null && state.backgroundCollision != null) {
                    tarinBackgroundInteraction = RoomEntityBackgroundInteraction.fromBoolean(
                        state.backgroundCollision);
                }
                TarinRaccoonMotion.Update tarinUpdate = state.runtime.tarinRaccoonMotion.advance(state.updated,
                    new TarinRaccoonMotion.Input(state.frame, state.linkEntityX, state.linkEntityY,
                        state.romLinkDirection, state.runtime.actionButtonAHeld, state.runtime.dialogActive, false,
                        state.runtime.linkAttackStepAnimationCountdown, state.linkAirborne,
                        state.runtime.inventoryAppearing, state.runtime.dialogCooldown, state.runtime.windowY,
                        state.runtime.slowTransitionCountdown[state.entity.slot()],
                        state.runtime.enemyTransitionCountdown[state.entity.slot()]),
                    tarinBackgroundInteraction);
                state.updated = tarinUpdate.entity();
                state.runtime.slowTransitionCountdown[state.entity.slot()] =
                    tarinUpdate.slowTransitionCountdown();
                state.runtime.enemyTransitionCountdown[state.entity.slot()] = tarinUpdate.transitionCountdown();
                state.preserveTarinPresentation = true;
                state.runtime.shouldGetLostInMysteriousWoods = tarinUpdate.shouldGetLost();
                if (tarinUpdate.linkMotionBlocked()) {
                    state.runtime.pendingLinkMotionBlockRequests.add(
                        new LinkMotionBlockRequest(state.entity.slot()));
                }
                if (tarinUpdate.dialogGlobalId() >= 0) {
                    state.runtime.pendingDialogRequests.add(new DialogRequest(
                        tarinUpdate.dialogGlobalId() >>> 8,
                        tarinUpdate.dialogGlobalId() & 0xFF));
                }
                if (tarinUpdate.clearLinkAttack()) {
                    state.runtime.pendingLinkAttackClearRequests.add(
                        new LinkAttackClearRequest(state.entity.slot()));
                }
                if (tarinUpdate.linkFacingDirection() >= 0) {
                    state.runtime.pendingLinkFacingRequests.add(new LinkFacingRequest(
                        state.entity.slot(), tarinUpdate.linkFacingDirection(), true));
                }
                if (tarinUpdate.pushLink()) {
                    if (RoomEntityCombatRules.overlapsLink(
                        state.updated, state.linkEntityX, state.linkEntityY)) {
                        state.runtime.requestLinkPush(state.updated, EntityLinkCollisionRules.STANDARD_PUSH);
                    }
                }
                if (tarinUpdate.soundId() >= 0) {
                    state.runtime.pendingEntityEvents.add(new EntityCombatEvent(state.entity.slot(), state.entity.type(),
                        0, false, EntityCombatEvent.SoundChannel.JINGLE,
                        tarinUpdate.soundId()));
                    if (tarinUpdate.soundId() == 0x23) {
                        state.runtime.pendingMusicTrack = state.runtime.defaultMusicTrack;
                    }
                }
                if (tarinUpdate.spawnBomb()) {
                    state.runtime.spawnTarinTransformationBomb(state.updated);
                }
                if (tarinUpdate.roomChanged() || tarinUpdate.tarinFlag()) {
                    state.runtime.pendingRoomStatusPersistenceRequests.add(
                        new RoomStatusPersistenceRequest(state.entity.slot(), 0x10, 1));
                }
            }
            if (!state.runtime.entityHandlers.tick(87, state)) return false;
            if (!state.runtime.entityHandlers.tick(88, state)) return false;
            if (!state.runtime.entityHandlers.tick(89, state)) return false;
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && isGhiniType(state.entity.type())) {
                state.updated = state.runtime.ghiniMotion.advance(state.entity, state.frame, state.entity.type(),
                    state.linkEntityX, state.linkEntityY, state.romCollisionType, state.randomByteSupplier);
                state.preserveGhiniPresentation = true;
            }
            if (!state.runtime.entityHandlers.tick(90, state)) return false;
            if (!state.runtime.entityHandlers.tick(91, state)) return false;
            if (!state.runtime.entityHandlers.tick(92, state)) return false;
            if (state.status == EntityStatus.ACTIVE && !state.wasInitializing
                && (isDynamicFollowingNpc(state.entity)
                    || state.entity.type() == state.runtime.ENTITY_BOW_WOW)) {
                if (state.entity.type() == state.runtime.ENTITY_BOW_WOW) {
                    BowWowMotion.Update bowWowUpdate = state.runtime.bowWowMotion.advanceAfterTimers(
                        state.entity, state.frame, state.linkEntityX, state.linkEntityY, state.runtime.followingLinkZ,
                        state.randomByteSupplier, state.backgroundCollision, Arrays.asList(state.runtime.slots),
                        state.runtime.enemyCombatTables == null
                            ? type -> false : state.runtime.enemyCombatTables::canBowWowEatEntity,
                        state.runtime.bowWowState);
                    state.updated = bowWowUpdate.entity();
                    if (bowWowUpdate.targetContact() != null) {
                        state.runtime.handleBowWowTargetContact(state.entity, bowWowUpdate.targetContact(),
                            state.randomByteSupplier);
                    }
                } else {
                    state.updated = state.runtime.followingNpcMotion.advance(state.entity, state.frame, state.linkEntityX, state.linkEntityY,
                        state.runtime.followingLinkZ, state.runtime.followingLinkDirection, state.runtime.followingEntityYOffset,
                        state.runtime.followingLinkPositionHistory, state.backgroundCollision);
                }
            }
        return true;
    }
}
