package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import java.util.function.IntSupplier;
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
    final EntityRewardControllerServices runtime;

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
        InstrumentPickupBehavior.advanceInstrumentPickup(
                this, entity, frameCounter, linkEntityX, linkEntityY, linkZ, randomByteSupplier);
    }

    void spawnInstrumentParticle(RoomEntity source, int offsetIndex, int variant) {
        InstrumentPickupBehavior.spawnInstrumentParticle(this, source, offsetIndex, variant);
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
        ItemPickupBehavior.advanceToadstoolPickup(this, entity, linkEntityX, linkEntityY, linkZ);
    }

    public void advanceSwordPickup(RoomEntity entity, int frameCounter, int linkEntityX, int linkEntityY, int linkZ) {
        ItemPickupBehavior.advanceSwordPickup(this, entity, frameCounter, linkEntityX, linkEntityY, linkZ);
    }

    void advancePowerUpPickup(RoomEntity entity, int frame, int linkX, int linkY, int linkZ) {
        ItemPickupBehavior.advancePowerUpPickup(this, entity, frame, linkX, linkY, linkZ);
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

    final ItemPickupFrameStep itemPickupFrameStep = new ItemPickupFrameStep();
    final InstrumentPickupStep instrumentPickupStep = new InstrumentPickupStep();
}
