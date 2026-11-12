package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class EntityFrameBuilderReuseTest {
    @Test
    void reusedFrameBuilderPreservesPreviouslyCapturedImmutableInputs() {
        var initial =
                TestEntities.room(TestEntities.active(9).slot(0).at(64, 64).build());
        var fromValues = RoomEntityRuntime.builder(initial).build();
        var fromSource = RoomEntityRuntime.builder(initial).build();
        var builder = EntityFrameContext.builder()
                .randomByteSupplier(() -> 0x24)
                .projectileLinkState(EnemyProjectileCollision.LinkState.nonInteractive())
                .handlerLinkCollisionEnabled(false);
        var retained = builder.frameCounter(7).linkEntityX(30).build();
        for (int i = 0; i < 32; i++) {
            builder.frameCounter(i).linkEntityX(30 + i);
            assertEquals(fromValues.tick(builder.build()), fromSource.tick(builder.build()));
            assertEquals(
                    fromValues.inspection().snapshot().slots(),
                    fromSource.inspection().snapshot().slots());
        }
        assertEquals(7, retained.frameCounter());
        assertEquals(30, retained.linkEntityX());
    }
}
