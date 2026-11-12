package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;
import static linksawakening.world.room.RoomSessionRules.*;

import java.util.List;
import linksawakening.world.entity.EnemyAttackContext;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.EntityProjectileEvent;

interface RoomEntitySimulationCompatibility1 {
    RoomSession facade();
    /**
     * Advances entities and returns the ROM projectile interactions generated
     * during this frame.  The older tick overloads route through this method
     * with a non-interactive Link state, so callers that do not provide player
     * interaction state retain their previous behavior.
     */
    default List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            int linkDirection,
            boolean usingShield) {
        return facade().context
                .entityBridge
                .tickEntitiesWithProjectileEvents(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        linkMotionState,
                        linkDirection,
                        usingShield);
    }

    default List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            boolean linkAirborne,
            int linkMotionState,
            int linkDirection,
            boolean usingShield) {
        return facade().context
                .entityBridge
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
    /** Advances entities with the additional ROM shield and invincibility fields. */
    default List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            int linkDirection,
            boolean usingShield,
            int shieldLevel,
            int invincibilityCounter) {
        return facade().context
                .entityBridge
                .tickEntitiesWithProjectileEvents(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        linkMotionState,
                        linkDirection,
                        usingShield,
                        shieldLevel,
                        invincibilityCounter);
    }
    /** Advances entities with Link's optional current sword collision rectangle. */
    default List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            int linkDirection,
            boolean usingShield,
            int shieldLevel,
            int invincibilityCounter,
            boolean swordCollisionActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight) {
        return facade().context
                .entityBridge
                .tickEntitiesWithProjectileEvents(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        linkMotionState,
                        linkDirection,
                        usingShield,
                        shieldLevel,
                        invincibilityCounter,
                        swordCollisionActive,
                        swordX,
                        swordWidth,
                        swordY,
                        swordHeight);
    }
    /** Advances entities with Link's current ROM collision byte. */
    default List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            int linkDirection,
            int collisionType,
            boolean usingShield,
            int shieldLevel,
            int invincibilityCounter,
            boolean swordCollisionActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight) {
        return facade().context
                .entityBridge
                .tickEntitiesWithProjectileEvents(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        linkMotionState,
                        linkDirection,
                        collisionType,
                        usingShield,
                        shieldLevel,
                        invincibilityCounter,
                        swordCollisionActive,
                        swordX,
                        swordWidth,
                        swordY,
                        swordHeight);
    }
    /** Advances entities with the current Link speed bytes for enemy-bomb recoil. */
    default List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            int linkDirection,
            int collisionType,
            boolean usingShield,
            int shieldLevel,
            int invincibilityCounter,
            boolean swordCollisionActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight,
            int linkSpeedX,
            int linkSpeedY) {
        return facade().context
                .entityBridge
                .tickEntitiesWithProjectileEvents(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        linkMotionState,
                        linkDirection,
                        collisionType,
                        usingShield,
                        shieldLevel,
                        invincibilityCounter,
                        swordCollisionActive,
                        swordX,
                        swordWidth,
                        swordY,
                        swordHeight,
                        linkSpeedX,
                        linkSpeedY);
    }
    /** Rich live path with Link's explicit airborne state. */
    default List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            boolean linkAirborne,
            int linkMotionState,
            int linkDirection,
            int collisionType,
            boolean usingShield,
            int shieldLevel,
            int invincibilityCounter,
            boolean swordCollisionActive,
            int swordX,
            int swordWidth,
            int swordY,
            int swordHeight,
            int linkSpeedX,
            int linkSpeedY) {
        return facade().context
                .entityBridge
                .tickEntitiesWithProjectileEvents(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        linkAirborne,
                        linkMotionState,
                        linkDirection,
                        collisionType,
                        usingShield,
                        shieldLevel,
                        invincibilityCounter,
                        swordCollisionActive,
                        swordX,
                        swordWidth,
                        swordY,
                        swordHeight,
                        linkSpeedX,
                        linkSpeedY);
    }
    /** Applies the active room's ROM enemy/sword collision pass. */
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
            int swordHeight) {
        return facade().context
                .entityBridge
                .combat
                .resolveEntityCombat(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkAirborne,
                        linkInteractive,
                        swordCollisionActive,
                        swordX,
                        swordWidth,
                        swordY,
                        swordHeight);
    }

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
        return facade().context
                .entityBridge
                .combat
                .resolveEntityCombat(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkAirborne,
                        linkInteractive,
                        swordCollisionActive,
                        swordX,
                        swordWidth,
                        swordY,
                        swordHeight,
                        attackContext);
    }

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
        return facade().context
                .entityBridge
                .combat
                .resolveEntityCombat(
                        frameCounter,
                        linkEntityX,
                        linkEntityY,
                        linkAirborne,
                        linkVerticalVelocity,
                        linkInteractive,
                        swordCollisionActive,
                        swordX,
                        swordWidth,
                        swordY,
                        swordHeight,
                        attackContext);
    }
}
