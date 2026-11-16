package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.addFallingSpeedToPosition;
import static linksawakening.world.entity.EntityMotionMath.romDirectionForJavaDirection;
import static linksawakening.world.entity.EntityRomConstants.ENTITY_PHYSICS_HARMLESS;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;
import static linksawakening.world.entity.EntitySnapshots.withVariant;
import static linksawakening.world.entity.EntitySnapshots.withZ;
import static linksawakening.world.entity.EntityTypeRules.isCommonDroppableType;
import static linksawakening.world.entity.EntityTypeRules.isRuntimeFloatingItem;
import static linksawakening.world.entity.EntityTypeRules.persistentClearMask;
import static linksawakening.world.entity.EntityTypeRules.requiresHeldPickupTransition;

import java.util.Objects;
import java.util.function.IntSupplier;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.entity.EntitySpriteHandlerCatalog;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.InstrumentCompletionEvent;
import linksawakening.world.entity.event.InstrumentRewardEvent;
import linksawakening.world.entity.event.KeyRewardEvent;
import linksawakening.world.entity.event.LinkHeldItemPoseRequest;
import linksawakening.world.entity.event.LinkMotionBlockRequest;
import linksawakening.world.entity.event.LinkSwordFinalPoseRequest;
import linksawakening.world.entity.event.LinkSwordSpinPoseRequest;
import linksawakening.world.entity.event.SwordPickupRewardEvent;
import linksawakening.world.entity.event.ToadstoolRewardEvent;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.FloatingItemMotion;
import linksawakening.world.entity.motion.HeartPiecePickupMotion;
import linksawakening.world.entity.motion.KeyDropPointMotion;
import linksawakening.world.entity.motion.SecretSeashellMotion;
import linksawakening.world.room.ChestContentsTable;

/** PickableCollectIfNeeded, SirensInstrumentEntityHandler and SwordShieldPickableEntityHandler. */
public final class EntityRewardController {
    final EntityRewardState state;
    final EntityMotionRegistry motions;
    final linksawakening.world.entity.EntitySpriteViews spriteHandlers;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLinkState linkState;
    final EntityPhysicsTables physicsTables;
    final EntityPresentationState presentationState;
    final EntityRoomVariables roomVariables;
    final EntitySlotLifecycle lifecycle;
    final EntityStoryState story;
    final EntitySlots slots;
    final EntitySpriteViews spriteViews;
    final EntityPlayerCommands playerCommands;

    final EntityPickupState pickups;
    final EntityBossState bossRewards;

    EntityRewardController(
            EntityRewardState state,
            EntityMotionRegistry motions,
            linksawakening.world.entity.EntitySpriteViews spriteHandlers,
            EntityEventChannels events,
            EntityLinkState linkState,
            EntityPresentationState presentationState,
            EntityRoomVariables roomVariables,
            EntitySlotLifecycle lifecycle,
            EntityStoryState story,
            EntitySlots slots,
            EntityPlayerCommands playerCommands,
            EntityPickupState pickups,
            EntityBossState bossRewards) {
        EntityCombatTables combatTables = slots.combat();
        EntityPhysicsTables physicsTables = slots.physics();
        EntitySpriteViews spriteViews = spriteHandlers;
        this.pickups = Objects.requireNonNull(pickups, "pickups");
        this.bossRewards = Objects.requireNonNull(bossRewards, "bossRewards");

        this.state = Objects.requireNonNull(state, "state");
        this.motions = Objects.requireNonNull(motions, "motions");
        this.spriteHandlers = Objects.requireNonNull(spriteHandlers, "sprite source");
        this.combatTables = Objects.requireNonNull(combatTables, "combatTables");
        this.events = Objects.requireNonNull(events, "events");
        this.linkState = Objects.requireNonNull(linkState, "linkState");
        this.physicsTables = Objects.requireNonNull(physicsTables, "physicsTables");
        this.presentationState = Objects.requireNonNull(presentationState, "presentationState");
        this.roomVariables = Objects.requireNonNull(roomVariables, "roomVariables");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.story = Objects.requireNonNull(story, "story");
        this.slots = Objects.requireNonNull(slots, "slots");
        this.spriteViews = Objects.requireNonNull(spriteViews, "spriteViews");
        this.playerCommands = Objects.requireNonNull(playerCommands, "playerCommands");
    }

    void startInstrumentPickup(int slot) {
        state.writeInstrumentPickupSequenceActive(slot, true);
        state.writeInstrumentPickupState(slot, 0);
        state.writeInstrumentWarpFadeStep(slot, 0);
        state.writeInstrumentHintBatches(slot, 0);
        state.writeInstrumentObjectFade(slot, 0);
        state.writeInstrumentBackgroundFade(slot, 0);
        state.writeInstrumentHintCountdown(slot, 0);
        state.writeInstrumentHintOpened(slot, false);
        state.writeInstrumentPickupSparklesPending(slot, true);
        this.physicsTables.writeEnemyTransitionCountdown(slot, 0x68);
        this.presentationState.writePendingMusicTrack(0x1B);
    }

    public void advanceInstrumentPickup(
            RoomEntity entity,
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkZ,
            IntSupplier randomByteSupplier) {
        this.instrumentPickupStep.run(entity, frameCounter, linkEntityX, linkEntityY, linkZ, randomByteSupplier);
    }

    void spawnInstrumentParticle(RoomEntity source, int offsetIndex, int variant) {
        int freeSlot = this.slots.findFree(this.slots.size() - 1);
        if (freeSlot < 0 || !this.spriteHandlers.supportsPresentation()) {
            return;
        }
        int[] xOffsets = {0x0A, -0x06};
        int[] speedX = {0x04, 0xFC};
        EntitySpriteDefinition definition = this.spriteViews.forInstrumentShard();
        this.slots.replace(
                freeSlot,
                new RoomEntity(
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
                        false));
        this.lifecycle.initializeHealthGroup(freeSlot);
        this.state.writeInstrumentParticleKind(freeSlot, 1);
        this.state.writeInstrumentParticleSpeedX(freeSlot, speedX[offsetIndex]);
        this.state.writeInstrumentParticleSpeedY(freeSlot, 0xFD);
        this.state.writeInstrumentParticleXAccumulator(freeSlot, 0);
        this.state.writeInstrumentParticleYAccumulator(freeSlot, 0);
        this.physicsTables.writeEnemyTransitionCountdown(freeSlot, 0x38);
        this.combatTables.writeEnemyStunnedCountdown(freeSlot, 0);
        this.combatTables.writeDyingCountdown(freeSlot, 0);
        this.combatTables.writeEnemyHealth(freeSlot, 0);
        this.physicsTables.writeEnemyPhysicsFlags(freeSlot, ENTITY_PHYSICS_HARMLESS);
        this.combatTables.writeEnemyIgnoreHitsCountdown(freeSlot, 1);
        this.combatTables.writeEnemyHitboxFlags(freeSlot, 0);
        this.physicsTables.writeEntityOptions1Override(freeSlot, EntityRomConstants.ENTITY_OPT1_NO_GROUND_INTERACTION);
        this.physicsTables.writeDynamicEntitySpawnedThisFrame(freeSlot, true);
    }

    public void advanceInstrumentParticle(RoomEntity entity) {
        int slot = entity.slot();
        if (this.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
            this.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        int x = addFallingSpeedToPosition(
                entity.x(),
                state.readInstrumentParticleSpeedX(slot),
                state.storageInstrumentParticleXAccumulator(),
                slot);
        int y = addFallingSpeedToPosition(
                entity.y(),
                state.readInstrumentParticleSpeedY(slot),
                state.storageInstrumentParticleYAccumulator(),
                slot);
        this.slots.replace(slot, withPositionAndVariant(entity, x, y, entity.spriteVariant()));
    }

    public void advanceToadstoolPickup(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ) {
        int slot = entity.slot();
        int countdown = this.physicsTables.readEnemyTransitionCountdown(slot);
        if (this.linkState.readDialogActive()) {
            // UpdateEntityTimers is paused while a dialogue is open in the
            // ROM. Undo this runtime's generic tick before holding the item.
            if (countdown > 0) this.physicsTables.postEnemyTransitionCountdown(slot, 1);
        } else if (countdown == 0x10) {
            this.physicsTables.writeEnemyTransitionCountdown(slot, 0x0F);
            this.events.dialog(0, 0x0F);
        } else if (countdown == 1) {
            this.events.toadstoolReward(new ToadstoolRewardEvent(slot));
            this.presentationState.writePendingMusicTrack(this.story
                    .owlState()
                    .readOwlDefaultMusicResolver()
                    .applyAsInt(this.roomVariables.readEntityRoomId()));
            this.state.writeToadstoolPickupSequenceActive(slot, false);
            this.lifecycle.disableEntityWithoutPersistence(slot);
            return;
        }
        this.events.linkMotionBlock(new LinkMotionBlockRequest(slot));
        this.events.linkHeldItemPose(new LinkHeldItemPoseRequest(slot));
        RoomEntity held =
                withPositionAndVariant(entity, linkEntityX, (linkEntityY - 0x0C) & 0xFF, entity.spriteVariant());
        this.slots.replace(slot, withZ(held, linkZ & 0xFF));
    }

    public void advanceSwordPickup(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, int linkZ) {
        this.itemPickupFrameStep.run(entity, frameCounter, linkEntityX, linkEntityY, linkZ);
    }

    void advancePowerUpPickup(RoomEntity entity, int frame, int linkX, int linkY, int linkZ) {
        int slot = entity.slot();
        if (this.state.readPowerUpPickupCountdown(slot) == 0x30) {
            int[] xOffsets = {-0x1C, 0x14, -0x1C, 0x14};
            int[] yOffsets = {-0x2C, -0x2C, 0x04, 0x04};
            for (int variant = 3; variant >= 0; variant--) {
                this.events.vfx(new TransientVfxRequest(
                        TransientVfxType.MOVING_SPARKLE,
                        (linkX + xOffsets[variant]) & 0xFF,
                        (linkY + yOffsets[variant]) & 0xFF,
                        variant));
            }
        }
        if (!this.linkState.readDialogActive()) {
            int countdown = this.state.prePowerUpPickupCountdown(slot, -1);
            if (countdown == 0x2E) {
                this.events.sound(slot, entity.type(), EntityCombatEvent.SoundChannel.JINGLE, 0x17);
            }
            if (countdown == 2) {
                this.state.writePowerUpPickupCountdown(slot, 1);
                this.events.dialog(0, entity.type() == 0x33 ? 0x08 : 0xEC);
            } else if (countdown == 0) {
                this.presentationState.writePendingMusicTrack(0x49);
                this.lifecycle.clearEntity(slot);
                return;
            }
        }
        this.events.linkMotionBlock(new LinkMotionBlockRequest(slot));
        this.events.linkHeldItemPose(new LinkHeldItemPoseRequest(slot, true));
        RoomEntity held = withPositionAndVariant(
                entity,
                linkX,
                (linkY - 0x10) & 0xFF,
                entity.type() == 0x33 ? (frame >>> 4) & 1 : entity.spriteVariant());
        this.slots.replace(slot, withZ(held, linkZ & 0xFF));
    }

    public void refreshInstrumentPalette(EntityStatus status, int slot, int frame) {
        if (status != EntityStatus.ACTIVE
                || state.readInstrumentParticleKind(slot) != 0
                || state.readInstrumentPickupState(slot) >= 3
                || this.roomVariables.readTransitionSequenceCounter() != 4
                || !this.presentationState.hasSpriteSelection()) return;
        int[][] palettes = this.presentationState.readSpriteSelection().objectPalettes();
        int offset = ((frame >> 1) & 0x3C) / 2;
        palettes[1][2] = state.readInstrumentPaletteCycle(offset);
        palettes[1][3] = state.readInstrumentPaletteCycle(offset + 1);
        this.presentationState.writeSpriteSelection(
                this.presentationState.readSpriteSelection().withObjectPalettes(palettes));
    }

    public int instrumentParticleKind(int slot) {
        return state.readInstrumentParticleKind(slot);
    }

    public boolean instrumentPickupSequenceActive(int slot) {
        return state.readInstrumentPickupSequenceActive(slot);
    }

    public boolean swordPickupSequenceActive(int slot) {
        return state.readSwordPickupSequenceActive(slot);
    }

    public boolean toadstoolPickupSequenceActive(int slot) {
        return state.readToadstoolPickupSequenceActive(slot);
    }

    public boolean secretSeashellScreenShakeActive() {
        var secretSeashellMotion = this.motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
        return secretSeashellMotion.readSecretSeashellScreenShakeActive();
    }

    public boolean secretSeashellPegasusCollisionActive() {
        var secretSeashellMotion = this.motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
        return secretSeashellMotion.readSecretSeashellPegasusCollisionActive();
    }

    public boolean secretSeashellPegasusCollisionCoordinatesKnown() {
        var secretSeashellMotion = this.motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
        return secretSeashellMotion.readSecretSeashellPegasusCollisionCoordinatesKnown();
    }

    public int secretSeashellPegasusCollisionX() {
        var secretSeashellMotion = this.motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
        return secretSeashellMotion.readSecretSeashellPegasusCollisionX();
    }

    public int secretSeashellPegasusCollisionY() {
        var secretSeashellMotion = this.motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
        return secretSeashellMotion.readSecretSeashellPegasusCollisionY();
    }

    final ItemPickupFrameStep itemPickupFrameStep = new ItemPickupFrameStep(this);
    final InstrumentPickupStep instrumentPickupStep = new InstrumentPickupStep(this);

    public EntityPickupEvent collectIfNeeded(
            int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive) {
        return collectIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, 0, 0);
    }

    public EntityPickupEvent collectIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection) {
        return collectIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection, 0);
    }

    public EntityPickupEvent collectIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection,
            int linkZ) {
        return collectIfNeeded(new EntityPickupFrame(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkAirborne,
                linkInteractive,
                linkDirection,
                linkZ,
                false,
                0,
                0,
                0,
                0));
    }

    public EntityPickupEvent collectIfNeeded(EntityPickupFrameInputs inputs) {
        return entitySlotLifecycleFrameStep.run(inputs);
    }

    final EntitySlotLifecycleFrameStep entitySlotLifecycleFrameStep = new EntitySlotLifecycleFrameStep(this);

    static final class EntitySlotLifecycleFrameStep {
        private final EntityRewardController owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private EntityPickupFrameInputs inputs;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        private boolean linkAirborne;
        private boolean linkInteractive;
        private int linkDirection;
        private int linkZ;
        private boolean swordActive;
        private int swordX;
        private int swordWidth;
        private int swordY;
        private int swordHeight;
        private int romDirection;

        EntitySlotLifecycleFrameStep(EntityRewardController owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        linksawakening.world.entity.EntityPickupEvent run() {
            frameCounter = inputs.frameCounter();
            linkEntityX = inputs.linkEntityX();
            linkEntityY = inputs.linkEntityY();
            linkAirborne = inputs.linkAirborne();
            linkInteractive = inputs.linkInteractive();
            linkDirection = inputs.linkDirection();
            linkZ = inputs.linkZ();
            swordActive = inputs.swordActive();
            swordX = inputs.swordX();
            swordWidth = inputs.swordWidth();
            swordY = inputs.swordY();
            swordHeight = inputs.swordHeight();
            if (!linkInteractive) {
                return null;
            }
            romDirection = romDirectionForJavaDirection(linkDirection);

            for (int index = owner.slots.size() - 1; index >= 0; index--) {
                EntityPickupEvent pickup = collectSlot(index);
                if (pickup != null) return pickup;
            }
            return null;
        }

        private EntityPickupEvent collectSlot(int index) {

            RoomEntity entity = owner.slots.get(index);
            if (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL) {
                var secretSeashellMotion = owner.motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
                secretSeashellMotion.ensureInitialized(entity.slot(), owner.roomVariables.readEntityRoomId());
            }
            boolean swordPickup =
                    swordActive && RoomEntityPickupRules.overlapsSword(entity, swordX, swordWidth, swordY, swordHeight);
            boolean floatingType = FloatingItemMotion.isFloatingItem(entity.type());
            boolean floating = isRuntimeFloatingItem(entity);
            if (!canCollect(entity, swordPickup, floatingType, floating)) return null;

            int persistentClearMask = entity.type() == EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP
                            && owner.pickups.readChestSwordLevel() == 0
                    ? 0
                    : persistentClearMask(entity);
            if (entity.type() == EntityRomConstants.ENTITY_KEY_DROP_POINT) {
                if (owner.roomVariables.readEntityRoomId() == 0x80 || entity.spriteVariant() != 0) {
                    owner.physicsTables.writeEnemyTransitionCountdown(
                            entity.slot(), KeyDropPointMotion.TRANSITION_COUNTDOWN);
                } else {
                    owner.events.keyReward(new KeyRewardEvent(entity.slot(), ChestContentsTable.CHEST_SMALL_KEY));
                    owner.lifecycle.clearEntity(entity.slot());
                }
            } else if (entity.type() == 0x35) {
                owner.pickups.writeHeartPiecePickup(entity.slot(), new HeartPiecePickupMotion());
                owner.physicsTables.writeEnemyTransitionCountdown(entity.slot(), 5);
                owner.presentationState.writePendingMusicTrack(0x10);
            } else if (entity.type() == EntityRomConstants.ENTITY_HEART_CONTAINER) {
                owner.physicsTables.writeEnemyTransitionCountdown(entity.slot(), 0x70);
            } else if (entity.type() == EntityRomConstants.ENTITY_SWORD_SHIELD_PICKUP
                    && owner.pickups.readChestSwordLevel() == 0) {
                owner.playerCommands.startSwordPickup(entity.slot());
            } else if (entity.type() == EntityRomConstants.ENTITY_SLEEPY_TOADSTOOL) {
                owner.playerCommands.startToadstoolPickup(entity.slot());
            } else if (entity.type() == EntitySpriteHandlerCatalog.ENTITY_INSTRUMENT_OF_THE_SIRENS) {
                owner.startInstrumentPickup(entity.slot());
            } else if (entity.type() == 0x33 || entity.type() == 0x34) {
                owner.state.writePowerUpPickupCountdown(entity.slot(), 0x30);
                owner.roomVariables.writeEntityInteractionCountdown(0x30);
                owner.presentationState.writePendingMusicTrack(0x27);
            } else if (requiresHeldPickupTransition(entity.type())) {
                owner.playerCommands.beginLift(entity.slot(), romDirection);
            } else {
                owner.lifecycle.clearEntity(entity.slot());
            }
            return new EntityPickupEvent(
                    entity.slot(), entity.type(), persistentClearMask, floating ? entity.spriteVariant() : -1);
        }

        private boolean canCollect(RoomEntity entity, boolean swordPickup, boolean floatingType, boolean floating) {
            var secretSeashellMotion = owner.motions.get(SecretSeashellMotion.class, SecretSeashellMotion::new);
            return !(!entity.loaded()
                    || entity.status() != EntityStatus.ACTIVE
                    || !RoomEntityPickupRules.isPickable(entity.type())
                    || (floatingType && !floating)
                    || (entity.type() == 0x35
                            && owner.pickups.readHeartPiecePickup(entity.slot()).started())
                    || owner.state.readPowerUpPickupCountdown(entity.slot()) != 0
                    || owner.bossRewards.readDropPrivateCountdown1(entity.slot()) > 0
                    || (entity.type() == EntityRomConstants.ENTITY_HEART_CONTAINER
                            && owner.physicsTables.readEnemyTransitionCountdown(entity.slot()) != 0)
                    || (entity.type() == EntityRomConstants.ENTITY_SLEEPY_TOADSTOOL
                            && owner.state.readToadstoolPickupSequenceActive(entity.slot()))
                    || (isCommonDroppableType(entity.type())
                            && owner.bossRewards.readDroppablePrivateState3(entity.slot()) != 0)
                    || (entity.type() == EntityRomConstants.ENTITY_DROPPABLE_SECRET_SEASHELL
                            && (secretSeashellMotion.privateState3(entity.slot()) != 0
                                    || secretSeashellMotion.privateCountdown1(entity.slot()) != 0))
                    || (!swordPickup
                            && !floating
                            && !RoomEntityPickupRules.collisionCadenceMatches(frameCounter, entity.slot()))
                    || (!swordPickup && !floating && linkAirborne)
                    || (floating
                            && !FloatingItemMotion.linkZAllowsCollection(
                                    owner.physicsTables.readGroundInteractionSideScrolling(), linkZ))
                    || !(swordPickup
                            || (floating
                                    ? RoomEntityPickupRules.overlapsFloatingItem(entity, linkEntityX, linkEntityY)
                                    : RoomEntityPickupRules.overlapsLink(entity, linkEntityX, linkEntityY))));
        }

        private boolean inUse;

        linksawakening.world.entity.EntityPickupEvent run(EntityPickupFrameInputs inputs) {
            if (inUse) {
                return new EntitySlotLifecycleFrameStep(owner).run(inputs);
            }
            inUse = true;
            frameCounter = 0;
            linkEntityX = 0;
            linkEntityY = 0;
            linkAirborne = false;
            linkInteractive = false;
            linkDirection = 0;
            linkZ = 0;
            swordActive = false;
            swordX = 0;
            swordWidth = 0;
            swordY = 0;
            swordHeight = 0;
            romDirection = 0;

            this.inputs = inputs;
            try {
                return run();
            } finally {
                inUse = false;

                this.inputs = null;
            }
        }
    }

    static final class InstrumentPickupStep {
        private final linksawakening.world.entity.EntityRewardController owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        private int linkZ;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private IntSupplier randomByteSupplier;
        private int slot;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity held;

        InstrumentPickupStep(linksawakening.world.entity.EntityRewardController owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        void run() {
            if (!prepareSparkles()) return;
            if (!advancePickupSequence()) return;
            if (!holdPickupPose()) return;
        }

        private boolean prepareSparkles() {

            slot = entity.slot();
            if (owner.state.readInstrumentPickupSparklesPending(slot)) {
                owner.state.writeInstrumentPickupSparklesPending(slot, false);
                int[] xOffsets = {-0x18, 0x18, -0x18, 0x18};
                int[] yOffsets = {-0x2C, -0x2C, 0x04, 0x04};
                for (int variant = 3; variant >= 0; variant--) {
                    owner.events.vfx(new TransientVfxRequest(
                            TransientVfxType.MOVING_SPARKLE,
                            (linkEntityX + xOffsets[variant]) & 0xFF,
                            (linkEntityY + yOffsets[variant]) & 0xFF,
                            variant));
                }
            }
            return true;
        }

        private boolean advancePickupSequence() {

            if (owner.state.readInstrumentPickupState(slot) == 0
                    && owner.physicsTables.readEnemyTransitionCountdown(slot) == 0x10) {
                owner.physicsTables.writeEnemyTransitionCountdown(slot, 0x0F);
                owner.state.writeInstrumentPickupState(slot, 1);
                owner.events.dialog(1, owner.roomVariables.readEntityMapId() & 0xFF);
                owner.events.instrumentReward(new InstrumentRewardEvent(slot));
            } else if (owner.state.readInstrumentPickupState(slot) == 1
                    && !owner.presentationState.readActiveMusic()
                    && !owner.linkState.readDialogActive()) {
                int mapId = owner.roomVariables.readEntityMapId() & 0xFF;
                if (mapId < EntityRomConstants.INSTRUMENT_MUSIC_TRACKS.length) {
                    owner.presentationState.writePendingMusicTrack(EntityRomConstants.INSTRUMENT_MUSIC_TRACKS[mapId]);
                    owner.state.writeInstrumentPickupState(slot, 2);
                    owner.physicsTables.writeEnemyTransitionCountdown(slot, 0xFF);
                }
            } else if (owner.state.readInstrumentPickupState(slot) == 2
                    && owner.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                owner.state.writeInstrumentPickupState(slot, 3);
                owner.state.writeInstrumentWarpX(slot, (entity.x() - 1) & 255); // $03:5F43
                owner.state.writeInstrumentWarpY(slot, entity.y());
                owner.physicsTables.writeSlowTransitionCountdown(slot, 0x80);
                owner.physicsTables.writeSlowTimerInitialized(slot, true);
                owner.events.sound(slot, entity.type(), EntityCombatEvent.SoundChannel.JINGLE, 0x2B);
            } else if (owner.state.readInstrumentPickupState(slot) == 2) {
                owner.state.writeInstrumentParticleSpawnCountdown(
                        slot, (owner.state.readInstrumentParticleSpawnCountdown(slot) - 1) & 0xFF);
                if (owner.state.readInstrumentParticleSpawnCountdown(slot) == 0xFF) {
                    owner.state.writeInstrumentParticleSpawnCountdown(slot, 0x17);
                    owner.state.writeInstrumentParticleSpawnCounter(
                            slot, (owner.state.readInstrumentParticleSpawnCounter(slot) + 1) & 0xFF);
                    owner.spawnInstrumentParticle(
                            entity,
                            owner.state.readInstrumentParticleSpawnCounter(slot) & 0x01,
                            randomByteSupplier.getAsInt() & 0x01);
                }
            } else if (owner.state.readInstrumentPickupState(slot) == 3
                    && !owner.linkState.readDialogActive()
                    && !owner.linkState.readInventoryAppearing()
                    && !owner.roomVariables.readRoomTransitionActive()) {
                return advanceWarpFade();
            }
            return true;
        }

        private boolean holdPickupPose() {

            owner.events.linkMotionBlock(new LinkMotionBlockRequest(slot));
            owner.events.linkHeldItemPose(new LinkHeldItemPoseRequest(slot));
            held = withPositionAndVariant(entity, linkEntityX, (linkEntityY - 0x0C) & 0xFF, entity.spriteVariant());
            owner.slots.replace(slot, withZ(held, linkZ & 0xFF));
            return true;
        }

        private boolean advanceWarpFade() {
            owner.lifecycle.decrementSlowTransitionCountdown(slot, frameCounter);
            // $03:5E29: the warp controller advances its fade every eight
            // frames, then starts the fullscreen hint controller ($07:6523).
            if (owner.state.readInstrumentHintCountdown(slot) > 0) {
                owner.state.writeInstrumentHintBatches(
                        slot, Math.min(9, owner.state.readInstrumentHintBatches(slot) + 1));
                if (owner.state.preInstrumentHintCountdown(slot, -1) == 0
                        && !owner.state.readInstrumentHintOpened(slot)) {
                    owner.state.writeInstrumentHintOpened(slot, true);
                    owner.events.dialog(1, 0x50 + (owner.roomVariables.readEntityMapId() & 0xFF));
                }
            }
            if (owner.physicsTables.readSlowTransitionCountdown(slot) < 0x50
                    && owner.state.readInstrumentWarpFadeStep(slot) < 0x19
                    && (frameCounter & 7) == 0) {
                int step = owner.state.postInstrumentWarpFadeStep(slot, 1);
                if (step == 0) {
                    owner.events.sound(slot, entity.type(), EntityCombatEvent.SoundChannel.NOISE, 0x2C);
                }
                if (step == 0x18) owner.state.writeInstrumentHintCountdown(slot, 0x60);
            }
            // func_020_6D0E raises RGB555 channels by one on even frames.
            // BG starts at private state 5; OBJ starts at state 15.
            int fadeStep = owner.state.readInstrumentWarpFadeStep(slot);
            if (owner.physicsTables.readSlowTransitionCountdown(slot) < 0x50
                    && fadeStep <= 0x19
                    && (frameCounter & 1) == 0) {
                if (fadeStep >= 5)
                    owner.state.writeInstrumentBackgroundFade(
                            slot, Math.min(31, owner.state.readInstrumentBackgroundFade(slot) + 1));
                if (fadeStep >= 15)
                    owner.state.writeInstrumentObjectFade(
                            slot, Math.min(31, owner.state.readInstrumentObjectFade(slot) + 1));
            }
            if (owner.physicsTables.readSlowTransitionCountdown(slot) == 0) {
                owner.state.writeInstrumentPickupState(slot, 4);
                owner.state.writeInstrumentPickupSequenceActive(slot, false);
                owner.events.instrumentCompletion(
                        new InstrumentCompletionEvent(slot, owner.roomVariables.readEntityMapId() & 0xFF));
                owner.lifecycle.disableEntityWithoutPersistence(slot);
                return false;
            }

            return true;
        }

        private boolean inUse;

        void run(
                linksawakening.world.entity.RoomEntity entity,
                int frameCounter,
                int linkEntityX,
                int linkEntityY,
                int linkZ,
                IntSupplier randomByteSupplier) {
            if (inUse) {
                new InstrumentPickupStep(owner)
                        .run(entity, frameCounter, linkEntityX, linkEntityY, linkZ, randomByteSupplier);
                return;
            }
            inUse = true;

            slot = 0;
            held = null;

            this.entity = entity;
            this.frameCounter = frameCounter;
            this.linkEntityX = linkEntityX;
            this.linkEntityY = linkEntityY;
            this.linkZ = linkZ;
            this.randomByteSupplier = randomByteSupplier;
            try {
                run();
            } finally {
                inUse = false;

                this.entity = null;
                this.randomByteSupplier = null;

                this.held = null;
            }
        }
    }

    static final class ItemPickupFrameStep {
        private final linksawakening.world.entity.EntityRewardController owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frameCounter;
        private int linkEntityX;
        private int linkEntityY;
        private int linkZ;
        private int slot;

        ItemPickupFrameStep(linksawakening.world.entity.EntityRewardController owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        void run() {

            slot = entity.slot();
            if (owner.linkState.readDialogActive()) {
                if (owner.physicsTables.readEnemyTransitionCountdown(slot) > 0) {
                    owner.physicsTables.postEnemyTransitionCountdown(slot, 1);
                }
                if (owner.state.readSwordPickupState(slot) != 2) {
                    owner.playerCommands.holdSwordAboveLink(
                            entity, linkEntityX, linkEntityY, linkZ, owner.state.readSwordPickupState(slot) == 3);
                } else {
                    owner.events.linkMotionBlock(new LinkMotionBlockRequest(slot));
                }
                return;
            }
            if (owner.state.readSwordPickupState(slot) == 1) {
                owner.lifecycle.decrementSlowTransitionCountdown(slot, frameCounter);
            }
            switch (owner.state.readSwordPickupState(slot)) {
                case 0 -> {
                    if (!advanceState0()) return;
                }
                case 1 -> {
                    if (!advanceState1()) return;
                }
                case 2 -> {
                    if (!advanceState2()) return;
                }
                default -> {
                    if (!advanceDefault()) return;
                }
            }
        }

        private boolean advanceState0() {
            owner.playerCommands.holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, false);
            int countdown = owner.physicsTables.readEnemyTransitionCountdown(slot);
            if (countdown == 0x10) {
                owner.physicsTables.postEnemyTransitionCountdown(slot, -1);
                owner.events.dialog(0, 0x9B);
            } else if (countdown == 1) {
                owner.presentationState.writePendingMusicTrack(0x31);
                owner.physicsTables.writeSlowTransitionCountdown(slot, 0x52);
                owner.physicsTables.writeSlowTimerInitialized(slot, true);
                owner.state.writeSwordPickupState(slot, 1);
            }

            return true;
        }

        private boolean advanceState1() {
            owner.playerCommands.holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, false);
            if (owner.physicsTables.readSlowTransitionCountdown(slot) == 0) {
                owner.slots.replace(slot, withVariant(owner.slots.get(slot), -1));
                owner.physicsTables.writeEnemyTransitionCountdown(slot, 0x20);
                owner.state.writeSwordPickupState(slot, 2);
                owner.events.linkSwordSpinPose(new LinkSwordSpinPoseRequest(slot, 0x20));
                owner.events.sound(slot, entity.type(), EntityCombatEvent.SoundChannel.NOISE, 0x03);
            }

            return true;
        }

        private boolean advanceState2() {
            // SwordShieldPickableState2Handler only advances the entity
            // countdown and restores variant 0; Link's spin handler owns
            // the motion lock and animation pose for these frames.
            owner.events.linkSwordSpinPose(
                    new LinkSwordSpinPoseRequest(slot, owner.physicsTables.readEnemyTransitionCountdown(slot)));
            if (owner.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                owner.physicsTables.writeEnemyTransitionCountdown(slot, 0x20);
                owner.slots.replace(slot, withVariant(owner.slots.get(slot), 0));
                owner.state.writeSwordPickupState(slot, 3);
            }

            return true;
        }

        private boolean advanceDefault() {
            owner.events.linkSwordFinalPose(new LinkSwordFinalPoseRequest(slot));
            owner.playerCommands.holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, true);
            if (owner.physicsTables.readEnemyTransitionCountdown(slot) == 0x1A) {
                owner.events.vfx(
                        new TransientVfxRequest(TransientVfxType.SWORD_POKE, entity.x(), (entity.y() - 0x0C) & 0xFF));
                owner.events.sound(slot, entity.type(), EntityCombatEvent.SoundChannel.JINGLE, 0x07);
            }
            if (owner.physicsTables.readEnemyTransitionCountdown(slot) == 0) {
                // The ROM leaves MUSIC_OVERWORLD_INTRO playing after the pose;
                // the track data itself continues into the overworld loop.
                owner.events.swordPickupReward(new SwordPickupRewardEvent(slot));
                owner.state.writeSwordPickupSequenceActive(slot, false);
                owner.lifecycle.disableEntityWithoutPersistence(slot);
            }

            return true;
        }

        private boolean inUse;

        void run(
                linksawakening.world.entity.RoomEntity entity,
                int frameCounter,
                int linkEntityX,
                int linkEntityY,
                int linkZ) {
            if (inUse) {
                new ItemPickupFrameStep(owner).run(entity, frameCounter, linkEntityX, linkEntityY, linkZ);
                return;
            }
            inUse = true;

            slot = 0;

            this.entity = entity;
            this.frameCounter = frameCounter;
            this.linkEntityX = linkEntityX;
            this.linkEntityY = linkEntityY;
            this.linkZ = linkZ;
            try {
                run();
            } finally {
                inUse = false;

                this.entity = null;
            }
        }
    }
}
