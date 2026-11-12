package linksawakening.entity;

import static linksawakening.entity.Link.*;
import static linksawakening.entity.LinkRomConstants.*;

import linksawakening.state.Direction;

/** LinkAnimationState owns its Link state and operations. */
final class LinkAnimationState {
    private final LinkComponentRef<LinkAirborne> linkAirborne;
    private final LinkComponentRef<LinkMotion> linkMotion;

    LinkAnimationState(Dependencies dependencies) {
        linkAirborne = dependencies.linkAirborne;
        linkMotion = dependencies.linkMotion;
    }

    static final class Dependencies {
        LinkComponentRef<LinkAirborne> linkAirborne;
        LinkComponentRef<LinkMotion> linkMotion;
    }

    int direction = Direction.DOWN.javaIndex();
    int walkTickCounter;
    int walkFrame;
    boolean movingThisFrame;
    int romConsecutiveStepsCount = 7;
    boolean topDownFeatherStarted;

    int accessRomEntityZ() {
        return linkAirborne.get().accessZPixels();
    }

    boolean accessHasFacingCollision() {
        int flag =
                switch (Direction.fromJavaOrUnknown(direction)) {
                    case UP -> COLLISION_TYPE_UP;
                    case DOWN -> COLLISION_TYPE_DOWN;
                    case LEFT -> COLLISION_TYPE_LEFT;
                    case RIGHT -> COLLISION_TYPE_RIGHT;
                    default -> 0;
                };
        return (linkMotion.get().romCollisionType & flag) != 0;
    }

    int accessDirection() {
        return direction;
    }

    void accessSetDirection(int newDirection) {
        if (newDirection < Direction.DOWN.javaIndex() || newDirection > Direction.RIGHT.javaIndex()) {
            throw new IllegalArgumentException("Invalid Link direction: " + newDirection);
        }
        direction = newDirection;
        walkTickCounter = 0;
        walkFrame = 0;
    }

    void accessSetRomDirection(int romDirection) {
        accessSetDirection(Direction.fromRom(romDirection).javaIndex());
    }

    void accessFaceRomDirectionPreservingWalkPhase(int romDirection) {
        direction = Direction.fromRom(romDirection).javaIndex();
    }

    boolean accessGrassGroundEffectFlipX() {
        return (romConsecutiveStepsCount & 8) != 0;
    }

    int accessRomConsecutiveStepsCount() {
        return romConsecutiveStepsCount;
    }

    void accessTickAnimation() {
        walkTickCounter++;
        if (walkTickCounter >= WALK_FRAME_TICKS) {
            walkTickCounter = 0;
            walkFrame ^= 1;
        }
    }
}
