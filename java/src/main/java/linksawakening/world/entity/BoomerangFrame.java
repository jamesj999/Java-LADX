package linksawakening.world.entity;

/** Inputs for EntityProjectileController.advanceBoomerangEntity. */
public record BoomerangFrame(
        int index,
        RoomEntity entity,
        int frame,
        int linkEntityX,
        int linkEntityY,
        int linkEntityZ,
        int linkMotionState,
        boolean handlerInteractive,
        boolean timersInteractive) {}
