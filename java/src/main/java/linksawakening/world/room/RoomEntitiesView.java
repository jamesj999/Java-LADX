package linksawakening.world.room;

import java.util.List;
import linksawakening.world.entity.EnemyCombatInputs;
import linksawakening.world.entity.EntityPickupEvent;
import linksawakening.world.entity.EntityPickupFrameInputs;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;

final class RoomEntitiesView implements RoomEntities {
    private final Dependencies dependencies;

    RoomEntitiesView(Dependencies dependencies) {
        this.dependencies = dependencies;
    }

    @Override
    public boolean inBossBattle() {
        return dependencies.session().inBossBattle();
    }

    @Override
    public EntityPickupEvent collectEntityIfNeeded(
            int frameCounter, int linkEntityX, int linkEntityY, boolean linkAirborne, boolean linkInteractive) {
        return dependencies
                .session()
                .collectEntityIfNeeded(frameCounter, linkEntityX, linkEntityY, linkAirborne, linkInteractive);
    }

    @Override
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

    @Override
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

    @Override
    public EntityPickupEvent collectEntityIfNeeded(EntityPickupFrameInputs inputs) {
        return dependencies.session().collectEntityIfNeeded(inputs);
    }

    @Override
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

    @Override
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

    @Override
    public List<EntityCombatEvent> resolveEntityCombat(EnemyCombatInputs inputs) {
        return dependencies.session().resolveEntityCombat(inputs);
    }

    @Override
    public void tickEntities(int frameCounter) {
        dependencies.session().tickEntities(frameCounter);
    }

    @Override
    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY) {
        dependencies.session().tickEntities(frameCounter, linkEntityX, linkEntityY);
    }

    @Override
    public void tickEntities(int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkDirection) {
        dependencies.session().tickEntities(frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkDirection);
    }

    @Override
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

    @Override
    public List<EntityProjectileEvent> tickEntities(EntityTickValues inputs) {
        return dependencies.session().tickEntities(inputs);
    }

    record Dependencies(RoomSession session) {
        Dependencies {}
    }
}
