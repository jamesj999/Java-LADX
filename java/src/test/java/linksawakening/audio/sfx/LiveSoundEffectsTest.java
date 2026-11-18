package linksawakening.audio.sfx;

import linksawakening.audio.apu.GameBoyApu;
import org.junit.jupiter.api.Test;

import static linksawakening.audio.sfx.SoundEffectNamespace.JINGLE;
import static linksawakening.audio.sfx.SoundEffectNamespace.NOISE;
import static linksawakening.audio.sfx.SoundEffectNamespace.WAVE;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

final class LiveSoundEffectsTest {
    @Test
    void waveAndNoiseOverlapAndContinueWithoutNewRequests() throws Exception {
        byte[] rom = loadRom();
        GameBoyApu apu = new GameBoyApu(48_001);
        LiveSoundEffects live = new LiveSoundEffects(apu, rom);
        LiveSoundEffects reference = new LiveSoundEffects(new GameBoyApu(48_001), rom);

        assertEquals(0, live.renderFrame().length);
        reference.renderFrame();
        live.request(NOISE, 3);
        live.request(WAVE, 1);
        live.request(NOISE, 2);
        reference.request(WAVE, 1);
        reference.request(NOISE, 2);

        assertArrayEquals(reference.renderFrame(), live.renderFrame());
        assertEquals(0x0C, apu.readRegister(GameBoyApu.NR52) & 0x0C);
        assertEquals(0x21, apu.readRegister(GameBoyApu.NR43));
        short[] nextFrame = live.renderFrame();
        assertEquals(1600, nextFrame.length);
        assertArrayEquals(reference.renderFrame(), nextFrame);
        assertEquals(0x0C, apu.readRegister(GameBoyApu.NR52) & 0x0C);
    }

    @Test
    void treasureJingleSuppressesSameFrameWaveRegardlessOfRequestOrder() throws Exception {
        byte[] rom = loadRom();
        LiveSoundEffects live = new LiveSoundEffects(new GameBoyApu(48_000), rom);
        LiveSoundEffects treasureOnly = new LiveSoundEffects(new GameBoyApu(48_000), rom);

        live.request(JINGLE, 1);
        live.request(WAVE, 1);
        treasureOnly.request(JINGLE, 1);

        short[] treasureFrame = treasureOnly.renderFrame();
        assertEquals(1600, treasureFrame.length);
        assertArrayEquals(treasureFrame, live.renderFrame());
        for (int frame = 0; frame < 60; frame++) {
            assertArrayEquals(treasureOnly.renderFrame(), live.renderFrame(), "Frame " + frame);
        }
    }

    @Test
    void tickExposesChannelOwnershipAndLeavesRenderingToTheCaller() throws Exception {
        byte[] rom = loadRom();
        GameBoyApu apu = new GameBoyApu(48_000);
        LiveSoundEffects live = new LiveSoundEffects(apu, rom);
        LiveSoundEffects reference = new LiveSoundEffects(new GameBoyApu(48_000), rom);

        live.request(WAVE, 1);
        live.request(NOISE, 2);
        reference.request(WAVE, 1);
        reference.request(NOISE, 2);
        assertEquals(0, live.musicSuppressionMask());

        live.tick();
        assertEquals(0x0C, live.musicSuppressionMask());
        assertArrayEquals(reference.renderFrame(), apu.render(800));

        for (int frame = 0; frame < 60; frame++) {
            live.tick();
            apu.render(800);
        }
        assertEquals(0, live.musicSuppressionMask());

        live.request(JINGLE, 1);
        live.tick();
        assertEquals(0x07, live.musicSuppressionMask());
    }

    @Test
    void bothMusicBanksCancelWaveButKeepBombNoiseAcrossTrackChanges() throws Exception {
        byte[] rom = loadRom();
        for (int bank : new int[] { 0x1B, 0x1E }) {
            LiveSoundEffects live = new LiveSoundEffects(new GameBoyApu(48_000), rom);
            live.request(WAVE, 1);
            live.request(NOISE, 0x0C);
            live.tick();
            assertEquals(0x0C, live.musicSuppressionMask());
            live.changeMusicTrack(bank, false);
            assertEquals(0x08, live.musicSuppressionMask());
            live.changeMusicTrack(bank, true);
            assertEquals(0x08, live.musicSuppressionMask());

            live.request(NOISE, 2);
            live.tick();
            assertEquals(0x08, live.musicSuppressionMask());
            live.changeMusicTrack(bank, false);
            assertEquals(0, live.musicSuppressionMask());
        }
    }

    @Test
    void renderFrameCarriesFractionalSamplesAcrossSixtyTicks() throws Exception {
        GameBoyApu apu = new GameBoyApu(48_001);
        LiveSoundEffects live = new LiveSoundEffects(apu, loadRom());
        apu.writeRegister(GameBoyApu.NR11, 0x80);
        apu.writeRegister(GameBoyApu.NR12, 0xF0);
        apu.writeRegister(GameBoyApu.NR14, 0x80);

        int renderedSamples = 0;
        for (int frame = 0; frame < 60; frame++) {
            renderedSamples += live.renderFrame().length / 2;
        }
        assertEquals(48_001, renderedSamples);
    }

    private static byte[] loadRom() throws Exception {
        try (var stream = LiveSoundEffectsTest.class.getResourceAsStream("/rom/azle.gbc")) {
            return stream.readAllBytes();
        }
    }
}
