package linksawakening.gameplay;

import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.sfx.SoundEffect;
import linksawakening.audio.sfx.SoundEffectCatalog;
import linksawakening.audio.sfx.SoundEffectNamespace;
import linksawakening.audio.sfx.SoundEffectPlayer;
import linksawakening.dialog.DialogController;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class SfxDialogSoundSinkTest {
    @Test
    void dialogSoundMapResolvesMessageboxEventsToRealLadxEffects() {
        DialogSoundEffectMap map = DialogSoundEffectMap.fromCatalog(SoundEffectCatalog.fromRom(loadRom()));

        assertMapped(map, DialogController.SoundEvent.TEXT_PRINT,
                SoundEffectNamespace.WAVE, 0x0F, "WAVE_SFX_TEXT_PRINT");
        assertMapped(map, DialogController.SoundEvent.DIALOG_BREAK,
                SoundEffectNamespace.JINGLE, 0x15, "JINGLE_DIALOG_BREAK");
        assertMapped(map, DialogController.SoundEvent.MOVE_SELECTION,
                SoundEffectNamespace.JINGLE, 0x0A, "JINGLE_MOVE_SELECTION");
    }

    @Test
    void sinkPlaysTextPrintThroughSoundEffectPlayerSequence() {
        GameBoyApu apu = new GameBoyApu(44_100);
        RecordingPcmSoundOutput output = new RecordingPcmSoundOutput();
        SfxDialogSoundSink sink = new SfxDialogSoundSink(
                apu,
                new SoundEffectPlayer(apu),
                DialogSoundEffectMap.fromCatalog(SoundEffectCatalog.fromRom(loadRom())),
                output);

        sink.play(DialogController.SoundEvent.TEXT_PRINT);

        assertEquals(1, output.buffers.size());
        assertEquals(44_100, output.sampleRates.getFirst());
        assertTrue(hasNonZeroSample(output.buffers.getFirst()));
        assertEquals(0x00, apu.readRegister(GameBoyApu.NR52));
    }

    @Test
    void textPrintSequenceStartsWithRealWaveRegisters() {
        GameBoyApu apu = new GameBoyApu(44_100);
        SoundEffectPlayer player = new SoundEffectPlayer(apu);
        SoundEffect textPrint = SoundEffectCatalog.fromRom(loadRom())
                .find(SoundEffectNamespace.WAVE, 0x0F)
                .orElseThrow();

        player.play(textPrint);

        assertEquals(0x80, apu.readRegister(GameBoyApu.NR30));
        assertEquals(0xFB, apu.readRegister(GameBoyApu.NR31));
        assertEquals(0x60, apu.readRegister(GameBoyApu.NR32));
        assertEquals(0xD2, apu.readRegister(GameBoyApu.NR33));
        assertEquals(0xC7, apu.readRegister(GameBoyApu.NR34));
    }

    @Test
    void sinkUsesRealDialogBreakJingleSequence() {
        GameBoyApu apu = new GameBoyApu(44_100);
        RecordingPcmSoundOutput output = new RecordingPcmSoundOutput();
        SfxDialogSoundSink sink = new SfxDialogSoundSink(
                apu,
                new SoundEffectPlayer(apu),
                DialogSoundEffectMap.fromCatalog(SoundEffectCatalog.fromRom(loadRom())),
                output);

        sink.play(DialogController.SoundEvent.DIALOG_BREAK);

        assertEquals(1, output.buffers.size());
        assertTrue(hasNonZeroSample(output.buffers.getFirst()));
        assertEquals(0x00, apu.readRegister(GameBoyApu.NR52));
    }

    @Test
    void pageBreakJingleDoesNotContaminateFollowingTextPrintSound() {
        RecordingPcmSoundOutput afterPageBreakOutput = new RecordingPcmSoundOutput();
        GameBoyApu afterPageBreakApu = new GameBoyApu(44_100);
        SfxDialogSoundSink afterPageBreakSink = new SfxDialogSoundSink(
                afterPageBreakApu,
                new SoundEffectPlayer(afterPageBreakApu),
                DialogSoundEffectMap.fromCatalog(SoundEffectCatalog.fromRom(loadRom())),
                afterPageBreakOutput);
        afterPageBreakSink.play(DialogController.SoundEvent.DIALOG_BREAK);
        afterPageBreakSink.play(DialogController.SoundEvent.TEXT_PRINT);

        RecordingPcmSoundOutput freshOutput = new RecordingPcmSoundOutput();
        GameBoyApu freshApu = new GameBoyApu(44_100);
        SfxDialogSoundSink freshSink = new SfxDialogSoundSink(
                freshApu,
                new SoundEffectPlayer(freshApu),
                DialogSoundEffectMap.fromCatalog(SoundEffectCatalog.fromRom(loadRom())),
                freshOutput);
        freshSink.play(DialogController.SoundEvent.TEXT_PRINT);

        assertEquals(2, afterPageBreakOutput.buffers.size());
        assertEquals(1, freshOutput.buffers.size());
        assertArrayEquals(freshOutput.buffers.getFirst(), afterPageBreakOutput.buffers.get(1));
    }

    private static void assertMapped(
            DialogSoundEffectMap map,
            DialogController.SoundEvent event,
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
        try (InputStream stream = SfxDialogSoundSinkTest.class.getClassLoader()
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
        private final List<short[]> buffers = new ArrayList<>();
        private final List<Integer> sampleRates = new ArrayList<>();

        @Override
        public void play(short[] stereoPcm, int sampleRate) {
            buffers.add(stereoPcm);
            sampleRates.add(sampleRate);
        }
    }
}
