package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.RoomEntityRuntime.ProjectileEntityCollision;
import linksawakening.world.entity.RoomEntityRuntime.ProjectileObjectCollision;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.HookshotChainMotion;

/** ROM player-projectile collision and object effects, sharing the owning room slot table. */
public final class EntityProjectileController {
    private final int[] magicPowderPrivateState4 = new int[EntityRoomLoader.MAX_ENTITIES];
    private int latestShotArrowEntityIndex = -1;
    private boolean lastArrowShotPlayedWhoosh;
    private final int[] magicPowderState = new int[EntityRoomLoader.MAX_ENTITIES];
    private int torchPaletteTargetAddress;
    private boolean torchPaletteTransitionActive;
    private final int[] musicalNoteInertia = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] musicalNoteSpeedX = new int[EntityRoomLoader.MAX_ENTITIES];
    private final int[] musicalNoteSpeedY = new int[EntityRoomLoader.MAX_ENTITIES];
    private int boomerangSfxCounter;
    private boolean boomerangSoundClockProvided;
    final EntityProjectileControllerServices runtime;

    EntityProjectileController(EntityProjectileControllerServices runtime) {
        this.runtime = runtime;
    }

    public void playBoomerangSoundIfDue(RoomEntity entity) {
        // PlayBoomerangSfx ($20:4C98) only reads the shared clock.
        if (this.readBoomerangSfxCounter() == 0) {
            runtime.events()
                    .pendingEntityEvents
                    .add(new EntityCombatEvent(
                            entity.slot(),
                            entity.type(),
                            0,
                            false,
                            EntityCombatEvent.SoundChannel.NOISE,
                            EntityRomConstants.BOOMERANG_SFX_ID));
        }
    }

    public void advanceBoomerangEntity(BoomerangFrame inputs) {
        BoomerangBehavior.advanceBoomerangEntity(this, inputs);
    }

    public void advanceSwordBeamEntity(
            int index,
            RoomEntity entity,
            int frame,
            int linkEntityX,
            int linkEntityY,
            int romLinkDirection,
            boolean handlerInteractive) {
        SwordBeamBehavior.advanceSwordBeamEntity(
                this, index, entity, frame, linkEntityX, linkEntityY, romLinkDirection, handlerInteractive);
    }

    public void advanceMagicRodFireballEntity(
            int index, RoomEntity entity, int frame, boolean handlerInteractive, boolean timersInteractive) {
        MagicRodBehavior.advanceMagicRodFireballEntity(
                this, index, entity, frame, handlerInteractive, timersInteractive);
    }

    boolean isMagicRodBurnableObject(RoomEntityObjectSample object) {
        if (object == null) {
            return false;
        }
        int objectId = object.objectId() & 0xFF;
        if (runtime.indoorRoom()) {
            return objectId == 0x8A; // OBJECT_FROZEN_BLOCK
        }
        return objectId == EntityRomConstants.OBJECT_BUSH || objectId == EntityRomConstants.OBJECT_BUSH_GROUND_STAIRS;
    }

    public void advanceMagicPowderSprinkleEntity(
            int index,
            RoomEntity entity,
            int frame,
            int linkMotionState,
            boolean handlerInteractive,
            boolean timersInteractive) {
        MagicPowderBehavior.advanceMagicPowderSprinkleEntity(
                this, index, entity, frame, linkMotionState, handlerInteractive, timersInteractive);
    }

    boolean collideMagicPowderWithEntities(RoomEntity sprinkle, int frame) {
        return MagicPowderBehavior.collideMagicPowderWithEntities(this, sprinkle, frame);
    }

    ProjectileObjectCollision projectileObjectCollision(
            RoomEntity entity, RoomEntityObjectSample object, int frame, int slot) {
        return PlayerProjectileDamage.projectileObjectCollision(this, entity, object, frame, slot);
    }

    ProjectileEntityCollision collideSwordBeamWithEntities(RoomEntity beam, int frame) {
        return SwordBeamBehavior.collideSwordBeamWithEntities(this, beam, frame);
    }

    boolean collideMagicRodFireballWithEntities(RoomEntity fireball, int frame) {
        return MagicRodBehavior.collideMagicRodFireballWithEntities(this, fireball, frame);
    }

    public boolean collidePlayerArrowWithEntities(RoomEntity arrow, int frame) {
        return PlayerProjectileDamage.collidePlayerArrowWithEntities(this, arrow, frame);
    }

    boolean collideBoomerangWithEntities(RoomEntity boomerang, int frame) {
        return BoomerangBehavior.collideBoomerangWithEntities(this, boomerang, frame);
    }

    boolean ironMaskBlocksProjectile(RoomEntity target, int direction) {
        return target.type() == EntityRomConstants.ENTITY_IRON_MASK
                && runtime.motions()
                                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                                .maskRemovalState(target.slot())
                        == 0
                && (runtime.motions()
                                        .get(RoamingEnemyMotion.class, RoamingEnemyMotion::new)
                                        .direction(target.slot())
                                ^ 1)
                        == direction;
    }

    public boolean collideBouncingBombiteWithEntities(RoomEntity source, int frame) {
        return PlayerProjectileDamage.collideBouncingBombiteWithEntities(this, source, frame);
    }

    public boolean collideHookshotWithEntities(
            RoomEntity hookshot, HookshotChainMotion.State hookshotState, int frame) {
        return HookshotCollisionBehavior.collideHookshotWithEntities(this, hookshot, hookshotState, frame);
    }

    void applyPlayerArrowDamage(RoomEntity arrow, RoomEntity target) {
        applyPlayerProjectileDamage(
                target,
                runtime.bombInteractions().readPlayerArrowBombArrow(arrow.slot())
                        ? EntityRomConstants.DAMAGE_TYPE_BOMB_ARROW
                        : EntityRomConstants.DAMAGE_TYPE_ARROW,
                runtime.motions()
                        .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                        .speedX(arrow.slot()),
                runtime.motions()
                        .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                        .speedY(arrow.slot()));
    }

    void applyPlayerProjectileDamage(RoomEntity target, int damageType, int speedX, int speedY) {
        PlayerProjectileDamage.applyPlayerProjectileDamage(this, target, damageType, speedX, speedY);
    }

    public boolean playerArrowBombArrow(int slot) {
        return runtime.bombInteractions().readPlayerArrowBombArrow(slot);
    }

    public void setWingedStateTwoThisFrame(int slot, boolean value) {
        runtime.slotProperties().writeWingedStateTwoThisFrame(slot, value);
    }

    public void setWingedSwordAttackThisFrame(int slot, boolean value) {
        runtime.slotProperties().writeWingedSwordAttackThisFrame(slot, value);
    }

    public int swordMoblinAlertingSoundCounter() {
        return runtime.motions()
                .get(MoblinSwordMotion.class, MoblinSwordMotion::new)
                .readSwordMoblinAlertingSoundCounter();
    }

    public void setSwordMoblinAlertingSoundCounter(int value) {
        runtime.motions()
                .get(MoblinSwordMotion.class, MoblinSwordMotion::new)
                .writeSwordMoblinAlertingSoundCounter(value);
    }

    int readMagicPowderPrivateState4(int slot) {
        return magicPowderPrivateState4[slot];
    }

    int writeMagicPowderPrivateState4(int slot, int value) {
        return magicPowderPrivateState4[slot] = value;
    }

    int readLatestShotArrowEntityIndex() {
        return latestShotArrowEntityIndex;
    }

    int writeLatestShotArrowEntityIndex(int value) {
        return latestShotArrowEntityIndex = value;
    }

    boolean readLastArrowShotPlayedWhoosh() {
        return lastArrowShotPlayedWhoosh;
    }

    boolean writeLastArrowShotPlayedWhoosh(boolean value) {
        return lastArrowShotPlayedWhoosh = value;
    }

    int readMagicPowderState(int slot) {
        return magicPowderState[slot];
    }

    int writeMagicPowderState(int slot, int value) {
        return magicPowderState[slot] = value;
    }

    int readTorchPaletteTargetAddress() {
        return torchPaletteTargetAddress;
    }

    int writeTorchPaletteTargetAddress(int value) {
        return torchPaletteTargetAddress = value;
    }

    boolean readTorchPaletteTransitionActive() {
        return torchPaletteTransitionActive;
    }

    boolean writeTorchPaletteTransitionActive(boolean value) {
        return torchPaletteTransitionActive = value;
    }

    int readMusicalNoteInertia(int slot) {
        return musicalNoteInertia[slot];
    }

    int writeMusicalNoteInertia(int slot, int value) {
        return musicalNoteInertia[slot] = value;
    }

    int readMusicalNoteSpeedX(int slot) {
        return musicalNoteSpeedX[slot];
    }

    int writeMusicalNoteSpeedX(int slot, int value) {
        return musicalNoteSpeedX[slot] = value;
    }

    int readMusicalNoteSpeedY(int slot) {
        return musicalNoteSpeedY[slot];
    }

    int writeMusicalNoteSpeedY(int slot, int value) {
        return musicalNoteSpeedY[slot] = value;
    }

    int readBoomerangSfxCounter() {
        return boomerangSfxCounter;
    }

    int writeBoomerangSfxCounter(int value) {
        return boomerangSfxCounter = value;
    }

    boolean readBoomerangSoundClockProvided() {
        return boomerangSoundClockProvided;
    }

    boolean writeBoomerangSoundClockProvided(boolean value) {
        return boomerangSoundClockProvided = value;
    }
}
