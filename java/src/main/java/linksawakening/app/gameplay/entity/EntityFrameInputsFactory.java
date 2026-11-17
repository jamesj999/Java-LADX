package linksawakening.app.gameplay.entity;

import java.util.Objects;
import linksawakening.app.gameplay.interaction.LiftInteraction;
import linksawakening.app.state.FrameClock;
import linksawakening.audio.music.FrameMusicPlayer;
import linksawakening.dialog.DialogController;
import linksawakening.equipment.Sword;
import linksawakening.input.InputConfig;
import linksawakening.input.InputState;
import linksawakening.state.PlayerState;
import linksawakening.state.PowerUp;
import linksawakening.state.Tunic;
import linksawakening.ui.InventoryMenu;
import linksawakening.world.room.ButtonInputs;
import linksawakening.world.room.EntityFrameInputs;
import linksawakening.world.room.EntityFrameValues;
import linksawakening.world.room.PlayerInputs;
import linksawakening.world.room.SwordBox;
import linksawakening.world.room.UiInputs;

/** Captures current entity-frame inputs, retaining unchanged immutable leaf values for one session. */
public final class EntityFrameInputsFactory implements EntityFrameValues {
    private final DialogController dialogController;
    private final FrameClock clock;
    private final InputConfig inputConfig;
    private final InputState inputState;
    private final InventoryMenu inventoryMenu;
    private final LiftInteraction lift;
    private final linksawakening.entity.Link playerLink;
    private final FrameMusicPlayer musicPlayer;
    private final PlayerState playerState;

    private final EntityInputCache cache = new EntityInputCache();

    private int frameCounter;
    private final CapturedLink link = new CapturedLink();
    // Captured domain values are published only after borrow/create fills every group.
    private PlayerInputs player;
    private UiInputs ui;
    private ButtonInputs buttons;
    private SwordBox sword;

    public EntityFrameInputsFactory(Inputs inputs) {
        this.dialogController = Objects.requireNonNull(inputs.dialogController);
        this.clock = Objects.requireNonNull(inputs.frameCounter);
        this.inputConfig = Objects.requireNonNull(inputs.inputConfig);
        this.inputState = Objects.requireNonNull(inputs.inputState);
        this.inventoryMenu = Objects.requireNonNull(inputs.inventoryMenu);
        this.lift = Objects.requireNonNull(inputs.lift);
        this.playerLink = Objects.requireNonNull(inputs.link);
        this.musicPlayer = Objects.requireNonNull(inputs.musicPlayer);
        this.playerState = Objects.requireNonNull(inputs.playerState);
    }

    /** Owned snapshot. Earlier captures remain immutable when this factory is reused. */
    public EntityFrameInputs create(Sword.CollisionBox b) {
        borrow(b);
        var position = cache.linkPosition(link.x, link.y, link.z, link.rawDirection, link.collisionType);
        var motion = cache.linkMotion(
                link.airborne, link.rawState, link.shield, link.invincibility, link.speedX, link.speedY);
        var linkSnapshot = cache.linkInputs(
                position, motion, link.attackCountdown, link.finalX, link.freeMovement, link.velocityZ);
        return new EntityFrameInputs(frameCounter, linkSnapshot, player, ui, buttons, sword);
    }

    /** Borrowed until the next borrow/create; the world tick consumes it synchronously. */
    public EntityFrameValues borrow(Sword.CollisionBox b) {
        frameCounter = this.clock.value();
        captureLink();
        player = buildPlayerInputs();
        ui = buildUiInputs();
        buttons = buildButtonInputs();
        sword = buildSwordBox(b);
        return this;
    }

    @Override
    public int frameCounter() {
        return frameCounter;
    }

    @Override
    public CapturedLink link() {
        return link;
    }

    @Override
    public PlayerInputs player() {
        return player;
    }

    @Override
    public UiInputs ui() {
        return ui;
    }

    @Override
    public ButtonInputs buttons() {
        return buttons;
    }

    @Override
    public SwordBox sword() {
        return sword;
    }

    private void captureLink() {
        var l = this.playerLink;
        var p = this.playerState;
        link.x = l.position().romEntityX();
        link.y = l.position().romEntityY();
        link.z = l.animation().romEntityZ();
        link.rawDirection = l.animation().direction();
        link.collisionType = l.position().romCollisionType();
        link.airborne = l.airborne().isAirborne();
        link.rawState = l.airborne().romMotionState();
        link.shield = l.equipment().isUsingShield();
        link.invincibility =
                l.status().isCollisionDamageImmune() ? 1 : p.vitals().invincibilityCounter();
        link.speedX = l.position().romSpeedX();
        link.speedY = l.position().romSpeedY();
        link.attackCountdown = l.equipment().romAttackStepAnimationCountdown();
        link.finalX = l.position().romFinalPositionX();
        link.freeMovement = l.motion().debugNoClip();
        link.velocityZ = l.airborne().zVelocity();
    }

    private PlayerInputs buildPlayerInputs() {
        var p = this.playerState;
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

    private UiInputs buildUiInputs() {
        var d = this.dialogController;
        var m = this.inventoryMenu;
        var p = this.musicPlayer;
        return cache.uiInputs(
                d.isActive(),
                m.inventoryAppearing(),
                d.dialogCooldown(),
                m.windowY(),
                d.choiceSelectionIndex(),
                p.isMusicPlaying());
    }

    private ButtonInputs buildButtonInputs() {
        var i = this.inputState;
        var k = this.inputConfig;
        var l = this.playerLink;
        var lift = this.lift;
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

    private SwordBox buildSwordBox(Sword.CollisionBox b) {
        return cache.swordBox(b.active(), b.x(), b.width(), b.y(), b.height());
    }

    private static final class CapturedLink
            implements EntityFrameValues.Link, EntityFrameValues.Position, EntityFrameValues.Motion {
        private int x, y, z, rawDirection, collisionType, rawState, invincibility, speedX, speedY;
        private int attackCountdown, finalX, velocityZ;
        private boolean airborne, shield, freeMovement;

        @Override
        public CapturedLink position() {
            return this;
        }

        @Override
        public CapturedLink motion() {
            return this;
        }

        @Override
        public int x() {
            return x;
        }

        @Override
        public int y() {
            return y;
        }

        @Override
        public int z() {
            return z;
        }

        @Override
        public int rawDirection() {
            return rawDirection;
        }

        @Override
        public int collisionType() {
            return collisionType;
        }

        @Override
        public int rawState() {
            return rawState;
        }

        @Override
        public int invincibility() {
            return invincibility;
        }

        @Override
        public int speedX() {
            return speedX;
        }

        @Override
        public int speedY() {
            return speedY;
        }

        @Override
        public int attackCountdown() {
            return attackCountdown;
        }

        @Override
        public int finalX() {
            return finalX;
        }

        @Override
        public int velocityZ() {
            return velocityZ;
        }

        @Override
        public boolean airborne() {
            return airborne;
        }

        @Override
        public boolean shield() {
            return shield;
        }

        @Override
        public boolean freeMovement() {
            return freeMovement;
        }
    }

    /** Named construction inputs, consumed once by the phase constructor. */
    public static final class Inputs {
        private DialogController dialogController;
        private FrameClock frameCounter;
        private InputConfig inputConfig;
        private InputState inputState;
        private InventoryMenu inventoryMenu;
        private LiftInteraction lift;
        private linksawakening.entity.Link link;
        private FrameMusicPlayer musicPlayer;
        private PlayerState playerState;

        public Inputs dialogController(DialogController value) {
            this.dialogController = value;
            return this;
        }

        public Inputs frameCounter(FrameClock value) {
            this.frameCounter = value;
            return this;
        }

        public Inputs inputConfig(InputConfig value) {
            this.inputConfig = value;
            return this;
        }

        public Inputs inputState(InputState value) {
            this.inputState = value;
            return this;
        }

        public Inputs inventoryMenu(InventoryMenu value) {
            this.inventoryMenu = value;
            return this;
        }

        public Inputs lift(LiftInteraction value) {
            this.lift = value;
            return this;
        }

        public Inputs link(linksawakening.entity.Link value) {
            this.link = value;
            return this;
        }

        public Inputs musicPlayer(FrameMusicPlayer value) {
            this.musicPlayer = value;
            return this;
        }

        public Inputs playerState(PlayerState value) {
            this.playerState = value;
            return this;
        }
    }
}
