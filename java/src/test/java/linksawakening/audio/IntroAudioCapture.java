package linksawakening.audio;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintWriter;
import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import linksawakening.app.GameSession;
import linksawakening.cutscene.CutsceneManager;

/** Local research capture; never check its ROM-derived output into source control. */
@linksawakening.testing.RequiresRom
public final class IntroAudioCapture {
    public static void main(String[] args) throws Exception {
        File directory = new File(args[0]);
        directory.mkdirs();
        int count = Integer.parseInt(args[1]);
        int[] frame = {0};
        ByteArrayOutputStream pcm = new ByteArrayOutputStream();
        try (PrintWriter trace =
                new PrintWriter(new File(directory, "engine.jsonl"), java.nio.charset.Charset.defaultCharset())) {
            GameSession session = new GameSession((samples, rate) -> {
                if (rate != 48000) throw new IllegalStateException("Unexpected sample rate");
                for (short sample : samples) {
                    pcm.write(sample & 255);
                    pcm.write((sample >>> 8) & 255);
                }
                trace.printf("{\"frame\":%d,\"samples\":%d}%n", frame[0], samples.length / 2);
            });
            session.initializeHeadlessIntro();
            var audio = (linksawakening.audio.music.FrameAudioPlayer)
                    session.services().audio().musicPlayer();
            audio.setRegisterObserver((register, value) ->
                    trace.printf("{\"frame\":%d,\"register\":%d,\"value\":%d}%n", frame[0], register, value));
            CutsceneManager cutscene = session.services().ui().cutsceneManager();
            cutscene.observeIntroNoiseSound(id -> trace.printf("{\"frame\":%d,\"noise\":%d}%n", frame[0], id));
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
