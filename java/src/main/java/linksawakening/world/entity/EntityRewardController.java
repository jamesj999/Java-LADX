package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.function.IntSupplier;
import linksawakening.world.entity.motion.*;

/** SirensInstrumentEntityHandler and SwordShieldPickableEntityHandler pickup sequences and owned state. */
public final class EntityRewardController {
    final EntityRewardState state;
    final EntityMotionRegistry motions;
    // Optional ROM data, absent in ROM-free runtimes.
    final linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLinkState linkState;
    final EntityPhysicsTables physicsTables;
    final EntityPresentationState presentationState;
    final EntityRoomVariables roomVariables;
    final EntitySlotLifecycle lifecycle;
    final EntityStoryState story;
    final RoomEntity[] slots;
    final EntitySpriteViews spriteViews;
    final EntityPlayerCommands playerCommands;

    final EntityPickupState pickups;
    final EntityBossState bossRewards;

    EntityRewardController(
            EntityRewardState state,
            EntityMotionRegistry motions,
            linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityLinkState linkState,
            EntityPhysicsTables physicsTables,
            EntityPresentationState presentationState,
            EntityRoomVariables roomVariables,
            EntitySlotLifecycle lifecycle,
            EntityStoryState story,
            RoomEntity[] slots,
            EntitySpriteViews spriteViews,
            EntityPlayerCommands playerCommands,
            EntityPickupState pickups,
            EntityBossState bossRewards) {
        this.pickups = java.util.Objects.requireNonNull(pickups, "pickups");
        this.bossRewards = java.util.Objects.requireNonNull(bossRewards, "bossRewards");

        this.state = java.util.Objects.requireNonNull(state, "state");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.spriteHandlers = spriteHandlers;
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.linkState = java.util.Objects.requireNonNull(linkState, "linkState");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.presentationState = java.util.Objects.requireNonNull(presentationState, "presentationState");
        this.roomVariables = java.util.Objects.requireNonNull(roomVariables, "roomVariables");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
        this.story = java.util.Objects.requireNonNull(story, "story");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.spriteViews = java.util.Objects.requireNonNull(spriteViews, "spriteViews");
        this.playerCommands = java.util.Objects.requireNonNull(playerCommands, "playerCommands");
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
        InstrumentPickupBehavior.advanceInstrumentPickup(
                this, entity, frameCounter, linkEntityX, linkEntityY, linkZ, randomByteSupplier);
    }

    void spawnInstrumentParticle(RoomEntity source, int offsetIndex, int variant) {
        InstrumentPickupBehavior.spawnInstrumentParticle(this, source, offsetIndex, variant);
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
        this.slots[slot] = withPositionAndVariant(entity, x, y, entity.spriteVariant());
    }

    public void advanceToadstoolPickup(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ) {
        ItemPickupBehavior.advanceToadstoolPickup(this, entity, linkEntityX, linkEntityY, linkZ);
    }

    public void advanceSwordPickup(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, int linkZ) {
        ItemPickupBehavior.advanceSwordPickup(this, entity, frameCounter, linkEntityX, linkEntityY, linkZ);
    }

    void advancePowerUpPickup(RoomEntity entity, int frame, int linkX, int linkY, int linkZ) {
        ItemPickupBehavior.advancePowerUpPickup(this, entity, frame, linkX, linkY, linkZ);
    }

    public void refreshInstrumentPalette(EntityStatus status, int slot, int frame) {
        if (status != EntityStatus.ACTIVE
                || state.readInstrumentParticleKind(slot) != 0
                || state.readInstrumentPickupState(slot) >= 3
                || this.roomVariables.readTransitionSequenceCounter() != 4
                || this.presentationState.readSpriteSelection() == null) return;
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
        return this.motions
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .readSecretSeashellScreenShakeActive();
    }

    public boolean secretSeashellPegasusCollisionActive() {
        return this.motions
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .readSecretSeashellPegasusCollisionActive();
    }

    public boolean secretSeashellPegasusCollisionCoordinatesKnown() {
        return this.motions
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .readSecretSeashellPegasusCollisionCoordinatesKnown();
    }

    public int secretSeashellPegasusCollisionX() {
        return this.motions
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .readSecretSeashellPegasusCollisionX();
    }

    public int secretSeashellPegasusCollisionY() {
        return this.motions
                .get(SecretSeashellMotion.class, SecretSeashellMotion::new)
                .readSecretSeashellPegasusCollisionY();
    }

    final ItemPickupFrameStep itemPickupFrameStep = new ItemPickupFrameStep();
    final InstrumentPickupStep instrumentPickupStep = new InstrumentPickupStep();

    EntityPickupEvent collectIfNeeded(
            int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive) {
        return collectIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, 0, 0);
    }

    EntityPickupEvent collectIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection) {
        return collectIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection, 0);
    }

    EntityPickupEvent collectIfNeeded(
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

    EntityPickupEvent collectIfNeeded(EntityPickupFrameInputs inputs) {
        return entitySlotLifecycleFrameStep.run(this, inputs);
    }

    final EntitySlotLifecycleFrameStep entitySlotLifecycleFrameStep = new EntitySlotLifecycleFrameStep();
}
