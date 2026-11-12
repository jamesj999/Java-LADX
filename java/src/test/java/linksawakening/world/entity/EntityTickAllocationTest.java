package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.testing.AllocationProbe;
import org.junit.jupiter.api.Test;

/** ROM-free steady-state AnimateEntities allocation, including three read-only inspections. */
final class EntityTickAllocationTest {
    @Test
    void representativeTickAllocation() {
        Mix mix = new Mix();
        double bytes = AllocationProbe.bytesPerIteration(4_000, 1_000, mix::tick);
        System.out.printf("Entity mix allocation bytes/tick: %.2f%n", bytes);
        // Baseline budget, tightened after hot-path allocation removal.
        assertTrue(bytes < 40_000, "Entity mix bytes/tick: " + bytes);
        assertEquals(6, mix.runtime.inspection().snapshot().loadedEntities().size());
        assertTrue(mix.checksum != 0, "Consume the tick and inspection results");
    }

    private static final class Mix {
        private final RoomEntity[] starting = new RoomEntity[RoomEntity.SLOT_COUNT];
        private final EntityFrameContext[] frames = new EntityFrameContext[256];
        private final RoomEntityRuntime runtime;
        private int frame;
        private long checksum;

        private Mix() {
            Arrays.setAll(starting, RoomEntity::disabled);
            int[] types = {0x09, 0x0B, 0x0C, EntityRomConstants.ENTITY_MOLDORM, 0x2D, 0x71};
            for (int slot = 0; slot < types.length; slot++) {
                starting[slot] = new RoomEntityBuilder()
                        .slot(slot)
                        .sourceLoadOrder(slot)
                        .type(types[slot])
                        .at(0x40 + slot * 8, 0x50)
                        .spriteDefinition(EntitySpriteDefinition.unsupported(types[slot]))
                        .build();
            }
            runtime = RoomEntityRuntime.builder(new RoomEntitySnapshot(Arrays.asList(starting)))
                    .build();
            for (int i = 0; i < frames.length; i++) {
                frames[i] = EntityFrameContext.builder()
                        .frameCounter(i)
                        .linkEntityX(0xA0)
                        .linkEntityY(0x90)
                        .randomByteSupplier(() -> 0x24)
                        .projectileLinkState(EnemyProjectileCollision.LinkState.nonInteractive())
                        .handlerLinkCollisionEnabled(true)
                        .build();
            }
        }

        private void tick() {
            // Keep the same six active types in bounds throughout warm-up and every window.
            // Motion state/timers advance normally; repositioning itself allocates nothing.
            System.arraycopy(starting, 0, runtime.slots, 0, starting.length);
            checksum += runtime.tick(frames[frame++ & 255]).size();
            for (int read = 0; read < 3; read++) {
                RoomEntitySnapshot snapshot = runtime.inspection().snapshot();
                checksum +=
                        snapshot.slots().get(0).x() + snapshot.loadedEntities().size();
            }
        }
    }
}
