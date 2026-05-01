package linksawakening.gameplay;

import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.sfx.SoundEffect;
import linksawakening.audio.sfx.SoundEffectCatalog;
import linksawakening.audio.sfx.SoundEffectNamespace;
import linksawakening.audio.sfx.SoundEffectPlayer;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SfxGameplaySoundSinkTest {
    @Test
    void gameplaySoundMapResolvesImplementedEventsToRealLadxEffects() {
        GameplaySoundEffectMap map = GameplaySoundEffectMap.fromCatalog(SoundEffectCatalog.fromRom(loadRom()));

        assertMapped(map, GameplaySoundEvent.ROC_FEATHER_JUMP,
                SoundEffectNamespace.JINGLE, 0x0D, "JINGLE_FEATHER_JUMP");
        assertMapped(map, GameplaySoundEvent.PIT_FALL,
                SoundEffectNamespace.WAVE, 0x0C, "WAVE_SFX_LINK_FALL");
        assertMapped(map, GameplaySoundEvent.SWORD_SWING_A,
                SoundEffectNamespace.NOISE, 0x02, "NOISE_SFX_SWORD_SWING_A");
        assertMapped(map, GameplaySoundEvent.SWORD_SWING_B,
                SoundEffectNamespace.NOISE, 0x14, "NOISE_SFX_SWORD_SWING_B");
        assertMapped(map, GameplaySoundEvent.SWORD_SWING_C,
                SoundEffectNamespace.NOISE, 0x15, "NOISE_SFX_SWORD_SWING_C");
        assertMapped(map, GameplaySoundEvent.SWORD_SWING_D,
                SoundEffectNamespace.NOISE, 0x18, "NOISE_SFX_SWORD_SWING_D");
        assertMapped(map, GameplaySoundEvent.SWORD_FULLY_CHARGED,
                SoundEffectNamespace.JINGLE, 0x04, "JINGLE_CHARGING_SWORD");
        assertMapped(map, GameplaySoundEvent.SPIN_ATTACK,
                SoundEffectNamespace.NOISE, 0x03, "NOISE_SFX_SPIN_ATTACK");
        assertMapped(map, GameplaySoundEvent.CUT_GRASS,
                SoundEffectNamespace.NOISE, 0x05, "NOISE_SFX_CUT_GRASS");
        assertMapped(map, GameplaySoundEvent.INVENTORY_OPEN,
                SoundEffectNamespace.JINGLE, 0x11, "JINGLE_OPEN_INVENTORY");
        assertMapped(map, GameplaySoundEvent.INVENTORY_CLOSE,
                SoundEffectNamespace.JINGLE, 0x12, "JINGLE_CLOSE_INVENTORY");
        assertMapped(map, GameplaySoundEvent.MENU_MOVE,
                SoundEffectNamespace.JINGLE, 0x0A, "JINGLE_MOVE_SELECTION");
        assertMapped(map, GameplaySoundEvent.MENU_VALIDATE,
                SoundEffectNamespace.JINGLE, 0x13, "JINGLE_VALIDATE");
    }

    @Test
    void sinkReplacesCurrentOneShotWhenPlayingGameplaySound() {
        GameBoyApu apu = new GameBoyApu(44_100);
        RecordingPcmSoundOutput output = new RecordingPcmSoundOutput();
        SfxGameplaySoundSink sink = new SfxGameplaySoundSink(
                apu,
                new SoundEffectPlayer(apu),
                GameplaySoundEffectMap.fromCatalog(SoundEffectCatalog.fromRom(loadRom())),
                output);

        sink.play(GameplaySoundEvent.ROC_FEATHER_JUMP);

        assertEquals(0, output.queuedPlays);
        assertEquals(1, output.replacementBuffers.size());
        assertEquals(44_100, output.replacementSampleRates.getFirst());
        assertTrue(hasNonZeroSample(output.replacementBuffers.getFirst()));
        assertEquals(0x00, apu.readRegister(GameBoyApu.NR52));
    }

    private static void assertMapped(
            GameplaySoundEffectMap map,
            GameplaySoundEvent event,
            SoundEffectNamespace namespace,
            int id,
            String name) {
        SoundEffect effect = map.resolve(event).orElseThrow();

        assertEquals(namespace, effect.namespace());
        assertEquals(id, effect.id());
        assertEquals(name, effect.name());
    }

    private static boolean hasNonZeroSample(short[] pcm) {
        for (short sample : pcm) {
            if (sample != 0) {
                return true;
            }
        }
        return false;
    }

    private static byte[] loadRom() {
        try (InputStream stream = SfxGameplaySoundSinkTest.class.getClassLoader()
                .getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IOException("Missing ROM resource: rom/azle.gbc");
            }
            return stream.readAllBytes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static final class RecordingPcmSoundOutput implements PcmSoundOutput {
        private final List<short[]> replacementBuffers = new ArrayList<>();
        private final List<Integer> replacementSampleRates = new ArrayList<>();
        private int queuedPlays;

        @Override
        public void play(short[] stereoPcm, int sampleRate) {
            queuedPlays++;
        }

        @Override
        public void playReplacing(short[] stereoPcm, int sampleRate) {
            replacementBuffers.add(stereoPcm);
            replacementSampleRates.add(sampleRate);
        }
    }
}
