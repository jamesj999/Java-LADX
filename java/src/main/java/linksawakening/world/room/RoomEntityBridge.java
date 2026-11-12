package linksawakening.world.room;

import static linksawakening.world.room.RoomSession.*;

import java.util.List;
import linksawakening.world.entity.EnemyProjectileCollision;
import linksawakening.world.entity.EntityFrameContext;
import linksawakening.world.entity.RoomEntityRuntime;
import linksawakening.world.entity.event.EntityProjectileEvent;
import linksawakening.world.entity.event.WarpLinkStateRequest;
import linksawakening.world.object.SwitchBlockAnimation;

/** Owns room-level entity output queues and their consumption boundary. */
final class RoomEntityBridge {
    RoomEntityRuntime entityRuntime;
    final RoomEntityOutbox outbox = new RoomEntityOutbox(() -> entityRuntime);
    final RoomEntityCombatBridge combat;
    private final RoomEntityFrameSetup frameSetup;
    private final RoomEntityAfterTick afterTick;
    private final RoomSession session;

    RoomEntityBridge(RoomSession session) {
        this.session = session;
        afterTick = new RoomEntityAfterTick(session);
        frameSetup = new RoomEntityFrameSetup(session);
        combat = new RoomEntityCombatBridge(session);
    }

    void setEnemyDropPlayerState(int maxHearts, int health, boolean activePowerUp) {
        if (maxHearts < 0 || maxHearts > 0xFF) {
            throw new IllegalArgumentException("Maximum hearts must be an unsigned byte: " + maxHearts);
        }
        if (health < 0 || health > 0xFF) {
            throw new IllegalArgumentException("Health must be an unsigned byte: " + health);
        }
        session.context.enemyDropMaxHearts = maxHearts;
        session.context.enemyDropHealth = health;
        session.context.enemyDropLowHealth = null;
        session.context.enemyDropActivePowerUp = activePowerUp;
        session.configureEnemyDropRuntime();
    }

    void advanceBoomerangSoundClock(boolean worldGameplay) {
        if (worldGameplay) {
            session.context.boomerangSoundCounter =
                    (session.context.boomerangSoundCounter + 1) % RoomEntityRuntime.BOOMERANG_SFX_COUNTER_PERIOD;
        }
        // Main may run several world frames without an entity pass during a
        // transition. The next pass must use that clock without ticking twice.
        session.context.boomerangSoundClockProvided = true;
    }

    void tickEntities(int frameCounter) {
        session.tickEntities(frameCounter, 0, 0);
    }

    void tickEntities(int frameCounter, int linkEntityX, int linkEntityY) {
        session.tickEntities(
                frameCounter,
                linkEntityX,
                linkEntityY,
                session.context.followingLinkZ,
                session.context.followingLinkDirection);
    }

    void tickEntities(int frameCounter, int linkEntityX, int linkEntityY, int linkEntityZ, int linkDirection) {
        session.tickEntitiesWithProjectileEvents(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkEntityZ,
                EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE,
                linkDirection,
                false);
    }

    void tickEntities(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            boolean linkAirborne,
            int linkDirection) {
        session.tickEntitiesWithProjectileEvents(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkEntityZ,
                linkAirborne,
                EnemyProjectileCollision.LINK_MOTION_NON_INTERACTIVE,
                linkDirection,
                false);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            int linkDirection,
            boolean usingShield) {
        return session.tickEntitiesWithProjectileEvents(
                frameCounter, linkEntityX, linkEntityY, linkEntityZ, linkMotionState, linkDirection, usingShield, 1, 0);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            boolean linkAirborne,
            int linkMotionState,
            int linkDirection,
            boolean usingShield) {
        return session.tickEntitiesWithProjectileEvents(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkEntityZ,
                linkAirborne,
                linkMotionState,
                linkDirection,
                0,
                usingShield,
                1,
                0,
                false,
                0,
                0,
                0,
                0,
                0,
                0);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
            int frameCounter,
            int linkEntityX,
            int linkEntityY,
            int linkEntityZ,
            int linkMotionState,
            int linkDirection,
            boolean usingShield,
            int shieldLevel,
            int invincibilityCounter) {
        return session.tickEntitiesWithProjectileEvents(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkEntityZ,
                linkMotionState,
                linkDirection,
                usingShield,
                shieldLevel,
                invincibilityCounter,
                false,
                0,
                0,
                0,
                0);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
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
        return session.tickEntitiesWithProjectileEvents(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkEntityZ,
                linkMotionState,
                linkDirection,
                0,
                usingShield,
                shieldLevel,
                invincibilityCounter,
                swordCollisionActive,
                swordX,
                swordWidth,
                swordY,
                swordHeight);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
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
        return session.tickEntitiesWithProjectileEvents(
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
                0,
                0);
    }

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
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
        return session.tickEntitiesWithProjectileEvents(
                frameCounter,
                linkEntityX,
                linkEntityY,
                linkEntityZ,
                false,
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

    List<EntityProjectileEvent> tickEntitiesWithProjectileEvents(
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
        session.context.followingLinkX = linkEntityX & 0xFF;
        session.context.followingLinkY = linkEntityY & 0xFF;
        session.context.followingLinkZ = linkEntityZ & 0xFF;
        session.context.followingLinkDirection = linkDirection & 0xFF;
        session.context.currentLinkMotionState = linkMotionState & 0xFF;
        session.context.shouldGetLostInMysteriousWoods = false;
        if (session.context.activeRoom == null || session.context.entityBridge.entityRuntime == null) {
            return List.of();
        }
        frameSetup.configure();
        // rLY is not a meaningful value in the host renderer. Keep the
        // non-emulator policy explicit while preserving the ROM seed update.
        session.context.entityRandomByteSource.beginFrame(frameCounter & 0xFF, 0);
        if (session.context.pendingShovelDrop != null) {
            PendingShovelDrop drop = session.context.pendingShovelDrop;
            session.context.pendingShovelDrop = null;
            session.context.entityBridge.entityRuntime.spawnShovelDrop(
                    drop.objectLeft(), drop.objectTop(), drop.linkEntityX(), drop.linkEntityY(), drop.allowDrop());
        }
        List<EntityProjectileEvent> events = session.context.entityBridge.entityRuntime.tick(new EntityFrameContext(
                frameCounter,
                linkEntityX,
                linkEntityY,
                collisionType & 0xFF,
                session.context.entityRandomByteSource,
                session::entityBackgroundCollision,
                session::pairoddProjectileObjectCollision,
                session.context.followingLinkPositionHistory,
                session.context.followingLinkZ,
                linkAirborne,
                session.context.followingLinkDirection,
                session.context.followingEntityYOffset,
                new EnemyProjectileCollision.LinkState(
                        linkEntityX,
                        linkEntityY,
                        linkEntityZ,
                        linkMotionState,
                        session.romDirectionForProjectileCollision(linkDirection),
                        usingShield,
                        shieldLevel,
                        invincibilityCounter),
                swordCollisionActive,
                swordX,
                swordWidth,
                swordY,
                swordHeight,
                linkSpeedX,
                linkSpeedY));
        afterTick.complete(linkEntityX, linkEntityY);
        return events;
    }

    List<WarpLinkStateRequest> consumeWarpLinkStateRequests() {
        List<WarpLinkStateRequest> requests = session.context.entityBridge.entityRuntime == null
                ? List.of()
                : session.context.entityBridge.entityRuntime.consumePendingWarpLinkStateRequests();
        if (!requests.isEmpty()) {
            session.context.ocarinaPlaybackCountdown = 0;
            session.context.ocarinaAnimationCounter = 0;
            session.context.ocarinaAnimationPhase = 0;
            session.context.entityInputs.entityDialogActive = false;
            session.context.entityInputs.entityDialogCooldown = 0;
            session.context.entityInputs.entityInventoryAppearing = false;
        }
        return requests;
    }

    boolean consumeWorldLinkMotionBlockRequest() {
        boolean pending = session.context.worldLinkMotionBlockPending;
        session.context.worldLinkMotionBlockPending = false;
        return pending;
    }

    void tickGameplayVBlank() {
        if (SwitchBlockAnimation.isAnimating(session.context.switchableObjectAnimationStage)) {
            SwitchBlockAnimation.Step step = SwitchBlockAnimation.advance(
                    session.context.switchableObjectAnimationStage, session.context.switchBlocksState);
            session.context.switchBlocksState = step.switchBlocksState();
            session.context.switchableObjectAnimationStage = step.nextStage();
            session.synchronizeSwitchBlockCollisionState();
            if (step.tileCopy() != null) {
                session.context.gpu.copySwitchBlockTiles(
                        session.context.romData,
                        step.tileCopy().sourceOffset(),
                        step.tileCopy().destinationTile());
            }
            // AnimateTiles returns after UpdateSwitchBlockTiles while the
            // switch animation is active, so ordinary animated BG tiles wait
            // until the next idle VBlank.
            return;
        }
        session.context.gpu.tickAnimatedTiles(session.context.romData);
    }
}
