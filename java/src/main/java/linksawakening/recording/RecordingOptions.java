package linksawakening.recording;

import java.nio.file.Path;
import java.util.Objects;

/** MP4 encoding settings, independent of the desktop window scale. */
public record RecordingOptions(Path output, int scale, int crf, String ffmpeg) {
    public RecordingOptions {
        Objects.requireNonNull(output);
        if (!output.toString().toLowerCase(java.util.Locale.ROOT).endsWith(".mp4"))
            throw new IllegalArgumentException("Recording output must end in .mp4");
        if (scale < 1 || scale > 8) throw new IllegalArgumentException("Recording scale must be 1–8");
        if (crf < 0 || crf > 51) throw new IllegalArgumentException("Recording CRF must be 0–51");
        if (ffmpeg == null || ffmpeg.isBlank()) throw new IllegalArgumentException("FFmpeg executable is required");
    }

    public static RecordingOptions defaults(Path output) {
        return new RecordingOptions(output, 1, 18, "ffmpeg");
    }
}
