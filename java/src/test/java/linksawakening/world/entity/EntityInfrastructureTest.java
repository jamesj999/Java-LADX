package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.function.IntSupplier;
import linksawakening.world.entity.event.LinkAttackStepRequest;
import linksawakening.world.entity.event.LinkFacingRequest;
import linksawakening.world.entity.event.ScreenShakeRequest;
import linksawakening.world.entity.motion.CrowMotion;
import org.junit.jupiter.api.Test;

class EntityInfrastructureTest {
    @Test
    void eventChannelViewsCannotBypassOrderedEmissionAndDraining() {
        var channels = new EntityEventChannels();
        var effects = new EntityEffectConsumption(channels, new EntityPresentationState(), new EntityRoomVariables());
        var live = channels.pendingEntityEvents();
        channels.sound(2, 0x42, linksawakening.world.entity.event.EntityCombatEvent.SoundChannel.JINGLE, 5);
        channels.preTickCombat(new linksawakening.world.entity.event.EntityCombatEvent(
                7, 0x42, 0, false, linksawakening.world.entity.event.EntityCombatEvent.SoundChannel.WAVE, 6));
        channels.sound(1, 0x42, linksawakening.world.entity.event.EntityCombatEvent.SoundChannel.NOISE, 8);
        assertEquals(2, live.size());
        assertThrows(UnsupportedOperationException.class, live::clear);
        var owned = effects.consumePendingEntityEvents();
        assertEquals(
                List.of(7, 2, 1),
                owned.stream()
                        .map(linksawakening.world.entity.event.EntityCombatEvent::slot)
                        .toList());
        assertTrue(live.isEmpty());
        assertTrue(effects.consumePendingEntityEvents().isEmpty());
        assertEquals(3, owned.size());
    }

    @Test
    void acquisitionReusesGroupsAndReentrantExecutionOwnsSeparateScratch() {
        var runtime = EntityRuntimeFixture.emptyRuntime();
        var execution = runtime.execution();
        var context = EntityTestFrames.frame(runtime, 0);
        var acquired = execution.acquire(context);
        var slot = acquired.slotFrame();
        var inputs = acquired.inputs();
        var timers = acquired.gates();
        var presentation = acquired.presentation();
        slot.index(9);
        inputs.frameCounter(44);
        timers.freezeGenieTimers(true);
        presentation.preserveGhiniPresentation(true);

        var nested = execution.acquire(context);
        assertNotSame(acquired, nested);
        assertNotSame(slot, nested.slotFrame());
        assertNotSame(inputs, nested.inputs());
        assertNotSame(timers, nested.gates());
        assertNotSame(presentation, nested.presentation());
        assertEquals(0, nested.slotFrame().index());
        assertEquals(0, nested.inputs().frameCounter());
        assertFalse(nested.gates().freezeGenieTimers());
        assertFalse(nested.presentation().preserveGhiniPresentation());
        assertEquals(9, slot.index());
        assertEquals(44, inputs.frameCounter());
        assertTrue(timers.freezeGenieTimers());
        assertTrue(presentation.preserveGhiniPresentation());
        nested.release();
        acquired.release();

        var reused = execution.acquire(context);
        assertSame(acquired, reused);
        assertSame(slot, reused.slotFrame());
        assertSame(inputs, reused.inputs());
        assertSame(timers, reused.gates());
        assertSame(presentation, reused.presentation());
        assertEquals(0, slot.index());
        assertEquals(0, inputs.frameCounter());
        assertFalse(timers.freezeGenieTimers());
        assertFalse(presentation.preserveGhiniPresentation());
        reused.release();
    }

    @Test
    void dispatchSiteKeepsRepeatedVisitsAndStopsAtTheFirstEarlyReturn() {
        EntityHandlerRegistry registry = new EntityHandlerRegistry();
        EntityFrameExecution frame = EntityRuntimeFixture.emptyRuntime().execution();
        frame.slotFrame().entity(RoomEntity.disabled(15));
        var visited = new ArrayList<Integer>();
        var site = EntityTickStage.PHASE_1_BEFORE_PICKUP_SEQUENCE;
        registry.registerTick(255, site, 2, actual -> {
            visited.add(2);
            return false;
        });
        registry.registerTick(255, site, 0, actual -> {
            visited.add(0);
            return true;
        });
        registry.registerTick(255, site, 1, actual -> {
            visited.add(1);
            return true;
        });
        registry.registerTick(255, site, 3, actual -> {
            fail("Visit after early return");
            return true;
        });
        assertFalse(registry.tick(site, frame));
        assertEquals(List.of(0, 1, 2), visited);
    }

    @Test
    void dispatchSiteRereadsEntityTypeBetweenVisits() {
        EntityHandlerRegistry registry = new EntityHandlerRegistry();
        EntityFrameExecution frame = EntityRuntimeFixture.emptyRuntime().execution();
        frame.slotFrame().entity(RoomEntity.disabled(15));
        var visited = new ArrayList<Integer>();
        var site = EntityTickStage.PHASE_1_BEFORE_STATUS_TIMERS;
        registry.registerTick(254, site, 0, actual -> {
            fail("Earlier visits must not repeat");
            return true;
        });
        registry.registerTick(255, site, 0, actual -> {
            actual.slotFrame()
                    .entity(EntitySnapshots.withType(
                            actual.slotFrame().entity(),
                            254,
                            actual.slotFrame().entity().spriteDefinition()));
            visited.add(0);
            return true;
        });
        registry.registerTick(254, site, 1, actual -> {
            visited.add(1);
            return true;
        });
        assertTrue(registry.tick(site, frame));
        assertEquals(List.of(0, 1), visited);
        assertEquals(15, frame.slotFrame().entity().slot());
    }

    @Test
    void motionRegistryRetainsTheFirstInstanceForEachType() {
        EntityMotionRegistry registry = new EntityMotionRegistry();
        var motion = registry.get(CrowMotion.class, CrowMotion::new);
        assertSame(motion, registry.get(CrowMotion.class, () -> {
            throw new AssertionError("A motion must not be recreated");
        }));
    }

    @Test
    void motionRegistriesDoNotShareStateBetweenRooms() {
        EntityMotionRegistry firstRoom = new EntityMotionRegistry();
        EntityMotionRegistry secondRoom = new EntityMotionRegistry();
        assertNotSame(
                firstRoom.get(CrowMotion.class, CrowMotion::new), secondRoom.get(CrowMotion.class, CrowMotion::new));
    }

    @Test
    void outboxKeepsStagedAndLiveEventsSeparate() {
        EntityOutbox outbox = new EntityOutbox();
        var staged = outbox.channel(LinkAttackStepRequest.class, "staged");
        var live = outbox.channel(LinkAttackStepRequest.class, "live");
        staged.add(new LinkAttackStepRequest(3));
        live.add(new LinkAttackStepRequest(7));
        assertEquals(List.of(new LinkAttackStepRequest(7)), List.copyOf(live));
        live.clear();
        live.addAll(staged);
        staged.clear();
        assertEquals(List.of(new LinkAttackStepRequest(3)), List.copyOf(live));
        assertTrue(staged.isEmpty());
    }

    @Test
    void registryRejectsDuplicateAndInvalidRomTypes() {
        EntityHandlerRegistry registry = new EntityHandlerRegistry();
        registry.register(255, (entity, random) -> {});
        assertTrue(registry.supports(255));
        assertFalse(registry.supports(0));
        assertFalse(registry.supports(256));
        assertThrows(IllegalArgumentException.class, () -> registry.register(255, (e, r) -> {}));
        assertThrows(IndexOutOfBoundsException.class, () -> registry.register(256, (e, r) -> {}));
    }

    @Test
    void registryPreservesSlotAndRandomServiceIdentity() {
        EntityHandlerRegistry registry = new EntityHandlerRegistry();
        RoomEntity entity = RoomEntity.disabled(15);
        IntSupplier random = () -> 123;
        registry.register(255, (actual, source) -> {
            assertSame(entity, actual);
            assertSame(random, source);
            assertEquals(15, actual.slot());
        });
        registry.initialize(entity, random);
    }

    @Test
    void typedDrainLeavesOtherEffectsAndChannelsPending() {
        EntityOutbox outbox = new EntityOutbox();
        var facing = outbox.channel(LinkFacingRequest.class, "effects");
        var shake = outbox.channel(ScreenShakeRequest.class, "effects");
        var staged = outbox.channel(LinkFacingRequest.class, "staged");
        for (int slot = 0; slot < 3; slot++) {
            facing.add(new LinkFacingRequest(slot, 2));
            shake.add(new ScreenShakeRequest(slot, 8, 0));
            staged.add(new LinkFacingRequest(slot, 1));
        }
        var expected = List.copyOf(facing);
        var pendingShake = List.copyOf(shake);
        var pendingStaged = List.copyOf(staged);
        var drained = outbox.drain(LinkFacingRequest.class, "effects");
        assertEquals(expected, drained);
        assertThrows(UnsupportedOperationException.class, () -> drained.clear());
        assertTrue(outbox.drain(LinkFacingRequest.class, "effects").isEmpty());
        facing.add(new LinkFacingRequest(4, 1));
        assertEquals(expected, drained);
        assertEquals(pendingShake, outbox.drain(ScreenShakeRequest.class, "effects"));
        assertEquals(pendingStaged, outbox.drain(LinkFacingRequest.class, "staged"));
    }

    @Test
    void registryKeepsStageRoutingWhenAdvanceIsAddedLater() {
        EntityHandlerRegistry registry = new EntityHandlerRegistry();
        EntityFrameExecution frame = EntityRuntimeFixture.emptyRuntime().execution();
        frame.slotFrame().entity(RoomEntity.disabled(15));
        var stage = EntityTickStage.PHASE_1_BEFORE_STATUS_TIMERS;
        registry.registerTick(255, stage, actual -> {
            assertSame(frame, actual);
            return false;
        });
        registry.registerAdvance(255, (entity, context) -> entity);
        assertFalse(registry.tick(stage, frame));
        assertTrue(registry.tick(EntityTickStage.PHASE_1_BEFORE_BOSS_INTRO, frame));
        assertSame(
                frame.slotFrame().entity(), registry.advance(frame.slotFrame().entity(), null));
        assertThrows(IllegalArgumentException.class, () -> registry.registerTick(255, stage, actual -> true));
        assertThrows(IllegalArgumentException.class, () -> registry.registerAdvance(255, (entity, context) -> entity));
    }

    @Test
    void effectiveFrameCopyPreservesInputAndServiceIdentity() {
        IntSupplier random = () -> 91;
        RoomEntityBackgroundCollision collision = (entity, direction, x, y) -> false;
        EntityFrameContext original = EntityFrameContext.builder()
                .frameCounter(4)
                .linkEntityX(10)
                .linkEntityY(20)
                .collisionType(0)
                .randomByteSupplier(random)
                .backgroundCollision(collision)
                .objectCollision(linksawakening.world.entity.RoomEntityObjectCollision.NONE)
                .linkPositionHistory(null)
                .linkZ(0)
                .linkAirborne(false)
                .linkDirection(2)
                .entityYOffset(0)
                .projectileLinkState(EnemyProjectileCollision.LinkState.nonInteractive())
                .swordCollisionActive(false)
                .swordX(0)
                .swordWidth(0)
                .swordY(0)
                .swordHeight(0)
                .linkSpeedX(1)
                .linkSpeedY(2)
                .build();
        EntityFrameContext effective = original.withRuntimeInputs(5, 30, 40, collision);
        assertEquals(10, original.linkEntityX());
        assertEquals(30, effective.linkEntityX());
        assertEquals(40, effective.linkEntityY());
        assertEquals(5, effective.frameCounter());
        assertEquals(original.linkSpeedX(), effective.linkSpeedX());
        assertEquals(original.handlerLinkCollisionEnabled(), effective.handlerLinkCollisionEnabled());
        assertSame(random, effective.randomByteSupplier());
        assertSame(collision, effective.backgroundCollision());
        assertSame(original.projectileLinkState(), effective.projectileLinkState());
    }
}
