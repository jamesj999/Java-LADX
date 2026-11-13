package linksawakening.app.gameplay;

import linksawakening.world.entity.EnemyAttackContext;
import linksawakening.world.room.EntityPickupInputs;
import linksawakening.world.room.LinkGroundFrame;
import linksawakening.world.room.SwordBox;

/** Reuses unchanged immutable world-boundary values for one simulation; frame counters stay fresh. */
// Direction and motion inputs retain raw snapshot bytes, including unknown values accepted by the world boundary.
final class FrameInputCache {
    private LinkGroundFrame ground;
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

    private EntityPickupInputs.Link pickup;

    EntityPickupInputs.Link pickup(int x, int y, boolean airborne, boolean interactive, int direction, int z) {
        if (pickup == null
                || pickup.x() != x
                || pickup.y() != y
                || pickup.airborne() != airborne
                || pickup.interactive() != interactive
                || pickup.rawDirection() != direction
                || pickup.z() != z) pickup = new EntityPickupInputs.Link(x, y, airborne, interactive, direction, z);
        return pickup;
    }

    LinkGroundFrame ground(GameplaySimulation c, int x, int y, boolean blocked) {
        boolean airborne = c.link().airborne().isAirborne();
        boolean dialog = c.dialogController().isActive();
        boolean transition =
                c.scrollController().isActive() || c.transitionController().isInputBlocked();
        int motion = c.link().airborne().romMotionState();
        int slow = c.link().motion().romSlowWalkingSpeed();
        int usage = c.link().motion().romItemUsageContext();
        if (ground == null
                || ground.pixelX() != x
                || ground.pixelY() != y
                || ground.airborne() != airborne
                || ground.interactionBlocked() != blocked
                || ground.dialogActive() != dialog
                || ground.roomTransitionActive() != transition
                || ground.rawMotionState() != motion
                || ground.slowWalkingSpeed() != slow
                || ground.itemUsageContext() != usage) {
            ground = new LinkGroundFrame(x, y, airborne, blocked, dialog, transition, motion, slow, usage);
        }
        return ground;
    }
}
