package linksawakening.world.room;

public record LinkInputs(
        LinkPosition position,
        LinkMotion motion,
        int attackCountdown,
        int finalX,
        boolean freeMovement,
        int velocityZ) {}
