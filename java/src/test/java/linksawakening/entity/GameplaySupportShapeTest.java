package linksawakening.entity;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

/** Guards responsibility boundaries rather than obsolete line/parameter limits. */
class GameplaySupportShapeTest {
    @Test
    void immutableRegistrationDataHasOneOwnerRatherThanStatelessFamilySlices() throws Exception {
        Path area = Path.of("src/main/java/linksawakening/entity");
        assertTrue(Files.exists(area.resolve("EntitySpriteRegistrationData.java")));
        try (var files = Files.list(area)) {
            assertFalse(files.anyMatch(p -> p.getFileName().toString().matches("EntitySprite.*Registrations\\.java")));
        }
    }

    @Test
    void equipmentRegistrationOrderBelongsToOneWiringFactory() throws Exception {
        Path area = Path.of("src/main/java/linksawakening/app/wiring");
        assertTrue(Files.exists(area.resolve("EquipmentWiring.java")));
        assertFalse(Files.exists(area.resolve("CombatEquipment.java")));
        assertFalse(Files.exists(area.resolve("ToolEquipment.java")));
        assertTrue(Files.readString(area.resolve("EquipmentWiring.java")).contains("record EquipmentInputs"));
    }
}
