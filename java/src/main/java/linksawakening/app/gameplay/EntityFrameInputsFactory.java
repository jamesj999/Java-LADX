package linksawakening.app.gameplay;

import linksawakening.equipment.Sword;
import linksawakening.state.PowerUp;
import linksawakening.state.Tunic;
import linksawakening.world.room.ButtonInputs;
import linksawakening.world.room.EntityFrameInputs;
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
                new LinkPosition(
                        l.position().romEntityX(),
                        l.position().romEntityY(),
                        l.animation().romEntityZ(),
                        l.animation().direction(),
                        l.position().romCollisionType()),
                new LinkMotion(
                        l.airborne().isAirborne(),
                        l.airborne().romMotionState(),
                        l.equipment().isUsingShield(),
                        l.status().isCollisionDamageImmune() ? 1 : p.vitals().invincibilityCounter(),
                        l.position().romSpeedX(),
                        l.position().romSpeedY()),
                l.equipment().romAttackStepAnimationCountdown(),
                l.position().romFinalPositionX(),
                l.motion().debugNoClip(),
                l.airborne().zVelocity());
    }

    private static PlayerInputs buildPlayerInputs(GameplaySimulation c) {
        var p = c.playerState();
        return new PlayerInputs(
                new ProgressInputs(
                        p.progression().heartPieces(),
                        p.progression().birdKeyCount() != 0,
                        p.progression().tailKeyCount() != 0,
                        p.progression().tradeSequenceItem(),
                        p.ammunition().hasToadstool(),
                        p.ammunition().magicPowderCount(),
                        p.progression().goldenLeavesCount()),
                new InventoryInputs(
                        p.inventory().itemA(),
                        p.inventory().itemB(),
                        p.equipmentLevels().tunicType() == Tunic.RED.romValue(),
                        p.equipmentLevels().activePowerUp() == PowerUp.PIECE_OF_POWER.romValue()),
                new PlayerLevels(
                        p.equipmentLevels().shieldLevel(),
                        p.equipmentLevels().swordLevel(),
                        p.equipmentLevels().powerBraceletLevel()),
                new PlayerHealth(
                        p.vitals().maxHearts(),
                        p.vitals().health(),
                        p.equipmentLevels().activePowerUp() != PowerUp.NONE.romValue(),
                        p.vitals().isOnLowHealth()));
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
                l.controls().romPressedButtonsMask(),
                lift.isPowerBraceletButtonHeld(),
                lift.isBombButtonHeld());
    }

    private static SwordBox buildSwordBox(GameplaySimulation c, Sword.CollisionBox b) {
        return new SwordBox(b.active(), b.x(), b.width(), b.y(), b.height());
    }
}
