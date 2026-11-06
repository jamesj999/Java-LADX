package linksawakening.render;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Golden full frames from the shipped ROM; no emulator dependency during Gradle tests. */
final class DialogEmulatorParityTest {
    @Test
    void allSixOpeningPagesMatchIncludingHudNpcAnimationAndArrowBlink() throws Exception {
        verifyScenario("marin", 192);
    }

    @Test
    void tarinOfferMatchesIncludingTopBoxSpriteHiding() throws Exception {
        verifyScenario("tarin-offer", 192);
    }

    @Test
    void heldShieldPoseMatches() throws Exception {
        verifyScenario("shield-pose", 32);
    }

    @Test
    void receivedShieldDialogueMatchesIncludingHudReward() throws Exception {
        verifyScenario("tarin-shield", 96);
    }

    @Test
    void bedExitMatchesEveryFrameOfHopLandingAndNpcOverlap() throws Exception {
        verifyScenario("bed-exit", 64);
    }

    @Test
    void openingRowsAndEveryPrintedCharacterMatchTheEmulator() throws Exception {
        verifyScenario("marin-timing", 100);
    }

    @Test
    void pageScrollAndBothNewLinesMatchEveryEmulatorFrame() throws Exception {
        verifyScenario("marin-scroll", 220);
    }

    @Test
    void closingRestoresAllFiveRowsAndNpcSpritesAtRomTiming() throws Exception {
        verifyScenario("marin-close", 32);
    }

    private void verifyScenario(String scenario, int expectedFrames) throws Exception {
        String prefix = "marin".equals(scenario) ? "" : scenario + "/";
        Map<String, String> hashes = new HashMap<>();
        for (String line : resource(prefix + "emulator-rgb555-sha256.csv").lines().toList()) {
            String[] fields = line.split(",");
            hashes.put(fields[0], fields[1]);
        }
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        try (var in = getClass().getResourceAsStream("/rom/azle.gbc")) {
            assertEquals("c95dd3d9cb798e86c28e3269554e458d434bf3277b96cadf5bc81cfacfb5ee60",
                HexFormat.of().formatHex(digest.digest(in.readAllBytes())), "Fixture ROM revision");
        }
        AtomicInteger checked = new AtomicInteger();
        DialogEmulatorCapture.capture(scenario, resource(prefix + "initial.tsv"),
            resource(prefix + "frames.tsv").lines().toList(),
            (name, rgba) -> {
                byte[] rgb555 = new byte[160 * 144 * 3];
                for (int pixel = 0; pixel < 160 * 144; pixel++) {
                    for (int channel = 0; channel < 3; channel++) {
                        rgb555[pixel * 3 + channel] = (byte) ((rgba[pixel * 4 + channel] & 255) >>> 3);
                    }
                }
                assertEquals(hashes.get(name), HexFormat.of().formatHex(digest.digest(rgb555)), name);
                checked.incrementAndGet();
            });
        assertEquals(expectedFrames, checked.get());
        assertEquals(hashes.size(), checked.get());
    }

    private static String resource(String name) throws Exception {
        try (var in = DialogEmulatorParityTest.class.getResourceAsStream("/dialog/" + name)) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
