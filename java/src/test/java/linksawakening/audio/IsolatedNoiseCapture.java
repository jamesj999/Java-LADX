package linksawakening.audio;

import java.io.*;
import javax.sound.sampled.*;
import linksawakening.audio.apu.GameBoyApu;

/** Synthetic CH4 register scenario, independent of ROM/music/session timing. */
public final class IsolatedNoiseCapture {
    public static void main(String[] args) throws Exception {
        GameBoyApu apu = new GameBoyApu(48000);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0x88);
        apu.writeRegister(GameBoyApu.NR42, 0xF0);
        apu.writeRegister(GameBoyApu.NR43, 3);
        apu.writeRegister(GameBoyApu.NR44, 0x80);
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        for (int frame = 0; frame < 24; frame++) {
            if (frame == 8) apu.writeRegister(GameBoyApu.NR42, 0x30);
            if (frame == 16) apu.writeRegister(GameBoyApu.NR44, 0x80);
            if (frame == 20) apu.writeRegister(GameBoyApu.NR42, 0);
            for (short sample : apu.render(800)) {
                bytes.write(sample & 255);
                bytes.write((sample >>> 8) & 255);
            }
        }
        byte[] pcm = bytes.toByteArray();
        AudioSystem.write(
                new AudioInputStream(
                        new ByteArrayInputStream(pcm), new AudioFormat(48000, 16, 2, true, false), pcm.length / 4),
                AudioFileFormat.Type.WAVE,
                new File(args[0]));
    }
}
