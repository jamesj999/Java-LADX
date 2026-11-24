package linksawakening.app.gameplay;

import java.util.Objects;
import linksawakening.app.state.DebugState;
import linksawakening.app.state.FrameClock;
import linksawakening.dialog.DialogController;
import linksawakening.entity.Link;
import linksawakening.equipment.Sword;
import linksawakening.world.entity.EnemyAttackContext;
import linksawakening.world.entity.EnemyCombatInputs;
import linksawakening.world.entity.EntityPickupFrameInputs;
import linksawakening.world.room.LinkGroundInputs;
import linksawakening.world.room.RoomSession;
import linksawakening.world.room.SwordBox;
import linksawakening.world.transition.ScrollController;
import linksawakening.world.transition.TransitionController;
import linksawakening.world.transition.Warp;

/** Owns retained synchronous phase registers and immutable attack/sword values for one simulation. */
// Direction and motion inputs retain raw snapshot bytes, including unknown values accepted by the world boundary.
public final class FrameInputCache {
    private final DebugState debugState;
    private final DialogController dialogController;
    private final FrameClock clock;
    private final Link playerLink;
    private final RoomSession rooms;
    private final ScrollController scrollController;
    private final TransitionController transitionController;

    // Absent until first use; these are cached immutable values, not session collaborators.
    private EnemyAttackContext attack;

    private SwordBox sword;

    private final CombatFrame combat = new CombatFrame();
    private final PickupFrame pickup = new PickupFrame();
    private final GroundFrame ground = new GroundFrame();

    public FrameInputCache(
            DebugState debugNoClip,
            DialogController dialogController,
            FrameClock frameCounter,
            Link link,
            RoomSession roomSession,
            ScrollController scrollController,
            TransitionController transitionController) {
        this.debugState = Objects.requireNonNull(debugNoClip);
        this.dialogController = Objects.requireNonNull(dialogController);
        this.clock = Objects.requireNonNull(frameCounter);
        this.playerLink = Objects.requireNonNull(link);
        this.rooms = Objects.requireNonNull(roomSession);
        this.scrollController = Objects.requireNonNull(scrollController);
        this.transitionController = Objects.requireNonNull(transitionController);
    }

    public EnemyAttackContext attack(
            int swordLevel,
            boolean redTunic,
            boolean pieceOfPower,
            boolean spinAttack,
            boolean pegasusBoots,
            boolean usingShield,
            int romDirection,
            int linkZ) {
        if (attack == null
                || attack.swordLevel() != swordLevel
                || attack.redTunic() != redTunic
                || attack.pieceOfPower() != pieceOfPower
                || attack.spinAttack() != spinAttack
                || attack.pegasusBoots() != pegasusBoots
                || attack.usingShield() != usingShield
                || attack.romDirection() != romDirection
                || attack.linkZ() != linkZ)
            attack = new EnemyAttackContext(
                    swordLevel, redTunic, pieceOfPower, spinAttack, pegasusBoots, usingShield, romDirection, linkZ);
        return attack;
    }

    public SwordBox sword(boolean active, int x, int width, int y, int height) {
        if (sword == null
                || sword.active() != active
                || sword.x() != x
                || sword.width() != width
                || sword.y() != y
                || sword.height() != height) sword = new SwordBox(active, x, width, y, height);
        return sword;
    }

    public EnemyCombatInputs combat(
            Sword.CollisionBox b,
            EnemyAttackContext attackContext,
            boolean bladeClashCoolingDown,
            boolean goombaBounceActive,
            int swordHitTarget,
            boolean linkCanTakeContactDamage) {
        combat.frameCounter = this.clock.value();
        combat.linkEntityX = this.playerLink.position().romEntityX();
        combat.linkEntityY = this.playerLink.position().romEntityY();
        combat.linkAirborne = this.playerLink.airborne().isAirborne();
        combat.linkVerticalVelocity = this.rooms.rooms().mapCategory() == Warp.CATEGORY_SIDESCROLL
                ? this.playerLink.position().romSpeedY()
                : this.playerLink.airborne().zVelocity();
        combat.linkInteractive = !this.debugState.debugNoClip();
        combat.linkIgnoresContact = this.playerLink.status().ignoringContactCollisions();
        combat.goombaBounceActive = goombaBounceActive;
        combat.linkBladeClashCoolingDown = bladeClashCoolingDown;
        combat.swordHitTarget = swordHitTarget;
        combat.linkCanTakeContactDamage = linkCanTakeContactDamage;
        combat.sword = sword(b.active(), b.x(), b.width(), b.y(), b.height());
        combat.attackContext = attackContext;
        return combat;
    }

    boolean keepsShieldCombatRectangle(EnemyAttackContext attack) {
        // $02:4878 skips overwriting the shield rectangle on odd frames.
        return attack.usingShield() && !attack.spinAttack() && (clock.value() & 1) != 0;
    }

    public EntityPickupFrameInputs pickup(Sword.CollisionBox b) {
        pickup.frameCounter = this.clock.value();
        pickup.linkEntityX = this.playerLink.position().romEntityX();
        pickup.linkEntityY = this.playerLink.position().romEntityY();
        pickup.linkAirborne = this.playerLink.airborne().isAirborne();
        pickup.linkInteractive = true;
        pickup.linkDirection = this.playerLink.animation().direction();
        pickup.linkZ = this.playerLink.animation().romEntityZ();
        pickup.sword = sword(b.active(), b.x(), b.width(), b.y(), b.height());
        return pickup;
    }

    public LinkGroundInputs ground(int x, int y, boolean blocked) {
        ground.pixelX = x;
        ground.pixelY = y;
        ground.airborne = this.playerLink.airborne().isAirborne();
        ground.interactionBlocked = blocked;
        ground.dialogActive = this.dialogController.isActive();
        ground.roomTransitionActive = this.scrollController.isActive() || this.transitionController.isInputBlocked();
        ground.rawMotionState = this.playerLink.airborne().romMotionState();
        ground.slowWalkingSpeed = this.playerLink.motion().romSlowWalkingSpeed();
        ground.itemUsageContext = this.playerLink.motion().romItemUsageContext();
        return ground;
    }

    /** Phase-owned registers: each view is consumed synchronously before its next refill. */
    private static final class GroundFrame implements LinkGroundInputs {
        private int pixelX;
        private int pixelY;
        private boolean airborne;
        private boolean interactionBlocked;
        private boolean dialogActive;
        private boolean roomTransitionActive;
        private int rawMotionState;
        private int slowWalkingSpeed;
        private int itemUsageContext;

        @Override
        public int pixelX() {
            return pixelX;
        }

        @Override
        public int pixelY() {
            return pixelY;
        }

        @Override
        public boolean airborne() {
            return airborne;
        }

        @Override
        public boolean interactionBlocked() {
            return interactionBlocked;
        }

        @Override
        public boolean dialogActive() {
            return dialogActive;
        }

        @Override
        public boolean roomTransitionActive() {
            return roomTransitionActive;
        }

        @Override
        public int rawMotionState() {
            return rawMotionState;
        }

        @Override
        public int slowWalkingSpeed() {
            return slowWalkingSpeed;
        }

        @Override
        public int itemUsageContext() {
            return itemUsageContext;
        }
    }

    private static final class CombatFrame implements EnemyCombatInputs {
        private int frameCounter, linkEntityX, linkEntityY, linkVerticalVelocity;
        private boolean linkAirborne, linkInteractive;
        private boolean linkIgnoresContact, linkBladeClashCoolingDown, goombaBounceActive;
        private int swordHitTarget;
        private boolean linkCanTakeContactDamage;
        // Captured values are filled before publication, not optional collaborators.
        private SwordBox sword;
        private EnemyAttackContext attackContext;

        @Override
        public int frameCounter() {
            return frameCounter;
        }

        @Override
        public int linkEntityX() {
            return linkEntityX;
        }

        @Override
        public int linkEntityY() {
            return linkEntityY;
        }

        @Override
        public int linkVerticalVelocity() {
            return linkVerticalVelocity;
        }

        @Override
        public boolean linkAirborne() {
            return linkAirborne;
        }

        @Override
        public boolean linkInteractive() {
            return linkInteractive;
        }

        @Override
        public boolean linkIgnoresContact() {
            return linkIgnoresContact;
        }

        @Override
        public boolean goombaBounceActive() {
            return goombaBounceActive;
        }

        @Override
        public boolean linkBladeClashCoolingDown() {
            return linkBladeClashCoolingDown;
        }

        @Override
        public int swordHitTarget() {
            return swordHitTarget;
        }

        @Override
        public boolean linkCanTakeContactDamage() {
            return linkCanTakeContactDamage;
        }

        @Override
        public EnemyAttackContext attackContext() {
            return attackContext;
        }

        @Override
        public boolean swordCollisionActive() {
            return sword.active();
        }

        @Override
        public int swordX() {
            return sword.x();
        }

        @Override
        public int swordWidth() {
            return sword.width();
        }

        @Override
        public int swordY() {
            return sword.y();
        }

        @Override
        public int swordHeight() {
            return sword.height();
        }
    }

    private static final class PickupFrame implements EntityPickupFrameInputs {
        private int frameCounter, linkEntityX, linkEntityY, linkDirection, linkZ;
        private boolean linkAirborne, linkInteractive;
        // Filled before this borrowed capture is passed to the room.
        private SwordBox sword;

        @Override
        public int frameCounter() {
            return frameCounter;
        }

        @Override
        public int linkEntityX() {
            return linkEntityX;
        }

        @Override
        public int linkEntityY() {
            return linkEntityY;
        }

        @Override
        public int linkDirection() {
            return linkDirection;
        }

        @Override
        public int linkZ() {
            return linkZ;
        }

        @Override
        public boolean linkAirborne() {
            return linkAirborne;
        }

        @Override
        public boolean linkInteractive() {
            return linkInteractive;
        }

        @Override
        public boolean swordActive() {
            return sword.active();
        }

        @Override
        public int swordX() {
            return sword.x();
        }

        @Override
        public int swordWidth() {
            return sword.width();
        }

        @Override
        public int swordY() {
            return sword.y();
        }

        @Override
        public int swordHeight() {
            return sword.height();
        }
    }
}
