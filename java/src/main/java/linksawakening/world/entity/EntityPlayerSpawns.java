package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;

/** ROM slot allocation and player-created entity spawning. */
public final class EntityPlayerSpawns {
    final boolean indoorRoom;
    // Optional ROM-backed input or caller override; absent values retain the established fallback.
    final java.util.function.IntSupplier defaultRandomByteSupplier;
    // Optional ROM-backed input or caller override; absent values retain the established fallback.
    final linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers;
    final EntityBombInteractions bombInteractions;
    final EntityBossRewards bossRewards;
    final EntityCombatTables combatTables;
    final EntityEventChannels events;
    final EntityLiftThrowController liftThrow;
    final EntityMotionRegistry motions;
    final EntityPhysicsTables physicsTables;
    final EntityPickupController pickups;
    final EntityPresentationState presentationState;
    final EntityProjectileState projectiles;
    final EntityRoomInspection inspection;
    final EntityRoomInspection roomInspection;
    final EntityRoomVariables roomVariables;
    final EntitySlotLifecycle lifecycle;
    final EntitySlotProperties slotProperties;
    final RoomEntity[] slots;
    // Optional ROM-backed input or caller override; absent values retain the established fallback.
    final linksawakening.world.room.ChestContentsTable chestContentsTable;
    final EntityPlayerCommands playerCommands;

    EntityPlayerSpawns(
            boolean indoorRoom,
            java.util.function.IntSupplier defaultRandomByteSupplier,
            linksawakening.entity.EntitySpriteHandlerCatalog spriteHandlers,
            EntityBombInteractions bombInteractions,
            EntityBossRewards bossRewards,
            EntityCombatTables combatTables,
            EntityEventChannels events,
            EntityLiftThrowController liftThrow,
            EntityMotionRegistry motions,
            EntityPhysicsTables physicsTables,
            EntityPickupController pickups,
            EntityPresentationState presentationState,
            EntityProjectileState projectiles,
            EntityRoomInspection inspection,
            EntityRoomInspection roomInspection,
            EntityRoomVariables roomVariables,
            EntitySlotLifecycle lifecycle,
            EntitySlotProperties slotProperties,
            RoomEntity[] slots,
            linksawakening.world.room.ChestContentsTable chestContentsTable,
            EntityPlayerCommands playerCommands) {
        this.indoorRoom = indoorRoom;
        this.defaultRandomByteSupplier = defaultRandomByteSupplier;
        this.spriteHandlers = spriteHandlers;
        this.bombInteractions = java.util.Objects.requireNonNull(bombInteractions, "bombInteractions");
        this.bossRewards = java.util.Objects.requireNonNull(bossRewards, "bossRewards");
        this.combatTables = java.util.Objects.requireNonNull(combatTables, "combatTables");
        this.events = java.util.Objects.requireNonNull(events, "events");
        this.liftThrow = java.util.Objects.requireNonNull(liftThrow, "liftThrow");
        this.motions = java.util.Objects.requireNonNull(motions, "motions");
        this.physicsTables = java.util.Objects.requireNonNull(physicsTables, "physicsTables");
        this.pickups = java.util.Objects.requireNonNull(pickups, "pickups");
        this.presentationState = java.util.Objects.requireNonNull(presentationState, "presentationState");
        this.projectiles = java.util.Objects.requireNonNull(projectiles, "projectiles");
        this.inspection = java.util.Objects.requireNonNull(inspection, "inspection");
        this.roomInspection = java.util.Objects.requireNonNull(roomInspection, "roomInspection");
        this.roomVariables = java.util.Objects.requireNonNull(roomVariables, "roomVariables");
        this.lifecycle = java.util.Objects.requireNonNull(lifecycle, "lifecycle");
        this.slotProperties = java.util.Objects.requireNonNull(slotProperties, "slotProperties");
        this.slots = java.util.Objects.requireNonNull(slots, "slots");
        this.chestContentsTable = chestContentsTable;
        this.playerCommands = java.util.Objects.requireNonNull(playerCommands, "playerCommands");
    }

    void spawnIronMasksMask(int x, int y, int variant, int sourceHookshotSlot) {
        HookshotSpawns.spawnIronMasksMask(this, x, y, variant, sourceHookshotSlot);
    }

    int spawnHookshotChain(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return HookshotSpawns.spawnHookshotChain(this, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    int spawnPushedBlock(int x, int y) {
        return PlayerObjectSpawns.spawnPushedBlock(this, x, y);
    }

    int spawnLiftedRoomObject(int x, int y, int romDirection, int spriteVariant) {
        return PlayerObjectSpawns.spawnLiftedRoomObject(this, x, y, romDirection, spriteVariant);
    }

    int spawnRoomEventKeyDrop() {
        return PlayerRewardSpawns.spawnRoomEventKeyDrop(this);
    }

    int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return PlayerProjectileSpawns.spawnArrow(this, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return PlayerProjectileSpawns.spawnArrow(
                this, linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }

    int spawnBoomerang(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) {
        return PlayerProjectileSpawns.spawnBoomerang(
                this, linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask);
    }

    int spawnSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return PlayerProjectileSpawns.spawnSwordBeam(this, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    int spawnMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return PlayerProjectileSpawns.spawnMagicRodFireball(this, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    int spawnMagicRodFireball(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return PlayerProjectileSpawns.spawnMagicRodFireball(
                this, linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }

    int spawnMagicPowderSprinkle(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return PlayerProjectileSpawns.spawnMagicPowderSprinkle(
                this, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    void spawnMusicalNote(int linkEntityX, int linkEntityY, int animationPhase) {
        PlayerRewardSpawns.spawnMusicalNote(this, linkEntityX, linkEntityY, animationPhase);
    }

    int spawnBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return PlayerBombSpawns.spawnBomb(this, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    public int spawnLiftableRockSmash(int entityX, int entityY, int sourceSpriteVariant) {
        return PlayerObjectSpawns.spawnLiftableRockSmash(this, entityX, entityY, sourceSpriteVariant);
    }

    public int spawnLiftableRockRubble(int entityX, int entityY) {
        return PlayerObjectSpawns.spawnLiftableRockRubble(this, entityX, entityY);
    }

    int spawnLiftableRockAnimation(
            int entityX, int entityY, int sourceSpriteVariant, int initialCountdown, int physicsFlags, int soundId) {
        return PlayerObjectSpawns.spawnLiftableRockAnimation(
                this, entityX, entityY, sourceSpriteVariant, initialCountdown, physicsFlags, soundId);
    }

    public int spawnBombFromBombArrow(RoomEntity arrow) {
        return PlayerBombSpawns.spawnBombFromBombArrow(this, arrow);
    }

    public boolean spawnHookshotBridge(int bridgeX, int bridgeY, int direction, int transitionCountdown) {
        return HookshotSpawns.spawnHookshotBridge(this, bridgeX, bridgeY, direction, transitionCountdown);
    }

    int spawnShovelDrop(int objectLeft, int objectTop, int linkEntityX, int linkEntityY, boolean allowDrop) {
        return PlayerRewardSpawns.spawnShovelDrop(this, objectLeft, objectTop, linkEntityX, linkEntityY, allowDrop);
    }

    int spawnChestWithItem(int objectLeft, int objectTop, int itemType) {
        return PlayerRewardSpawns.spawnChestWithItem(this, objectLeft, objectTop, itemType);
    }
}
