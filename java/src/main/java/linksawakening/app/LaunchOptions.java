package linksawakening.app;

import java.nio.file.Path;
import linksawakening.recording.RecordingOptions;

/** Validates command-line settings before opening native resources. */
public record LaunchOptions(boolean headless, long frames, boolean newGame, RecordingOptions recording, boolean help) {
    public static LaunchOptions parse(String[] args) {
        boolean headless = false, newGame = false, help = false;
        long frames = 0;
        Path output = null;
        int scale = 1, crf = 18;
        String ffmpeg = "ffmpeg";
        boolean encodingOptions = false;
        for (int i = 0; i < args.length; i++) {
            String option = args[i];
            switch (option) {
                case "--headless" -> headless = true;
                case "--new-game" -> newGame = true;
                case "--help", "-h" -> help = true;
                case "--record", "--frames", "--record-scale", "--record-crf", "--ffmpeg" -> {
                    if (++i == args.length) throw new IllegalArgumentException("Missing value for " + option);
                    String value = args[i];
                    switch (option) {
                        case "--record" -> output = Path.of(value);
                        case "--frames" -> {
                            frames = Long.parseLong(value);
                            if (frames <= 0) throw new IllegalArgumentException("--frames must be positive");
                        }
                        case "--record-scale" -> {
                            scale = Integer.parseInt(value);
                            encodingOptions = true;
                        }
                        case "--record-crf" -> {
                            crf = Integer.parseInt(value);
                            encodingOptions = true;
                        }
                        case "--ffmpeg" -> {
                            ffmpeg = value;
                            encodingOptions = true;
                        }
                        default -> throw new AssertionError(option);
                    }
                }
                default -> throw new IllegalArgumentException("Unknown option: " + option + "; use --help");
            }
        }
        if (!help && headless && frames == 0) throw new IllegalArgumentException("--headless requires --frames");
        if (!help && encodingOptions && output == null)
            throw new IllegalArgumentException("Recording settings require --record output.mp4");
        return new LaunchOptions(
                headless,
                frames,
                newGame,
                output == null ? null : new RecordingOptions(output, scale, crf, ffmpeg),
                help);
    }

    public static String usage() {
        return """
                Usage: linksawakening [options]
                  --record FILE.mp4    Record video and audio (requires FFmpeg with libx264/AAC)
                  --headless           Run without a window/audio device, as fast as encoding permits
                  --frames N           Stop after N frames (required for headless; 60 frames = 1 second)
                  --new-game           Start a fresh game instead of the configured title/intro
                  --record-scale N     Integer nearest-neighbor scale, 1–8 (default 1: 160×144)
                  --record-crf N       H.264 quality, 0–51; lower is higher quality (default 18)
                  --ffmpeg PATH        Encoder executable (default ffmpeg on PATH)
                  --help               Show this help
                Existing output files are never overwritten. Close normally to finalize the MP4.
                """;
    }
}
