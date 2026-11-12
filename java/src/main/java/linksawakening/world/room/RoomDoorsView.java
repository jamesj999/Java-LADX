package linksawakening.world.room;

import linksawakening.world.room.RoomSession.*;

final class RoomDoorsView implements RoomDoors {
    private final RoomDoorInteractions delegate;

    RoomDoorsView(RoomDoorInteractions delegate) {
        this.delegate = delegate;
    }

    @Override
    public ChestOpenResult tryOpenChest(
            int linkPixelX, int linkPixelY, int linkDirection, boolean actionPressed, int swordLevel) {
        return delegate.tryOpenChest(linkPixelX, linkPixelY, linkDirection, actionPressed, swordLevel);
    }

    @Override
    public boolean tryUnlockIndoorKeyDoor(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        return delegate.tryUnlockIndoorKeyDoor(linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    @Override
    public boolean tryPushIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        return delegate.tryPushIndoorBlock(linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    @Override
    public boolean tryLiftObject(
            int x, int y, int direction, boolean held, int buttons, boolean power, int level, boolean anyPowerUp) {
        return delegate.tryLiftObject(x, y, direction, held, buttons, power, level, anyPowerUp);
    }

    @Override
    public boolean tryLiftObject(int x, int y, int direction, boolean held, int buttons, boolean power, int level) {
        return delegate.tryLiftObject(x, y, direction, held, buttons, power, level);
    }

    @Override
    public boolean tryLiftIndoorObject(
            int linkPixelX, int linkPixelY, int linkDirection, boolean braceletButtonHeld, int pressedButtonsMask) {
        return delegate.tryLiftIndoorObject(
                linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask);
    }

    @Override
    public boolean tryLiftIndoorObject(
            int linkPixelX,
            int linkPixelY,
            int linkDirection,
            boolean braceletButtonHeld,
            int pressedButtonsMask,
            boolean pieceOfPowerActive) {
        return delegate.tryLiftIndoorObject(
                linkPixelX, linkPixelY, linkDirection, braceletButtonHeld, pressedButtonsMask, pieceOfPowerActive);
    }

    @Override
    public boolean tryInteractWithIndoorBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        return delegate.tryInteractWithIndoorBlock(linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    @Override
    public boolean tryUnlockIndoorKeyholeBlock(int linkPixelX, int linkPixelY, int linkDirection, int collisionType) {
        return delegate.tryUnlockIndoorKeyholeBlock(linkPixelX, linkPixelY, linkDirection, collisionType);
    }

    @Override
    public boolean tryUnlockTailCaveKeyhole(
            int linkPixelX, int linkPixelY, int linkDirection, int collisionType, boolean hasTailKey) {
        return delegate.tryUnlockTailCaveKeyhole(linkPixelX, linkPixelY, linkDirection, collisionType, hasTailKey);
    }

    @Override
    public boolean tryUnlockKeyCavernKeyhole(
            int linkPixelX, int linkPixelY, int linkDirection, int collisionType, int goldenLeavesCount) {
        return delegate.tryUnlockKeyCavernKeyhole(
                linkPixelX, linkPixelY, linkDirection, collisionType, goldenLeavesCount);
    }

    @Override
    public int tickTailCaveKeyholeSequence() {
        return delegate.tickTailCaveKeyholeSequence();
    }

    @Override
    public boolean tailCaveKeyholeSequenceActive() {
        return delegate.tailCaveKeyholeSequenceActive();
    }
}
