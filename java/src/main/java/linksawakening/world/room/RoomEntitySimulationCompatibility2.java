package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.world.entity.EntityPickupEvent;

public interface RoomEntitySimulationCompatibility2 {
    RoomSession facade();
    /** @deprecated Use {@code entities().collectEntityIfNeeded()}. */
    @Deprecated(forRemoval = true)
    default EntityPickupEvent collectEntityIfNeeded(
            int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive) {
        return facade().entities()
                .collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive);
    }

    /** @deprecated Use {@code entities().collectEntityIfNeeded()}. */
    @Deprecated(forRemoval = true)
    default EntityPickupEvent collectEntityIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection) {
        return facade().entities()
                .collectEntityIfNeeded(
                        frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection);
    }

    /** @deprecated Use {@code entities().collectEntityIfNeeded()}. */
    @Deprecated(forRemoval = true)
    default EntityPickupEvent collectEntityIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection,
            int linkZ) {
        return facade().entities()
                .collectEntityIfNeeded(
                        frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection, linkZ);
    }

    /** @deprecated Use {@code entities().collectEntityIfNeeded(EntityPickupInputs)}. Retained for java/src/main/java/linksawakening/app/gameplay/LinkEquipmentPhase.java. */
    @Deprecated(forRemoval = true)
    default EntityPickupEvent collectEntityIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection,
            int linkZ,
            boolean swordActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight) {
        return facade().entities()
                .collectEntityIfNeeded(new EntityPickupInputs(
                        frameCounter,
                        new EntityPickupInputs.Link(
                                linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection, linkZ),
                        new SwordBox(swordActive, swordX, swordWidth, swordY, swordHeight)));
    }

    /** @deprecated Use {@code entities().collectEntityIfNeeded()}. */
    @Deprecated(forRemoval = true)
    default EntityPickupEvent collectEntityIfNeeded(EntityPickupInputs inputs) {
        return facade().entities().collectEntityIfNeeded(inputs);
    }
}
