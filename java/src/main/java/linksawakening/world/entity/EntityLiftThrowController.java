package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.SideViewPotLinkRequest;
import linksawakening.world.entity.motion.*;
import linksawakening.world.entity.motion.BombMotion;
import linksawakening.world.entity.motion.ThrownEntityMotion;

/** ROM lift, carry and throw mechanics, sharing the owning room slot table. */
public final class EntityLiftThrowController {
    final EntityLiftState state;
    final boolean indoorRoom;
    // Optional ROM data, absent in ROM-free runtimes.
    final linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers;
    final EntityBombState bombInteractions;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLinkState linkState;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityPickupState pickups;
    final EntityPresentationState presentationState;
    final EntityProjectileController projectiles;
    final EntitySlotProperties slotProperties;
    final GenieEntityController genie;
    final RoomEntity[] slots;
    final EntitySlotLifecycle lifecycle;
    final EntityPlayerCommands playerCommands;
    final EntityPlayerSpawns playerSpawns;

    EntityLiftThrowController(
            EntityLiftState state,
            boolean indoorRoom,
            linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers,
            EntityBombState bombInteractions,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityLinkState linkState,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityPickupState pickups,
            EntityPresentationState presentationState,
            EntityProjectileController projectiles,
            EntitySlotProperties slotProperties,
            GenieEntityController genie,
            RoomEntity[] slots,
            EntitySlotLifecycle lifecycle,
            EntityPlayerCommands playerCommands,
            EntityPlayerSpawns playerSpawns) {
        this.state = java.util.Objects.requireNonNull(state, "state");
        this.indoorRoom = indoorRoom;
        this.spriteHandlers = spriteHandlers;
        this.bombInteractions = java.util.Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.linkState = java.util.Objects.requireNonNull(linkState, "linkState");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = java.util.Objects.requireNonNull(pickups, "pickups");
        this.presentationState = java.util.Objects.requireNonNull(presentationState, "presentationState");
        this.projectiles = java.util.Objects.requireNonNull(projectiles, "projectiles");
        this.slotProperties = java.util.Objects.requireNonNull(slotProperties, "slotProperties");
        this.genie = java.util.Objects.requireNonNull(genie, "genie");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
        this.playerCommands = java.util.Objects.requireNonNull(playerCommands, "playerCommands");
        this.playerSpawns = java.util.Objects.requireNonNull(playerSpawns, "playerSpawns");
    }

    public RoomEntity advanceLiftedEntity(
            RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, int romLinkDirection) {
        return EntityCarryBehavior.advanceLiftedEntity(this, entity, linkEntityX, linkEntityY, linkZ, romLinkDirection);
    }

    boolean advanceStatueRubble(RoomEntity entity, boolean interactive) {
        if (entity.type() != 0x9D || entity.spriteDefinition().shape() != EntitySpriteDefinition.Shape.SINGLE)
            return false;
        if (!interactive) return true;
        var update = this.motions
                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                .advance(entity, false, null);
        this.slots[entity.slot()] = update.entity().z() == 0 ? RoomEntity.disabled(entity.slot()) : update.entity();
        return true;
    }

    void smashStatue(RoomEntity entity) {
        LiftableObjectBehavior.smashStatue(this, entity);
    }

    RoomEntity advanceThrownEntity(
            RoomEntity entity,
            RoomEntityBackgroundCollision backgroundCollision,
            boolean sideScrolling,
            int romLinkDirection,
            int frame) {
        return ThrownEntityCollisions.advanceThrownEntity(
                this, entity, backgroundCollision, sideScrolling, romLinkDirection, frame);
    }

    public boolean sideViewPotBackgroundCollision(
            RoomEntity original, RoomEntity moved, RoomEntityBackgroundCollision fallback, int frame) {
        return ThrownEntityCollisions.sideViewPotBackgroundCollision(this, original, moved, fallback, frame);
    }

    void collideThrownEntityWithEntities(RoomEntity thrown, int frame) {
        ThrownEntityCollisions.collideThrownEntityWithEntities(this, thrown, frame);
    }

    public RoomEntity advancePlacedBombEntity(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision, int frame) {
        return LiftableObjectBehavior.advancePlacedBombEntity(this, entity, backgroundCollision, frame);
    }

    public RoomEntity advanceEnemyBombEntity(
            RoomEntity entity, RoomEntityBackgroundCollision backgroundCollision, int frame) {
        int slot = entity.slot();
        if (!this.bombInteractions.readEnemyBombMotionInitialized(slot)) {
            // Room-loaded type-$02 entities and legacy test-created enemy
            // bombs have no source speed tables to advance.
            return entity;
        }
        return this.motions
                .get(ThrownEntityMotion.class, ThrownEntityMotion::new)
                .advance(
                        entity,
                        this.physicsTables.readGroundInteractionSideScrolling(),
                        backgroundCollision,
                        frame,
                        this.physicsTables.readEntityGroundStatus(slot))
                .entity();
    }

    public boolean cuccoPowerBraceletHeld() {
        if (this.linkState.linkItemB == 0x03) {
            return this.linkState.actionButtonBHeld;
        }
        return this.linkState.linkItemA == 0x03 && this.linkState.actionButtonAHeld;
    }

    public boolean beginBushCrawlerLift(int index, RoomEntity source, int privateState4, int romLinkDirection) {
        return EntityCarryBehavior.beginBushCrawlerLift(this, index, source, privateState4, romLinkDirection);
    }

    public void advanceLiftableRockSmash(int index, RoomEntity entity, int frameCounter) {
        LiftableObjectBehavior.advanceLiftableRockSmash(this, index, entity, frameCounter);
    }

    public RoomEntity renderLiftedEntity(RoomEntity entity, int frameCounter) {
        if (entity.type() != EntityRomConstants.ENTITY_BOMB) {
            return entity.type() == EntityRomConstants.ENTITY_CUCCO
                    ? withVariant(entity, (frameCounter >>> 2) & 0x01)
                    : entity;
        }
        this.combatTables.writeEnemyFlashCountdown(entity.slot(), 0);
        return withDefinition(entity, this.slotProperties.spriteDefinitionFor(EntityRomConstants.ENTITY_BOMB), 0);
    }

    RoomEntity renderLiftedBomb(RoomEntity entity) {
        return renderLiftedEntity(entity, 0);
    }

    public boolean tryLiftBombIfRequested(
            int index,
            BombMotion.Decision decision,
            int linkEntityX,
            int linkEntityY,
            int linkZ,
            int romLinkDirection) {
        return EntityCarryBehavior.tryLiftBombIfRequested(
                this, index, decision, linkEntityX, linkEntityY, linkZ, romLinkDirection);
    }

    public boolean sideViewPotLiftDelay(int slot) {
        return state.readLiftedPhase(slot) == 0 && this.physicsTables.readEnemyTransitionCountdown(slot) != 0;
    }

    public boolean sideViewPotPowerBraceletHeld() {
        if (!this.linkState.linkInventoryConfigured) {
            return this.linkState.powerBraceletButtonHeld;
        }
        // EntityGetLiftedUp checks the B slot first. An equipped bracelet in
        // B suppresses the A-slot branch even when A also contains one.
        if (this.linkState.linkItemB == 0x03) {
            return this.linkState.actionButtonBHeld;
        }
        return this.linkState.linkItemA == 0x03 && this.linkState.actionButtonAHeld;
    }

    public void pendingLinkPotContact(int sourceSlot, SideViewPotContact.Result contact) {
        this.events.pendingSideViewPotLinkRequests.add(new SideViewPotLinkRequest(
                sourceSlot,
                contact.resetPegasusBoots(),
                contact.restoreFinalPositionX(),
                contact.ignoreCollisionCountdown(),
                contact.speedX(),
                contact.snapTop(),
                contact.positionY(),
                contact.speedY()));
    }

    public boolean beginLift(int slot, int direction) {
        return this.playerCommands.beginLift(slot, direction);
    }

    public void setLiftedSourceDirection(int slot, int value) {
        state.writeLiftedSourceDirection(slot, value);
    }

    public boolean liftableRockSmashActive(int slot) {
        return state.readLiftableRockSmashActive(slot);
    }
}
