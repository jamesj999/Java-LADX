package linksawakening.app.gameplay.entity;

import linksawakening.app.gameplay.GameplaySimulation;
import linksawakening.equipment.Sword;
import linksawakening.state.PowerUp;
import linksawakening.state.Tunic;
import linksawakening.world.room.ButtonInputs;
import linksawakening.world.room.EntityFrameInputs;
import linksawakening.world.room.LinkInputs;
import linksawakening.world.room.PlayerInputs;
import linksawakening.world.room.SwordBox;
import linksawakening.world.room.UiInputs;

/** Captures current entity-frame inputs, retaining unchanged immutable leaf values for one session. */
final class EntityFrameInputsFactory {
    private final EntityInputCache cache = new EntityInputCache();

    EntityFrameInputs create(GameplaySimulation c, Sword.CollisionBox b) {
        return new EntityFrameInputs(
                c.frameCounter(),
                buildLinkInputs(c),
                buildPlayerInputs(c),
                buildUiInputs(c),
                buildButtonInputs(c),
                buildSwordBox(c, b));
    }

    private LinkInputs buildLinkInputs(GameplaySimulation c) {
        var l = c.link();
        var p = c.playerState();
        return cache.linkInputs(
                cache.linkPosition(
                        l.position().romEntityX(),
                        l.position().romEntityY(),
                        l.animation().romEntityZ(),
                        l.animation().direction(),
                        l.position().romCollisionType()),
                cache.linkMotion(
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

    private PlayerInputs buildPlayerInputs(GameplaySimulation c) {
        var p = c.playerState();
        return cache.playerInputs(
                cache.progressInputs(
                        p.progression().heartPieces(),
                        p.progression().birdKeyCount() != 0,
                        p.progression().tailKeyCount() != 0,
                        p.progression().tradeSequenceItem(),
                        p.ammunition().hasToadstool(),
                        p.ammunition().magicPowderCount(),
                        p.progression().goldenLeavesCount()),
                cache.inventoryInputs(
                        p.inventory().itemA(),
                        p.inventory().itemB(),
                        p.equipmentLevels().tunicType() == Tunic.RED.romValue(),
                        p.equipmentLevels().activePowerUp() == PowerUp.PIECE_OF_POWER.romValue()),
                cache.playerLevels(
                        p.equipmentLevels().shieldLevel(),
                        p.equipmentLevels().swordLevel(),
                        p.equipmentLevels().powerBraceletLevel()),
                cache.playerHealth(
                        p.vitals().maxHearts(),
                        p.vitals().health(),
                        p.equipmentLevels().activePowerUp() != PowerUp.NONE.romValue(),
                        p.vitals().isOnLowHealth()));
    }

    private UiInputs buildUiInputs(GameplaySimulation c) {
        var d = c.dialogController();
        var m = c.inventoryMenu();
        var p = c.musicPlayer();
        return cache.uiInputs(
                d.isActive(),
                m.inventoryAppearing(),
                d.dialogCooldown(),
                m.windowY(),
                d.choiceSelectionIndex(),
                p.isMusicPlaying());
    }

    private ButtonInputs buildButtonInputs(GameplaySimulation c) {
        var i = c.inputState();
        var k = c.inputConfig();
        var l = c.link();
        var lift = c.lift();
        return cache.buttonInputs(
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

    private SwordBox buildSwordBox(GameplaySimulation c, Sword.CollisionBox b) {
        return cache.swordBox(b.active(), b.x(), b.width(), b.y(), b.height());
    }
}
