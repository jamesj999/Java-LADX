package linksawakening.audio;

import java.io.*;
import java.lang.reflect.Field;
import javax.sound.sampled.*;
import linksawakening.app.GameSession;
import linksawakening.audio.apu.GameBoyApu;
import linksawakening.cutscene.CutsceneManager;

/** Local research capture; never check its ROM-derived output into source control. */
@linksawakening.testing.RequiresRom
public final class IntroAudioCapture {
    private static Object field(Object owner, String name) throws Exception {
        Field f = owner.getClass().getDeclaredField(name);
        f.setAccessible(true);
        return f.get(owner);
    }

    public static void main(String[] args) throws Exception {
        File directory = new File(args[0]);
        directory.mkdirs();
        int count = Integer.parseInt(args[1]);
        int[] frame = {0};
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        try (PrintWriter trace = new PrintWriter(new File(directory, "engine.jsonl"))) {
            GameSession session = new GameSession((samples, rate) -> {
                if (rate != 48000) throw new IllegalStateException("Unexpected sample rate");
                for (short sample : samples) {
                    pcm.write(sample & 255);
                    pcm.write((sample >>> 8) & 255);
                }
                trace.printf("{\"frame\":%d,\"samples\":%d}%n", frame[0], samples.length / 2);
            });
            session.initializeHeadlessIntro();
            Object audio = field(session, "musicPlayer");
            ((GameBoyApu) field(audio, "apu"))
                    .setRegisterObserver((register, value) ->
                            trace.printf("{\"frame\":%d,\"register\":%d,\"value\":%d}%n", frame[0], register, value));
            // Decorate the already installed production sink rather than replacing audio routing.
            CutsceneManager cutscene = (CutsceneManager) field(session, "cutsceneManager");
            @SuppressWarnings("unchecked")
            java.util.function.IntConsumer original = (java.util.function.IntConsumer) field(cutscene, "noiseSound");
            cutscene.setIntroNoiseSound(id -> {
                trace.printf("{\"frame\":%d,\"noise\":%d}%n", frame[0], id);
                original.accept(id);
            });
            for (; frame[0] < count; frame[0]++) {
                session.headlessFrame();
                trace.printf(
                        "{\"frame\":%d,\"stage\":\"%s\"}%n",
                        frame[0], cutscene.frameSnapshot().substate());
            }
        }
        byte[] bytes = pcm.toByteArray();
        AudioFormat format = new AudioFormat(48000, 16, 2, true, false);
        AudioSystem.write(
                new AudioInputStream(new ByteArrayInputStream(bytes), format, bytes.length / 4),
                AudioFileFormat.Type.WAVE,
                new File(directory, "engine.wav"));
    }
}
