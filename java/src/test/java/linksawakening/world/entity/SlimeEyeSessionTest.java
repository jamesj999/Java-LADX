package linksawakening.world.entity;

import static org.junit.jupiter.api.Assertions.*;

import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;
import linksawakening.app.GameSession;
import linksawakening.entity.Link;
import linksawakening.gameplay.GameplaySoundEvent;
import linksawakening.gameplay.GameplaySoundSink;
import linksawakening.state.PlayerState;
import org.junit.jupiter.api.Test;

/** Isolated D3 setup followed by the real GameSession update and framebuffer composition. */
@linksawakening.testing.RequiresRom
final class SlimeEyeSessionTest {
    @Test
    void productionSessionComposesLinkHudBossAndConsumesEncounterSounds() throws Exception {
        int[] pcmSamples = {0};
        var game = new GameSession((pcm, rate) -> pcmSamples[0] += pcm.length);
        var sounds = new ArrayList<GameplaySoundEvent>();
        var output = Path.of("build/slime-eye");
        Files.createDirectories(output);
        try {
            game.initializeHeadlessNewGame();
            // Disclosed setup: bypass the opening bed sequence, grant sword/boots, load D3 directly.
            set(game, "newGameWakeUpMotion", null);
            set(game, "newGameTarinShieldMotion", null);
            var diagnostics = game.diagnostics();
            var room = diagnostics.roomSession();
            var link = diagnostics.link();
            var player = diagnostics.playerState();
            player.setSwordLevel(1);
            player.setItemA(PlayerState.INVENTORY_SWORD);
            player.setItemB(PlayerState.INVENTORY_PEGASUS_BOOTS);
            player.setMaxHearts(14);
            player.setHealth(14 * 8);
            room.loadIndoor(2, 0x5A);
            link.setRoomEntryRomPosition(80, 96);
            link.setDirection(Link.DIRECTION_UP);
            GameplaySoundSink original = (GameplaySoundSink) get(game, "gameplaySoundSink");
            set(game, "gameplaySoundSink", (GameplaySoundSink) sound -> {
                sounds.add(sound);
                original.play(sound);
            });
            for (int frame = 0; frame < 180; frame++) {
                game.headlessButton("a", frame % 24 < 8);
                game.headlessFrame();
            }
            game.headlessButton("a", false);
            // Setup shake enters via production scroll -> LinkEquipment -> room input, no boss state write.
            diagnostics.scrollController().startScreenShake(6, 0);
            for (int frame = 0; frame < 160; frame++) game.headlessFrame();
            assertTrue(eyes(room).stream().anyMatch(e -> e.z() == 0));
            save(game.headlessFrame(), output.resolve("session-body.png"));
            int ticks = 0;
            var initialEye = eyes(room).getFirst();
            // This older assisted fixture keeps target placement explicit. The separate
            // SlimeEyeControlsTest covers the whole encounter using buttons only.
            while (eyes(room).size() == 1 && ticks < 900) {
                var eye = eyes(room).getFirst();
                link.setRoomEntryRomPosition(eye.x(), eye.y() + 16);
                link.setDirection(Link.DIRECTION_UP);
                int phase = ticks % 256;
                game.headlessButton("a", phase >= 192 || phase % 24 < 8);
                game.headlessButton("b", phase >= 192);
                game.headlessFrame();
                ticks++;
            }
            game.headlessButton("a", false);
            game.headlessButton("b", false);
            assertEquals(2, eyes(room).size(), "Live sword/boots combat must split the body");
            save(game.headlessFrame(), output.resolve("session-halves.png"));
            for (int frame = 0; frame < 2200 && !eyes(room).isEmpty(); frame++) {
                var target = eyes(room).stream()
                        .filter(e -> e.status() == EntityStatus.ACTIVE)
                        .findFirst()
                        .orElse(null);
                if (target != null) {
                    link.setRoomEntryRomPosition(target.x(), Math.min(112, target.y() + 16));
                    link.setDirection(Link.DIRECTION_UP);
                }
                game.headlessButton("a", frame % 24 < 8);
                game.headlessFrame();
            }
            game.headlessButton("a", false);
            assertTrue(eyes(room).isEmpty());
            assertEquals(
                    1,
                    room.activeRoom().entities().loadedEntities().stream()
                            .filter(e -> e.type() == 0x36)
                            .count());
            save(game.headlessFrame(), output.resolve("session-defeated.png"));
            assertTrue(sounds.contains(GameplaySoundEvent.ENEMY_HIT));
            assertTrue(sounds.contains(GameplaySoundEvent.BOMB_EXPLOSION));
            assertTrue(sounds.contains(GameplaySoundEvent.BOSS_EXPLOSION));
            assertTrue(pcmSamples[0] > 0);
            Files.writeString(
                    output.resolve("session-setup.txt"),
                    "Production GameSession with headless PCM; opening wake/shield skipped; sword/boots granted; max HP14; direct map02 room5A load; Link start80,96 facing up; scroll shake6 injected after180 frames; assisted fixture repositions Link below target each frame for body and halves, while actual sword input drives collision; no boss state or OAM writes.\nSounds="
                            + sounds + "\nPCM shorts=" + pcmSamples[0] + "\n");
        } finally {
            var close = GameSession.class.getDeclaredMethod("cleanup");
            close.setAccessible(true);
            close.invoke(game);
        }
    }

    private List<RoomEntity> eyes(linksawakening.world.room.RoomSession room) {
        return room.activeRoom().entities().loadedEntities().stream()
                .filter(e -> e.type() == 0x5B)
                .toList();
    }

    private Object get(Object object, String name) throws Exception {
        var field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(object);
    }

    private void set(Object object, String name, Object value) throws Exception {
        var field = object.getClass().getDeclaredField(name);
        field.setAccessible(true);
        field.set(object, value);
    }

    private void save(byte[] pixels, Path file) throws Exception {
        var image = new BufferedImage(160, 144, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 144; y++)
            for (int x = 0; x < 160; x++) {
                int offset = (y * 160 + x) * 4;
                image.setRGB(
                        x,
                        y,
                        0xff000000
                                | ((pixels[offset] & 255) << 16)
                                | ((pixels[offset + 1] & 255) << 8)
                                | (pixels[offset + 2] & 255));
            }
        ImageIO.write(image, "png", file.toFile());
    }
}
