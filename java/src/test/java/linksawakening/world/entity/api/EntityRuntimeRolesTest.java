package linksawakening.world.entity.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;
import linksawakening.world.entity.EntitySwordContactFrame;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityRuntimeBuilder;
import linksawakening.world.entity.RoomEntitySnapshot;
import org.junit.jupiter.api.Test;

/** Verifies the replacement API from outside the implementation package, as room clients use it. */
class EntityRuntimeRolesTest {
    @Test
    void rolesPreserveEmptyRoomBehaviorAndPublicInputValidation() {
        var initial = new RoomEntitySnapshot(IntStream.range(0, RoomEntity.SLOT_COUNT)
                .mapToObj(RoomEntity::disabled)
                .toList());
        var runtime = new RoomEntityRuntimeBuilder(initial)
                .randomByteSupplier(() -> 0)
                .build();
        assertThrows(IllegalArgumentException.class, () -> runtime.combat().deferContactRecoil(-1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> runtime.combat().deferContactRecoil(0, 256, 0));
        assertFalse(runtime.combat().deferContactRecoil(0, 0, 0));
        assertTrue(runtime.combat()
                .resolveCombat(new EntitySwordContactFrame(0, 80, 80, false, true, false, 0, 0, 0, 0))
                .isEmpty());
        assertFalse(runtime.commands().beginLift(0, 0));
        assertFalse(runtime.commands().throwLiftedEntity(0));
        assertThrows(IllegalArgumentException.class, () -> runtime.commands().thrownDirection(16));
        assertTrue(runtime.outbox().consumePendingEntityEvents().isEmpty());
        assertTrue(runtime.outbox().consumePendingDialogRequests().isEmpty());
    }
}
