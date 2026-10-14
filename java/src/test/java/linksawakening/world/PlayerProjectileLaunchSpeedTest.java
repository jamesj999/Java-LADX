package linksawakening.world;

import linksawakening.entity.EntitySpriteDefinition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class PlayerProjectileLaunchSpeedTest {
    @ParameterizedTest
    @CsvSource({"0, false", "1, false", "2, false", "3, false",
        "0, true", "1, true", "2, true", "3, true"})
    void arrowAndFireballUseTheShippedRomsFinalSpeedBytes(int direction, boolean power)
            throws IOException {
        byte[] rom;
        try (var stream = getClass().getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing shipped ROM");
            }
            rom = stream.readAllBytes();
        }
        // $140F compares the active power-up with 1; only equality falls
        // through the JR NZ at $1414 to add four to the direction index.
        int index = direction + (power ? 4 : 0);
        int expectedX = rom[0x13AD + index] & 0xFF;
        int expectedY = rom[0x13B5 + index] & 0xFF;
        PlayerArrowMotion arrow = new PlayerArrowMotion();
        MagicRodFireballMotion fireball = new MagicRodFireballMotion();
        arrow.initializeSpawn(0, direction, power);
        fireball.initializeSpawn(0, direction, power);
        assertEquals(expectedX, arrow.speedX(0));
        assertEquals(expectedY, arrow.speedY(0));
        assertEquals(expectedX, fireball.speedX(0));
        assertEquals(expectedY, fireball.speedY(0));

        RoomEntity entity = new RoomEntity(0, -1, 0x00, 0x40, 0x50,
            EntityStatus.ACTIVE, EntitySpriteDefinition.unsupported(0x00), direction);
        RoomEntity movedArrow = arrow.advance(entity, null).entity();
        RoomEntity movedFireball = fireball.advancePosition(entity);
        assertEquals(0x40 + ((byte) expectedX >> 4), movedArrow.x());
        assertEquals(0x50 + ((byte) expectedY >> 4), movedArrow.y());
        assertEquals(movedArrow.x(), movedFireball.x());
        assertEquals(movedArrow.y(), movedFireball.y());
    }
}
