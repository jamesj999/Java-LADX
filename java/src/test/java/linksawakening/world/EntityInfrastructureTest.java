package linksawakening.world;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;

class EntityInfrastructureTest {
  @Test
  void outboxKeepsStagedAndLiveEventsSeparate() {
    EntityOutbox outbox = new EntityOutbox();
    var staged = outbox.channel(RoomEntityRuntime.LinkAttackStepRequest.class, "staged");
    var live = outbox.channel(RoomEntityRuntime.LinkAttackStepRequest.class, "live");
    staged.add(new RoomEntityRuntime.LinkAttackStepRequest(3));
    live.add(new RoomEntityRuntime.LinkAttackStepRequest(7));
    assertEquals(List.of(new RoomEntityRuntime.LinkAttackStepRequest(7)), List.copyOf(live));
    live.clear();
    live.addAll(staged);
    staged.clear();
    assertEquals(List.of(new RoomEntityRuntime.LinkAttackStepRequest(3)), List.copyOf(live));
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
    java.util.function.IntSupplier random = () -> 123;
    registry.register(
        255,
        (actual, source) -> {
          assertSame(entity, actual);
          assertSame(random, source);
          assertEquals(15, actual.slot());
        });
    registry.initialize(entity, random);
  }

  @Test
  void typedDrainLeavesOtherEffectsAndChannelsPending() {
    EntityOutbox outbox = new EntityOutbox();
    outbox
        .channel(RoomEntityRuntime.LinkFacingRequest.class, "effects")
        .add(new RoomEntityRuntime.LinkFacingRequest(1, 2));
    outbox
        .channel(RoomEntityRuntime.ScreenShakeRequest.class, "effects")
        .add(new RoomEntityRuntime.ScreenShakeRequest(2, 8, 0));
    var drained = outbox.drain(RoomEntityRuntime.LinkFacingRequest.class, "effects");
    assertEquals(1, drained.size());
    assertThrows(UnsupportedOperationException.class, () -> drained.clear());
    assertTrue(outbox.drain(RoomEntityRuntime.LinkFacingRequest.class, "effects").isEmpty());
    assertEquals(1, outbox.drain(RoomEntityRuntime.ScreenShakeRequest.class, "effects").size());
  }

  @Test
  void registryKeepsStageRoutingWhenAdvanceIsAddedLater() {
    EntityHandlerRegistry registry = new EntityHandlerRegistry();
    EntityFrameExecution frame = new EntityFrameExecution(null, null);
    frame.entity = RoomEntity.disabled(15);
    var stage = EntityTickStage.PHASE_1_TARIN;
    registry.registerTick(
        255,
        stage,
        actual -> {
          assertSame(frame, actual);
          return false;
        });
    registry.registerAdvance(255, (entity, context) -> entity);
    assertFalse(registry.tick(stage, frame));
    assertTrue(registry.tick(EntityTickStage.PHASE_1_BOW_WOW, frame));
    assertSame(frame.entity, registry.advance(frame.entity, null));
    assertThrows(
        IllegalArgumentException.class, () -> registry.registerTick(255, stage, actual -> true));
    assertThrows(
        IllegalArgumentException.class,
        () -> registry.registerAdvance(255, (entity, context) -> entity));
  }

  @Test
  void effectiveFrameCopyPreservesInputAndServiceIdentity() {
    java.util.function.IntSupplier random = () -> 91;
    RoomEntityBackgroundCollision collision = (entity, direction, x, y) -> false;
    EntityFrameContext original =
        new EntityFrameContext(
            4,
            10,
            20,
            0,
            random,
            collision,
            null,
            null,
            0,
            false,
            2,
            0,
            EnemyProjectileCollision.LinkState.nonInteractive(),
            false,
            0,
            0,
            0,
            0,
            1,
            2);
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
