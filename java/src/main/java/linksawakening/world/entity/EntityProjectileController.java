package linksawakening.world.entity;

import static linksawakening.world.entity.EntitySnapshots.withDeathPresentation;
import static linksawakening.world.entity.EntitySnapshots.withDefinition;
import static linksawakening.world.entity.EntitySnapshots.withPositionAndVariant;
import static linksawakening.world.entity.EntitySnapshots.withStatus;
import static linksawakening.world.entity.EntitySnapshots.withVariant;
import static linksawakening.world.entity.EntityTypeRules.isZolGelType;

import java.util.Objects;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.vfx.TransientVfxType;
import linksawakening.world.entity.event.BoomerangObjectRequest;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.MagicPowderObjectAction;
import linksawakening.world.entity.event.MagicPowderObjectRequest;
import linksawakening.world.entity.event.MagicRodObjectRequest;
import linksawakening.world.entity.event.TransientVfxRequest;
import linksawakening.world.entity.motion.BombiteMotion;
import linksawakening.world.entity.motion.BoomerangMotion;
import linksawakening.world.entity.motion.EnemyRecoilMotion;
import linksawakening.world.entity.motion.HookshotChainMotion;
import linksawakening.world.entity.motion.MagicRodFireballMotion;
import linksawakening.world.entity.motion.MoblinSwordMotion;
import linksawakening.world.entity.motion.PlayerArrowMotion;
import linksawakening.world.entity.motion.RoamingEnemyMotion;
import linksawakening.world.entity.motion.SwordBeamMotion;
import linksawakening.world.entity.motion.TarinRaccoonMotion;
import linksawakening.world.entity.motion.UnmaskedIronMaskMotion;
import linksawakening.world.entity.motion.ZolGelMotion;

/** ArrowEntityHandler, BoomerangEntityHandler and MagicRodFireballEntityHandler collision/object effects.
 *
 * Player projectile collision arbitration shares target-slot mutation, damage scratch, object probes and ordered effects across projectile types; persistent family state belongs to the registry-owned motions.
 */
public final class EntityProjectileController {
    record ProjectileEntityCollision(boolean unload, boolean blocked) {}

    enum ProjectileObjectCollision {
        NONE,
        BLOCKED,
        UNLOAD
    }

    final boolean indoorRoom;
    final linksawakening.world.entity.EntitySpriteViews spriteHandlers;
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
    final RomEnemyCombatTables enemyCombatTables;
    final EntitySlots slots;
    private final EntityProjectileState state;

    EntityProjectileController(
            boolean indoorRoom,
            linksawakening.world.entity.EntitySpriteViews spriteHandlers,
            EntityBombState bombInteractions,
            EntityEventChannels events,
            EntityLiftState liftThrow,
            EntityMotionRegistry motions,
            EntityPlayerSpawns entityPlayerSpawns,
            EntityRoomVariables roomVariables,
            EntitySlotLifecycle lifecycle,
            EntitySlotProperties slotProperties,
            RomEnemyCombatTables enemyCombatTables,
            EntitySlots slots,
            EntityProjectileState state) {
        EntityCombatTables combatTables = slots.combat();
        EntityPhysicsTables physicsTables = slots.physics();
        this.indoorRoom = indoorRoom;
        this.spriteHandlers = Objects.requireNonNull(spriteHandlers, "sprite source");
        this.bombInteractions = Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.combatTables = Objects.requireNonNull(combatTables, "combatTables");
        this.events = Objects.requireNonNull(events, "events");
        this.liftThrow = Objects.requireNonNull(liftThrow, "liftThrow");
        this.motions = Objects.requireNonNull(motions, "motions");
        this.physicsTables = Objects.requireNonNull(physicsTables, "physicsTables");
        this.entityPlayerSpawns = Objects.requireNonNull(entityPlayerSpawns, "entityPlayerSpawns");
        this.roomVariables = Objects.requireNonNull(roomVariables, "roomVariables");
        this.lifecycle = Objects.requireNonNull(lifecycle, "lifecycle");
        this.slotProperties = Objects.requireNonNull(slotProperties, "slotProperties");
        this.enemyCombatTables = Objects.requireNonNull(enemyCombatTables, "combat rules");
        this.slots = Objects.requireNonNull(slots, "slots");
        this.state = Objects.requireNonNull(state, "state");
    }

    public void playBoomerangSoundIfDue(RoomEntity entity) {
        // PlayBoomerangSfx ($20:4C98) only reads the shared clock.
        if (state.readBoomerangSfxCounter() == 0) {
            this.events.sound(
                    entity.slot(),
                    entity.type(),
                    EntityCombatEvent.SoundChannel.NOISE,
                    EntityRomConstants.BOOMERANG_SFX_ID);
        }
    }

    public void advanceBoomerangEntity(BoomerangFrame inputs) {
        this.boomerangStep.run(inputs);
    }

    public void advanceSwordBeamEntity(
            EntitySlotExecution slotState, EntityFrameInputLatch inputs, EntityTimerGates timers) {
        int index = slotState.index();
        RoomEntity entity = slotState.entity();
        int frame = inputs.frame();
        int linkEntityX = inputs.linkEntityX();
        int linkEntityY = inputs.linkEntityY();
        int romLinkDirection = inputs.romLinkDirection();
        boolean handlerInteractive = timers.handlerInteractive();
        int slot = entity.slot();
        var swordBeamMotion = this.motions.get(SwordBeamMotion.class, SwordBeamMotion::new);
        if (swordBeamMotion.state(slot) == 0) {
            int x = (linkEntityX + SwordBeamMotion.firstHandlerOffsetX(romLinkDirection)) & 0xFF;
            int y = (linkEntityY + SwordBeamMotion.firstHandlerOffsetY(romLinkDirection)) & 0xFF;
            swordBeamMotion.initializeFirstHandler(slot, romLinkDirection);
            this.combatTables.writeEnemyFlashCountdown(slot, 0xFF);
            this.events.sound(
                    slot,
                    EntityRomConstants.ENTITY_SWORD_BEAM,
                    EntityCombatEvent.SoundChannel.JINGLE,
                    EntityRomConstants.SWORD_BEAM_JINGLE_ID);
            // The state-0 handler only applies the Link-relative launch
            // offset and enters state 1; it does not render the beam yet.
            this.slots.replace(index, withPositionAndVariant(entity, x, y, -1));
            return;
        }

        int variant = swordBeamMotion.direction(slot);
        if (!handlerInteractive) {
            // ReturnIfNonInteractive_19 renders state 1, then returns before
            // damage, movement, object intersection, or VFX creation.
            this.slots.replace(index, withPositionAndVariant(entity, entity.x(), entity.y(), variant));
            return;
        }

        ProjectileEntityCollision entityCollision = this.collideSwordBeamWithEntities(entity, frame);
        RoomEntity moved = swordBeamMotion.advancePosition(entity);
        RoomEntityObjectQuery intersectionQuery = this.physicsTables.readObjectIntersectionQuery();
        RoomEntityObjectSample object = intersectionQuery.sampleOrAbsent(moved);
        ProjectileObjectCollision objectCollision = !object.sampled()
                ? ProjectileObjectCollision.NONE
                : this.projectileObjectCollision(moved, object, frame, slot);
        if (entityCollision.unload()
                || entityCollision.blocked()
                || objectCollision != ProjectileObjectCollision.NONE) {
            this.lifecycle.clearEntity(slot);
            // $45A0 clears the slot and falls through to the shared poke VFX.
            // Physics $FF unloads inside bank $03, bypassing this branch.
            if (objectCollision != ProjectileObjectCollision.UNLOAD
                    && (entityCollision.blocked() || objectCollision == ProjectileObjectCollision.BLOCKED)) {
                this.events.vfx(new TransientVfxRequest(
                        TransientVfxType.SWORD_POKE, entity.x(), (entity.y() - entity.z() + 3) & 0xFF));
            }
            return;
        }

        if (((frame + 1) & 0x03) == 0) {
            this.events.vfx(new TransientVfxRequest(
                    TransientVfxType.SWORD_BEAM, entity.x(), (entity.y() - entity.z()) & 0xFF, variant));
        }
        this.slots.replace(index, withPositionAndVariant(moved, moved.x(), moved.y(), variant));
    }

    public void advanceMagicRodFireballEntity(
            EntitySlotExecution slotState, EntityFrameInputLatch inputs, EntityTimerGates timers) {
        this.magicRodFrameStep.run(slotState, inputs, timers);
    }

    boolean isMagicRodBurnableObject(RoomEntityObjectSample object) {
        if (!object.sampled()) {
            return false;
        }
        int objectId = object.objectId() & 0xFF;
        if (this.indoorRoom) {
            return objectId == 0x8A; // OBJECT_FROZEN_BLOCK
        }
        return objectId == EntityRomConstants.OBJECT_BUSH || objectId == EntityRomConstants.OBJECT_BUSH_GROUND_STAIRS;
    }

    public void advanceMagicPowderSprinkleEntity(
            EntitySlotExecution slotState, EntityFrameInputLatch inputs, EntityTimerGates timers) {
        this.magicPowderStep.run(slotState, inputs, timers);
    }

    boolean collideMagicPowderWithEntities(RoomEntity sprinkle, int frame) {
        int sourceSlot = sprinkle.slot();
        int sourceVisualY = (sprinkle.y() - sprinkle.z()) & 0xFF;
        boolean collided = false;
        for (int targetSlot = this.slots.size() - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = this.slots.get(targetSlot);
            int targetPhysics = this.physicsTables.readEnemyPhysicsFlags(targetSlot);
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP) != 0
                    || target.spriteVariant() < 0
                    || ((sprinkle.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - sourceVisualY + 0x0C) & 0xFF) >= 0x18) {
                continue;
            }
            var tarinRaccoonMotion = this.motions.get(TarinRaccoonMotion.class, TarinRaccoonMotion::new);
            if (!this.indoorRoom
                    && (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE) == 0
                    && target.type() == EntityRomConstants.ENTITY_TARIN
                    && tarinRaccoonMotion.state(targetSlot) == 0) {
                tarinRaccoonMotion.startPowderTransformation(targetSlot);
                this.physicsTables.writeSlowTransitionCountdown(targetSlot, 0x7F);
                this.physicsTables.writeSlowTimerInitialized(targetSlot, true);
                this.combatTables.writeEnemyFlashCountdown(targetSlot, 0x10);
                collided = true;
                continue;
            }
            if ((targetPhysics & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE) != 0
                    || !RoomEntityCombatRules.supportsEnemyCollision(target.type())
                    || (this.combatTables.readEnemyHitboxFlags(targetSlot) & EntityRomConstants.HITFLAGS_IGNORE_HITS)
                            != 0
                    || this.combatTables.readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }

            collided = true;
            this.applyPlayerProjectileDamage(target, EntityRomConstants.DAMAGE_TYPE_MAGIC_POWDER, 0, 0);
        }
        return collided;
    }

    ProjectileObjectCollision projectileObjectCollision(
            RoomEntity entity, RoomEntityObjectSample object, int frame, int slot) {
        int physics = object.physicsFlag() & 0xFF;
        if (physics == 0xFF) {
            // $7E05 redirects boomerangs to the ordinary collision branch;
            // other projectiles unload immediately.
            return entity.type() == EntityRomConstants.ENTITY_BOOMERANG
                    ? ProjectileObjectCollision.BLOCKED
                    : ProjectileObjectCollision.UNLOAD;
        }
        if (physics >= 0xD0 && physics <= 0xD3) {
            if (this.liftThrow.readThrownDirection(slot) == physics - 0xD0) {
                if (entity.z() != 0) {
                    this.physicsTables.writeEntityUnknownJ(
                            slot, (this.physicsTables.readEntityUnknownJ(slot) + 1) & 0xFF);
                    return ProjectileObjectCollision.NONE;
                }
                return ProjectileObjectCollision.BLOCKED;
            }
            if (this.physicsTables.readEntityUnknownJ(slot) == 0) {
                return ProjectileObjectCollision.BLOCKED;
            }
            if ((frame & 0x03) == 0 || (!this.indoorRoom && (frame & 0x01) == 0)) {
                return ProjectileObjectCollision.NONE;
            }
            this.physicsTables.writeEntityUnknownJ(slot, (this.physicsTables.readEntityUnknownJ(slot) - 1) & 0xFF);
            return ProjectileObjectCollision.NONE;
        }
        return BoomerangMotion.objectPhysicsCollides(physics)
                ? ProjectileObjectCollision.BLOCKED
                : ProjectileObjectCollision.NONE;
    }

    ProjectileEntityCollision collideSwordBeamWithEntities(RoomEntity beam, int frame) {
        int sourceSlot = beam.slot();
        int sourceVisualY = (beam.y() - beam.z()) & 0xFF;
        boolean unload = false;
        boolean blocked = false;
        for (int targetSlot = this.slots.size() - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = this.slots.get(targetSlot);
            int targetPhysics = this.physicsTables.readEnemyPhysicsFlags(targetSlot);
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE) != 0
                    || !RoomEntityCombatRules.supportsEnemyCollision(target.type())
                    || (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP) != 0
                    || target.spriteVariant() < 0
                    || ((beam.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - sourceVisualY + 0x0C) & 0xFF) >= 0x18) {
                continue;
            }

            // $7656 forces a collision before reading the ignore-hits timer.
            if ((this.combatTables.readEnemyHitboxFlags(targetSlot) & EntityRomConstants.HITFLAGS_IGNORE_HITS) != 0) {
                blocked = true;
                continue;
            }
            if (this.combatTables.readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }
            var swordBeamMotion = this.motions.get(SwordBeamMotion.class, SwordBeamMotion::new);
            if (this.ironMaskBlocksProjectile(target, swordBeamMotion.direction(sourceSlot))) {
                blocked = true;
                continue;
            }
            unload |= !blocked;
            this.applyPlayerProjectileDamage(
                    target,
                    EntityRomConstants.DAMAGE_TYPE_SWORD_BEAM,
                    swordBeamMotion.speedX(sourceSlot),
                    swordBeamMotion.speedY(sourceSlot));
        }
        return new ProjectileEntityCollision(unload, blocked);
    }

    boolean collideMagicRodFireballWithEntities(RoomEntity fireball, int frame) {
        var resolvedMagicRodFireballMotion =
                this.motions.get(MagicRodFireballMotion.class, MagicRodFireballMotion::new);

        int sourceSlot = fireball.slot();
        int sourceVisualY = (fireball.y() - fireball.z()) & 0xFF;
        boolean unloadOnHit = false;
        for (int targetSlot = this.slots.size() - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = this.slots.get(targetSlot);
            int targetPhysics = this.physicsTables.readEnemyPhysicsFlags(targetSlot);
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE) != 0
                    || !RoomEntityCombatRules.supportsEnemyCollision(target.type())
                    || (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP) != 0
                    || target.spriteVariant() < 0
                    || ((fireball.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - sourceVisualY + 0x0C) & 0xFF) >= 0x18) {
                continue;
            }

            if ((this.combatTables.readEnemyHitboxFlags(targetSlot) & EntityRomConstants.HITFLAGS_IGNORE_HITS) != 0) {
                resolvedMagicRodFireballMotion.setCollision(sourceSlot, true);
                continue;
            }
            if (this.combatTables.readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }

            var roamingEnemyMotion = this.motions.get(RoamingEnemyMotion.class, RoamingEnemyMotion::new);
            var unmaskedIronMaskMotion = this.motions.get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new);
            if (target.type() == EntityRomConstants.ENTITY_IRON_MASK
                    && unmaskedIronMaskMotion.maskRemovalState(targetSlot) == 0
                    && (roamingEnemyMotion.direction(targetSlot) ^ 0x01)
                            == resolvedMagicRodFireballMotion.direction(sourceSlot)) {
                resolvedMagicRodFireballMotion.setCollision(sourceSlot, true);
                continue;
            }

            unloadOnHit |= !resolvedMagicRodFireballMotion.collided(sourceSlot);
            this.applyPlayerProjectileDamage(
                    target,
                    EntityRomConstants.DAMAGE_TYPE_MAGIC_ROD,
                    resolvedMagicRodFireballMotion.speedX(sourceSlot),
                    resolvedMagicRodFireballMotion.speedY(sourceSlot));
        }
        return unloadOnHit;
    }

    public boolean collidePlayerArrowWithEntities(RoomEntity arrow, int frame) {
        var resolvedPlayerArrowMotion = this.motions.get(PlayerArrowMotion.class, PlayerArrowMotion::new);

        boolean unload = false;
        int arrowSlot = arrow.slot();
        int attackType = this.bombInteractions.readPlayerArrowBombArrow(arrowSlot)
                ? EntityRomConstants.DAMAGE_TYPE_BOMB_ARROW
                : EntityRomConstants.DAMAGE_TYPE_ARROW;
        int arrowVisualY = (arrow.y() - arrow.z()) & 0xFF;
        for (int targetSlot = this.slots.size() - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == arrowSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = this.slots.get(targetSlot);
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || !RoomEntityCombatRules.supportsEnemyCollision(target.type())
                    || (this.physicsTables.readEnemyPhysicsFlags(targetSlot)
                                    & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE)
                            != 0
                    || (this.physicsTables.readEnemyPhysicsFlags(targetSlot)
                                    & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP)
                            != 0
                    || target.spriteVariant() < 0) {
                continue;
            }

            if (((arrow.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - arrowVisualY + 0x0C) & 0xFF) >= 0x18) {
                continue;
            }

            if ((this.combatTables.readEnemyHitboxFlags(targetSlot) & EntityRomConstants.HITFLAGS_IGNORE_HITS) != 0) {

                resolvedPlayerArrowMotion.markCollision(arrowSlot);
                continue;
            }
            if (this.combatTables.readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }

            if (this.ironMaskBlocksProjectile(target, resolvedPlayerArrowMotion.direction(arrowSlot))) {
                resolvedPlayerArrowMotion.markCollision(arrowSlot);
                continue;
            }
            unload |= !resolvedPlayerArrowMotion.collided(arrowSlot);
            if (attackType == EntityRomConstants.DAMAGE_TYPE_BOMB_ARROW) {
                // BombArrowHandler selects DAMAGE_TYPE_BOMB_ARROW. The
                // shared damage helper deliberately treats this active-arrow
                // state as a harmless target transition rather than calling
                // ApplySwordDamagesToEnemy.
                this.physicsTables.writeEnemyTransitionCountdown(targetSlot, 0x03);
            } else {
                this.applyPlayerArrowDamage(arrow, target);
            }
        }
        return unload;
    }

    boolean collideBoomerangWithEntities(RoomEntity boomerang, int frame) {
        var resolvedBoomerangMotion = this.motions.get(BoomerangMotion.class, BoomerangMotion::new);

        int sourceSlot = boomerang.slot();
        int sourceVisualY = (boomerang.y() - boomerang.z()) & 0xFF;
        boolean collided = false;
        for (int targetSlot = this.slots.size() - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = this.slots.get(targetSlot);
            int targetPhysics = this.physicsTables.readEnemyPhysicsFlags(targetSlot);
            boolean grabbable = (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_GRABBABLE) != 0;
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (!grabbable && !RoomEntityCombatRules.supportsEnemyCollision(target.type()))
                    || (targetPhysics & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP) != 0
                    || target.spriteVariant() < 0
                    || ((boomerang.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - sourceVisualY + 0x0C) & 0xFF) >= 0x18) {
                continue;
            }

            if (grabbable) {
                collided = true;

                resolvedBoomerangMotion.setTransitionCountdown(sourceSlot, 0);
                // The source writes sourceSlot + 1 to the target's private
                // state five. The current runtime has no consumer for that
                // field, but the source-side transition is still observable.
                continue;
            }
            if ((this.combatTables.readEnemyHitboxFlags(targetSlot) & EntityRomConstants.HITFLAGS_IGNORE_HITS) != 0) {
                collided = true;

                resolvedBoomerangMotion.setTransitionCountdown(sourceSlot, 0);
                continue;
            }
            if (this.combatTables.readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }
            collided = true;

            resolvedBoomerangMotion.setTransitionCountdown(sourceSlot, 0);
            if (this.ironMaskBlocksProjectile(target, resolvedBoomerangMotion.direction(sourceSlot))) {
                continue;
            }
            this.applyPlayerProjectileDamage(
                    target,
                    EntityRomConstants.DAMAGE_TYPE_BOOMERANG,
                    resolvedBoomerangMotion.speedX(sourceSlot),
                    resolvedBoomerangMotion.speedY(sourceSlot));
        }
        return collided;
    }

    boolean ironMaskBlocksProjectile(RoomEntity target, int direction) {
        var roamingEnemyMotion = this.motions.get(RoamingEnemyMotion.class, RoamingEnemyMotion::new);
        var unmaskedIronMaskMotion = this.motions.get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new);
        return target.type() == EntityRomConstants.ENTITY_IRON_MASK
                && unmaskedIronMaskMotion.maskRemovalState(target.slot()) == 0
                && (roamingEnemyMotion.direction(target.slot()) ^ 1) == direction;
    }

    public boolean collideBouncingBombiteWithEntities(RoomEntity source, int frame) {
        int sourceSlot = source.slot();
        int sourceVisualY = (source.y() - source.z()) & 0xFF;
        var bombiteMotion = this.motions.get(BombiteMotion.class, BombiteMotion::new);
        int sourceSpeedX = bombiteMotion.speedX(sourceSlot);
        int sourceSpeedY = bombiteMotion.speedY(sourceSlot);
        boolean collided = false;
        for (int targetSlot = this.slots.size() - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = this.slots.get(targetSlot);
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (this.physicsTables.readEnemyPhysicsFlags(targetSlot)
                                    & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP)
                            != 0
                    || ((source.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - sourceVisualY + 0x0C) & 0xFF) >= 0x18
                    || target.spriteVariant() == -1) {
                continue;
            }

            collided = true;
            // GetEntityTransitionCountdown uses BC, which remains the active
            // source slot throughout func_003_75A2.
            this.physicsTables.writeEnemyTransitionCountdown(sourceSlot, 0);
            if (target.type() == EntityRomConstants.ENTITY_BOUNCING_BOMBITE) {
                bombiteMotion.enterBouncingLitFromEntityCollision(targetSlot, sourceSpeedX, sourceSpeedY);
                this.physicsTables.writeEnemyTransitionCountdown(targetSlot, 0x40);
                this.bombInteractions.writeBombCollisionGraceCountdown(targetSlot, 0x08);
            }
        }
        return collided;
    }

    public boolean collideHookshotWithEntities(
            RoomEntity hookshot, HookshotChainMotion.State hookshotState, int frame) {
        boolean unmasked = false;
        int sourceSlot = hookshot.slot();
        // $7C39 moves WRAM, but $7C59's shared hit test still reads the
        // hActiveEntity coordinates cached at the beginning of this pass.
        int sourceVisualY = (hookshot.y() - hookshot.z()) & 0xFF;
        for (int targetSlot = this.slots.size() - 1; targetSlot >= 0; targetSlot--) {
            if (targetSlot == sourceSlot || ((frame ^ targetSlot) & 0x01) != 0) {
                continue;
            }

            RoomEntity target = this.slots.get(targetSlot);
            if (!target.loaded()
                    || target.status().value() < EntityStatus.ACTIVE.value()
                    || (this.physicsTables.readEnemyPhysicsFlags(targetSlot)
                                    & EntityRomConstants.ENTITY_PHYSICS_PROJECTILE_NOCLIP)
                            != 0
                    || ((hookshot.x() - target.x() + 0x0C) & 0xFF) >= 0x18
                    || ((target.y() - target.z() - sourceVisualY + 0x0C) & 0xFF) >= 0x18
                    || target.spriteVariant() == -1
                    || this.combatTables.readEnemyIgnoreHitsCountdown(targetSlot) != 0) {
                continue;
            }

            var roamingEnemyMotion = this.motions.get(RoamingEnemyMotion.class, RoamingEnemyMotion::new);
            var unmaskedIronMaskMotion = this.motions.get(UnmaskedIronMaskMotion.class, UnmaskedIronMaskMotion::new);
            if (target.type() != EntityRomConstants.ENTITY_IRON_MASK
                    || ((roamingEnemyMotion.direction(targetSlot) ^ 0x01) != hookshotState.direction())
                    || unmaskedIronMaskMotion.maskRemovalState(targetSlot) != 0) {
                continue;
            }

            unmaskedIronMaskMotion.setMaskRemovalState(targetSlot, 1);
            unmaskedIronMaskMotion.initializeFromMasked(
                    targetSlot,
                    roamingEnemyMotion.direction(targetSlot),
                    roamingEnemyMotion.speedX(targetSlot),
                    roamingEnemyMotion.speedY(targetSlot));
            this.entityPlayerSpawns.spawnIronMasksMask(
                    hookshotState.x(), hookshotState.y(), hookshot.spriteVariant() & 0x01, sourceSlot);
            unmasked = true;
        }
        return unmasked;
    }

    void applyPlayerArrowDamage(RoomEntity arrow, RoomEntity target) {
        var playerArrowMotion = this.motions.get(PlayerArrowMotion.class, PlayerArrowMotion::new);
        applyPlayerProjectileDamage(
                target,
                this.bombInteractions.readPlayerArrowBombArrow(arrow.slot())
                        ? EntityRomConstants.DAMAGE_TYPE_BOMB_ARROW
                        : EntityRomConstants.DAMAGE_TYPE_ARROW,
                playerArrowMotion.speedX(arrow.slot()),
                playerArrowMotion.speedY(arrow.slot()));
    }

    void applyPlayerProjectileDamage(RoomEntity target, int damageType, int speedX, int speedY) {
        this.playerProjectileDamageFrameStep.run(target, damageType, speedX, speedY);
    }

    public boolean playerArrowBombArrow(int slot) {
        return this.bombInteractions.readPlayerArrowBombArrow(slot);
    }

    public void setWingedStateTwoThisFrame(int slot, boolean value) {
        this.slotProperties.writeWingedStateTwoThisFrame(slot, value);
    }

    public void setWingedSwordAttackThisFrame(int slot, boolean value) {
        this.slotProperties.writeWingedSwordAttackThisFrame(slot, value);
    }

    public int swordMoblinAlertingSoundCounter() {
        var moblinSwordMotion = this.motions.get(MoblinSwordMotion.class, MoblinSwordMotion::new);
        return moblinSwordMotion.readSwordMoblinAlertingSoundCounter();
    }

    public void setSwordMoblinAlertingSoundCounter(int value) {
        var moblinSwordMotion = this.motions.get(MoblinSwordMotion.class, MoblinSwordMotion::new);
        moblinSwordMotion.writeSwordMoblinAlertingSoundCounter(value);
    }

    final PlayerProjectileDamageFrameStep playerProjectileDamageFrameStep = new PlayerProjectileDamageFrameStep(this);
    final MagicPowderStep magicPowderStep = new MagicPowderStep(this);
    final MagicRodFrameStep magicRodFrameStep = new MagicRodFrameStep(this);
    final BoomerangStep boomerangStep = new BoomerangStep(this);

    static final class BoomerangStep {
        private final linksawakening.world.entity.EntityProjectileController owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.EntityProjectileController.BoomerangFrame inputs;
        private int index;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frame;
        private int linkEntityX;
        private int linkEntityY;
        private int linkEntityZ;
        private int linkMotionState;
        private boolean handlerInteractive;
        private boolean timersInteractive;
        private int slot;
        private int variant;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity moved;
        private linksawakening.world.entity.RoomEntityObjectQuery intersectionQuery;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntityObjectSample object;
        private boolean objectCollision;
        private boolean bushCollision;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity positioned;
        private boolean stateCollision;

        BoomerangStep(linksawakening.world.entity.EntityProjectileController owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        void run() {
            if (!prepareMotion()) return;
            if (!applyBushCollision()) return;
            if (!advanceOutboundOrReturn()) return;
        }

        private boolean prepareMotion() {
            var resolvedBoomerangMotion = owner.motions.get(BoomerangMotion.class, BoomerangMotion::new);

            index = inputs.index();
            entity = inputs.entity();
            frame = inputs.frame();
            linkEntityX = inputs.linkEntityX();
            linkEntityY = inputs.linkEntityY();
            linkEntityZ = inputs.linkEntityZ();
            linkMotionState = inputs.linkMotionState();
            handlerInteractive = inputs.handlerInteractive();
            timersInteractive = inputs.timersInteractive();
            slot = entity.slot();
            if (timersInteractive) {

                resolvedBoomerangMotion.decrementTransitionCountdown(slot);
            }

            variant = resolvedBoomerangMotion.advanceSpriteVariant(slot, frame, handlerInteractive);
            if (!handlerInteractive) {
                owner.slots.replace(index, withVariant(entity, variant));
                return false;
            }
            owner.playBoomerangSoundIfDue(entity);

            // label_3B7B runs before the position update and uses the launch
            // vector for recoil. This pass also clears the outbound countdown on
            // a successful enemy/object collision, as the ROM does.
            owner.collideBoomerangWithEntities(entity, frame);
            moved = resolvedBoomerangMotion.advancePosition(entity);
            intersectionQuery = owner.physicsTables.readObjectIntersectionQuery();
            object = intersectionQuery.sampleOrAbsent(moved);
            objectCollision = object.sampled()
                    && owner.projectileObjectCollision(moved, object, frame, slot) == ProjectileObjectCollision.BLOCKED;
            bushCollision = object.sampled()
                    && !owner.indoorRoom
                    && (object.objectId() == EntityRomConstants.OBJECT_BUSH
                            || object.objectId() == EntityRomConstants.OBJECT_BUSH_GROUND_STAIRS);
            positioned = objectCollision && resolvedBoomerangMotion.transitionCountdown(slot) != 0 ? entity : moved;
            return true;
        }

        private boolean applyBushCollision() {

            if (bushCollision) {
                int location = (object.objectTop() | (object.objectLeft() >>> 4)) & 0xFF;
                owner.events.boomerangObject(
                        new BoomerangObjectRequest(slot, location, object.objectLeft(), object.objectTop()));
            }

            // BoomerangDestroyBushIfNeeded clears the source collision byte before
            // the state handler, so a bush does not also produce the wall-poke
            // branch in the same frame.
            stateCollision = objectCollision && !bushCollision;
            return true;
        }

        private boolean advanceOutboundOrReturn() {

            var boomerangMotion = owner.motions.get(BoomerangMotion.class, BoomerangMotion::new);
            if (boomerangMotion.state(slot) == 0) {
                if (boomerangMotion.transitionCountdown(slot) == 0) {
                    boomerangMotion.setVectorTowardsLink(slot, positioned, linkEntityX, linkEntityY, 0x08);
                    boomerangMotion.setState(slot, 1);
                } else if (stateCollision) {
                    boomerangMotion.setTransitionCountdown(slot, 0);
                    owner.events.vfx(new TransientVfxRequest(
                            TransientVfxType.SWORD_POKE,
                            positioned.x(),
                            (positioned.y() - positioned.z() + 0x03) & 0xFF));
                    owner.events.sound(
                            slot, EntityRomConstants.ENTITY_BOOMERANG, EntityCombatEvent.SoundChannel.JINGLE, 0x07);
                }
            } else {
                if ((frame & 0x03) == 0) {
                    boomerangMotion.setVectorTowardsLink(slot, positioned, linkEntityX, linkEntityY, 0x20);
                }
                if (((frame ^ slot) & 0x01) != 0
                        && linkEntityZ == 0
                        && linkMotionState < 0x02
                        // label_3B44 reads hActiveEntityPosX/VisualPosY, cached before
                        // movement. The vector above instead reads the updated WRAM.
                        && BoomerangMotion.overlapsLink(entity, linkEntityX, linkEntityY)) {
                    owner.lifecycle.clearEntity(slot);
                    return false;
                }
            }
            owner.slots.replace(index, withPositionAndVariant(positioned, positioned.x(), positioned.y(), variant));
            return true;
        }

        private boolean inUse;

        void run(linksawakening.world.entity.EntityProjectileController.BoomerangFrame inputs) {
            if (inUse) {
                new BoomerangStep(owner).run(inputs);
                return;
            }
            inUse = true;

            index = 0;
            entity = null;
            frame = 0;
            linkEntityX = 0;
            linkEntityY = 0;
            linkEntityZ = 0;
            linkMotionState = 0;
            handlerInteractive = false;
            timersInteractive = false;
            slot = 0;
            variant = 0;
            moved = null;
            intersectionQuery = null;
            object = null;
            objectCollision = false;
            bushCollision = false;
            positioned = null;
            stateCollision = false;

            this.inputs = inputs;
            try {
                run();
            } finally {
                inUse = false;

                this.inputs = null;

                this.entity = null;
                this.moved = null;
                this.intersectionQuery = null;
                this.object = null;
                this.positioned = null;
            }
        }
    }

    static final class MagicPowderStep {
        private final linksawakening.world.entity.EntityProjectileController owner;
        private int index;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frame;
        private int linkMotionState;
        private boolean handlerInteractive;
        private boolean timersInteractive;
        private int slot;
        private int transitionCountdown;
        private linksawakening.world.entity.RoomEntityObjectQuery intersectionQuery;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntityObjectSample object;
        private int objectLeft;
        private int objectTop;
        private int location;
        private int objectId;

        MagicPowderStep(linksawakening.world.entity.EntityProjectileController owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        void run() {
            if (!prepareCountdowns()) return;
            if (!prepareObjectInteraction()) return;
            if (!applyTorchInteraction()) return;
        }

        private boolean prepareCountdowns() {

            slot = entity.slot();
            if (timersInteractive) {
                owner.lifecycle.decrementSlowTransitionCountdown(slot, frame);
            }

            // func_018_7B02 renders before ReturnIfNonInteractive_18, even when
            // the handler is paused. Zero means no sprinkle sprite, not unloading.
            transitionCountdown = owner.physicsTables.readEnemyTransitionCountdown(slot);
            owner.slots.replace(
                    index, withVariant(entity, transitionCountdown == 0 ? -1 : (transitionCountdown >>> 2) & 0x07));
            if (!handlerInteractive) {
                return false;
            }

            if (owner.readMagicPowderState(slot) != 0) {
                // State one has no ordinary sprinkle rectangle: the source emits
                // its short torch pair only at the state transition boundaries.
                if (owner.physicsTables.readSlowTransitionCountdown(slot) != 0) {
                    owner.slots.replace(index, withVariant(entity, -1));
                    return false;
                }

                int objectLeft = entity.x() & 0xF0;
                int objectTop = entity.y() & 0xF0;
                int location = (objectTop | (objectLeft >>> 4)) & 0xFF;
                owner.events.magicPowderObject(new MagicPowderObjectRequest(
                        slot, location, objectLeft, objectTop, MagicPowderObjectAction.EXTINGUISH_TORCH));
                // Keep the palette state visible to later powder handlers in
                // this pass, before RoomSession applies their object requests.
                if (owner.roomVariables.readEntityRoomId() != 0x74 && owner.readTorchPaletteTargetAddress() < 0x0C) {
                    owner.writeTorchPaletteTargetAddress(owner.readTorchPaletteTargetAddress() + 4);
                    if (linkMotionState != 0x05) {
                        owner.writeTorchPaletteTransitionActive(true);
                    }
                }
                owner.lifecycle.clearEntity(slot);
                return false;
            }

            if (transitionCountdown == 0) {
                owner.lifecycle.clearEntity(slot);
                return false;
            }
            return true;
        }

        private boolean prepareObjectInteraction() {

            if (transitionCountdown != 0x07) {
                if (transitionCountdown < 0x10 && owner.readPowderSpent(slot) == 0) {
                    owner.collideMagicPowderWithEntities(entity, frame);
                }
                return false;
            }

            intersectionQuery = owner.physicsTables.readObjectIntersectionQuery();
            object = intersectionQuery.sampleOrAbsent(withPositionAndVariant(
                    entity, (entity.x() - 1) & 0xFF, (entity.y() - 1) & 0xFF, entity.spriteVariant()));
            objectLeft = EntityMotionMath.magicPowderObjectLeft(entity);
            objectTop = EntityMotionMath.magicPowderObjectTop(entity);
            location = (objectTop | (objectLeft >>> 4)) & 0xFF;
            objectId = !object.sampled() ? 0xFF : object.objectId() & 0xFF;

            if (!owner.indoorRoom
                    && (objectId == EntityRomConstants.OBJECT_BUSH
                            || objectId == EntityRomConstants.OBJECT_BUSH_GROUND_STAIRS)) {
                owner.events.magicPowderObject(new MagicPowderObjectRequest(
                        slot, location, objectLeft, objectTop, MagicPowderObjectAction.REVEAL));
                owner.events.vfx(
                        new TransientVfxRequest(TransientVfxType.POOF, entity.x(), (entity.y() - entity.z()) & 0xFF));
                owner.events.sound(
                        slot,
                        EntityRomConstants.ENTITY_MAGIC_POWDER_SPRINKLE,
                        EntityCombatEvent.SoundChannel.JINGLE,
                        EntityRomConstants.MAGIC_POWDER_POOF_JINGLE_ID);
                owner.lifecycle.clearEntity(slot);
                return false;
            }
            return true;
        }

        private boolean applyTorchInteraction() {

            if (owner.indoorRoom && objectId == EntityRomConstants.OBJECT_TORCH_UNLIT) {
                // This motion-state check belongs only to GBC torch ignition.
                if (linkMotionState == 0x05 || owner.readTorchPaletteTransitionActive()) {
                    return false;
                }
                if (owner.readTorchPaletteTargetAddress() != 0) {
                    owner.writeTorchPaletteTargetAddress((owner.readTorchPaletteTargetAddress() - 4) & 0xFF);
                    owner.writeTorchPaletteTransitionActive(true);
                }
                owner.writeMagicPowderState(slot, 1);
                owner.physicsTables.writeSlowTransitionCountdown(
                        slot, EntityRomConstants.MAGIC_POWDER_SLOW_TRANSITION_COUNTDOWN);
                owner.physicsTables.writeSlowTimerInitialized(slot, true);
                owner.physicsTables.writeEnemyTransitionCountdown(slot, 0);
                RoomEntity torch = withPositionAndVariant(entity, objectLeft, objectTop, 0);
                EntitySpriteDefinition torchDefinition = !owner.spriteHandlers.supportsPresentation()
                        ? entity.spriteDefinition()
                        : owner.slotProperties.spriteViews().forMagicPowderTorchState(true);
                owner.slots.replace(index, withDefinition(torch, torchDefinition, 0));
                owner.events.magicPowderObject(new MagicPowderObjectRequest(
                        slot, location, objectLeft, objectTop, MagicPowderObjectAction.IGNITE_TORCH));
                owner.events.sound(
                        slot,
                        EntityRomConstants.ENTITY_MAGIC_POWDER_SPRINKLE,
                        EntityCombatEvent.SoundChannel.NOISE,
                        0x12);
                return false;
            }

            // label_018_7A4B reaches the common collision helper only during the
            // final sixteen transition ticks and only when private state 4 is 0.
            // $79DA returns for non-torch indoor objects. Outdoors, $79CF leaves
            // A=0 and jumps to the collision branch regardless of the object id.
            if (!owner.indoorRoom && owner.readPowderSpent(slot) == 0) {
                owner.collideMagicPowderWithEntities(entity, frame);
            }
            return true;
        }

        private boolean inUse;

        void run(EntitySlotExecution slotState, EntityFrameInputLatch inputs, EntityTimerGates timers) {
            int index = slotState.index();
            RoomEntity entity = slotState.entity();
            int frame = inputs.frame();
            int linkMotionState = inputs.projectileLinkState().motionState();
            boolean handlerInteractive = timers.handlerInteractive() && !inputs.creditsGameplay();
            boolean timersInteractive = timers.projectileTimersInteractive();
            if (inUse) {
                new MagicPowderStep(owner).run(slotState, inputs, timers);
                return;
            }
            inUse = true;

            slot = 0;
            transitionCountdown = 0;
            intersectionQuery = null;
            object = null;
            objectLeft = 0;
            objectTop = 0;
            location = 0;
            objectId = 0;

            this.index = index;
            this.entity = entity;
            this.frame = frame;
            this.linkMotionState = linkMotionState;
            this.handlerInteractive = handlerInteractive;
            this.timersInteractive = timersInteractive;
            try {
                run();
            } finally {
                inUse = false;

                this.entity = null;

                this.intersectionQuery = null;
                this.object = null;
            }
        }
    }

    static final class MagicRodFrameStep {
        private final linksawakening.world.entity.EntityProjectileController owner;
        private int index;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity entity;
        private int frame;
        private boolean handlerInteractive;
        private boolean timersInteractive;
        private int slot;
        private boolean unloadOnHit;
        private int variant;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity moved;
        private linksawakening.world.entity.RoomEntityObjectQuery intersectionQuery;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntityObjectSample object;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.EntityProjectileController.ProjectileObjectCollision objectCollision;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity positioned;

        MagicRodFrameStep(linksawakening.world.entity.EntityProjectileController owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        void run() {
            if (!advanceProjectile()) return;
            if (!resolveObjectCollision()) return;
        }

        private boolean advanceProjectile() {

            slot = entity.slot();
            // The source calls func_003_75A2 before either the wall-fire branch or
            // ReturnIfNonInteractive. A fireball can therefore still deliver the
            // common target collision while Link is being transitioned.
            unloadOnHit = owner.collideMagicRodFireballWithEntities(entity, frame);
            if (unloadOnHit) {
                // $7782-$7795 unloads a normal projectile hit. Blocking targets
                // and existing wall flames retain a nonzero collision byte.
                owner.lifecycle.disableEntityWithoutPersistence(slot);
                return false;
            }
            var magicRodFireballMotion = owner.motions.get(MagicRodFireballMotion.class, MagicRodFireballMotion::new);
            variant = magicRodFireballMotion.advanceSpriteVariant(slot, frame);
            if (magicRodFireballMotion.fireTransitionCountdown(slot) != 0) {
                if (magicRodFireballMotion.tickFireTransition(slot, timersInteractive)) {
                    owner.lifecycle.clearEntity(slot);
                    return false;
                }
                EntitySpriteDefinition fireDefinition = !owner.spriteHandlers.supportsPresentation()
                        ? entity.spriteDefinition()
                        : owner.slotProperties.spriteViews().forMagicRodFireState();
                owner.slots.replace(index, withDefinition(entity, fireDefinition, variant));
                return false;
            }

            // ReturnIfNonInteractive_03 is after the normal sprite write and
            // before movement/object lookup in the ROM helper.
            if (!handlerInteractive) {
                owner.slots.replace(index, withVariant(entity, variant));
                return false;
            }

            moved = magicRodFireballMotion.advancePosition(entity);
            intersectionQuery = owner.physicsTables.readObjectIntersectionQuery();
            object = intersectionQuery.sampleOrAbsent(moved);
            return true;
        }

        private boolean resolveObjectCollision() {
            var resolvedMagicRodFireballMotion =
                    owner.motions.get(MagicRodFireballMotion.class, MagicRodFireballMotion::new);

            objectCollision = !object.sampled()
                    ? ProjectileObjectCollision.NONE
                    : owner.projectileObjectCollision(moved, object, frame, slot);
            if (objectCollision == ProjectileObjectCollision.UNLOAD) {
                owner.lifecycle.disableEntityWithoutPersistence(slot);
                return false;
            }
            // $7DF3-$7E00 restores BOTH coordinates from the active pre-movement
            // position before the fireball handler can clear a burnable collision.
            positioned = objectCollision == ProjectileObjectCollision.BLOCKED ? entity : moved;
            if (owner.isMagicRodBurnableObject(object)) {
                resolvedMagicRodFireballMotion.setCollision(slot, false);
                int location = (object.objectTop() | (object.objectLeft() >>> 4)) & 0xFF;
                owner.events.magicRodObject(
                        new MagicRodObjectRequest(slot, location, object.objectLeft(), object.objectTop()));
                owner.slots.replace(index, withVariant(positioned, variant));
                return false;
            }

            if (resolvedMagicRodFireballMotion.collided(slot) || objectCollision == ProjectileObjectCollision.BLOCKED) {
                // ArrowRenderAndMove writes privateCountdown1 after the normal
                // fireball display has already been rendered. Keep the normal
                // definition for this frame; the next handler tick switches to
                // FireSpriteVariants and decrements the $30 countdown.
                resolvedMagicRodFireballMotion.beginFireTransition(slot);
            }
            owner.slots.replace(index, withVariant(positioned, variant));
            return true;
        }

        private boolean inUse;

        void run(EntitySlotExecution slotState, EntityFrameInputLatch inputs, EntityTimerGates timers) {
            int index = slotState.index();
            RoomEntity entity = slotState.entity();
            int frame = inputs.frame();
            boolean handlerInteractive = timers.handlerInteractive();
            boolean timersInteractive = timers.projectileTimersInteractive();
            if (inUse) {
                new MagicRodFrameStep(owner).run(slotState, inputs, timers);
                return;
            }
            inUse = true;

            slot = 0;
            unloadOnHit = false;
            variant = 0;
            moved = null;
            intersectionQuery = null;
            object = null;
            objectCollision = null;
            positioned = null;

            this.index = index;
            this.entity = entity;
            this.frame = frame;
            this.handlerInteractive = handlerInteractive;
            this.timersInteractive = timersInteractive;
            try {
                run();
            } finally {
                inUse = false;

                this.entity = null;

                this.moved = null;
                this.intersectionQuery = null;
                this.object = null;
                this.objectCollision = null;
                this.positioned = null;
            }
        }
    }

    static final class PlayerProjectileDamageFrameStep {
        private final linksawakening.world.entity.EntityProjectileController owner;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RoomEntity target;
        private int damageType;
        private int speedX;
        private int speedY;
        private int targetSlot;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.RomEnemyCombatTables.SwordDamageResult damageResult;
        private int rawDamage;
        private int enemyDamage;
        private int specialAction;
        // Absent before initialization or while this reusable execution scratch is inactive.
        private linksawakening.world.entity.event.EntityCombatEvent.SoundChannel secondaryChannel;
        private int secondarySoundId;

        PlayerProjectileDamageFrameStep(linksawakening.world.entity.EntityProjectileController owner) {
            this.owner = Objects.requireNonNull(owner, "owner");
        }

        void run() {
            if (!prepareDamage()) return;
            if (!applyDamageOrStatus()) return;
            if (!emitDamageEvent()) return;
        }

        private boolean prepareDamage() {

            targetSlot = target.slot();
            var enemyRecoilMotion = owner.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);
            enemyRecoilMotion.configureFromSpeed(targetSlot, speedX, speedY);

            damageResult = owner.enemyCombatTables.resolveAttackDamage(
                    target.type(), owner.combatTables.readEnemyHealthGroup(target.slot()), damageType);
            rawDamage = damageResult.rawValue();
            if (rawDamage == 0) {
                return false;
            }

            enemyDamage = rawDamage < 0xF0 ? rawDamage : 0;
            specialAction = rawDamage >= 0xF0 ? rawDamage : -1;
            secondaryChannel = EntityCombatEvent.SoundChannel.NONE;
            secondarySoundId = -1;
            return true;
        }

        private boolean applyDamageOrStatus() {
            var resolvedEnemyRecoilMotion = owner.motions.get(EnemyRecoilMotion.class, EnemyRecoilMotion::new);

            if (rawDamage == 0xFE) {
                owner.physicsTables.writeEnemyTransitionCountdown(targetSlot, 0x60);
                owner.combatTables.writeEnemyStunnedCountdown(targetSlot, 0);
                owner.combatTables.writeEnemyFlashCountdown(targetSlot, 0);
                owner.combatTables.writeEnemyIgnoreHitsCountdown(targetSlot, 0x0A);
                owner.physicsTables.writeEnemyPhysicsFlags(
                        targetSlot, (owner.physicsTables.readEnemyPhysicsFlags(targetSlot) + 2) & 0xFF);

                resolvedEnemyRecoilMotion.clear(targetSlot);
                owner.slots.replace(targetSlot, withStatus(target, EntityStatus.BURNING));
                secondaryChannel = EntityCombatEvent.SoundChannel.NOISE;
                secondarySoundId = 0x12;
            } else if (rawDamage == 0xFF) {
                owner.physicsTables.writeEnemyTransitionCountdown(targetSlot, 0);
                owner.combatTables.writeEnemyStunnedCountdown(targetSlot, 0xFF);
                owner.combatTables.writeEnemyFlashCountdown(targetSlot, 0);
                owner.combatTables.writeEnemyIgnoreHitsCountdown(targetSlot, 0x0A);

                resolvedEnemyRecoilMotion.clear(targetSlot);
                owner.slots.replace(targetSlot, withStatus(target, EntityStatus.STUNNED));
            } else if (rawDamage == 0xFD) {
                // The fairy-conversion branch is entity-specific in the ROM. The
                // generic table result is still surfaced so the pending special
                // action is not silently changed into numeric damage.

                resolvedEnemyRecoilMotion.clear(targetSlot);
            } else {
                owner.combatTables.writeEnemyHealth(
                        targetSlot, Math.max(0, owner.combatTables.readEnemyHealth(targetSlot) - enemyDamage));
                if (owner.combatTables.readEnemyHealth(targetSlot) == 0) {
                    owner.combatTables.writeDyingCountdown(targetSlot, 0x2F);
                    owner.combatTables.writePowerRecoilDeath(targetSlot, false);
                    owner.slots.replace(
                            targetSlot, withDeathPresentation(withStatus(target, EntityStatus.DYING), -1, false));
                } else if (isZolGelType(target.type())) {
                    var zolGelMotion = owner.motions.get(ZolGelMotion.class, ZolGelMotion::new);
                    zolGelMotion.onSwordHit(targetSlot);
                    if (target.type() == EntityRomConstants.ENTITY_ZOL) {
                        owner.slots.replace(
                                targetSlot,
                                owner.spriteHandlers.present(
                                        target,
                                        owner.slotProperties.spriteViews().forZolSlimeEye(),
                                        target.spriteVariant()));
                    }
                }
                owner.combatTables.writeEnemyFlashCountdown(targetSlot, 0x18);
                owner.combatTables.writeEnemyIgnoreHitsCountdown(targetSlot, 0x0A);
            }
            return true;
        }

        private boolean emitDamageEvent() {

            owner.events.combat(linksawakening.world.entity.event.EntityCombatEvent.builder(targetSlot, target.type())
                    .linkDamage(0)
                    .swordHit(false)
                    .enemyDamage(enemyDamage)
                    .enemySpecialAction(specialAction)
                    .soundChannel(EntityCombatEvent.SoundChannel.JINGLE)
                    .soundId(0x03)
                    .secondarySoundChannel(secondaryChannel)
                    .secondarySoundId(secondarySoundId)
                    .swordPokeVfx(null)
                    .build());
            return true;
        }

        private boolean inUse;

        void run(linksawakening.world.entity.RoomEntity target, int damageType, int speedX, int speedY) {
            if (inUse) {
                new PlayerProjectileDamageFrameStep(owner).run(target, damageType, speedX, speedY);
                return;
            }
            inUse = true;

            targetSlot = 0;
            damageResult = null;
            rawDamage = 0;
            enemyDamage = 0;
            specialAction = 0;
            secondaryChannel = null;
            secondarySoundId = 0;

            this.target = target;
            this.damageType = damageType;
            this.speedX = speedX;
            this.speedY = speedY;
            try {
                run();
            } finally {
                inUse = false;

                this.target = null;

                this.damageResult = null;
                this.secondaryChannel = null;
            }
        }
    }

    public record BoomerangFrame(
            int index,
            RoomEntity entity,
            int frame,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            boolean handlerInteractive,
            boolean timersInteractive) {}

    int readBoomerangSfxCounter() {
        return state.readBoomerangSfxCounter();
    }

    boolean readBoomerangSoundClockProvided() {
        return state.readBoomerangSoundClockProvided();
    }

    int readMagicPowderState(int slot) {
        return state.readMagicPowderState(slot);
    }

    int readMusicalNoteInertia(int slot) {
        return state.readMusicalNoteInertia(slot);
    }

    int readPowderSpent(int slot) {
        return state.readPowderSpent(slot);
    }

    int readTorchPaletteTargetAddress() {
        return state.readTorchPaletteTargetAddress();
    }

    boolean readTorchPaletteTransitionActive() {
        return state.readTorchPaletteTransitionActive();
    }

    int writeBoomerangSfxCounter(int value) {
        return state.writeBoomerangSfxCounter(value);
    }

    boolean writeBoomerangSoundClockProvided(boolean value) {
        return state.writeBoomerangSoundClockProvided(value);
    }

    int writeMagicPowderState(int slot, int value) {
        return state.writeMagicPowderState(slot, value);
    }

    int writeTorchPaletteTargetAddress(int value) {
        return state.writeTorchPaletteTargetAddress(value);
    }

    boolean writeTorchPaletteTransitionActive(boolean value) {
        return state.writeTorchPaletteTransitionActive(value);
    }
}
