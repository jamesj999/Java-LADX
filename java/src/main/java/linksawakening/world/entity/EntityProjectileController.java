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
    final boolean indoorRoom;
    // Optional ROM data; ROM-free runtimes retain the existing fallback behavior.
    final linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers;
    final EntityBombState bombInteractions;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLiftState liftThrow;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityPlayerSpawns entityPlayerSpawns;
    final EntityRoomVariables roomVariables;
    final EntitySlotLifecycle lifecycle;
    final EntitySlotProperties slotProperties;
    // Optional ROM data; ROM-free runtimes retain the existing fallback behavior.
    final RomEnemyCombatTables enemyCombatTables;
    final RoomEntity[] slots;
    final EntityProjectileState state;

    EntityProjectileController(
            boolean indoorRoom,
            linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers,
            EntityBombState bombInteractions,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityLiftState liftThrow,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityPlayerSpawns entityPlayerSpawns,
            EntityRoomVariables roomVariables,
            EntitySlotLifecycle lifecycle,
            EntitySlotProperties slotProperties,
            RomEnemyCombatTables enemyCombatTables,
            RoomEntity[] slots,
            EntityProjectileState state) {
        this.indoorRoom = indoorRoom;
        this.spriteHandlers = spriteHandlers;
        this.bombInteractions = java.util.Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.liftThrow = java.util.Objects.requireNonNull(liftThrow, "liftThrow");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.entityPlayerSpawns = java.util.Objects.requireNonNull(entityPlayerSpawns, "entityPlayerSpawns");
        this.roomVariables = java.util.Objects.requireNonNull(roomVariables, "roomVariables");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
        this.slotProperties = java.util.Objects.requireNonNull(slotProperties, "slotProperties");
        this.enemyCombatTables = enemyCombatTables;
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.state = java.util.Objects.requireNonNull(state, "state");
    }

    public void playBoomerangSoundIfDue(RoomEntity entity) {
        // PlayBoomerangSfx ($20:4C98) only reads the shared clock.
        if (state.readBoomerangSfxCounter() == 0) {
            this.events.pendingEntityEvents.add(new EntityCombatEvent(
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
        if (this.indoorRoom) {
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
                && this.motions
                                .get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new)
                                .maskRemovalState(target.slot())
                        == 0
                && (this.motions
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
                this.bombInteractions.readPlayerArrowBombArrow(arrow.slot())
                        ? EntityRomConstants.DAMAGE_TYPE_BOMB_ARROW
                        : EntityRomConstants.DAMAGE_TYPE_ARROW,
                this.motions
                        .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                        .speedX(arrow.slot()),
                this.motions
                        .get(PlayerArrowMotion.class, PlayerArrowMotion::new)
                        .speedY(arrow.slot()));
    }

    void applyPlayerProjectileDamage(RoomEntity target, int damageType, int speedX, int speedY) {
        PlayerProjectileDamage.applyPlayerProjectileDamage(this, target, damageType, speedX, speedY);
    }

    public boolean playerArrowBombArrow(int slot) {
        return this.bombInteractions.readPlayerArrowBombArrow(slot);
    }

    public void setWingedStateTwoThisFrame(int slot, boolean value) {
        this.slotProperties.state.writeWingedStateTwoThisFrame(slot, value);
    }

    public void setWingedSwordAttackThisFrame(int slot, boolean value) {
        this.slotProperties.state.writeWingedSwordAttackThisFrame(slot, value);
    }

    public int swordMoblinAlertingSoundCounter() {
        return this.motions.get(MoblinSwordMotion.class, MoblinSwordMotion::new).readSwordMoblinAlertingSoundCounter();
    }

    public void setSwordMoblinAlertingSoundCounter(int value) {
        this.motions.get(MoblinSwordMotion.class, MoblinSwordMotion::new).writeSwordMoblinAlertingSoundCounter(value);
    }

    final PlayerProjectileDamageFrameStep playerProjectileDamageFrameStep = new PlayerProjectileDamageFrameStep();
    final MagicPowderStep magicPowderStep = new MagicPowderStep();
    final MagicRodFrameStep magicRodFrameStep = new MagicRodFrameStep();
    final BoomerangStep boomerangStep = new BoomerangStep();
}
