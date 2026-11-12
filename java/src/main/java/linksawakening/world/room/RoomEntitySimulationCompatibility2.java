package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

import linksawakening.world.entity.EntityPickupEvent;

interface RoomEntitySimulationCompatibility2 {
    RoomSession facade();
    /** Link coordinates use ROM entity origins, not sprite top-left; direction uses Link constants. */
    default EntityPickupEvent collectEntityIfNeeded(
            int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive) {
        return facade().context
                .entityBridge
                .combat
                .collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive);
    }

    default EntityPickupEvent collectEntityIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection) {
        return facade().context
                .entityBridge
                .combat
                .collectEntityIfNeeded(
                        frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection);
    }

    default EntityPickupEvent collectEntityIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection,
            int linkZ) {
        return facade().context
                .entityBridge
                .combat
                .collectEntityIfNeeded(
                        frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection, linkZ);
    }

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
        return facade().context
                .entityBridge
                .combat
                .collectEntityIfNeeded(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkAirborne,
                        linkInteractive,
                        linkDirection,
                        linkZ,
                        swordActive,
                        swordX,
                        swordWidth,
                        swordY,
                        swordHeight);
    }
}
