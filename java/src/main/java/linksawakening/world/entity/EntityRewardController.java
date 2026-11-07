package linksawakening.world.entity;

import linksawakening.world.entity.event.EntityCombatEvent;

import linksawakening.world.entity.event.DialogRequest;
import linksawakening.world.entity.event.InstrumentCompletionEvent;
import linksawakening.world.entity.event.InstrumentRewardEvent;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.LinkSwordFinalPoseRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.TransientVfxRequest;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;
import java.util.function.IntSupplier;
import static linksawakening.world.entity.RoomEntityRuntime.*;

/** ROM reward presentation sequences, sharing the owning room slot table. */
public final class EntityRewardController {
    private final RoomEntityRuntime runtime;
    EntityRewardController(RoomEntityRuntime runtime) { this.runtime = runtime; }

    void startInstrumentPickup(int slot) {
        runtime.instrumentPickupSequenceActive[slot] = true;
        runtime.instrumentPickupState[slot] = 0;
        runtime.instrumentWarpFadeStep[slot] = 0;
        runtime.instrumentBackgroundFade[slot] = runtime.instrumentObjectFade[slot] = runtime.instrumentHintBatches[slot] = 0;
        runtime.instrumentHintCountdown[slot] = 0;
        runtime.instrumentHintOpened[slot] = false;
        runtime.instrumentPickupSparklesPending[slot] = true;
        runtime.enemyTransitionCountdown[slot] = 0x68;
        runtime.pendingMusicTrack = 0x1B;
        }

    public void advanceInstrumentPickup(RoomEntity entity, int frameCounter,
                                         int linkEntityX, int linkEntityY, int linkZ,
                                         IntSupplier randomByteSupplier) {
        int slot = entity.slot();
        if (runtime.instrumentPickupSparklesPending[slot]) {
            runtime.instrumentPickupSparklesPending[slot] = false;
            int[] xOffsets = {-0x18, 0x18, -0x18, 0x18};
            int[] yOffsets = {-0x2C, -0x2C, 0x04, 0x04};
            for (int variant = 3; variant >= 0; variant--) {
                runtime.events.transientVfxRequests.add(new TransientVfxRequest(
                    TransientVfxType.MOVING_SPARKLE,
                    (linkEntityX + xOffsets[variant]) & 0xFF,
                    (linkEntityY + yOffsets[variant]) & 0xFF,
                    variant));
            }
        }
        if (runtime.instrumentPickupState[slot] == 0
            && runtime.enemyTransitionCountdown[slot] == 0x10) {
            runtime.enemyTransitionCountdown[slot] = 0x0F;
            runtime.instrumentPickupState[slot] = 1;
            runtime.events.pendingDialogRequests.add(new DialogRequest(1, runtime.entityMapId & 0xFF));
            runtime.events.pendingInstrumentRewards.add(new InstrumentRewardEvent(slot));
        } else if (runtime.instrumentPickupState[slot] == 1 && !runtime.activeMusic && !runtime.linkState.dialogActive) {
            int mapId = runtime.entityMapId & 0xFF;
            if (mapId < runtime.INSTRUMENT_MUSIC_TRACKS.length) {
                runtime.pendingMusicTrack = runtime.INSTRUMENT_MUSIC_TRACKS[mapId];
                runtime.instrumentPickupState[slot] = 2;
                runtime.enemyTransitionCountdown[slot] = 0xFF;
            }
        } else if (runtime.instrumentPickupState[slot] == 2
            && runtime.enemyTransitionCountdown[slot] == 0) {
            runtime.instrumentPickupState[slot] = 3;
            runtime.instrumentWarpX[slot] = (entity.x() - 1) & 255; // $03:5F43
            runtime.instrumentWarpY[slot] = entity.y();
            runtime.slowTransitionCountdown[slot] = 0x80;
            runtime.slowTimerInitialized[slot] = true;
            runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                slot, entity.type(), 0, false,
                EntityCombatEvent.SoundChannel.JINGLE, 0x2B));
        } else if (runtime.instrumentPickupState[slot] == 2) {
            runtime.instrumentParticleSpawnCountdown[slot] =
                (runtime.instrumentParticleSpawnCountdown[slot] - 1) & 0xFF;
            if (runtime.instrumentParticleSpawnCountdown[slot] == 0xFF) {
                runtime.instrumentParticleSpawnCountdown[slot] = 0x17;
                runtime.instrumentParticleSpawnCounter[slot] =
                    (runtime.instrumentParticleSpawnCounter[slot] + 1) & 0xFF;
                spawnInstrumentParticle(entity,
                    runtime.instrumentParticleSpawnCounter[slot] & 0x01,
                    randomByteSupplier.getAsInt() & 0x01);
            }
        } else if (runtime.instrumentPickupState[slot] == 3 && !runtime.linkState.dialogActive
            && !runtime.linkState.inventoryAppearing && !runtime.roomTransitionActive) {
            runtime.lifecycle.decrementSlowTransitionCountdown(slot, frameCounter);
            // $03:5E29: the warp controller advances its fade every eight
            // frames, then starts the fullscreen hint controller ($07:6523).
            if (runtime.instrumentHintCountdown[slot] > 0) {
                runtime.instrumentHintBatches[slot] = Math.min(9, runtime.instrumentHintBatches[slot] + 1);
                if (--runtime.instrumentHintCountdown[slot] == 0 && !runtime.instrumentHintOpened[slot]) {
                    runtime.instrumentHintOpened[slot] = true;
                    runtime.events.pendingDialogRequests.add(new DialogRequest(1, 0x50 + (runtime.entityMapId & 0xFF)));
                }
            }
            if (runtime.slowTransitionCountdown[slot] < 0x50
                && runtime.instrumentWarpFadeStep[slot] < 0x19 && (frameCounter & 7) == 0) {
                int step = runtime.instrumentWarpFadeStep[slot]++;
                if (step == 0) {
                    runtime.events.pendingEntityEvents.add(new EntityCombatEvent(slot, entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.NOISE, 0x2C));
                }
                if (step == 0x18) runtime.instrumentHintCountdown[slot] = 0x60;
            }
            // func_020_6D0E raises RGB555 channels by one on even frames.
            // BG starts at private state 5; OBJ starts at state 15.
            int fadeStep = runtime.instrumentWarpFadeStep[slot];
            if (runtime.slowTransitionCountdown[slot] < 0x50 && fadeStep <= 0x19
                && (frameCounter & 1) == 0) {
                if (fadeStep >= 5) runtime.instrumentBackgroundFade[slot] =
                    Math.min(31, runtime.instrumentBackgroundFade[slot] + 1);
                if (fadeStep >= 15) runtime.instrumentObjectFade[slot] =
                    Math.min(31, runtime.instrumentObjectFade[slot] + 1);
            }
            if (runtime.slowTransitionCountdown[slot] == 0) {
                runtime.instrumentPickupState[slot] = 4;
                runtime.instrumentPickupSequenceActive[slot] = false;
                runtime.events.pendingInstrumentCompletions.add(
                    new InstrumentCompletionEvent(slot, runtime.entityMapId & 0xFF));
                runtime.lifecycle.disableEntityWithoutPersistence(slot);
                return;
            }
        }
        runtime.events.pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        runtime.events.pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        RoomEntity held = withPositionAndVariant(entity, linkEntityX,
            (linkEntityY - 0x0C) & 0xFF, entity.spriteVariant());
        runtime.slots[slot] = withZ(held, linkZ & 0xFF);
        }

    void spawnInstrumentParticle(RoomEntity source, int offsetIndex, int variant) {
        int freeSlot = runtime.findFreeEntitySlot();
        if (freeSlot < 0 || runtime.spriteHandlers == null) {
            return;
        }
        int[] xOffsets = {0x0A, -0x06};
        int[] speedX = {0x04, 0xFC};
        EntitySpriteDefinition definition = runtime.spriteHandlers.forInstrumentShard();
        runtime.slots[freeSlot] = new RoomEntity(freeSlot, -1, source.type(),
            (source.x() + xOffsets[offsetIndex]) & 0xFF,
            (source.y() - 0x08) & 0xFF,
            EntityStatus.ACTIVE, definition, variant & 0x01, 0, 0, 0, -1, false);
        runtime.instrumentParticleKind[freeSlot] = 1;
        runtime.instrumentParticleSpeedX[freeSlot] = speedX[offsetIndex];
        runtime.instrumentParticleSpeedY[freeSlot] = 0xFD;
        runtime.instrumentParticleXAccumulator[freeSlot] = 0;
        runtime.instrumentParticleYAccumulator[freeSlot] = 0;
        runtime.enemyTransitionCountdown[freeSlot] = 0x38;
        runtime.enemyStunnedCountdown[freeSlot] = 0;
        runtime.dyingCountdown[freeSlot] = 0;
        runtime.enemyHealth[freeSlot] = 0;
        runtime.enemyPhysicsFlags[freeSlot] = ENTITY_PHYSICS_HARMLESS;
        runtime.enemyIgnoreHitsCountdown[freeSlot] = 1;
        runtime.enemyHitboxFlags[freeSlot] = 0;
        runtime.entityOptions1Override[freeSlot] = runtime.ENTITY_OPT1_NO_GROUND_INTERACTION;
        runtime.dynamicEntitySpawnedThisFrame[freeSlot] = true;
        }

    public void advanceInstrumentParticle(RoomEntity entity) {
        int slot = entity.slot();
        if (runtime.enemyTransitionCountdown[slot] == 0) {
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        int x = addFallingSpeedToPosition(entity.x(), runtime.instrumentParticleSpeedX[slot],
            runtime.instrumentParticleXAccumulator, slot);
        int y = addFallingSpeedToPosition(entity.y(), runtime.instrumentParticleSpeedY[slot],
            runtime.instrumentParticleYAccumulator, slot);
        runtime.slots[slot] = withPositionAndVariant(entity, x, y, entity.spriteVariant());
        }

    public void advanceToadstoolPickup(RoomEntity entity,
                                        int linkEntityX, int linkEntityY, int linkZ) {
        int slot = entity.slot();
        int countdown = runtime.enemyTransitionCountdown[slot];
        if (runtime.linkState.dialogActive) {
            // UpdateEntityTimers is paused while a dialogue is open in the
            // ROM. Undo this runtime's generic tick before holding the item.
            if (countdown > 0) runtime.enemyTransitionCountdown[slot]++;
        } else if (countdown == 0x10) {
            runtime.enemyTransitionCountdown[slot] = 0x0F;
            runtime.events.pendingDialogRequests.add(new DialogRequest(0, 0x0F));
        } else if (countdown == 1) {
            runtime.events.pendingToadstoolRewards.add(new ToadstoolRewardEvent(slot));
            runtime.pendingMusicTrack = runtime.owlDefaultMusicResolver.applyAsInt(runtime.entityRoomId);
            runtime.toadstoolPickupSequenceActive[slot] = false;
            runtime.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        runtime.events.pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        runtime.events.pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        RoomEntity held = withPositionAndVariant(entity, linkEntityX,
            (linkEntityY - 0x0C) & 0xFF, entity.spriteVariant());
        runtime.slots[slot] = withZ(held, linkZ & 0xFF);
        }

    public void advanceSwordPickup(RoomEntity entity, int frameCounter,
                                    int linkEntityX, int linkEntityY, int linkZ) {
        int slot = entity.slot();
        if (runtime.linkState.dialogActive) {
            if (runtime.enemyTransitionCountdown[slot] > 0) {
                runtime.enemyTransitionCountdown[slot]++;
            }
            if (runtime.swordPickupState[slot] != 2) {
                runtime.holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ,
                    runtime.swordPickupState[slot] == 3);
            } else {
                runtime.events.pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
            }
            return;
        }
        if (runtime.swordPickupState[slot] == 1) {
            runtime.lifecycle.decrementSlowTransitionCountdown(slot, frameCounter);
        }
        switch (runtime.swordPickupState[slot]) {
            case 0 -> {
                runtime.holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, false);
                int countdown = runtime.enemyTransitionCountdown[slot];
                if (countdown == 0x10) {
                    runtime.enemyTransitionCountdown[slot]--;
                    runtime.events.pendingDialogRequests.add(new DialogRequest(0, 0x9B));
                } else if (countdown == 1) {
                    runtime.pendingMusicTrack = 0x31;
                    runtime.slowTransitionCountdown[slot] = 0x52;
                    runtime.slowTimerInitialized[slot] = true;
                    runtime.swordPickupState[slot] = 1;
                }
            }
            case 1 -> {
                runtime.holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, false);
                if (runtime.slowTransitionCountdown[slot] == 0) {
                    runtime.slots[slot] = withVariant(runtime.slots[slot], -1);
                    runtime.enemyTransitionCountdown[slot] = 0x20;
                    runtime.swordPickupState[slot] = 2;
                    runtime.events.pendingLinkSwordSpinPoseRequests.add(
                        new LinkSwordSpinPoseRequest(slot, 0x20));
                    runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.NOISE, 0x03));
                }
            }
            case 2 -> {
                // SwordShieldPickableState2Handler only advances the entity
                // countdown and restores variant 0; Link's spin handler owns
                // the motion lock and animation pose for these frames.
                runtime.events.pendingLinkSwordSpinPoseRequests.add(new LinkSwordSpinPoseRequest(
                    slot, runtime.enemyTransitionCountdown[slot]));
                if (runtime.enemyTransitionCountdown[slot] == 0) {
                    runtime.enemyTransitionCountdown[slot] = 0x20;
                    runtime.slots[slot] = withVariant(runtime.slots[slot], 0);
                    runtime.swordPickupState[slot] = 3;
                }
            }
            default -> {
                runtime.events.pendingLinkSwordFinalPoseRequests.add(new LinkSwordFinalPoseRequest(slot));
                runtime.holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, true);
                if (runtime.enemyTransitionCountdown[slot] == 0x1A) {
                    runtime.events.transientVfxRequests.add(new TransientVfxRequest(
                        TransientVfxType.SWORD_POKE, entity.x(),
                        (entity.y() - 0x0C) & 0xFF));
                    runtime.events.pendingEntityEvents.add(new EntityCombatEvent(
                        slot, entity.type(), 0, false,
                        EntityCombatEvent.SoundChannel.JINGLE, 0x07));
                }
                if (runtime.enemyTransitionCountdown[slot] == 0) {
                    runtime.pendingMusicTrack = 0x05;
                    runtime.events.pendingSwordPickupRewards.add(new SwordPickupRewardEvent(slot));
                    runtime.swordPickupSequenceActive[slot] = false;
                    runtime.lifecycle.disableEntityWithoutPersistence(slot);
                }
            }
        }
        }

    void advancePowerUpPickup(RoomEntity entity, int frame,
                                      int linkX, int linkY, int linkZ) {
        int slot = entity.slot();
        if (runtime.powerUpPickupCountdown[slot] == 0x30) {
            int[] xOffsets = {-0x1C, 0x14, -0x1C, 0x14};
            int[] yOffsets = {-0x2C, -0x2C, 0x04, 0x04};
            for (int variant = 3; variant >= 0; variant--) {
                runtime.events.transientVfxRequests.add(new TransientVfxRequest(TransientVfxType.MOVING_SPARKLE,
                    (linkX + xOffsets[variant]) & 0xFF,
                    (linkY + yOffsets[variant]) & 0xFF, variant));
            }
        }
        if (!runtime.linkState.dialogActive) {
            int countdown = --runtime.powerUpPickupCountdown[slot];
            if (countdown == 0x2E) {
                runtime.events.pendingEntityEvents.add(new EntityCombatEvent(slot, entity.type(), 0, false,
                    EntityCombatEvent.SoundChannel.JINGLE, 0x17));
            }
            if (countdown == 2) {
                runtime.powerUpPickupCountdown[slot] = 1;
                runtime.events.pendingDialogRequests.add(new DialogRequest(0, entity.type() == 0x33 ? 0x08 : 0xEC));
            } else if (countdown == 0) {
                runtime.pendingMusicTrack = 0x49;
                runtime.clearEntity(slot);
                return;
            }
        }
        runtime.events.pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        runtime.events.pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot, true));
        RoomEntity held = withPositionAndVariant(entity, linkX, (linkY - 0x10) & 0xFF,
            entity.type() == 0x33 ? (frame >>> 4) & 1 : entity.spriteVariant());
        runtime.slots[slot] = withZ(held, linkZ & 0xFF);
        }
  public void refreshInstrumentPalette(EntityStatus status, int slot, int frame) {
              if (status == EntityStatus.ACTIVE

                  && runtime.instrumentParticleKind[slot] == 0
                  && runtime.instrumentPickupState[slot] < 3
                  && runtime.transitionSequenceCounter == 4
                  && runtime.spriteSelection != null) {
                int[][] palettes = runtime.spriteSelection.objectPalettes();
                int offset = ((frame >> 1) & 0x3C) / 2;
                palettes[1][2] = runtime.instrumentPaletteCycle[offset];
                palettes[1][3] = runtime.instrumentPaletteCycle[offset + 1];
                runtime.spriteSelection =
                    runtime.spriteSelection.withObjectPalettes(palettes);
              }
  }

  public int instrumentParticleKind(int slot) { return runtime.instrumentParticleKind[slot]; }
  public boolean instrumentPickupSequenceActive(int slot) { return runtime.instrumentPickupSequenceActive[slot]; }
  public boolean swordPickupSequenceActive(int slot) { return runtime.swordPickupSequenceActive[slot]; }
  public boolean toadstoolPickupSequenceActive(int slot) { return runtime.toadstoolPickupSequenceActive[slot]; }
  public boolean secretSeashellScreenShakeActive() { return runtime.secretSeashellScreenShakeActive; }
  public boolean secretSeashellPegasusCollisionActive() { return runtime.secretSeashellPegasusCollisionActive; }
  public boolean secretSeashellPegasusCollisionCoordinatesKnown() { return runtime.secretSeashellPegasusCollisionCoordinatesKnown; }
  public int secretSeashellPegasusCollisionX() { return runtime.secretSeashellPegasusCollisionX; }
  public int secretSeashellPegasusCollisionY() { return runtime.secretSeashellPegasusCollisionY; }
}
