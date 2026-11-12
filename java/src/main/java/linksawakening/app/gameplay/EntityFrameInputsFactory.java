package linksawakening.app.gameplay;

import static linksawakening.app.gameplay.GameplaySimulation.*;
import static linksawakening.world.room.RoomConstants.*;

import linksawakening.equipment.Sword;
import linksawakening.state.PowerUp;
import linksawakening.state.Tunic;
import linksawakening.world.room.*;

final class EntityFrameInputsFactory {
    static EntityFrameInputs create(GameplaySimulation c, Sword.CollisionBox b) {
        return new EntityFrameInputs(
                c.frameCounter(),
                buildLinkInputs(c),
                buildPlayerInputs(c),
                buildUiInputs(c),
                buildButtonInputs(c),
                buildSwordBox(c, b));
    }

    private static LinkInputs buildLinkInputs(GameplaySimulation c) {
        var l = c.link();
        var p = c.playerState();
        return new LinkInputs(
                new LinkPosition(l.romEntityX(), l.romEntityY(), l.romEntityZ(), l.direction(), l.romCollisionType()),
                new LinkMotion(
                        l.isAirborne(),
                        l.romMotionState(),
                        l.isUsingShield(),
                        l.isCollisionDamageImmune() ? 1 : p.invincibilityCounter(),
                        l.romSpeedX(),
                        l.romSpeedY()),
                l.romAttackStepAnimationCountdown(),
                l.romFinalPositionX(),
                l.debugNoClip(),
                l.zVelocity());
    }

    private static PlayerInputs buildPlayerInputs(GameplaySimulation c) {
        var p = c.playerState();
        return new PlayerInputs(
                new ProgressInputs(
                        p.heartPieces(),
                        p.birdKeyCount() != 0,
                        p.tailKeyCount() != 0,
                        p.tradeSequenceItem(),
                        p.hasToadstool(),
                        p.magicPowderCount(),
                        p.goldenLeavesCount()),
                new InventoryInputs(
                        p.itemA(),
                        p.itemB(),
                        p.tunicType() == Tunic.RED.romValue(),
                        p.activePowerUp() == PowerUp.PIECE_OF_POWER.romValue()),
                new PlayerLevels(p.shieldLevel(), p.swordLevel(), p.powerBraceletLevel()),
                new PlayerHealth(
                        p.maxHearts(), p.health(), p.activePowerUp() != PowerUp.NONE.romValue(), p.isOnLowHealth()));
    }

    private static UiInputs buildUiInputs(GameplaySimulation c) {
        var d = c.dialogController();
        var m = c.inventoryMenu();
        var p = c.musicPlayer();
        return new UiInputs(
                d.isActive(),
                m.inventoryAppearing(),
                d.dialogCooldown(),
                m.windowY(),
                d.choiceSelectionIndex(),
                p.isMusicPlaying());
    }

    private static ButtonInputs buildButtonInputs(GameplaySimulation c) {
        var i = c.inputState();
        var k = c.inputConfig();
        var l = c.link();
        var lift = c.lift();
        return new ButtonInputs(
                i.isDown(k.aKey()),
                i.isDown(k.bKey()),
                (i.isDown(k.upKey())
                        || i.isDown(k.downKey())
                        || i.isDown(k.leftKey())
                        || i.isDown(k.rightKey())
                        || i.isDown(k.aKey())
                        || i.isDown(k.bKey())
                        || i.isDown(k.selectKey())
                        || i.isDown(k.menuOpenKey())),
                l.romPressedButtonsMask(),
                lift.isPowerBraceletButtonHeld(),
                lift.isBombButtonHeld());
    }

    private static SwordBox buildSwordBox(GameplaySimulation c, Sword.CollisionBox b) {
        return new SwordBox(b.active(), b.x(), b.width(), b.y(), b.height());
    }
}
