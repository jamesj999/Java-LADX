package linksawakening.world.room;

/**
 * Read-only entity input port. Immutable EntityFrameInputs may be retained; app captures are
 * borrowed until the next capture by their factory. Room consumers copy registers before returning.
 */
public interface EntityFrameValues extends EntityTickValues {
    Link link();

    PlayerInputs player();

    UiInputs ui();

    ButtonInputs buttons();

    @Override
    default Position position() {
        return link().position();
    }

    @Override
    default Motion motion() {
        return link().motion();
    }

    @Override
    default int shieldLevel() {
        return player().levels().shield();
    }

    interface Link {
        Position position();

        Motion motion();

        int attackCountdown();

        int finalX();

        boolean freeMovement();

        int velocityZ();
    }

    interface Position {
        int x();

        int y();

        int z();

        int rawDirection();

        int collisionType();
    }

    interface Motion {
        boolean airborne();

        int rawState();

        boolean shield();

        int invincibility();

        int speedX();

        int speedY();
    }
}
