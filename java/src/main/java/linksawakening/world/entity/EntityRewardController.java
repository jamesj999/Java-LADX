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
import linksawakening.world.entity.event.LinkSwordFinalPoseRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.*;

/** ROM reward presentation sequences, sharing the owning room slot table. */
public final class EntityRewardController {
    private final int[] swordPickupState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] swordPickupSequenceActive = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] powerUpPickupCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] toadstoolPickupSequenceActive = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] instrumentPickupSequenceActive = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentPickupState = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentBackgroundFade = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentObjectFade = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentHintBatches = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentWarpX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentWarpY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentPaletteCycle = new int[32];
    private final int[] instrumentWarpSprites = new int[256];
    private final int[] instrumentHintTiles = new int[20 * 18];
    private final int[] instrumentHintAttributes = new int[20 * 18];
    private final int[] instrumentWarpFadeStep = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentHintCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] instrumentHintOpened = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final boolean[] instrumentPickupSparklesPending = new boolean[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentParticleKind = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentParticleSpawnCountdown = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentParticleSpawnCounter = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentParticleSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentParticleSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentParticleXAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] instrumentParticleYAccumulator = new int[EntityRoomLoader.MAX_ENTITIES];
    private final EntityRewardControllerServices runtime;

    EntityRewardController(EntityRewardControllerServices runtime) {
        this.runtime = runtime;
    }

    void startInstrumentPickup(int slot) {
        this.writeInstrumentPickupSequenceActive(slot, true);
        this.writeInstrumentPickupState(slot, 0);
        this.writeInstrumentWarpFadeStep(slot, 0);
        this.writeInstrumentBackgroundFade(
                slot, this.writeInstrumentObjectFade(slot, this.writeInstrumentHintBatches(slot, 0)));
        this.writeInstrumentHintCountdown(slot, 0);
        this.writeInstrumentHintOpened(slot, false);
        this.writeInstrumentPickupSparklesPending(slot, true);
        runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x68);
        runtime.presentationState().writePendingMusicTrack(0x1B);
    }

    public void advanceInstrumentPickup(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkZ,
            IntSupplier randomByteSupplier) {
        int slot = entity.slot();
        if (this.readInstrumentPickupSparklesPending(slot)) {
            this.writeInstrumentPickupSparklesPending(slot, false);
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
        if (this.readInstrumentPickupState(slot) == 0
                && runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0x10) {
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x0F);
            this.writeInstrumentPickupState(slot, 1);
            runtime.events()
                    .pendingDialogRequests
                    .add(new DialogRequest(1, runtime.roomVariables().readEntityMapId() & 0xFF));
            runtime.events().pendingInstrumentRewards.add(new InstrumentRewardEvent(slot));
        } else if (this.readInstrumentPickupState(slot) == 1
                && !runtime.presentationState().readActiveMusic()
                && !runtime.linkState().dialogActive) {
            int mapId = runtime.roomVariables().readEntityMapId() & 0xFF;
            if (mapId < EntityRomConstants.INSTRUMENT_MUSIC_TRACKS.length) {
                runtime.presentationState().writePendingMusicTrack(EntityRomConstants.INSTRUMENT_MUSIC_TRACKS[mapId]);
                this.writeInstrumentPickupState(slot, 2);
                runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0xFF);
            }
        } else if (this.readInstrumentPickupState(slot) == 2
                && runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
            this.writeInstrumentPickupState(slot, 3);
            this.writeInstrumentWarpX(slot, (entity.x() - 1) & 255); // $03:5F43
            this.writeInstrumentWarpY(slot, entity.y());
            runtime.physicsTables().writeSlowTransitionCountdown(slot, 0x80);
            runtime.physicsTables().writeSlowTimerInitialized(slot, true);
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x2B));
        } else if (this.readInstrumentPickupState(slot) == 2) {
            this.writeInstrumentParticleSpawnCountdown(
                    slot, (this.readInstrumentParticleSpawnCountdown(slot) - 1) & 0xFF);
            if (this.readInstrumentParticleSpawnCountdown(slot) == 0xFF) {
                this.writeInstrumentParticleSpawnCountdown(slot, 0x17);
                this.writeInstrumentParticleSpawnCounter(
                        slot, (this.readInstrumentParticleSpawnCounter(slot) + 1) & 0xFF);
                spawnInstrumentParticle(
                        entity,
                        this.readInstrumentParticleSpawnCounter(slot) & 0x01,
                        randomByteSupplier.getAsInt() & 0x01);
            }
        } else if (this.readInstrumentPickupState(slot) == 3
                && !runtime.linkState().dialogActive
                && !runtime.linkState().inventoryAppearing
                && !runtime.roomVariables().readRoomTransitionActive()) {
            runtime.lifecycle().decrementSlowTransitionCountdown(slot, frameCounter);
            // $03:5E29: the warp controller advances its fade every eight
            // frames, then starts the fullscreen hint controller ($07:6523).
            if (this.readInstrumentHintCountdown(slot) > 0) {
                this.writeInstrumentHintBatches(slot, Math.min(9, this.readInstrumentHintBatches(slot) + 1));
                if (this.preInstrumentHintCountdown(slot, -1) == 0 && !this.readInstrumentHintOpened(slot)) {
                    this.writeInstrumentHintOpened(slot, true);
                    runtime.events()
                            .pendingDialogRequests
                            .add(new DialogRequest(
                                    1, 0x50 + (runtime.roomVariables().readEntityMapId() & 0xFF)));
                }
            }
            if (runtime.physicsTables().readSlowTransitionCountdown(slot) < 0x50
                    && this.readInstrumentWarpFadeStep(slot) < 0x19
                    && (frameCounter & 7) == 0) {
                int step = this.postInstrumentWarpFadeStep(slot, 1);
                if (step == 0) {
                    runtime.events()
                            .pendingEntityEvents
                            .add(new EntityCombatEvent(
                                    slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x2C));
                }
                if (step == 0x18) this.writeInstrumentHintCountdown(slot, 0x60);
            }
            // func_020_6D0E raises RGB555 channels by one on even frames.
            // BG starts at private state 5; OBJ starts at state 15.
            int fadeStep = this.readInstrumentWarpFadeStep(slot);
            if (runtime.physicsTables().readSlowTransitionCountdown(slot) < 0x50
                    && fadeStep <= 0x19
                    && (frameCounter & 1) == 0) {
                if (fadeStep >= 5)
                    this.writeInstrumentBackgroundFade(slot, Math.min(31, this.readInstrumentBackgroundFade(slot) + 1));
                if (fadeStep >= 15)
                    this.writeInstrumentObjectFade(slot, Math.min(31, this.readInstrumentObjectFade(slot) + 1));
            }
            if (runtime.physicsTables().readSlowTransitionCountdown(slot) == 0) {
                this.writeInstrumentPickupState(slot, 4);
                this.writeInstrumentPickupSequenceActive(slot, false);
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

    void spawnInstrumentParticle(RoomEntity source, int offsetIndex, int variant) {
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
        this.writeInstrumentParticleKind(freeSlot, 1);
        this.writeInstrumentParticleSpeedX(freeSlot, speedX[offsetIndex]);
        this.writeInstrumentParticleSpeedY(freeSlot, 0xFD);
        this.writeInstrumentParticleXAccumulator(freeSlot, 0);
        this.writeInstrumentParticleYAccumulator(freeSlot, 0);
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

    public void advanceInstrumentParticle(RoomEntity entity) {
        int slot = entity.slot();
        if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
            return;
        }
        int x = addFallingSpeedToPosition(
                entity.x(),
                this.readInstrumentParticleSpeedX(slot),
                this.storageInstrumentParticleXAccumulator(),
                slot);
        int y = addFallingSpeedToPosition(
                entity.y(),
                this.readInstrumentParticleSpeedY(slot),
                this.storageInstrumentParticleYAccumulator(),
                slot);
        runtime.slots()[slot] = withPositionAndVariant(entity, x, y, entity.spriteVariant());
    }

    public void advanceToadstoolPickup(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ) {
        int slot = entity.slot();
        int countdown = runtime.physicsTables().readEnemyTransitionCountdown(slot);
        if (runtime.linkState().dialogActive) {
            // UpdateEntityTimers is paused while a dialogue is open in the
            // ROM. Undo this runtime's generic tick before holding the item.
            if (countdown > 0) runtime.physicsTables().postEnemyTransitionCountdown(slot, 1);
        } else if (countdown == 0x10) {
            runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x0F);
            runtime.events().pendingDialogRequests.add(new DialogRequest(0, 0x0F));
        } else if (countdown == 1) {
            runtime.events().pendingToadstoolRewards.add(new ToadstoolRewardEvent(slot));
            runtime.presentationState()
                    .writePendingMusicTrack(runtime.story()
                            .readOwlDefaultMusicResolver()
                            .applyAsInt(runtime.roomVariables().readEntityRoomId()));
            this.writeToadstoolPickupSequenceActive(slot, false);
            runtime.lifecycle().disableEntityWithoutPersistence(slot);
            return;
        }
        runtime.events().pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        runtime.events().pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot));
        RoomEntity held =
                withPositionAndVariant(entity, linkEntityX, (linkEntityY - 0x0C) & 0xFF, entity.spriteVariant());
        runtime.slots()[slot] = withZ(held, linkZ & 0xFF);
    }

    public void advanceSwordPickup(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, int linkZ) {
        int slot = entity.slot();
        if (runtime.linkState().dialogActive) {
            if (runtime.physicsTables().readEnemyTransitionCountdown(slot) > 0) {
                runtime.physicsTables().postEnemyTransitionCountdown(slot, 1);
            }
            if (this.readSwordPickupState(slot) != 2) {
                runtime.holdSwordAboveLink(
                        entity, linkEntityX, linkEntityY, linkZ, this.readSwordPickupState(slot) == 3);
            } else {
                runtime.events().pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
            }
            return;
        }
        if (this.readSwordPickupState(slot) == 1) {
            runtime.lifecycle().decrementSlowTransitionCountdown(slot, frameCounter);
        }
        switch (this.readSwordPickupState(slot)) {
            case 0 -> {
                runtime.holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, false);
                int countdown = runtime.physicsTables().readEnemyTransitionCountdown(slot);
                if (countdown == 0x10) {
                    runtime.physicsTables().postEnemyTransitionCountdown(slot, -1);
                    runtime.events().pendingDialogRequests.add(new DialogRequest(0, 0x9B));
                } else if (countdown == 1) {
                    runtime.presentationState().writePendingMusicTrack(0x31);
                    runtime.physicsTables().writeSlowTransitionCountdown(slot, 0x52);
                    runtime.physicsTables().writeSlowTimerInitialized(slot, true);
                    this.writeSwordPickupState(slot, 1);
                }
            }
            case 1 -> {
                runtime.holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, false);
                if (runtime.physicsTables().readSlowTransitionCountdown(slot) == 0) {
                    runtime.slots()[slot] = withVariant(runtime.slots()[slot], -1);
                    runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x20);
                    this.writeSwordPickupState(slot, 2);
                    runtime.events().pendingLinkSwordSpinPoseRequests.add(new LinkSwordSpinPoseRequest(slot, 0x20));
                    runtime.events()
                            .pendingEntityEvents
                            .add(new EntityCombatEvent(
                                    slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x03));
                }
            }
            case 2 -> {
                // SwordShieldPickableState2Handler only advances the entity
                // countdown and restores variant 0; Link's spin handler owns
                // the motion lock and animation pose for these frames.
                runtime.events()
                        .pendingLinkSwordSpinPoseRequests
                        .add(new LinkSwordSpinPoseRequest(
                                slot, runtime.physicsTables().readEnemyTransitionCountdown(slot)));
                if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
                    runtime.physicsTables().writeEnemyTransitionCountdown(slot, 0x20);
                    runtime.slots()[slot] = withVariant(runtime.slots()[slot], 0);
                    this.writeSwordPickupState(slot, 3);
                }
            }
            default -> {
                runtime.events().pendingLinkSwordFinalPoseRequests.add(new LinkSwordFinalPoseRequest(slot));
                runtime.holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, true);
                if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0x1A) {
                    runtime.events()
                            .transientVfxRequests
                            .add(new TransientVfxRequest(
                                    TransientVfxType.SWORD_POKE, entity.x(), (entity.y() - 0x0C) & 0xFF));
                    runtime.events()
                            .pendingEntityEvents
                            .add(new EntityCombatEvent(
                                    slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x07));
                }
                if (runtime.physicsTables().readEnemyTransitionCountdown(slot) == 0) {
                    // The ROM leaves MUSIC_OVERWORLD_INTRO playing after the pose;
                    // the track data itself continues into the overworld loop.
                    runtime.events().pendingSwordPickupRewards.add(new SwordPickupRewardEvent(slot));
                    this.writeSwordPickupSequenceActive(slot, false);
                    runtime.lifecycle().disableEntityWithoutPersistence(slot);
                }
            }
        }
    }

    void advancePowerUpPickup(RoomEntity entity, int frame, int linkX, int linkY, int linkZ) {
        int slot = entity.slot();
        if (this.readPowerUpPickupCountdown(slot) == 0x30) {
            int[] xOffsets = {-0x1C, 0x14, -0x1C, 0x14};
            int[] yOffsets = {-0x2C, -0x2C, 0x04, 0x04};
            for (int variant = 3; variant >= 0; variant--) {
                runtime.events()
                        .transientVfxRequests
                        .add(new TransientVfxRequest(
                                TransientVfxType.MOVING_SPARKLE,
                                (linkX + xOffsets[variant]) & 0xFF,
                                (linkY + yOffsets[variant]) & 0xFF,
                                variant));
            }
        }
        if (!runtime.linkState().dialogActive) {
            int countdown = this.prePowerUpPickupCountdown(slot, -1);
            if (countdown == 0x2E) {
                runtime.events()
                        .pendingEntityEvents
                        .add(new EntityCombatEvent(
                                slot, entity.type(), 0, false, EntityCombatEvent.SoundChannel.JINGLE, 0x17));
            }
            if (countdown == 2) {
                this.writePowerUpPickupCountdown(slot, 1);
                runtime.events().pendingDialogRequests.add(new DialogRequest(0, entity.type() == 0x33 ? 0x08 : 0xEC));
            } else if (countdown == 0) {
                runtime.presentationState().writePendingMusicTrack(0x49);
                runtime.clearEntity(slot);
                return;
            }
        }
        runtime.events().pendingLinkMotionBlockRequests.add(new LinkMotionBlockRequest(slot));
        runtime.events().pendingLinkHeldItemPoseRequests.add(new LinkHeldItemPoseRequest(slot, true));
        RoomEntity held = withPositionAndVariant(
                entity,
                linkX,
                (linkY - 0x10) & 0xFF,
                entity.type() == 0x33 ? (frame >>> 4) & 1 : entity.spriteVariant());
        runtime.slots()[slot] = withZ(held, linkZ & 0xFF);
    }

    public void refreshInstrumentPalette(EntityStatus status, int slot, int frame) {
        if (status == EntityStatus.ACTIVE
                && this.readInstrumentParticleKind(slot) == 0
                && this.readInstrumentPickupState(slot) < 3
                && runtime.roomVariables().readTransitionSequenceCounter() == 4
                && runtime.presentationState().readSpriteSelection() != null) {
            int[][] palettes = runtime.presentationState().readSpriteSelection().objectPalettes();
            int offset = ((frame >> 1) & 0x3C) / 2;
            palettes[1][2] = this.readInstrumentPaletteCycle(offset);
            palettes[1][3] = this.readInstrumentPaletteCycle(offset + 1);
            runtime.presentationState()
                    .writeSpriteSelection(
                            runtime.presentationState().readSpriteSelection().withObjectPalettes(palettes));
        }
    }

    public int instrumentParticleKind(int slot) {
        return this.readInstrumentParticleKind(slot);
    }

    public boolean instrumentPickupSequenceActive(int slot) {
        return this.readInstrumentPickupSequenceActive(slot);
    }

    public boolean swordPickupSequenceActive(int slot) {
        return this.readSwordPickupSequenceActive(slot);
    }

    public boolean toadstoolPickupSequenceActive(int slot) {
        return this.readToadstoolPickupSequenceActive(slot);
    }

    public boolean secretSeashellScreenShakeActive() {
        return runtime.motions()
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .readSecretSeashellScreenShakeActive();
    }

    public boolean secretSeashellPegasusCollisionActive() {
        return runtime.motions()
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .readSecretSeashellPegasusCollisionActive();
    }

    public boolean secretSeashellPegasusCollisionCoordinatesKnown() {
        return runtime.motions()
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .readSecretSeashellPegasusCollisionCoordinatesKnown();
    }

    public int secretSeashellPegasusCollisionX() {
        return runtime.motions()
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .readSecretSeashellPegasusCollisionX();
    }

    public int secretSeashellPegasusCollisionY() {
        return runtime.motions()
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .readSecretSeashellPegasusCollisionY();
    }

    int readSwordPickupState(int slot) {
        return swordPickupState[slot];
    }

    int writeSwordPickupState(int slot, int value) {
        return swordPickupState[slot] = value;
    }

    boolean readSwordPickupSequenceActive(int slot) {
        return swordPickupSequenceActive[slot];
    }

    boolean writeSwordPickupSequenceActive(int slot, boolean value) {
        return swordPickupSequenceActive[slot] = value;
    }

    boolean[] storageSwordPickupSequenceActive() {
        return swordPickupSequenceActive;
    }

    int readPowerUpPickupCountdown(int slot) {
        return powerUpPickupCountdown[slot];
    }

    int writePowerUpPickupCountdown(int slot, int value) {
        return powerUpPickupCountdown[slot] = value;
    }

    int[] storagePowerUpPickupCountdown() {
        return powerUpPickupCountdown;
    }

    int prePowerUpPickupCountdown(int slot, int delta) {
        return powerUpPickupCountdown[slot] += delta;
    }

    boolean readToadstoolPickupSequenceActive(int slot) {
        return toadstoolPickupSequenceActive[slot];
    }

    boolean writeToadstoolPickupSequenceActive(int slot, boolean value) {
        return toadstoolPickupSequenceActive[slot] = value;
    }

    boolean readInstrumentPickupSequenceActive(int slot) {
        return instrumentPickupSequenceActive[slot];
    }

    boolean writeInstrumentPickupSequenceActive(int slot, boolean value) {
        return instrumentPickupSequenceActive[slot] = value;
    }

    int readInstrumentPickupState(int slot) {
        return instrumentPickupState[slot];
    }

    int writeInstrumentPickupState(int slot, int value) {
        return instrumentPickupState[slot] = value;
    }

    int[] storageInstrumentPickupState() {
        return instrumentPickupState;
    }

    int readInstrumentBackgroundFade(int slot) {
        return instrumentBackgroundFade[slot];
    }

    int writeInstrumentBackgroundFade(int slot, int value) {
        return instrumentBackgroundFade[slot] = value;
    }

    int readInstrumentObjectFade(int slot) {
        return instrumentObjectFade[slot];
    }

    int writeInstrumentObjectFade(int slot, int value) {
        return instrumentObjectFade[slot] = value;
    }

    int readInstrumentHintBatches(int slot) {
        return instrumentHintBatches[slot];
    }

    int writeInstrumentHintBatches(int slot, int value) {
        return instrumentHintBatches[slot] = value;
    }

    int readInstrumentWarpX(int slot) {
        return instrumentWarpX[slot];
    }

    int writeInstrumentWarpX(int slot, int value) {
        return instrumentWarpX[slot] = value;
    }

    int readInstrumentWarpY(int slot) {
        return instrumentWarpY[slot];
    }

    int writeInstrumentWarpY(int slot, int value) {
        return instrumentWarpY[slot] = value;
    }

    int readInstrumentPaletteCycle(int slot) {
        return instrumentPaletteCycle[slot];
    }

    int writeInstrumentPaletteCycle(int slot, int value) {
        return instrumentPaletteCycle[slot] = value;
    }

    int[] storageInstrumentPaletteCycle() {
        return instrumentPaletteCycle;
    }

    int writeInstrumentWarpSprites(int slot, int value) {
        return instrumentWarpSprites[slot] = value;
    }

    int[] storageInstrumentWarpSprites() {
        return instrumentWarpSprites;
    }

    int[] storageInstrumentHintTiles() {
        return instrumentHintTiles;
    }

    int[] storageInstrumentHintAttributes() {
        return instrumentHintAttributes;
    }

    int readInstrumentWarpFadeStep(int slot) {
        return instrumentWarpFadeStep[slot];
    }

    int writeInstrumentWarpFadeStep(int slot, int value) {
        return instrumentWarpFadeStep[slot] = value;
    }

    int postInstrumentWarpFadeStep(int slot, int delta) {
        int previous = instrumentWarpFadeStep[slot];
        instrumentWarpFadeStep[slot] += delta;
        return previous;
    }

    int readInstrumentHintCountdown(int slot) {
        return instrumentHintCountdown[slot];
    }

    int writeInstrumentHintCountdown(int slot, int value) {
        return instrumentHintCountdown[slot] = value;
    }

    int preInstrumentHintCountdown(int slot, int delta) {
        return instrumentHintCountdown[slot] += delta;
    }

    boolean readInstrumentHintOpened(int slot) {
        return instrumentHintOpened[slot];
    }

    boolean writeInstrumentHintOpened(int slot, boolean value) {
        return instrumentHintOpened[slot] = value;
    }

    boolean readInstrumentPickupSparklesPending(int slot) {
        return instrumentPickupSparklesPending[slot];
    }

    boolean writeInstrumentPickupSparklesPending(int slot, boolean value) {
        return instrumentPickupSparklesPending[slot] = value;
    }

    int readInstrumentParticleKind(int slot) {
        return instrumentParticleKind[slot];
    }

    int writeInstrumentParticleKind(int slot, int value) {
        return instrumentParticleKind[slot] = value;
    }

    int readInstrumentParticleSpawnCountdown(int slot) {
        return instrumentParticleSpawnCountdown[slot];
    }

    int writeInstrumentParticleSpawnCountdown(int slot, int value) {
        return instrumentParticleSpawnCountdown[slot] = value;
    }

    int readInstrumentParticleSpawnCounter(int slot) {
        return instrumentParticleSpawnCounter[slot];
    }

    int writeInstrumentParticleSpawnCounter(int slot, int value) {
        return instrumentParticleSpawnCounter[slot] = value;
    }

    int readInstrumentParticleSpeedX(int slot) {
        return instrumentParticleSpeedX[slot];
    }

    int writeInstrumentParticleSpeedX(int slot, int value) {
        return instrumentParticleSpeedX[slot] = value;
    }

    int readInstrumentParticleSpeedY(int slot) {
        return instrumentParticleSpeedY[slot];
    }

    int writeInstrumentParticleSpeedY(int slot, int value) {
        return instrumentParticleSpeedY[slot] = value;
    }

    int writeInstrumentParticleXAccumulator(int slot, int value) {
        return instrumentParticleXAccumulator[slot] = value;
    }

    int[] storageInstrumentParticleXAccumulator() {
        return instrumentParticleXAccumulator;
    }

    int writeInstrumentParticleYAccumulator(int slot, int value) {
        return instrumentParticleYAccumulator[slot] = value;
    }

    int[] storageInstrumentParticleYAccumulator() {
        return instrumentParticleYAccumulator;
    }
}
