package linksawakening.audio.sfx;

import java.util.ArrayList;
import java.util.List;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.rom.Rom;
import linksawakening.rom.RomBank;

/** Local research harness. Prints register writes, never exports ROM graphics/assets. */
@linksawakening.testing.RequiresRom
public final class SfxRegisterTrace {
    public record Write(int frame, int register, int value) {}

    final GameBoyApu apu = new GameBoyApu(48000);
    final SoundEffectPlayer player;
    private final List<Write> writes = new ArrayList<>();
    private int frame;

    SfxRegisterTrace(byte[] rom) {
        player = new SoundEffectPlayer(apu, Rom.of(rom));
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0xFF);
        apu.setRegisterObserver((register, value) -> writes.add(new Write(frame, register, value)));
    }

    void start(SoundEffectNamespace namespace, int id) {
        player.play(player.catalog().find(namespace, id).orElseThrow());
    }

    short[] advance() {
        short[] pcm = apu.render(800);
        frame++;
        player.tick60Hz();
        return pcm;
    }

    void clearWrites() {
        writes.clear();
    }

    List<Write> writes() {
        return List.copyOf(writes);
    }

    List<Write> frame(int frame) {
        return writes.stream().filter(w -> w.frame() == frame).toList();
    }

    static void add(List<SfxRegisterTrace.Write> writes, int frame, int register, int... values) {
        for (int value : values) writes.add(new SfxRegisterTrace.Write(frame, register++, value));
    }

    static boolean varying(short[] pcm) {
        for (int i = 2; i < pcm.length; i += 2) if (pcm[i] != pcm[0]) return true;
        return false;
    }

    static void put(byte[] rom, int address, int... values) {
        int offset = RomBank.romOffset(0x1B, address);
        for (int value : values) rom[offset++] = (byte) value;
    }

    public static void main(String[] args) throws Exception {
        byte[] rom;
        try (var stream = linksawakening.testing.TestRom.open()) {
            rom = stream.readAllBytes();
        }
        var trace = new SfxRegisterTrace(rom);
        var namespace = SoundEffectNamespace.valueOf(args[0].toUpperCase(java.util.Locale.ROOT));
        int id = Integer.decode(args[1]);
        int frames = args.length > 2 ? Integer.parseInt(args[2]) : 60;
        int restartInterval = args.length > 3 ? Integer.parseInt(args[3]) : 0;
        trace.start(namespace, id);
        for (int i = 1; i <= frames; i++) {
            trace.advance();
            if (restartInterval > 0 && i % restartInterval == 0) trace.start(namespace, id);
        }
        for (var write : trace.writes()) {
            System.out.printf(
                    "{\"frame\":%d,\"register\":%d,\"value\":%d}%n", write.frame(), write.register(), write.value());
        }
    }
}
