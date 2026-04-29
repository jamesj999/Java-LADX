package linksawakening.audio.music;

public final class MusicOpcode {
    private MusicOpcode() {
    }

    public static boolean isEnd(int opcode) {
        return unsigned(opcode) == 0x00;
    }

    public static boolean isRest(int opcode) {
        return unsigned(opcode) == 0x01;
    }

    public static boolean isDriverFlag(int opcode) {
        int value = unsigned(opcode);
        return value >= 0x94 && value <= 0x9A;
    }

    public static boolean isBeginLoop(int opcode) {
        return unsigned(opcode) == 0x9B;
    }

    public static boolean isNextLoop(int opcode) {
        return unsigned(opcode) == 0x9C;
    }

    public static boolean isSetEnvelopeOrWaveform(int opcode) {
        return unsigned(opcode) == 0x9D;
    }

    public static boolean isSetSpeed(int opcode) {
        return unsigned(opcode) == 0x9E;
    }

    public static boolean isSetTranspose(int opcode) {
        return unsigned(opcode) == 0x9F;
    }

    public static boolean isNoteLength(int opcode) {
        int value = unsigned(opcode);
        return value >= 0xA0 && value <= 0xAF;
    }

    public static boolean isPitchedNote(int opcode) {
        int value = unsigned(opcode);
        return value >= 0x02 && value <= 0x90;
    }

    public static boolean isNoiseNote(int opcode) {
        return unsigned(opcode) == 0xFF;
    }

    public static int operandSize(int opcode, int channel) {
        int value = unsigned(opcode);
        if (isEnd(value) || isRest(value) || isPitchedNote(value) || isDriverFlag(value)
                || isNextLoop(value) || isNoteLength(value)) {
            return 0;
        }
        if (isNoiseNote(value)) {
            if (channel == 4) {
                return 0;
            }
            throw unsupported(value, channel);
        }
        if (isBeginLoop(value)) {
            return 1;
        }
        if (isSetEnvelopeOrWaveform(value)) {
            return 3;
        }
        if (isSetSpeed(value)) {
            return 2;
        }
        if (isSetTranspose(value)) {
            return 1;
        }
        throw unsupported(value, channel);
    }

    private static int unsigned(int opcode) {
        return opcode & 0xFF;
    }

    private static IllegalArgumentException unsupported(int opcode, int channel) {
        return new IllegalArgumentException(
                "Unsupported music opcode 0x" + Integer.toHexString(opcode).toUpperCase()
                        + " on channel " + channel);
    }
}
