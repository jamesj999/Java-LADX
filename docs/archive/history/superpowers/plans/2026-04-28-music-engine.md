# Music Engine Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a music-first Game Boy sound recreation that reads LADX music data from ROM and plays it in a standalone Swing browser through LWJGL OpenAL.

**Architecture:** Implement a Java music driver that decodes ROM command streams at 60 Hz and writes Game Boy audio-register state into a software APU. The APU synthesizes deterministic signed 16-bit stereo PCM, and OpenAL streams that PCM for the Swing browser target.

**Tech Stack:** Java 21, Gradle application plugin, JUnit 5, LWJGL 3.3.4 OpenAL, Swing/AWT.

---

## Coordination Notes

The orchestrator will not edit Java production or test code. Workers own the file groups listed in their tasks and must not revert or rewrite files owned by other workers. Workers are not alone in the codebase; preserve unrelated changes and adapt to already-merged code.

Use TDD for every Java behavior change: write the failing test, run it and confirm the expected failure, implement the minimal code, run the targeted test, then run the relevant package test set.

The project root is `/Users/jamesjohnson/Documents/Personal/LinksAwakening`; Java commands run from `java/`.

## File Structure

Create these packages and files:

- `java/src/main/java/linksawakening/audio/music/MusicTrack.java`
- `java/src/main/java/linksawakening/audio/music/MusicCatalog.java`
- `java/src/main/java/linksawakening/audio/music/MusicOpcode.java`
- `java/src/main/java/linksawakening/audio/music/MusicChannelState.java`
- `java/src/main/java/linksawakening/audio/music/MusicDriver.java`
- `java/src/main/java/linksawakening/audio/apu/GameBoyApu.java`
- `java/src/main/java/linksawakening/audio/apu/SquareChannel.java`
- `java/src/main/java/linksawakening/audio/apu/WaveChannel.java`
- `java/src/main/java/linksawakening/audio/apu/NoiseChannel.java`
- `java/src/main/java/linksawakening/audio/openal/OpenAlMusicPlayer.java`
- `java/src/main/java/linksawakening/audio/browser/AudioBrowserMain.java`
- `java/src/main/java/linksawakening/audio/browser/AudioBrowserFrame.java`

Tests:

- `java/src/test/java/linksawakening/audio/music/MusicCatalogTest.java`
- `java/src/test/java/linksawakening/audio/music/MusicOpcodeTest.java`
- `java/src/test/java/linksawakening/audio/music/MusicDriverTest.java`
- `java/src/test/java/linksawakening/audio/apu/GameBoyApuTest.java`
- `java/src/test/java/linksawakening/audio/browser/AudioBrowserFrameTest.java`

Modify:

- `java/build.gradle`

---

### Task 1: Music Catalog And Opcode Model

**Ownership:** `linksawakening.audio.music.MusicTrack`, `MusicCatalog`, `MusicOpcode`, and their tests only.

**Files:**
- Create: `java/src/main/java/linksawakening/audio/music/MusicTrack.java`
- Create: `java/src/main/java/linksawakening/audio/music/MusicCatalog.java`
- Create: `java/src/main/java/linksawakening/audio/music/MusicOpcode.java`
- Test: `java/src/test/java/linksawakening/audio/music/MusicCatalogTest.java`
- Test: `java/src/test/java/linksawakening/audio/music/MusicOpcodeTest.java`

- [ ] **Step 1: Write failing catalog tests**

Create `MusicCatalogTest` with tests proving:

```java
package linksawakening.audio.music;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class MusicCatalogTest {
    @Test
    void resolvesRepresentativeTracksFromBothMusicBanks() {
        MusicCatalog catalog = MusicCatalog.fromRom(loadRom());

        assertTrack(catalog.requireTrack(0x01), "MUSIC_TITLE_SCREEN", 0x1B, 0x4077);
        assertTrack(catalog.requireTrack(0x10), "MUSIC_OBTAIN_ITEM", 0x1B, 0x4077 + (0x0F * 2));
        assertTrack(catalog.requireTrack(0x11), "MUSIC_FILE_SELECT", 0x1E, 0x407F);
        assertTrack(catalog.requireTrack(0x50), "MUSIC_MINIBOSS", 0x1E, 0x407F + (0x3F * 2));
        assertTrack(catalog.requireTrack(0x61), "MUSIC_COLOR_DUNGEON", 0x1B, 0x4077 + (0x20 * 2));
    }

    @Test
    void listsPlayableTracksInMusicIdOrder() {
        MusicCatalog catalog = MusicCatalog.fromRom(loadRom());

        assertEquals(0x70, catalog.tracks().size());
        assertEquals(0x01, catalog.tracks().get(0).id());
        assertEquals(0x70, catalog.tracks().get(catalog.tracks().size() - 1).id());
    }

    @Test
    void rejectsUnknownTrackIds() {
        MusicCatalog catalog = MusicCatalog.fromRom(loadRom());

        assertThrows(IllegalArgumentException.class, () -> catalog.requireTrack(0x00));
        assertThrows(IllegalArgumentException.class, () -> catalog.requireTrack(0x71));
    }

    private static void assertTrack(MusicTrack track, String name, int bank, int tableAddress) {
        assertEquals(name, track.name());
        assertEquals(bank, track.bank());
        assertEquals(tableAddress, track.pointerTableAddress());
        assertTrue(track.headerAddress() >= 0x4000 && track.headerAddress() < 0x8000);
        assertTrue(track.romOffset() >= 0);
    }

    private static byte[] loadRom() {
        try (var stream = MusicCatalogTest.class.getClassLoader().getResourceAsStream("rom/azle.gbc")) {
            if (stream == null) {
                throw new IllegalStateException("ROM resource missing");
            }
            return stream.readAllBytes();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load ROM", e);
        }
    }
}
```

- [ ] **Step 2: Write failing opcode tests**

Create `MusicOpcodeTest` proving operand sizing and classification:

```java
package linksawakening.audio.music;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

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
```

- [ ] **Step 3: Run tests to verify RED**

Run: `gradle test --tests linksawakening.audio.music.MusicCatalogTest --tests linksawakening.audio.music.MusicOpcodeTest`

Expected: compilation fails because `MusicCatalog`, `MusicTrack`, and `MusicOpcode` do not exist.

- [ ] **Step 4: Implement catalog and opcode model**

Implement these public APIs:

```java
public record MusicTrack(int id, String name, int bank, int pointerTableAddress,
                         int headerAddress, int romOffset) {}
```

```java
public final class MusicCatalog {
    public static MusicCatalog fromRom(byte[] romData);
    public List<MusicTrack> tracks();
    public Optional<MusicTrack> track(int id);
    public MusicTrack requireTrack(int id);
}
```

Rules:

- Bank `0x1B` pointer table starts at `0x4077` and has `0x30` entries.
- Bank `0x1E` pointer table starts at `0x407F` and has `0x40` entries.
- ID transforms:
  - bank `0x1B`: input `1..0x10 -> same`, `0x11..0x20 -> +0x20`, `0x21..0x30 -> +0x40`
  - bank `0x1E`: input `1..0x20 -> +0x10`, `0x21..0x40 -> +0x20`
- Track names come from `constants/sfx.asm` names represented as Java string constants. Include all ids `0x01..0x70`, preserving names like `MUSIC_TITLE_SCREEN`.
- Header pointer is little-endian at the pointer table entry.
- Header ROM offset uses `RomBank.romOffset(bank, headerAddress)`.
- Return `tracks()` sorted by public music id.

Implement `MusicOpcode` as a final utility class with the methods used in tests.

- [ ] **Step 5: Run tests to verify GREEN**

Run: `gradle test --tests linksawakening.audio.music.MusicCatalogTest --tests linksawakening.audio.music.MusicOpcodeTest`

Expected: PASS.

---

### Task 2: Deterministic APU Core

**Ownership:** `linksawakening.audio.apu.*` and `GameBoyApuTest` only.

**Files:**
- Create: `java/src/main/java/linksawakening/audio/apu/GameBoyApu.java`
- Create: `java/src/main/java/linksawakening/audio/apu/SquareChannel.java`
- Create: `java/src/main/java/linksawakening/audio/apu/WaveChannel.java`
- Create: `java/src/main/java/linksawakening/audio/apu/NoiseChannel.java`
- Test: `java/src/test/java/linksawakening/audio/apu/GameBoyApuTest.java`

- [ ] **Step 1: Write failing APU tests**

Create `GameBoyApuTest`:

```java
package linksawakening.audio.apu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class GameBoyApuTest {
    @Test
    void squareChannelProducesDeterministicNonSilentPcmAfterTrigger() {
        GameBoyApu apu = new GameBoyApu(48_000);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0xFF);
        apu.writeRegister(GameBoyApu.NR11, 0x80);
        apu.writeRegister(GameBoyApu.NR12, 0xF0);
        apu.writeRegister(GameBoyApu.NR13, 0x00);
        apu.writeRegister(GameBoyApu.NR14, 0x87);

        short[] first = apu.render(512);
        apu.reset();
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0xFF);
        apu.writeRegister(GameBoyApu.NR11, 0x80);
        apu.writeRegister(GameBoyApu.NR12, 0xF0);
        apu.writeRegister(GameBoyApu.NR13, 0x00);
        apu.writeRegister(GameBoyApu.NR14, 0x87);
        short[] second = apu.render(512);

        assertArrayEquals(first, second);
        assertTrue(hasNonZeroSample(first));
    }

    @Test
    void waveChannelUsesWaveRamAndOutputLevel() {
        GameBoyApu apu = new GameBoyApu(48_000);
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0xFF);
        for (int i = 0; i < 16; i++) {
            apu.writeWaveRam(i, (i << 4) | (15 - i));
        }
        apu.writeRegister(GameBoyApu.NR30, 0x80);
        apu.writeRegister(GameBoyApu.NR32, 0x20);
        apu.writeRegister(GameBoyApu.NR33, 0x00);
        apu.writeRegister(GameBoyApu.NR34, 0x87);

        short[] pcm = apu.render(512);

        assertTrue(hasNonZeroSample(pcm));
    }

    @Test
    void disabledMasterPowerOutputsSilence() {
        GameBoyApu apu = new GameBoyApu(48_000);
        apu.writeRegister(GameBoyApu.NR52, 0x00);
        apu.writeRegister(GameBoyApu.NR12, 0xF0);
        apu.writeRegister(GameBoyApu.NR14, 0x87);

        short[] pcm = apu.render(128);

        assertFalse(hasNonZeroSample(pcm));
    }

    private static boolean hasNonZeroSample(short[] pcm) {
        for (short sample : pcm) {
            if (sample != 0) {
                return true;
            }
        }
        return false;
    }
}
```

- [ ] **Step 2: Run test to verify RED**

Run: `gradle test --tests linksawakening.audio.apu.GameBoyApuTest`

Expected: compilation fails because `GameBoyApu` does not exist.

- [ ] **Step 3: Implement minimal APU**

Public API:

```java
public final class GameBoyApu {
    public static final int NR10 = 0x10;
    public static final int NR11 = 0x11;
    public static final int NR12 = 0x12;
    public static final int NR13 = 0x13;
    public static final int NR14 = 0x14;
    public static final int NR21 = 0x16;
    public static final int NR22 = 0x17;
    public static final int NR23 = 0x18;
    public static final int NR24 = 0x19;
    public static final int NR30 = 0x1A;
    public static final int NR31 = 0x1B;
    public static final int NR32 = 0x1C;
    public static final int NR33 = 0x1D;
    public static final int NR34 = 0x1E;
    public static final int NR41 = 0x20;
    public static final int NR42 = 0x21;
    public static final int NR43 = 0x22;
    public static final int NR44 = 0x23;
    public static final int NR50 = 0x24;
    public static final int NR51 = 0x25;
    public static final int NR52 = 0x26;

    public GameBoyApu(int sampleRate);
    public void reset();
    public int sampleRate();
    public void writeRegister(int register, int value);
    public int readRegister(int register);
    public void writeWaveRam(int index, int value);
    public short[] render(int frames);
    public boolean isChannelActive(int channel);
}
```

Implementation requirements:

- `render(frames)` returns interleaved stereo samples, length `frames * 2`.
- If bit 7 of `NR52` is clear, output silence.
- Square frequencies use Game Boy formula `131072 / (2048 - frequency)`.
- Duty patterns: 12.5%, 25%, 50%, 75%.
- Initial square volume comes from high nibble of `NRx2`.
- Trigger occurs when bit 7 is set in `NR14` or `NR24`.
- Wave channel uses 32 4-bit samples from 16-byte wave RAM.
- Wave output level follows `NR32` bits 5-6: mute, 100%, 50%, 25%.
- Mixer can be simple average/clamped signed 16-bit stereo; exact analog filtering is out of scope.
- `NoiseChannel` can initially return `0.0` while exposing reset/write/tick/render methods.

- [ ] **Step 4: Run test to verify GREEN**

Run: `gradle test --tests linksawakening.audio.apu.GameBoyApuTest`

Expected: PASS.

---

### Task 3: ROM Music Driver

**Ownership:** `MusicDriver`, `MusicChannelState`, and `MusicDriverTest` only. Use the public APIs from Tasks 1 and 2; do not edit APU implementation unless a reviewer asks.

**Files:**
- Create: `java/src/main/java/linksawakening/audio/music/MusicChannelState.java`
- Create: `java/src/main/java/linksawakening/audio/music/MusicDriver.java`
- Test: `java/src/test/java/linksawakening/audio/music/MusicDriverTest.java`

- [ ] **Step 1: Write failing driver tests**

Create tests using synthetic ROM data so behavior is isolated from full tracks:

```java
package linksawakening.audio.music;

import linksawakening.audio.apu.GameBoyApu;
import linksawakening.rom.RomBank;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

final class MusicDriverTest {
    @Test
    void startsTrackAndWritesSquareRegistersForFirstNote() {
        byte[] rom = syntheticRomWithOneChannel(new byte[] {
            (byte) 0x9D, (byte) 0xF0, 0x00, (byte) 0x80,
            (byte) 0xA2,
            0x4A,
            0x00
        });
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        driver.tick60Hz();

        assertEquals(0x80, apu.readRegister(GameBoyApu.NR11));
        assertEquals(0xF0, apu.readRegister(GameBoyApu.NR12));
        assertTrue((apu.readRegister(GameBoyApu.NR14) & 0x80) != 0);
        assertTrue(driver.isChannelActive(1));
    }

    @Test
    void loopOpcodeRepeatsChannelDefinitions() {
        byte[] rom = syntheticRomWithOneChannel(new byte[] {
            (byte) 0xA1,
            (byte) 0x9B, 0x02,
            0x4A,
            (byte) 0x9C,
            0x00
        });
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        driver.tick60Hz();
        assertTrue(driver.isChannelActive(1));
        driver.tick60Hz();
        assertTrue(driver.isChannelActive(1));
    }

    @Test
    void channel3SetWaveformCopiesWaveRam() {
        byte[] rom = syntheticRomWithChannel3(new byte[] {
            (byte) 0x9D, 0x00, 0x56, 0x20,
            (byte) 0xA1,
            0x4A,
            0x00
        });
        int waveformOffset = RomBank.romOffset(0x1B, 0x5600);
        for (int i = 0; i < 16; i++) {
            rom[waveformOffset + i] = (byte) i;
        }
        GameBoyApu apu = new GameBoyApu(48_000);
        MusicDriver driver = new MusicDriver(rom, apu);

        driver.start(new MusicTrack(0x01, "TEST", 0x1B, 0x4077, 0x5000, RomBank.romOffset(0x1B, 0x5000)));
        driver.tick60Hz();

        assertEquals(0x80, apu.readRegister(GameBoyApu.NR30));
        assertEquals(0x20, apu.readRegister(GameBoyApu.NR32));
    }

    private static byte[] syntheticRomWithOneChannel(byte[] definition) {
        byte[] rom = new byte[0x80000];
        int header = RomBank.romOffset(0x1B, 0x5000);
        writeHeader(rom, header, 0x5100, 0x5200, 0, 0, 0);
        writeWord(rom, RomBank.romOffset(0x1B, 0x5200), 0x5300);
        System.arraycopy(definition, 0, rom, RomBank.romOffset(0x1B, 0x5300), definition.length);
        return rom;
    }

    private static byte[] syntheticRomWithChannel3(byte[] definition) {
        byte[] rom = new byte[0x80000];
        int header = RomBank.romOffset(0x1B, 0x5000);
        writeHeader(rom, header, 0x5100, 0, 0, 0x5200, 0);
        writeWord(rom, RomBank.romOffset(0x1B, 0x5200), 0x5300);
        System.arraycopy(definition, 0, rom, RomBank.romOffset(0x1B, 0x5300), definition.length);
        return rom;
    }

    private static void writeHeader(byte[] rom, int offset, int speed, int ch1, int ch2, int ch3, int ch4) {
        rom[offset] = 0;
        writeWordAtOffset(rom, offset + 1, speed);
        writeWordAtOffset(rom, offset + 3, ch1);
        writeWordAtOffset(rom, offset + 5, ch2);
        writeWordAtOffset(rom, offset + 7, ch3);
        writeWordAtOffset(rom, offset + 9, ch4);
        int speedOffset = RomBank.romOffset(0x1B, speed);
        for (int i = 0; i < 16; i++) {
            rom[speedOffset + i] = (byte) (i + 1);
        }
    }

    private static void writeWord(byte[] rom, int offset, int value) {
        writeWordAtOffset(rom, offset, value);
    }

    private static void writeWordAtOffset(byte[] rom, int offset, int value) {
        rom[offset] = (byte) (value & 0xFF);
        rom[offset + 1] = (byte) ((value >> 8) & 0xFF);
    }
}
```

- [ ] **Step 2: Run test to verify RED**

Run: `gradle test --tests linksawakening.audio.music.MusicDriverTest`

Expected: compilation fails because `MusicDriver` and `MusicChannelState` do not exist.

- [ ] **Step 3: Implement driver**

Public API:

```java
public final class MusicDriver {
    public MusicDriver(byte[] romData, GameBoyApu apu);
    public void start(MusicTrack track);
    public void stop();
    public void tick60Hz();
    public boolean isPlaying();
    public boolean isChannelActive(int channel);
}
```

Driver behavior:

- On start, reset APU and write `NR52=0x80`, `NR50=0x77`, `NR51=0xFF`.
- Read track header from `track.romOffset()`.
- Store track bank and speed table pointer.
- Channel stream pointers point to lists of 16-bit definition pointers. `0x0000` ends, `0xFFFF` followed by pointer loops back.
- Each channel has a current definition pointer. Parse opcodes until a rest or note consumes time, then stop parsing for that tick.
- `notelen` selects `speedTable[opcode & 0x0F]`.
- For pitched notes, compute frequency from note code. Use the ROM-compatible semitone scale closely enough for tests; exact frequency-table refinement can follow observed disassembly constants.
- Channel 1 writes `NR11..NR14`; channel 2 writes `NR21..NR24`; channel 3 writes `NR30..NR34` and wave RAM.
- `0x9D` on channels 1/2 stores envelope and duty/length byte; on channel 3 copies 16 bytes from waveform pointer into APU wave RAM and writes output level.
- `0x9B/0x9C` loop within the current definition using the loop count byte.
- Unsupported opcodes throw `IllegalStateException` with channel, bank, and address.

- [ ] **Step 4: Run tests to verify GREEN**

Run: `gradle test --tests linksawakening.audio.music.MusicDriverTest`

Expected: PASS.

---

### Task 4: OpenAL Player And Gradle Target

**Ownership:** `OpenAlMusicPlayer` and `java/build.gradle` only. Do not edit browser files.

**Files:**
- Create: `java/src/main/java/linksawakening/audio/openal/OpenAlMusicPlayer.java`
- Modify: `java/build.gradle`

- [ ] **Step 1: Add LWJGL OpenAL dependencies**

Modify `java/build.gradle`:

```groovy
implementation "org.lwjgl:lwjgl-openal:3.3.4"
runtimeOnly "org.lwjgl:lwjgl-openal:3.3.4:natives-macos-arm64"
```

Add a launch task:

```groovy
tasks.register('runAudioBrowser', JavaExec) {
    group = 'application'
    description = 'Run the Link Awakening music browser'
    classpath = sourceSets.main.runtimeClasspath
    mainClass = 'linksawakening.audio.browser.AudioBrowserMain'
}
```

- [ ] **Step 2: Implement OpenAL player wrapper**

Create:

```java
package linksawakening.audio.openal;

import linksawakening.audio.apu.GameBoyApu;
import linksawakening.audio.music.MusicDriver;
import linksawakening.audio.music.MusicTrack;

public final class OpenAlMusicPlayer implements AutoCloseable {
    public OpenAlMusicPlayer(MusicDriver driver, GameBoyApu apu);
    public boolean isAvailable();
    public String statusMessage();
    public void play(MusicTrack track);
    public void pause();
    public void resume();
    public void stop();
    public void setLoopEnabled(boolean loopEnabled);
    public void setVolume(float volume);
    public void update();
    @Override public void close();
}
```

Implementation requirements:

- Initialize ALC device/context in the constructor.
- If initialization fails, set `available=false`, retain a concise status message, and let the object be safely closed.
- Use one OpenAL source and a small rotating queue of buffers.
- In `update()`, unqueue processed buffers, advance the driver at 60 Hz according to elapsed time, render PCM from `GameBoyApu`, and queue stereo 16-bit buffers.
- Clamp volume to `0.0..1.0`.
- `stop()` stops the source, clears queued buffers, and calls `driver.stop()`.
- `close()` releases buffers, source, context, and device.

- [ ] **Step 3: Compile**

Run: `gradle compileJava`

Expected: compilation may fail until Task 5 creates `AudioBrowserMain`; if the only failure is missing `AudioBrowserMain`, report `DONE_WITH_CONCERNS`. Otherwise fix OpenAL/Gradle compilation errors.

---

### Task 5: Swing Audio Browser

**Ownership:** `linksawakening.audio.browser.*` and browser tests only.

**Files:**
- Create: `java/src/main/java/linksawakening/audio/browser/AudioBrowserMain.java`
- Create: `java/src/main/java/linksawakening/audio/browser/AudioBrowserFrame.java`
- Test: `java/src/test/java/linksawakening/audio/browser/AudioBrowserFrameTest.java`

- [ ] **Step 1: Write failing browser tests**

Create `AudioBrowserFrameTest`:

```java
package linksawakening.audio.browser;

import linksawakening.audio.music.MusicTrack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

final class AudioBrowserFrameTest {
    @Test
    void filterTracksByNameOrHexId() {
        AudioBrowserFrame.BrowserModel model = new AudioBrowserFrame.BrowserModel(List.of(
            new MusicTrack(0x01, "MUSIC_TITLE_SCREEN", 0x1B, 0x4077, 0x5000, 0x6C000),
            new MusicTrack(0x04, "MUSIC_MABE_VILLAGE", 0x1B, 0x407D, 0x5100, 0x6C100),
            new MusicTrack(0x11, "MUSIC_FILE_SELECT", 0x1E, 0x407F, 0x5200, 0x78200)
        ));

        assertEquals(1, model.filteredTracks("mabe").size());
        assertEquals(1, model.filteredTracks("0x11").size());
        assertEquals(3, model.filteredTracks("").size());
    }

    @Test
    void selectedTrackMetadataIncludesBankAndHeader() {
        MusicTrack track = new MusicTrack(0x04, "MUSIC_MABE_VILLAGE", 0x1B, 0x407D, 0x5100, 0x6C100);

        String metadata = AudioBrowserFrame.BrowserModel.metadata(track);

        assertTrue(metadata.contains("0x04"));
        assertTrue(metadata.contains("MUSIC_MABE_VILLAGE"));
        assertTrue(metadata.contains("bank 0x1B"));
        assertTrue(metadata.contains("header 0x5100"));
    }
}
```

- [ ] **Step 2: Run test to verify RED**

Run: `gradle test --tests linksawakening.audio.browser.AudioBrowserFrameTest`

Expected: compilation fails because browser classes do not exist.

- [ ] **Step 3: Implement browser**

`AudioBrowserMain`:

- Load `rom/azle.gbc` from resources.
- Build `MusicCatalog.fromRom(romData)`.
- Create `GameBoyApu(48_000)`, `MusicDriver`, and `OpenAlMusicPlayer`.
- Start Swing on the event dispatch thread.

`AudioBrowserFrame`:

- Extends `JFrame`.
- Contains a nested public static `BrowserModel` used by tests.
- UI controls: track list, search field, Play, Pause/Resume, Stop, loop checkbox, volume slider, metadata label/text area, status label, channel activity labels.
- Uses a Swing `Timer` around 30 ms to call `player.update()` and refresh channel activity.
- If `player.isAvailable()` is false, disable Play/Pause/Stop and show `player.statusMessage()`.

- [ ] **Step 4: Run tests to verify GREEN**

Run: `gradle test --tests linksawakening.audio.browser.AudioBrowserFrameTest`

Expected: PASS.

---

### Task 6: Integration Verification And Browser Launch

**Ownership:** no broad refactors. Fix only integration compile/test errors in files already introduced by Tasks 1-5.

**Files:**
- Modify only files under `java/src/main/java/linksawakening/audio/`, `java/src/test/java/linksawakening/audio/`, and `java/build.gradle`.

- [ ] **Step 1: Run audio package tests**

Run: `gradle test --tests linksawakening.audio.*`

Expected: PASS. If tests fail, identify the owning task area and fix only that area.

- [ ] **Step 2: Run full Java test suite**

Run: `gradle test`

Expected: PASS, or existing unrelated failures documented with exact failing test names and compiler errors.

- [ ] **Step 3: Compile browser target**

Run: `gradle runAudioBrowser --dry-run`

Expected: Gradle recognizes `:runAudioBrowser`.

- [ ] **Step 4: Final smoke compile**

Run: `gradle compileJava`

Expected: PASS.

---

## Final Review Checklist

- Music catalog reads ROM pointer tables from banks `0x1B` and `0x1E`.
- Music opcode model covers all opcodes from the design.
- Driver writes `GameBoyApu` registers instead of writing directly to OpenAL.
- APU output is deterministic under unit tests.
- Browser opens independently from `linksawakening.Main`.
- OpenAL failures are non-fatal to the browser window.
- No SFX/jingle implementation was added in this milestone.
