package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
import linksawakening.world.entity.EnemyAttackContext;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;

public interface RoomEntitySimulationCompatibility1 {
    RoomSession facade();
    /** @deprecated Use {@code entities().tickEntitiesWithProjectileEvents()}. */
    @Deprecated(forRemoval = true)
    default List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            int linkDirection,
            boolean usingShield) {
        return facade().entities()
                .tickEntitiesWithProjectileEvents(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        linkMotionState,
                        linkDirection,
                        usingShield);
    }

    /** @deprecated Use {@code entities().tickEntitiesWithProjectileEvents()}. */
    @Deprecated(forRemoval = true)
    default List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            boolean linkAirborne,
            int linkMotionState,
            int linkDirection,
            boolean usingShield) {
        return facade().entities()
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

    /** @deprecated Use {@code entities().resolveEntityCombat(EntityCombatInputs)}. Retained for java/src/test/java/linksawakening/world/entity/SlimeEyeRuntimeTest.java. */
    @Deprecated(forRemoval = true)
    default List<EntityCombatEvent> resolveEntityCombat(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            boolean linkInteractive,
            boolean swordCollisionActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight,
            EnemyAttackContext attackContext) {
        return facade().entities()
                .resolveEntityCombat(new EntityCombatInputs(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkAirborne,
                        0,
                        linkInteractive,
                        new SwordBox(swordCollisionActive, swordX, swordWidth, swordY, swordHeight),
                        attackContext));
    }

    /** @deprecated Use {@code entities().resolveEntityCombat(EntityCombatInputs)}. Retained for java/src/main/java/linksawakening/app/gameplay/CombatPhase.java. */
    @Deprecated(forRemoval = true)
    default List<EntityCombatEvent> resolveEntityCombat(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            boolean linkAirborne,
            int linkVerticalVelocity,
            boolean linkInteractive,
            boolean swordCollisionActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight,
            EnemyAttackContext attackContext) {
        return facade().entities()
                .resolveEntityCombat(new EntityCombatInputs(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkAirborne,
                        linkVerticalVelocity,
                        linkInteractive,
                        new SwordBox(swordCollisionActive, swordX, swordWidth, swordY, swordHeight),
                        attackContext));
    }

    /** @deprecated Use {@code entities().resolveEntityCombat()}. */
    @Deprecated(forRemoval = true)
    default List<EntityCombatEvent> resolveEntityCombat(EntityCombatInputs inputs) {
        return facade().entities().resolveEntityCombat(inputs);
    }
}
