package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.event.EntityCombatEvent;
import linksawakening.world.entity.event.SideViewPlatformLinkRequest;
import linksawakening.world.entity.motion.SideViewPlatformMotion;
import org.junit.jupiter.api.Test;

final class SideViewPlatformRuntimeTest {

    @Test
    void room3BContactMovesLinkAndActivatesPlatform() {
        RoomEntityRuntime runtime = runtime(0x3B, platform());
        tick(runtime, 0, 0x48, 0x40, 0x00, 0x00);

        assertEquals(
                List.of(new SideViewPlatformLinkRequest(0, 0, 0x40, 0x02, true, true)),
                runtime.consumePendingSideViewPlatformLinkRequests());
        assertEquals(0x10, EntityRuntimeFixture.inspection(runtime).sideViewPlatformPrivateState2ForTest(0));
        assertEquals(1, EntityRuntimeFixture.inspection(runtime).sideViewPlatformPrivateState4ForTest(0));
        assertEquals(1, EntityRuntimeFixture.inspection(runtime).sideViewPlatformSpeedYForTest(0));
    }

    @Test
    void otherRoomContactPushesLinkButDoesNotActivatePlatform() {
        RoomEntityRuntime runtime = runtime(0x20, platform());
        tick(runtime, 0, 0x48, 0x40, 0x00, 0x00);

        SideViewPlatformLinkRequest request =
                runtime.consumePendingSideViewPlatformLinkRequests().get(0);
        assertEquals(0, request.horizontalDelta());
        assertEquals(0x40, request.positionY());
        assertTrue(request.standing());
        assertTrue(!request.activate());
        assertEquals(0x10, EntityRuntimeFixture.inspection(runtime).sideViewPlatformPrivateState2ForTest(0));
        assertEquals(0, EntityRuntimeFixture.inspection(runtime).sideViewPlatformSpeedYForTest(0));
    }

    @Test
    void carryingEntityActivatesPlatformOutsideRoom3B() {
        RoomEntityRuntime runtime = runtime(
                0x20,
                platform(),
                new linksawakening.world.entity.RoomEntityBuilder()
                        .slot(1)
                        .sourceLoadOrder(1)
                        .type(0x05)
                        .at(0x60, 0x50)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(0x05))
                        .build());
        runtime.beginLift(1, 0);
        // Carry state is established by the existing lifted-entity tick path.
        tick(runtime, 0, 0x48, 0x40, 0x00, 0x00);
        runtime.consumePendingSideViewPlatformLinkRequests();
        tick(runtime, 1, 0x48, 0x40, 0x00, 0x00);

        assertTrue(runtime.consumePendingSideViewPlatformLinkRequests().get(0).activate());
    }

    @Test
    void negativeLinkSpeedSkipsContact() {
        RoomEntityRuntime runtime = runtime(0x3B, platform());
        tick(runtime, 0, 0x48, 0x40, 0x00, 0x80);

        assertTrue(runtime.consumePendingSideViewPlatformLinkRequests().isEmpty());
    }

    @Test
    void noOverlapSkipsContact() {
        RoomEntityRuntime runtime = runtime(0x3B, platform());
        tick(runtime, 0, 0x10, 0x40, 0x00, 0x00);

        assertTrue(runtime.consumePendingSideViewPlatformLinkRequests().isEmpty());
    }

    @Test
    void airborneLinkDoesNotStandOnPlatform() {
        RoomEntityRuntime runtime = runtime(0x3B, platform());
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x48,
                0x40,
                () -> 0,
                null,
                new EnemyProjectileCollision.LinkState(0x48, 0x40, 1, 0, 3, false),
                0x00,
                0x00));
        assertTrue(runtime.consumePendingSideViewPlatformLinkRequests().isEmpty());
    }

    @Test
    void linkBelowPlatformDoesNotStandOnIt() {
        RoomEntityRuntime runtime = runtime(0x3B, platform());
        tick(runtime, 0, 0x48, 0x60, 0x00, 0x00);
        assertTrue(runtime.consumePendingSideViewPlatformLinkRequests().isEmpty());
    }

    @Test
    void exactHorizontalHitboxBoundaryDoesNotContact() {
        RoomEntityRuntime runtime = runtime(0x3B, platform());
        tick(runtime, 0, 0x5C, 0x40, 0x00, 0x00);
        assertTrue(runtime.consumePendingSideViewPlatformLinkRequests().isEmpty());
    }

    @Test
    void nonInteractiveSkipsContact() {
        RoomEntityRuntime runtime = runtime(0x3B, platform());
        EnemyProjectileCollision.LinkState nonInteractive = EnemyProjectileCollision.LinkState.nonInteractive();
        runtime.tick(
                EntityTestFrames.projectileFrame(runtime, 0, 0x48, 0x40, () -> 0, null, nonInteractive, 0x00, 0x00));

        assertTrue(runtime.consumePendingSideViewPlatformLinkRequests().isEmpty());
    }

    @Test
    void blockingMovedPlatformRestoresYStillContacts() {
        RoomEntityRuntime runtime = runtime(0x3B, platform());
        EntityRuntimeFixture.debug(runtime).setSideViewPlatformSpeedYForTest(0, 0x10);
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x48,
                0x40,
                () -> 0,
                (entity, direction, nextX, nextY) -> true,
                new EnemyProjectileCollision.LinkState(0x48, 0x40, 0, 0, 3, false),
                0x00,
                0x00));

        assertEquals(0x50, runtime.inspection().snapshot().slots().get(0).y());
        assertEquals(0x10, EntityRuntimeFixture.inspection(runtime).sideViewPlatformSpeedYForTest(0));
        assertEquals(1, runtime.consumePendingSideViewPlatformLinkRequests().size());
    }

    @Test
    void blockingVerticalMotionStillActivatesAndRampsState() {
        RoomEntityRuntime runtime = runtime(0x3B, platform());
        EntityRuntimeFixture.debug(runtime).setSideViewPlatformSpeedYForTest(0, 0x10);
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x48,
                0x40,
                () -> 0,
                (entity, direction, nextX, nextY) -> true,
                new EnemyProjectileCollision.LinkState(0x48, 0x40, 0, 0, 3, false),
                0x00,
                0x00));
        assertEquals(1, EntityRuntimeFixture.inspection(runtime).sideViewPlatformPrivateState4ForTest(0));
        assertEquals(0x10, EntityRuntimeFixture.inspection(runtime).sideViewPlatformSpeedYForTest(0));
    }

    @Test
    void horizontalBackgroundBlockRestoresXAndQueriesWithPostMoveY() {
        RoomEntityRuntime runtime = runtime(0x3B, platform());
        EntityRuntimeFixture.debug(runtime).setSideViewPlatformSpeedXForTest(0, 0x10);
        EntityRuntimeFixture.debug(runtime).setSideViewPlatformSpeedYForTest(0, 0x10);
        List<int[]> queries = new ArrayList<>();
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x48,
                0x40,
                () -> 0,
                (entity, direction, nextX, nextY) -> {
                    queries.add(new int[] {direction, nextX, nextY});
                    return true;
                },
                new EnemyProjectileCollision.LinkState(0x48, 0x40, 0, 0, 3, false),
                0x00,
                0x00));
        assertEquals(0x40, runtime.inspection().snapshot().slots().get(0).x());
        assertEquals(0x10, EntityRuntimeFixture.inspection(runtime).sideViewPlatformSpeedXForTest(0));
        assertTrue(queries.stream().anyMatch(q -> q[2] == 0x51));
        assertEquals(
                1, runtime.consumePendingSideViewPlatformLinkRequests().get(0).horizontalDelta());
    }

    @Test
    void verticalProbeUsesPostHorizontalPositionOnDiagonalMotion() {
        RoomEntityRuntime runtime = runtime(0x3B, platform());
        EntityRuntimeFixture.debug(runtime).setSideViewPlatformSpeedXForTest(0, 0x10);
        EntityRuntimeFixture.debug(runtime).setSideViewPlatformSpeedYForTest(0, 0x10);
        List<int[]> queries = new ArrayList<>();
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                0,
                0x48,
                0x40,
                () -> 0,
                (entity, direction, nextX, nextY) -> {
                    queries.add(new int[] {direction, nextX, nextY});
                    return queries.size() == 2;
                },
                new EnemyProjectileCollision.LinkState(0x48, 0x40, 0, 0, 3, false),
                0x00,
                0x00));
        assertEquals(2, queries.size());
        assertEquals(0x41, queries.get(1)[1]);
        assertEquals(0x51, queries.get(1)[2]);
    }

    @Test
    void leftwardContactEmitsSignedHorizontalDelta() {
        RoomEntityRuntime runtime = runtime(0x3B, platform());
        EntityRuntimeFixture.debug(runtime).setSideViewPlatformSpeedXForTest(0, 0xF0);
        tick(runtime, 0, 0x48, 0x40, 0x00, 0x00);
        assertEquals(
                -1, runtime.consumePendingSideViewPlatformLinkRequests().get(0).horizontalDelta());
    }

    @Test
    void initPlatformDoesNotMoveOrEmitUntilNextInteractiveTick() {
        RoomEntityRuntime runtime = runtime(0x3B, platform(EntityStatus.INIT));
        tick(runtime, 0, 0x48, 0x40, 0x00, 0x00);
        assertTrue(runtime.consumePendingSideViewPlatformLinkRequests().isEmpty());
        assertEquals(0x50, runtime.inspection().snapshot().slots().get(0).y());
        tick(runtime, 1, 0x48, 0x40, 0x00, 0x00);
        assertEquals(1, runtime.consumePendingSideViewPlatformLinkRequests().size());
    }

    @Test
    void dialogActivePlatformDoesNotMoveOrEmit() {
        RoomEntityRuntime runtime = runtime(0x3B, platform());
        runtime.configuration().setDialogActive(true);
        tick(runtime, 0, 0x48, 0x40, 0x00, 0x00);
        assertTrue(runtime.consumePendingSideViewPlatformLinkRequests().isEmpty());
        assertEquals(0x50, runtime.inspection().snapshot().slots().get(0).y());
    }

    @Test
    void nonGameplayTransitionSequenceBlocksPlatformHandler() {
        RoomEntityRuntime runtime = runtime(0x3B, platform());
        EntityRuntimeFixture.debug(runtime).setTransitionSequenceCounterForTest(0x03);
        tick(runtime, 0, 0x48, 0x40, 0x00, 0x00);
        assertTrue(runtime.consumePendingSideViewPlatformLinkRequests().isEmpty());
        assertEquals(0x50, runtime.inspection().snapshot().slots().get(0).y());
    }

    @Test
    void creditsGameplayBlocksPlatformHandler() {
        RoomEntityRuntime runtime = runtime(0x3B, platform());
        runtime.tick(EntityTestFrames.frame(runtime, 0, 0x48, 0x40, () -> 0, true));
        assertTrue(runtime.consumePendingSideViewPlatformLinkRequests().isEmpty());
        assertEquals(0x50, runtime.inspection().snapshot().slots().get(0).y());
    }

    @Test
    void platformRumbleEmitsNoiseOnFourthStandingFrame() {
        RoomEntityRuntime runtime = runtime(0x3B, platform());
        for (int frame = 0; frame < 4; frame++) {
            tick(runtime, frame, 0x48, 0x40, 0x00, 0x00);
        }
        assertEquals(
                new EntityCombatEvent(
                        0, SideViewPlatformMotion.ENTITY_TYPE, 0, false, EntityCombatEvent.SoundChannel.NOISE, 0x11),
                runtime.consumePendingEntityEvents().get(0));
    }

    @Test
    void horizontalDeltaPropagatesToRequest() {
        RoomEntityRuntime runtime = runtime(0x3B, platform());
        EntityRuntimeFixture.debug(runtime).setSideViewPlatformSpeedXForTest(0, 0x10);
        tick(runtime, 0, 0x48, 0x40, 0x00, 0x00);
        assertEquals(
                1, runtime.consumePendingSideViewPlatformLinkRequests().get(0).horizontalDelta());
    }

    @Test
    void clearingPlatformSlotResetsMotionState() {
        RoomEntityRuntime runtime = runtime(0x3B, platform());
        EntityRuntimeFixture.debug(runtime).setSideViewPlatformSpeedYForTest(0, 0x10);
        EntityRuntimeFixture.debug(runtime).setSideViewPlatformSpeedXForTest(0, 0x10);
        runtime.clearEntity(0);
        assertEquals(0, EntityRuntimeFixture.inspection(runtime).sideViewPlatformSpeedYForTest(0));
        assertEquals(0, EntityRuntimeFixture.inspection(runtime).sideViewPlatformSpeedXForTest(0));
        assertEquals(0, EntityRuntimeFixture.inspection(runtime).sideViewPlatformPrivateState2ForTest(0));
        assertEquals(0, EntityRuntimeFixture.inspection(runtime).sideViewPlatformPrivateState4ForTest(0));
    }

    private static void tick(RoomEntityRuntime runtime, int frame, int linkX, int linkY, int speedX, int speedY) {
        runtime.tick(EntityTestFrames.projectileFrame(
                runtime,
                frame,
                linkX,
                linkY,
                () -> 0,
                null,
                new EnemyProjectileCollision.LinkState(linkX, linkY, 0, 0, 3, false),
                speedX,
                speedY));
    }

    private static RoomEntityRuntime runtime(int roomId, RoomEntity entity) {
        return runtime(roomId, entity, null);
    }

    private static RoomEntityRuntime runtime(int roomId, RoomEntity entity, RoomEntity extra) {
        List<RoomEntity> slots = new ArrayList<>();
        slots.add(entity);
        if (extra != null) {
            slots.add(extra);
        }
        for (int slot = slots.size(); slot < EntityRoomLoader.MAX_ENTITIES; slot++) {
            slots.add(RoomEntity.disabled(slot));
        }
        RoomEntityRuntime runtime =
                RoomEntityRuntime.builder(new RoomEntitySnapshot(slots)).build();
        runtime.configuration().setGroundInteractionSideScrolling(true);
        EntityRuntimeFixture.debug(runtime).setEntityRoomIdForTest(roomId);
        EntityRuntimeFixture.debug(runtime).setTransitionSequenceCounterForTest(0x04);
        return runtime;
    }

    private static RoomEntity platform() {
        return platform(EntityStatus.ACTIVE);
    }

    private static RoomEntity platform(EntityStatus status) {
        return new linksawakening.world.entity.RoomEntityBuilder()
                .sourceLoadOrder(0)
                .type(SideViewPlatformMotion.ENTITY_TYPE)
                .at(0x40, 0x50)
                .status(status)
                .spriteDefinition(EntitySpriteDefinition.unsupported(SideViewPlatformMotion.ENTITY_TYPE))
                .build();
    }
}
