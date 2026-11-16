package linksawakening.app.gameplay;

import linksawakening.equipment.Sword;
import linksawakening.world.entity.EnemyAttackContext;
import linksawakening.world.entity.EnemyCombatInputs;
import linksawakening.world.entity.EntityPickupFrameInputs;
import linksawakening.world.room.LinkGroundInputs;
import linksawakening.world.room.SwordBox;
import linksawakening.world.transition.Warp;

/** Owns retained synchronous phase registers and immutable attack/sword values for one simulation. */
// Direction and motion inputs retain raw snapshot bytes, including unknown values accepted by the world boundary.
final class FrameInputCache {
    // Absent until first use; these are cached immutable values, not session collaborators.
    private EnemyAttackContext attack;

    EnemyAttackContext attack(
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

    private SwordBox sword;

    SwordBox sword(boolean active, int x, int width, int y, int height) {
        if (sword == null
                || sword.active() != active
                || sword.x() != x
                || sword.width() != width
                || sword.y() != y
                || sword.height() != height) sword = new SwordBox(active, x, width, y, height);
        return sword;
    }

    private final CombatFrame combat = new CombatFrame();
    private final PickupFrame pickup = new PickupFrame();
    private final GroundFrame ground = new GroundFrame();

    EnemyCombatInputs combat(GameplaySimulation c, Sword.CollisionBox b, EnemyAttackContext attackContext) {
        combat.frameCounter = c.frameCounter();
        combat.linkEntityX = c.link().position().romEntityX();
        combat.linkEntityY = c.link().position().romEntityY();
        combat.linkAirborne = c.link().airborne().isAirborne();
        combat.linkVerticalVelocity = c.roomSession().rooms().mapCategory() == Warp.CATEGORY_SIDESCROLL
                ? c.link().position().romSpeedY()
                : c.link().airborne().zVelocity();
        combat.linkInteractive = !c.debugNoClip();
        combat.sword = sword(b.active(), b.x(), b.width(), b.y(), b.height());
        combat.attackContext = attackContext;
        return combat;
    }

    EntityPickupFrameInputs pickup(GameplaySimulation c, Sword.CollisionBox b) {
        pickup.frameCounter = c.frameCounter();
        pickup.linkEntityX = c.link().position().romEntityX();
        pickup.linkEntityY = c.link().position().romEntityY();
        pickup.linkAirborne = c.link().airborne().isAirborne();
        pickup.linkInteractive = true;
        pickup.linkDirection = c.link().animation().direction();
        pickup.linkZ = c.link().animation().romEntityZ();
        pickup.sword = sword(b.active(), b.x(), b.width(), b.y(), b.height());
        return pickup;
    }

    LinkGroundInputs ground(GameplaySimulation c, int x, int y, boolean blocked) {
        ground.pixelX = x;
        ground.pixelY = y;
        ground.airborne = c.link().airborne().isAirborne();
        ground.interactionBlocked = blocked;
        ground.dialogActive = c.dialogController().isActive();
        ground.roomTransitionActive =
                c.scrollController().isActive() || c.transitionController().isInputBlocked();
        ground.rawMotionState = c.link().airborne().romMotionState();
        ground.slowWalkingSpeed = c.link().motion().romSlowWalkingSpeed();
        ground.itemUsageContext = c.link().motion().romItemUsageContext();
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
