package linksawakening.world.entity.motion;

/** Inputs for LiftedEntityMotion.advance. */
public record LiftedEntityFrame(
        int phase,
        int transitionCountdown,
        int sourceRomDirection,
        int currentRomDirection,
        int linkX,
        int linkY,
        int linkZ,
        int linkC13B,
        int entityType,
        boolean sideScrolling,
        boolean fastTransition,
        int existingEntityZ) {}
