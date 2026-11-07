package linksawakening.world;

import org.junit.jupiter.api.Test;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

class EntityInfrastructureTest {
    @Test void outboxKeepsStagedAndLiveEventsSeparate() {
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

    @Test void registryRejectsDuplicateAndInvalidRomTypes() {
        EntityHandlerRegistry registry = new EntityHandlerRegistry();
        registry.register(255, (entity, random) -> {});
        assertTrue(registry.supports(255));
        assertFalse(registry.supports(0));
        assertFalse(registry.supports(256));
        assertThrows(IllegalArgumentException.class, () -> registry.register(255, (e, r) -> {}));
        assertThrows(IndexOutOfBoundsException.class, () -> registry.register(256, (e, r) -> {}));
    }

    @Test void registryPreservesSlotAndRandomServiceIdentity() {
        EntityHandlerRegistry registry = new EntityHandlerRegistry();
        RoomEntity entity = RoomEntity.disabled(15);
        java.util.function.IntSupplier random = () -> 123;
        registry.register(255, (actual, source) -> {
            assertSame(entity, actual);
            assertSame(random, source);
            assertEquals(15, actual.slot());
        });
        registry.initialize(entity, random);
    }
}
