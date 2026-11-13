package linksawakening.world.room;

import java.util.List;
import linksawakening.world.entity.EntityPickupEvent;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;

final class RoomEntitiesView implements RoomEntities {
    private final Dependencies dependencies;

    RoomEntitiesView(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    public boolean inBossBattle() {
        return dependencies.session().inBossBattle();
    }

    public EntityPickupEvent collectEntityIfNeeded(
            int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive) {
        return dependencies
                .session()
                .collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive);
    }

    public EntityPickupEvent collectEntityIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection) {
        return dependencies
                .session()
                .collectEntityIfNeeded(
                        frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection);
    }

    public EntityPickupEvent collectEntityIfNeeded(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            int linkDirection,
            int linkZ) {
        return dependencies
                .session()
                .collectEntityIfNeeded(
                        frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive, linkDirection, linkZ);
    }

    public EntityPickupEvent collectEntityIfNeeded(EntityPickupInputs inputs) {
        return dependencies.session().collectEntityIfNeeded(inputs);
    }

    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            int linkDirection,
            boolean usingShield) {
        return dependencies
                .session()
                .tickEntitiesWithProjectileEvents(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        linkMotionState,
                        linkDirection,
                        usingShield);
    }

    public List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            boolean linkAirborne,
            int linkMotionState,
            int linkDirection,
            boolean usingShield) {
        return dependencies
                .session()
                .tickEntitiesWithProjectileEvents(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        linkAirborne,
                        linkMotionState,
                        linkDirection,
                        usingShield);
    }

    public List<EntityCombatEvent> resolveEntityCombat(EntityCombatInputs inputs) {
        return dependencies.session().resolveEntityCombat(inputs);
    }

    public void tickEntities(int frameCounter) {
        dependencies.session().tickEntities(frameCounter);
    }

    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY) {
        dependencies.session().tickEntities(frameCounter, linkEntityX, linkEntityY);
    }

    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkDirection) {
        dependencies.session().tickEntities(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkDirection);
    }

    public void tickEntities(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            boolean linkAirborne,
            int linkDirection) {
        dependencies
                .session()
                .tickEntities(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkAirborne, linkDirection);
    }

    public List<EntityProjectileEvent> tickEntities(EntityTickInputs inputs) {
        return dependencies.session().tickEntities(inputs);
    }

    record Dependencies(RoomSession session) {
        Dependencies {}
    }
}
