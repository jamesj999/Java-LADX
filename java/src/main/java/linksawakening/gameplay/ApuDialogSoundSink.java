package linksawakening.gameplay;

import linksawakening.audio.apu.GameBoyApu;
import linksawakening.dialog.DialogController;

import java.util.Objects;

public final class ApuDialogSoundSink implements DialogSoundSink {

    private static final int TRIGGER = 0x80;
    private static final int SAMPLE_RATE = 44_100;
    private static final int SOUND_FRAMES = 4096;

    private final GameBoyApu apu;
    private final PcmSoundOutput output;

    public ApuDialogSoundSink(PcmSoundOutput output) {
        this(new GameBoyApu(SAMPLE_RATE), output);
    }

    public ApuDialogSoundSink(GameBoyApu apu, PcmSoundOutput output) {
        this.apu = Objects.requireNonNull(apu);
        this.output = Objects.requireNonNull(output);
        this.apu.writeRegister(GameBoyApu.NR52, 0x80);
        this.apu.writeRegister(GameBoyApu.NR50, 0x77);
        this.apu.writeRegister(GameBoyApu.NR51, 0xFF);
    }

    @Override
    public void play(DialogController.SoundEvent event) {
        if (event == null) {
            return;
        }
        switch (event) {
            case TEXT_PRINT -> playTextPrint();
            case DIALOG_BREAK -> playDialogBreak();
            case MOVE_SELECTION -> playMoveSelection();
        }
        output.play(apu.render(SOUND_FRAMES), apu.sampleRate());
    }

    private void playTextPrint() {
        apu.writeRegister(GameBoyApu.NR21, 0x80);
        apu.writeRegister(GameBoyApu.NR22, 0x41);
        apu.writeRegister(GameBoyApu.NR23, 0x80);
        apu.writeRegister(GameBoyApu.NR24, 0x86 | TRIGGER);
    }

    private void playDialogBreak() {
        apu.writeRegister(GameBoyApu.NR21, 0x40);
        apu.writeRegister(GameBoyApu.NR22, 0x62);
        apu.writeRegister(GameBoyApu.NR23, 0x20);
        apu.writeRegister(GameBoyApu.NR24, 0x87 | TRIGGER);
    }

    private void playMoveSelection() {
        apu.writeRegister(GameBoyApu.NR21, 0x40);
        apu.writeRegister(GameBoyApu.NR22, 0x31);
        apu.writeRegister(GameBoyApu.NR23, 0xC0);
        apu.writeRegister(GameBoyApu.NR24, 0x85 | TRIGGER);
    }
}
