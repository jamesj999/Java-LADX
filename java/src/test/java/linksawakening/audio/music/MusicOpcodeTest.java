package linksawakening.audio.music;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class MusicOpcodeTest {
    @Test
    void classifiesFixedMusicOpcodes() {
        assertTrue(MusicOpcode.isEnd(0x00));
        assertTrue(MusicOpcode.isRest(0x01));
        assertTrue(MusicOpcode.isDriverFlag(0x94));
        assertTrue(MusicOpcode.isDriverFlag(0x9A));
        assertTrue(MusicOpcode.isBeginLoop(0x9B));
        assertTrue(MusicOpcode.isNextLoop(0x9C));
        assertTrue(MusicOpcode.isSetEnvelopeOrWaveform(0x9D));
        assertTrue(MusicOpcode.isSetSpeed(0x9E));
        assertTrue(MusicOpcode.isSetTranspose(0x9F));
        assertTrue(MusicOpcode.isNoteLength(0xA0));
        assertTrue(MusicOpcode.isNoteLength(0xAF));
        assertTrue(MusicOpcode.isPitchedNote(0x02));
        assertTrue(MusicOpcode.isPitchedNote(0x90));
        assertTrue(MusicOpcode.isNoiseNote(0xFF));
    }

    @Test
    void returnsOperandSizesForKnownOpcodes() {
        assertEquals(0, MusicOpcode.operandSize(0x00, 1));
        assertEquals(0, MusicOpcode.operandSize(0x01, 1));
        assertEquals(1, MusicOpcode.operandSize(0x9B, 1));
        assertEquals(3, MusicOpcode.operandSize(0x9D, 1));
        assertEquals(3, MusicOpcode.operandSize(0x9D, 3));
        assertEquals(2, MusicOpcode.operandSize(0x9E, 1));
        assertEquals(1, MusicOpcode.operandSize(0x9F, 1));
        assertEquals(0, MusicOpcode.operandSize(0xA7, 2));
        assertEquals(0, MusicOpcode.operandSize(0x44, 2));
    }

    @Test
    void rejectsUnsupportedOpcodes() {
        assertThrows(IllegalArgumentException.class, () -> MusicOpcode.operandSize(0x91, 1));
        assertThrows(IllegalArgumentException.class, () -> MusicOpcode.operandSize(0xFF, 1));
    }
}
