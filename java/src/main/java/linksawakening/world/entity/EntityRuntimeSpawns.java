package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySlotValidation.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;

/** Source-compatible room facade capability backed by focused entity services. */
interface EntityRuntimeSpawns {
    EntityBackgroundMechanics entityBackground();

    EntityEnemySpawns entityEnemySpawns();

    EntityPlayerSpawns entityPlayerSpawns();

    EntityPlayerCommands entityPlayerCommands();

    /** Creates the ROM's player-projectile entity type {@code $03}. */
    default int spawnHookshotChain(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return entityPlayerSpawns().spawnHookshotChain(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    /** Spawns bank 3's temporary entity $06 at an indoor object-cell center. */
    default int spawnPushedBlock(int x, int y) {
        return entityPlayerSpawns().spawnPushedBlock(x, y);
    }

    /** Spawns bank 0's temporary type-$05 entity already entering lifted status. */
    default int spawnLiftedRoomObject(int x, int y, int romDirection, int spriteVariant) {
        return entityPlayerSpawns().spawnLiftedRoomObject(x, y, romDirection, spriteVariant);
    }

    default void movePushedBlock(int slot, int x, int y) {
        entityPlayerCommands().movePushedBlock(slot, x, y);
    }

    /** $03:5269: after block movement, restore Link even during a jump. */
    default void resolvePushedBlockLinkContact(int slot, int linkEntityX, int linkEntityY) {
        entityBackground().resolvePushedBlockLinkContact(slot, linkEntityX, linkEntityY);
    }

    default void removePushedBlock(int slot) {
        entityPlayerCommands().removePushedBlock(slot);
    }

    /** Spawns DropKeyEffectHandler's ordinary dungeon key at ($28,$3C,$70). */
    default int spawnRoomEventKeyDrop() {
        return entityPlayerSpawns().spawnRoomEventKeyDrop();
    }

    /** Creates the ROM's ordinary player-arrow entity type {@code $00}. */
    default int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return entityPlayerSpawns().spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    default int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return entityPlayerSpawns().spawnArrow(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }

    /** Creates the ROM's player-boomerang entity type {@code $01}. */
    default int spawnBoomerang(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) {
        return entityPlayerSpawns()
                .spawnBoomerang(linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask);
    }

    /** Creates the ROM's level-two sword-beam entity type {@code $DF}. */
    default int spawnSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return entityPlayerSpawns().spawnSwordBeam(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    /** Creates the ROM's Magic Rod fireball entity type {@code $04}. */
    default int spawnMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return entityPlayerSpawns().spawnMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    default int spawnMagicRodFireball(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return entityPlayerSpawns()
                .spawnMagicRodFireball(linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }

    /** Creates the ROM's type-$08 Magic Powder sprinkle entity. */
    default int spawnMagicPowderSprinkle(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return entityPlayerSpawns().spawnMagicPowderSprinkle(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    /** Creates the ROM's ordinary player bomb entity type {@code $02}. */
    default int spawnBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return entityPlayerSpawns().spawnBomb(linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    /** Creates the source's type-$02 enemy bomb with privateState4 set to $01. */
    default int spawnEnemyBomb(int entityX, int entityY, int entityZ, int transitionCountdown) {
        return entityEnemySpawns().spawnEnemyBomb(entityX, entityY, entityZ, transitionCountdown);
    }

    /** Creates a Bomber-spawned type-$02 enemy bomb with its source speeds. */
    default int spawnEnemyBomb(
            int entityX, int entityY, int entityZ, int transitionCountdown, int speedX, int speedY, int speedZ) {
        return entityEnemySpawns()
                .spawnEnemyBomb(entityX, entityY, entityZ, transitionCountdown, speedX, speedY, speedZ);
    }

    default int spawnEnemyBomb(EnemyBombSpawn inputs) {
        int entityX = inputs.entityX();
        int entityY = inputs.entityY();
        int entityZ = inputs.entityZ();
        int transitionCountdown = inputs.transitionCountdown();
        int speedX = inputs.speedX();
        int speedY = inputs.speedY();
        int speedZ = inputs.speedZ();
        boolean initializeMotion = inputs.initializeMotion();
        int privateState4 = inputs.privateState4();
        return entityEnemySpawns()
                .spawnEnemyBomb(new EnemyBombSpawn(
                        entityX,
                        entityY,
                        entityZ,
                        transitionCountdown,
                        speedX,
                        speedY,
                        speedZ,
                        initializeMotion,
                        privateState4));
    }

    /** Creates the temporary type-$05 entity used by bombed bushes, grass, and pots. */
    default int spawnLiftableRockSmash(int entityX, int entityY, int sourceSpriteVariant) {
        return entityPlayerSpawns().spawnLiftableRockSmash(entityX, entityY, sourceSpriteVariant);
    }

    /** Creates one of the four silent type-$05 rubble entities from bank-$36. */
    default int spawnLiftableRockRubble(int entityX, int entityY) {
        return entityPlayerSpawns().spawnLiftableRockRubble(entityX, entityY);
    }

    /**
     * Mirrors the shovel's delayed random reward branch in
     * {@code label_002_4C92}. The first random byte is consumed even when the
     * current room is not allowed to produce a reward; the caller supplies
     * that room-specific restriction explicitly.
     */
    default int spawnShovelDrop(int objectLeft, int objectTop, int linkEntityX, int linkEntityY, boolean allowDrop) {
        return entityPlayerSpawns().spawnShovelDrop(objectLeft, objectTop, linkEntityX, linkEntityY, allowDrop);
    }

    /**
     * Mirrors SpawnChestWithItem. The arguments are the intersected object's
     * unaligned left/top coordinates; the source masks them to a room cell
     * and places the entity at (+8,+16).
     */
    default int spawnChestWithItem(int objectLeft, int objectTop, int itemType) {
        return entityPlayerSpawns().spawnChestWithItem(objectLeft, objectTop, itemType);
    }
}
