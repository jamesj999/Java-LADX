package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class HardHatMotionTest {
    @ParameterizedTest
    @CsvSource({"0, 96, 64, 1, 0", "1, 32, 64, 255, 0",
        "2, 64, 32, 0, 255", "3, 64, 96, 0, 1"})
    void wallContactPreservesSpeedUntilTheSteeringTickThenStopsTheBlockedAxis(
        int direction, int linkX, int linkY, int speedX, int speedY) {
        HardHatMotion motion = new HardHatMotion();
        RoomEntity beetle = motion.advance(entity(), 0, linkX, linkY, () -> 0, null);
        assertEquals(speedX, motion.speedX(0));
        assertEquals(speedY, motion.speedY(0));

        List<Integer> probes = new ArrayList<>();
        RoomEntityBackgroundCollision wall = (entity, probeDirection, x, y) -> {
            probes.add(probeDirection);
            return probeDirection == direction;
        };
        // $7A36/$7A5D gate probes on speed, not pixel displacement. Speeds of
        // +1 have not accumulated a pixel yet, but must still test the wall.
        for (int frame = 1; frame <= 3; frame++) {
            probes.clear();
            beetle = motion.advance(beetle, frame, linkX, linkY, () -> 0, wall);
            assertEquals(List.of(direction), probes);
            assertEquals(0x40, beetle.x());
            assertEquals(0x40, beetle.y());
            assertEquals(speedX, motion.speedX(0));
            assertEquals(speedY, motion.speedY(0));
        }

        probes.clear();
        beetle = motion.advance(beetle, 4, linkX, linkY, () -> 0, wall);
        // $4F7E/$4F94 clear the colliding speed AFTER HardhatBeetleUpdateSpeed.
        assertEquals(List.of(direction), probes);
        assertEquals(0, motion.speedX(0));
        assertEquals(0, motion.speedY(0));
        assertEquals(0x40, beetle.x());
        assertEquals(0x40, beetle.y());
    }

    private static RoomEntity entity() {
        EntitySpriteDefinition.OamAttribute tile =
            new EntitySpriteDefinition.OamAttribute(0, 0);
        EntitySpriteDefinition definition = new EntitySpriteDefinition(0x20, 6, 0x4F2C,
            EntitySpriteDefinition.Shape.PAIR, 0,
            List.of(new EntitySpriteDefinition.Variant(tile, tile)));
        return new RoomEntity(0, 0, 0x20, 0x40, 0x40,
            EntityStatus.ACTIVE, definition, 0);
    }
}
