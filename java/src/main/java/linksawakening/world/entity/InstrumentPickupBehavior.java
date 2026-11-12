package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.InstrumentCompletionEvent;
import linksawakening.world.entity.event.InstrumentRewardEvent;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;

/** Focused behavior using the state owned by EntityRewardController. */
final class InstrumentPickupBehavior {
    static void advanceInstrumentPickup(
            EntityRewardController owner,
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkZ,
            IntSupplier randomByteSupplier) {
        var runtime = owner.runtime;
        int slot = entity.slot();
        if (owner.readInstrumentPickupSparklesPending(slot)) {
            owner.writeInstrumentPickupSparklesPending(slot, false);
            int[] xOffsets = {-0x18, 0x18, -0x18, 0x18};
            int[] yOffsets = {-0x2C, -0x2C, 0x04, 0x04};
            for (int variant = 3; variant >= 0; variant--) {
                runtime.events()
                        .transientVfxRequests
                        .add(new TransientVfxRequest(
                                TransientVfxType.MOVING_SPARKLE,
                                (linkEntityX + xOffsets[variant]) & 0xFF,
                                (linkEntityY + yOffsets[variant]) & 0xFF,
                                variant));
            }
        }
        if (owner.readInstrumentPickupState(slot) == 0
                && runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0x10) {
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x0F);
            owner.writeInstrumentPickupState(slot, 1);
            runtime.events()
                    .pendingDialogRequests
                    .add(new DialogRequest(1, runtime.roomVariables().readEntityMapId() & 0xFF));
            runtime.events().pendingInstrumentRewards.add(new InstrumentRewardEvent(slot));
        } else if (owner.readInstrumentPickupState(slot) == 1
                && !runtime.presentationState().readActiveMusic()
                && !runtime.linkState().dialogActive) {
            int mapId = runtime.roomVariables().readEntityMapId() & 0xFF;
            if (mapId < EntityRomConstants.INSTRUMENT_MUSIC_TRACKS.length) {
                runtime.presentationState().writePendingMusicTrack(EntityRomConstants.INSTRUMENT_MUSIC_TRACKS[mapId]);
                owner.writeInstrumentPickupState(slot, 2);
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xFF);
            }
        } else if (owner.readInstrumentPickupState(slot) == 2
                && runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
            owner.writeInstrumentPickupState(slot, 3);
            owner.writeInstrumentWarpX(slot, (entity.x() - 1) & 255); // $03:5F43
            owner.writeInstrumentWarpY(slot, entity.y());
            runtime.physicsTables().writeSlowTransitionCountdown(slot, 0x80);
            runtime.physicsTables().writeSlowTimerInitialized(slot, true);
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x2B));
        } else if (owner.readInstrumentPickupState(slot) == 2) {
            owner.writeInstrumentParticleSpawnCountdown(
                    slot, (owner.readInstrumentParticleSpawnCountdown(slot) - 1) & 0xFF);
            if (owner.readInstrumentParticleSpawnCountdown(slot) == 0xFF) {
                owner.writeInstrumentParticleSpawnCountdown(slot, 0x17);
                owner.writeInstrumentParticleSpawnCounter(
                        slot, (owner.readInstrumentParticleSpawnCounter(slot) + 1) & 0xFF);
                owner.spawnInstrumentParticle(
                        entity,
                        owner.readInstrumentParticleSpawnCounter(slot) & 0x01,
                        randomByteSupplier.getAsInt() & 0x01);
            }
        } else if (owner.readInstrumentPickupState(slot) == 3
                && !runtime.linkState().dialogActive
                && !runtime.linkState().inventoryAppearing
                && !runtime.roomVariables().readRoomTransitionActive()) {
            runtime.lifecycle().decrementSlowTransitionCountdown(slot, frameCounter);
            // $03:5E29: the warp controller advances its fade every eight
            // frames, then starts the fullscreen hint controller ($07:6523).
            if (owner.readInstrumentHintCountdown(slot) > 0) {
                owner.writeInstrumentHintBatches(slot, Math.min(9, owner.readInstrumentHintBatches(slot) + 1));
                if (owner.preInstrumentHintCountdown(slot, -1) == 0 && !owner.readInstrumentHintOpened(slot)) {
                    owner.writeInstrumentHintOpened(slot, true);
                    runtime.events()
                            .pendingDialogRequests
                            .add(new DialogRequest(
                                    1, 0x50 + (runtime.roomVariables().readEntityMapId() & 0xFF)));
                }
            }
            if (runtime.physicsTables().readSlowTransitionCountdown(slot) < 0x50
                    && owner.readInstrumentWarpFadeStep(slot) < 0x19
                    && (frameCounter & 7) == 0) {
                int step = owner.postInstrumentWarpFadeStep(slot, 1);
                if (step == 0) {
                    runtime.events()
                            .pendingEntityEvents
                            .add(new EntityCombatEvent(
                                    slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x2C));
                }
                if (step == 0x18) owner.writeInstrumentHintCountdown(slot, 0x60);
            }
            // func_020_6D0E raises RGB555 channels by one on even frames.
            // BG starts at private state 5; OBJ starts at state 15.
            int fadeStep = owner.readInstrumentWarpFadeStep(slot);
            if (runtime.physicsTables().readSlowTransitionCountdown(slot) < 0x50
                    && fadeStep <= 0x19
                    && (frameCounter & 1) == 0) {
                if (fadeStep >= 5)
                    owner.writeInstrumentBackgroundFade(
                            slot, Math.min(31, owner.readInstrumentBackgroundFade(slot) + 1));
                if (fadeStep >= 15)
                    owner.writeInstrumentObjectFade(slot, Math.min(31, owner.readInstrumentObjectFade(slot) + 1));
            }
            if (runtime.physicsTables().readSlowTransitionCountdown(slot) == 0) {
                owner.writeInstrumentPickupState(slot, 4);
                owner.writeInstrumentPickupSequenceActive(slot, false);
                runtime.events()
                        .pendingInstrumentCompletions
                        .add(new InstrumentCompletionEvent(
                                slot, runtime.roomVariables().readEntityMapId() & 0xFF));
                runtime.lifecycle().disableEntityWithoutPersistence(slot);
                return;
            }
        }
        runtime.events().pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        runtime.events().pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        RoomEntity held =
                withPositionAndVariant(entity, linkEntityX, (linkEntityY - 0x0C) & 0xFF, entity.spriteVariant());
        runtime.slots()[slot] = withZ(held, linkZ & 0xFF);
    }

    static void spawnInstrumentParticle(EntityRewardController owner, RoomEntity source, int offsetIndex, int variant) {
        var runtime = owner.runtime;
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0 || runtime.spriteHandlers() == null) {
            return;
        }
        int[] xOffsets = {0x0A, -0x06};
        int[] speedX = {0x04, 0xFC};
        EntitySpriteDefinition definition = runtime.spriteHandlers().forInstrumentShard();
        runtime.slots()[freeSlot] = new RoomEntity(
                freeSlot,
                -1,
                source.type(),
                (source.x() + xOffsets[offsetIndex]) & 0xFF,
                (source.y() - 0x08) & 0xFF,
                EntityStatus.ACTIVE,
                definition,
                variant & 0x01,
                0,
                0,
                0,
                -1,
                false);
        runtime.initializeHealthGroup(freeSlot);
        owner.writeInstrumentParticleKind(freeSlot, 1);
        owner.writeInstrumentParticleSpeedX(freeSlot, speedX[offsetIndex]);
        owner.writeInstrumentParticleSpeedY(freeSlot, 0xFD);
        owner.writeInstrumentParticleXAccumulator(freeSlot, 0);
        owner.writeInstrumentParticleYAccumulator(freeSlot, 0);
        runtime.physicsTables().writeEnemyTransitionCountdown(freeSlot, 0x38);
        runtime.combatTables().writeEnemyStunnedCountdown(freeSlot, 0);
        runtime.combatTables().writeDyingCountdown(freeSlot, 0);
        runtime.combatTables().writeEnemyHealth(freeSlot, 0);
        runtime.physicsTables().writeEnemyPhysicsFlags(freeSlot, ENTITY_PHYSICS_HARMLESS);
        runtime.combatTables().writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        runtime.combatTables().writeEnemyHitboxFlags(freeSlot, 0);
        runtime.physicsTables()
                .writeEntityOptions1Override(freeSlot, EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION);
        runtime.physicsTables().writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }
}
