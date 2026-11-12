package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;

/** ROM slot allocation and player-created entity spawning. */
public final class EntityPlayerSpawns {
    private final EntityPlayerSpawnsServices runtime;

    EntityPlayerSpawns(EntityPlayerSpawnsServices runtime) {
        this.runtime = runtime;
    }

    void spawnIronMasksMask(int x, int y, int variant, int sourceHookshotSlot) {
        HookshotSpawns.spawnIronMasksMask(runtime, x, y, variant, sourceHookshotSlot);
    }

    int spawnHookshotChain(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return HookshotSpawns.spawnHookshotChain(runtime, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    int spawnPushedBlock(int x, int y) {
        return PlayerObjectSpawns.spawnPushedBlock(runtime, x, y);
    }

    int spawnLiftedRoomObject(int x, int y, int romDirection, int spriteVariant) {
        return PlayerObjectSpawns.spawnLiftedRoomObject(runtime, x, y, romDirection, spriteVariant);
    }

    int spawnRoomEventKeyDrop() {
        return PlayerRewardSpawns.spawnRoomEventKeyDrop(runtime);
    }

    int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return PlayerProjectileSpawns.spawnArrow(runtime, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    int spawnArrow(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return PlayerProjectileSpawns.spawnArrow(
                runtime, linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }

    int spawnBoomerang(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, int pressedButtonsMask) {
        return PlayerProjectileSpawns.spawnBoomerang(
                runtime, linkEntityX, linkEntityY, linkEntityZ, romDirection, pressedButtonsMask);
    }

    int spawnSwordBeam(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return PlayerProjectileSpawns.spawnSwordBeam(runtime, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    int spawnMagicRodFireball(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return PlayerProjectileSpawns.spawnMagicRodFireball(
                runtime, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    int spawnMagicRodFireball(
            int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection, boolean pieceOfPower) {
        return PlayerProjectileSpawns.spawnMagicRodFireball(
                runtime, linkEntityX, linkEntityY, linkEntityZ, romDirection, pieceOfPower);
    }

    int spawnMagicPowderSprinkle(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return PlayerProjectileSpawns.spawnMagicPowderSprinkle(
                runtime, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    void spawnMusicalNote(int linkEntityX, int linkEntityY, int animationPhase) {
        PlayerRewardSpawns.spawnMusicalNote(runtime, linkEntityX, linkEntityY, animationPhase);
    }

    int spawnBomb(int linkEntityX, int linkEntityY, int linkEntityZ, int romDirection) {
        return PlayerBombSpawns.spawnBomb(runtime, linkEntityX, linkEntityY, linkEntityZ, romDirection);
    }

    public int spawnLiftableRockSmash(int entityX, int entityY, int sourceSpriteVariant) {
        return PlayerObjectSpawns.spawnLiftableRockSmash(runtime, entityX, entityY, sourceSpriteVariant);
    }

    public int spawnLiftableRockRubble(int entityX, int entityY) {
        return PlayerObjectSpawns.spawnLiftableRockRubble(runtime, entityX, entityY);
    }

    int spawnLiftableRockAnimation(
            int entityX, int entityY, int sourceSpriteVariant, int initialCountdown, int physicsFlags, int soundId) {
        return PlayerObjectSpawns.spawnLiftableRockAnimation(
                runtime, entityX, entityY, sourceSpriteVariant, initialCountdown, physicsFlags, soundId);
    }

    public int spawnBombFromBombArrow(RoomEntity arrow) {
        return PlayerBombSpawns.spawnBombFromBombArrow(runtime, arrow);
    }

    public boolean spawnHookshotBridge(int bridgeX, int bridgeY, int direction, int transitionCountdown) {
        return HookshotSpawns.spawnHookshotBridge(runtime, bridgeX, bridgeY, direction, transitionCountdown);
    }

    int spawnShovelDrop(int objectLeft, int objectTop, int linkEntityX, int linkEntityY, boolean allowDrop) {
        return PlayerRewardSpawns.spawnShovelDrop(runtime, objectLeft, objectTop, linkEntityX, linkEntityY, allowDrop);
    }

    int spawnChestWithItem(int objectLeft, int objectTop, int itemType) {
        return PlayerRewardSpawns.spawnChestWithItem(runtime, objectLeft, objectTop, itemType);
    }
}
