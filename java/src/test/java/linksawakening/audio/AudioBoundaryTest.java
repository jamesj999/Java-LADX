package linksawakening.audio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.DataInputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import linksawakening.audio.apu.GameBoyApu;
import org.junit.jupiter.api.Test;

final class AudioBoundaryTest {
    @Test
    void onlyOpenAlClassesReferenceLwjgl() throws Exception {
        Path root = Path.of(GameBoyApu.class
                .getProtectionDomain()
                .getCodeSource()
                .getLocation()
                .toURI());
        for (String pkg : new String[] {"audio"}) {
            try (var paths = Files.walk(root.resolve("linksawakening/" + pkg))) {
                for (Path file :
                        paths.filter(p -> p.toString().endsWith(".class")).toList()) {
                    if (file.toString().contains("/audio/openal/")) continue;
                    try (var input = new DataInputStream(Files.newInputStream(file))) {
                        assertEquals(0xCAFEBABE, input.readInt());
                        input.readInt();
                        int count = input.readUnsignedShort();
                        for (int i = 1; i < count; i++) {
                            int tag = input.readUnsignedByte();
                            switch (tag) {
                                case 1 -> assertFalse(input.readUTF().contains("org/lwjgl/"), file.toString());
                                case 3, 4, 9, 10, 11, 12, 17, 18 -> input.skipNBytes(4);
                                case 5, 6 -> {
                                    input.skipNBytes(8);
                                    i++;
                                }
                                case 7, 8, 16, 19, 20 -> input.skipNBytes(2);
                                case 15 -> input.skipNBytes(3);
                                default -> throw new AssertionError("Unknown constant pool tag " + tag);
                            }
                        }
                    }
                }
            }
        }
    }
}
