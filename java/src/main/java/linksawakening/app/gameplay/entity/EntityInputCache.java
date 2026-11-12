package linksawakening.app.gameplay.entity;

import linksawakening.world.room.ButtonInputs;
import linksawakening.world.room.InventoryInputs;
import linksawakening.world.room.LinkInputs;
import linksawakening.world.room.LinkMotion;
import linksawakening.world.room.LinkPosition;
import linksawakening.world.room.PlayerHealth;
import linksawakening.world.room.PlayerInputs;
import linksawakening.world.room.PlayerLevels;
import linksawakening.world.room.ProgressInputs;
import linksawakening.world.room.SwordBox;
import linksawakening.world.room.UiInputs;

/** Session-local reuse of unchanged immutable input values; previously returned frames remain snapshots. */
// Direction and motion values remain raw snapshot inputs; preserve unknown ROM values alongside world classifications.
final class EntityInputCache {
    private LinkPosition linkPosition;
    private LinkMotion linkMotion;
    private LinkInputs linkInputs;
    private ProgressInputs progressInputs;
    private InventoryInputs inventoryInputs;
    private PlayerLevels playerLevels;
    private PlayerHealth playerHealth;
    private PlayerInputs playerInputs;
    private UiInputs uiInputs;
    private ButtonInputs buttonInputs;
    private SwordBox swordBox;

    LinkPosition linkPosition(int x, int y, int z, int direction, int collisionType) {
        if (linkPosition == null
                || linkPosition.x() != x
                || linkPosition.y() != y
                || linkPosition.z() != z
                || linkPosition.rawDirection() != direction
                || linkPosition.collisionType() != collisionType) {
            linkPosition = new LinkPosition(x, y, z, direction, collisionType);
        }
        return linkPosition;
    }

    LinkMotion linkMotion(boolean airborne, int state, boolean shield, int invincibility, int speedX, int speedY) {
        if (linkMotion == null
                || linkMotion.airborne() != airborne
                || linkMotion.rawState() != state
                || linkMotion.shield() != shield
                || linkMotion.invincibility() != invincibility
                || linkMotion.speedX() != speedX
                || linkMotion.speedY() != speedY) {
            linkMotion = new LinkMotion(airborne, state, shield, invincibility, speedX, speedY);
        }
        return linkMotion;
    }

    LinkInputs linkInputs(
            LinkPosition position,
            LinkMotion motion,
            int attackCountdown,
            int finalX,
            boolean freeMovement,
            int velocityZ) {
        if (linkInputs == null
                || linkInputs.position() != position
                || linkInputs.motion() != motion
                || linkInputs.attackCountdown() != attackCountdown
                || linkInputs.finalX() != finalX
                || linkInputs.freeMovement() != freeMovement
                || linkInputs.velocityZ() != velocityZ) {
            linkInputs = new LinkInputs(position, motion, attackCountdown, finalX, freeMovement, velocityZ);
        }
        return linkInputs;
    }

    ProgressInputs progressInputs(
            int heartPieces,
            boolean birdKey,
            boolean tailKey,
            int tradeItem,
            boolean hasToadstool,
            int powder,
            int leaves) {
        if (progressInputs == null
                || progressInputs.heartPieces() != heartPieces
                || progressInputs.birdKey() != birdKey
                || progressInputs.tailKey() != tailKey
                || progressInputs.tradeItem() != tradeItem
                || progressInputs.hasToadstool() != hasToadstool
                || progressInputs.powder() != powder
                || progressInputs.leaves() != leaves) {
            progressInputs = new ProgressInputs(heartPieces, birdKey, tailKey, tradeItem, hasToadstool, powder, leaves);
        }
        return progressInputs;
    }

    InventoryInputs inventoryInputs(int itemA, int itemB, boolean redTunic, boolean pieceOfPower) {
        if (inventoryInputs == null
                || inventoryInputs.itemA() != itemA
                || inventoryInputs.itemB() != itemB
                || inventoryInputs.redTunic() != redTunic
                || inventoryInputs.pieceOfPower() != pieceOfPower) {
            inventoryInputs = new InventoryInputs(itemA, itemB, redTunic, pieceOfPower);
        }
        return inventoryInputs;
    }

    PlayerLevels playerLevels(int shield, int sword, int bracelet) {
        if (playerLevels == null
                || playerLevels.shield() != shield
                || playerLevels.sword() != sword
                || playerLevels.bracelet() != bracelet) {
            playerLevels = new PlayerLevels(shield, sword, bracelet);
        }
        return playerLevels;
    }

    PlayerHealth playerHealth(int maxHearts, int health, boolean powerUp, boolean lowHealth) {
        if (playerHealth == null
                || playerHealth.maxHearts() != maxHearts
                || playerHealth.health() != health
                || playerHealth.powerUp() != powerUp
                || playerHealth.lowHealth() != lowHealth) {
            playerHealth = new PlayerHealth(maxHearts, health, powerUp, lowHealth);
        }
        return playerHealth;
    }

    PlayerInputs playerInputs(
            ProgressInputs progress, InventoryInputs inventory, PlayerLevels levels, PlayerHealth health) {
        if (playerInputs == null
                || playerInputs.progress() != progress
                || playerInputs.inventory() != inventory
                || playerInputs.levels() != levels
                || playerInputs.health() != health) {
            playerInputs = new PlayerInputs(progress, inventory, levels, health);
        }
        return playerInputs;
    }

    UiInputs uiInputs(
            boolean dialogActive,
            boolean inventoryAppearing,
            int dialogCooldown,
            int windowY,
            int selectionIndex,
            boolean musicActive) {
        if (uiInputs == null
                || uiInputs.dialogActive() != dialogActive
                || uiInputs.inventoryAppearing() != inventoryAppearing
                || uiInputs.dialogCooldown() != dialogCooldown
                || uiInputs.windowY() != windowY
                || uiInputs.selectionIndex() != selectionIndex
                || uiInputs.musicActive() != musicActive) {
            uiInputs = new UiInputs(
                    dialogActive, inventoryAppearing, dialogCooldown, windowY, selectionIndex, musicActive);
        }
        return uiInputs;
    }

    ButtonInputs buttonInputs(
            boolean aHeld, boolean bHeld, boolean joypadHeld, int pressedMask, boolean braceletHeld, boolean bombHeld) {
        if (buttonInputs == null
                || buttonInputs.aHeld() != aHeld
                || buttonInputs.bHeld() != bHeld
                || buttonInputs.joypadHeld() != joypadHeld
                || buttonInputs.pressedMask() != pressedMask
                || buttonInputs.braceletHeld() != braceletHeld
                || buttonInputs.bombHeld() != bombHeld) {
            buttonInputs = new ButtonInputs(aHeld, bHeld, joypadHeld, pressedMask, braceletHeld, bombHeld);
        }
        return buttonInputs;
    }

    SwordBox swordBox(boolean active, int x, int width, int y, int height) {
        if (swordBox == null
                || swordBox.active() != active
                || swordBox.x() != x
                || swordBox.width() != width
                || swordBox.y() != y
                || swordBox.height() != height) {
            swordBox = new SwordBox(active, x, width, y, height);
        }
        return swordBox;
    }
}
