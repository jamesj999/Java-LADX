package linksawakening.world.entity;

import static linksawakening.world.entity.EntityMotionMath.*;
import static linksawakening.world.entity.EntityRomConstants.*;
import static linksawakening.world.entity.EntitySlotValidation.*;
import static linksawakening.world.entity.EntitySnapshots.*;
import static linksawakening.world.entity.EntityTypeRules.*;

import linksawakening.world.entity.motion.*;

/** Source-compatible room facade capability backed by focused entity services. */
interface EntityRuntimePlayerCommands {
    EntityPlayerCommands entityPlayerCommands();

    /**
     * Starts EntityGetLiftedUp's shared state transition. Directions are the
     * ROM order (right, left, up, down); RoomSession performs the Java-to-ROM
     * conversion at its boundary.
     */
    default boolean beginLift(int slot, int romDirection) {
        return entityPlayerCommands().beginLift(slot, romDirection);
    }

    /** Hands the fully held entity to func_014_53A3's generic throw path. */
    default boolean throwLiftedEntity(int romDirection) {
        return entityPlayerCommands().throwLiftedEntity(romDirection);
    }

    /** Applies the position and variant writes made by MarinEntityHandler_Indoor. */
    default boolean applyMarinWakeUpPresentation(int x, int y, int spriteVariant) {
        return entityPlayerCommands().applyMarinWakeUpPresentation(x, y, spriteVariant);
    }

    default void updateHouseTarinPresentation(int linkX, int linkY) {
        entityPlayerCommands().updateHouseTarinPresentation(linkX, linkY);
    }

    /** Bank $15:7C37..7CA5: two death waits, then DidKillEnemy. */
    default void startSwordPickup(int slot) {
        entityPlayerCommands().startSwordPickup(slot);
    }

    default void startToadstoolPickup(int slot) {
        entityPlayerCommands().startToadstoolPickup(slot);
    }

    default void presentWitchGotItem(int slot) {
        entityPlayerCommands().presentWitchGotItem(slot);
    }

    /** ProcessPowerUp ($03:6400) and ApplyGotItem ($00:1033). */
    default void holdSwordAboveLink(RoomEntity entity, int linkEntityX, int linkEntityY, int linkZ, boolean finalPose) {
        entityPlayerCommands().holdSwordAboveLink(entity, linkEntityX, linkEntityY, linkZ, finalPose);
    }
}
