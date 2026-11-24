package linksawakening.world.entity.motion;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import linksawakening.entity.EntitySpriteDefinition;
import linksawakening.world.entity.RoomEntity;
import linksawakening.world.entity.RoomEntityBackgroundCollision;
import linksawakening.world.entity.RoomEntityBuilder;
import linksawakening.world.entity.motion.StalfosEvasiveMotion.StalfosEvasiveFrame;
import org.junit.jupiter.api.Test;

final class StalfosEvasiveMotionTest {
    @Test
    void fractionalStepProbesBothWallsAndOnlyReversesTheHorizontalVelocity() {
        var motion = new StalfosEvasiveMotion();
        motion.initializeForActiveEntity(0);
        motion.speedX()[0] = 6;
        motion.speedY()[0] = 6;
        RoomEntity stalfos = new RoomEntityBuilder()
                .type(0x1E)
                .at(21, 29)
                .spriteDefinition(EntitySpriteDefinition.unsupported(0x1E))
                .build();
        for (int frame = 0; frame < 4; frame++) {
            stalfos = motion.advance(new StalfosEvasiveFrame(
                            stalfos, frame, 120, 112, false, 0, 0, () -> 1, RoomEntityBackgroundCollision.OPEN, 0))
                    .entity();
        }

        // In the ROM room-$14 probe both $80 fractions stay below one pixel on this step.
        List<Integer> probes = new ArrayList<>();
        RoomEntityBackgroundCollision wall = (entity, direction, x, y) -> {
            assertEquals(22, x);
            assertEquals(30, y);
            probes.add(direction);
            return true;
        };
        stalfos = motion.advance(new StalfosEvasiveFrame(stalfos, 4, 120, 112, false, 0, 0, () -> 1, wall, 0))
                .entity();

        assertEquals(List.of(0, 3), probes);
        assertEquals(22, stalfos.x());
        assertEquals(30, stalfos.y());
        assertEquals(0xF6, motion.speedX(0));
        assertEquals(6, motion.speedY(0));
    }
}
