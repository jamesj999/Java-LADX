package linksawakening.audio.sfx;

import linksawakening.audio.apu.GameBoyApu;
import linksawakening.rom.RomBank;

import java.util.Arrays;
import java.util.Objects;

final class RomSoundEffectEngine {
    private static final int PLAY_ACTIVE_JINGLE = 0x4204;
    private static final int PLAY_ACTIVE_WAVE = 0x53ED;
    private static final int PLAY_ACTIVE_NOISE = 0x64EC;

    private static final int W_ACTIVE_JINGLE = 0xD360;
    private static final int W_ACTIVE_WAVE = 0xD370;
    private static final int W_ACTIVE_NOISE = 0xD378;
    private static final int AUDIO_STATE_START = 0xD300;
    private static final int AUDIO_STATE_END = 0xD3FF;
    private static final int MAX_INSTRUCTIONS_PER_TICK = 20_000;

    private final byte[] romData;
    private final GameBoyApu apu;
    private final int[] memory = new int[0x10000];
    private final RomSoundEffectTables tables;

    private SoundEffectNamespace activeNamespace;

    RomSoundEffectEngine(byte[] romData, GameBoyApu apu) {
        this.romData = Objects.requireNonNull(romData, "romData");
        this.apu = Objects.requireNonNull(apu, "apu");
        tables = new RomSoundEffectTables(romData);
    }

    void start(SoundEffect effect) {
        Objects.requireNonNull(effect, "effect");
        tables.beginHandlerAddress(effect);
        tables.continueHandlerAddress(effect);
        clearAudioState();
        activeNamespace = effect.namespace();
        write8(activeAddress(activeNamespace), effect.id());
        enableApu();
        new Cpu(entryAddress(activeNamespace)).run();
        if (read8(activeAddress(activeNamespace)) == effect.id()) {
            write8(activeAddress(activeNamespace), 0);
        }
        if (!hasNamespaceState(activeNamespace)) {
            activeNamespace = null;
        }
    }

    void tick60Hz() {
        if (activeNamespace == null) {
            return;
        }
        runTick();
    }

    boolean isPlaying() {
        return activeNamespace != null && hasNamespaceState(activeNamespace);
    }

    void stop() {
        activeNamespace = null;
        clearAudioState();
    }

    private void runTick() {
        if (activeNamespace == null) {
            return;
        }
        new Cpu(entryAddress(activeNamespace)).run();
        if (!hasNamespaceState(activeNamespace)) {
            activeNamespace = null;
        }
    }

    private void enableApu() {
        apu.writeRegister(GameBoyApu.NR52, 0x80);
        apu.writeRegister(GameBoyApu.NR50, 0x77);
        apu.writeRegister(GameBoyApu.NR51, 0xFF);
        memory[0xFF00 | GameBoyApu.NR52] = 0x80;
        memory[0xFF00 | GameBoyApu.NR50] = 0x77;
        memory[0xFF00 | GameBoyApu.NR51] = 0xFF;
    }

    private void clearAudioState() {
        Arrays.fill(memory, AUDIO_STATE_START, AUDIO_STATE_END + 1, 0);
    }

    private static int entryAddress(SoundEffectNamespace namespace) {
        return switch (namespace) {
            case JINGLE -> PLAY_ACTIVE_JINGLE;
            case WAVE -> PLAY_ACTIVE_WAVE;
            case NOISE -> PLAY_ACTIVE_NOISE;
        };
    }

    private static int activeAddress(SoundEffectNamespace namespace) {
        return switch (namespace) {
            case JINGLE -> W_ACTIVE_JINGLE;
            case WAVE -> W_ACTIVE_WAVE;
            case NOISE -> W_ACTIVE_NOISE;
        };
    }

    private boolean hasNamespaceState(SoundEffectNamespace namespace) {
        int[] addresses = switch (namespace) {
            case JINGLE -> new int[] { 0xD360, 0xD361, 0xD390, 0xD394, 0xD3A0, 0xD3BC, 0xD3C6, 0xD3E2 };
            case WAVE -> new int[] { 0xD370, 0xD371, 0xD392, 0xD396, 0xD3BE, 0xD3C8, 0xD3CD, 0xD3DC, 0xD3DD, 0xD3DE, 0xD3E1 };
            case NOISE -> new int[] { 0xD34F, 0xD378, 0xD379, 0xD393, 0xD398, 0xD3BF, 0xD3C9, 0xD3DF, 0xD3E0, 0xD3E8 };
        };
        for (int address : addresses) {
            if (read8(address) != 0) {
                return true;
            }
        }
        return false;
    }

    private int read8(int address) {
        address &= 0xFFFF;
        if (address >= 0x4000 && address <= 0x7FFF) {
            int offset = RomBank.romOffset(RomSoundEffectTables.SFX_BANK, address);
            if (offset < 0 || offset >= romData.length) {
                throw new IllegalStateException("SFX ROM read outside bank 0x1F at 0x"
                        + hex16(address));
            }
            return Byte.toUnsignedInt(romData[offset]);
        }
        if (address >= 0xFF10 && address <= 0xFF26) {
            return apu.readRegister(address & 0xFF);
        }
        if (address >= 0xFF30 && address <= 0xFF3F) {
            return apu.readWaveRam(address - 0xFF30);
        }
        return memory[address] & 0xFF;
    }

    private void write8(int address, int value) {
        address &= 0xFFFF;
        int unsigned = value & 0xFF;
        memory[address] = unsigned;
        if (address >= 0xFF10 && address <= 0xFF26) {
            apu.writeRegister(address & 0xFF, unsigned);
        } else if (address >= 0xFF30 && address <= 0xFF3F) {
            apu.writeWaveRam(address - 0xFF30, unsigned);
        }
    }

    private static int signed8(int value) {
        return (byte) (value & 0xFF);
    }

    private static int unsigned16(int value) {
        return value & 0xFFFF;
    }

    private static String hex8(int value) {
        return String.format("%02X", value & 0xFF);
    }

    private static String hex16(int value) {
        return String.format("%04X", value & 0xFFFF);
    }

    private final class Cpu {
        private final int[] stack = new int[512];
        private int pc;
        private int stackSize;
        private int a;
        private int b;
        private int c;
        private int d;
        private int e;
        private int h;
        private int l;
        private boolean flagZ;
        private boolean flagN;
        private boolean flagH;
        private boolean flagC;
        private boolean stopped;

        Cpu(int entryAddress) {
            pc = entryAddress & 0xFFFF;
        }

        void run() {
            for (int instructions = 0; !stopped && instructions < MAX_INSTRUCTIONS_PER_TICK; instructions++) {
                step();
            }
            if (!stopped) {
                throw new IllegalStateException("SFX handler did not return from 0x" + hex16(pc));
            }
        }

        private void step() {
            int opcodeAddress = pc;
            int opcode = next8();
            if (opcode >= 0x40 && opcode <= 0x7F) {
                if (opcode == 0x76) {
                    throw unsupported(opcode, opcodeAddress);
                }
                setReg8((opcode >>> 3) & 0x07, getReg8(opcode & 0x07));
                return;
            }
            if (opcode >= 0x80 && opcode <= 0x87) {
                addA(getReg8(opcode & 0x07));
                return;
            }
            if (opcode >= 0x90 && opcode <= 0x97) {
                subA(getReg8(opcode & 0x07));
                return;
            }
            if (opcode >= 0xA0 && opcode <= 0xA7) {
                andA(getReg8(opcode & 0x07));
                return;
            }
            if (opcode >= 0xA8 && opcode <= 0xAF) {
                xorA(getReg8(opcode & 0x07));
                return;
            }
            if (opcode >= 0xB0 && opcode <= 0xB7) {
                orA(getReg8(opcode & 0x07));
                return;
            }
            if (opcode >= 0xB8 && opcode <= 0xBF) {
                cpA(getReg8(opcode & 0x07));
                return;
            }

            switch (opcode) {
                case 0x00 -> {
                }
                case 0x01 -> setBC(next16());
                case 0x02 -> write8(getBC(), a);
                case 0x03 -> setBC(getBC() + 1);
                case 0x04 -> b = inc8(b);
                case 0x05 -> b = dec8(b);
                case 0x06 -> b = next8();
                case 0x09 -> addHL(getBC());
                case 0x0A -> a = read8(getBC());
                case 0x0B -> setBC(getBC() - 1);
                case 0x0C -> c = inc8(c);
                case 0x0D -> c = dec8(c);
                case 0x0E -> c = next8();
                case 0x11 -> setDE(next16());
                case 0x12 -> write8(getDE(), a);
                case 0x13 -> setDE(getDE() + 1);
                case 0x14 -> d = inc8(d);
                case 0x15 -> d = dec8(d);
                case 0x16 -> d = next8();
                case 0x18 -> jr(true);
                case 0x19 -> addHL(getDE());
                case 0x1A -> a = read8(getDE());
                case 0x1B -> setDE(getDE() - 1);
                case 0x1C -> e = inc8(e);
                case 0x1D -> e = dec8(e);
                case 0x1E -> e = next8();
                case 0x20 -> jr(!flagZ);
                case 0x21 -> setHL(next16());
                case 0x22 -> {
                    write8(getHL(), a);
                    setHL(getHL() + 1);
                }
                case 0x23 -> setHL(getHL() + 1);
                case 0x24 -> h = inc8(h);
                case 0x25 -> h = dec8(h);
                case 0x26 -> h = next8();
                case 0x28 -> jr(flagZ);
                case 0x29 -> addHL(getHL());
                case 0x2A -> {
                    a = read8(getHL());
                    setHL(getHL() + 1);
                }
                case 0x2B -> setHL(getHL() - 1);
                case 0x2C -> l = inc8(l);
                case 0x2D -> l = dec8(l);
                case 0x2E -> l = next8();
                case 0x2F -> a = (~a) & 0xFF;
                case 0x30 -> jr(!flagC);
                case 0x31 -> next16();
                case 0x32 -> {
                    write8(getHL(), a);
                    setHL(getHL() - 1);
                }
                case 0x33 -> {
                }
                case 0x34 -> write8(getHL(), inc8(read8(getHL())));
                case 0x35 -> write8(getHL(), dec8(read8(getHL())));
                case 0x36 -> write8(getHL(), next8());
                case 0x37 -> {
                    flagN = false;
                    flagH = false;
                    flagC = true;
                }
                case 0x38 -> jr(flagC);
                case 0x39 -> addHL(0);
                case 0x3A -> {
                    a = read8(getHL());
                    setHL(getHL() - 1);
                }
                case 0x3B -> {
                }
                case 0x3C -> a = inc8(a);
                case 0x3D -> a = dec8(a);
                case 0x3E -> a = next8();
                case 0x3F -> {
                    flagN = false;
                    flagH = false;
                    flagC = !flagC;
                }
                case 0xC0 -> ret(!flagZ);
                case 0xC1 -> setBC(pop16());
                case 0xC2 -> jp(!flagZ);
                case 0xC3 -> pc = next16();
                case 0xC4 -> call(!flagZ);
                case 0xC5 -> push16(getBC());
                case 0xC6 -> addA(next8());
                case 0xC8 -> ret(flagZ);
                case 0xC9 -> ret(true);
                case 0xCA -> jp(flagZ);
                case 0xCB -> cb(opcodeAddress);
                case 0xCC -> call(flagZ);
                case 0xCD -> call(true);
                case 0xD0 -> ret(!flagC);
                case 0xD1 -> setDE(pop16());
                case 0xD2 -> jp(!flagC);
                case 0xD5 -> push16(getDE());
                case 0xD6 -> subA(next8());
                case 0xD8 -> ret(flagC);
                case 0xDA -> jp(flagC);
                case 0xDE -> sbcA(next8());
                case 0xE0 -> write8(0xFF00 | next8(), a);
                case 0xE1 -> setHL(pop16());
                case 0xE2 -> write8(0xFF00 | c, a);
                case 0xE5 -> push16(getHL());
                case 0xE6 -> andA(next8());
                case 0xE9 -> pc = getHL();
                case 0xEA -> write8(next16(), a);
                case 0xEE -> xorA(next8());
                case 0xF0 -> a = read8(0xFF00 | next8());
                case 0xF1 -> setAF(pop16());
                case 0xF2 -> a = read8(0xFF00 | c);
                case 0xF5 -> push16(getAF());
                case 0xF6 -> orA(next8());
                case 0xFA -> a = read8(next16());
                case 0xFE -> cpA(next8());
                default -> throw unsupported(opcode, opcodeAddress);
            }
        }

        private int next8() {
            int value = read8(pc);
            pc = (pc + 1) & 0xFFFF;
            return value;
        }

        private int next16() {
            int low = next8();
            int high = next8();
            return low | (high << 8);
        }

        private void jr(boolean condition) {
            int offset = signed8(next8());
            if (condition) {
                pc = (pc + offset) & 0xFFFF;
            }
        }

        private void jp(boolean condition) {
            int address = next16();
            if (condition) {
                pc = address;
            }
        }

        private void call(boolean condition) {
            int address = next16();
            if (condition) {
                push16(pc);
                pc = address;
            }
        }

        private void ret(boolean condition) {
            if (!condition) {
                return;
            }
            if (stackSize == 0) {
                stopped = true;
            } else {
                pc = pop16();
            }
        }

        private void push16(int value) {
            if (stackSize >= stack.length) {
                throw new IllegalStateException("SFX interpreter stack overflow");
            }
            stack[stackSize++] = value & 0xFFFF;
        }

        private int pop16() {
            if (stackSize == 0) {
                throw new IllegalStateException("SFX interpreter stack underflow");
            }
            return stack[--stackSize];
        }

        private int inc8(int value) {
            int result = (value + 1) & 0xFF;
            flagZ = result == 0;
            flagN = false;
            flagH = ((value & 0x0F) + 1) > 0x0F;
            return result;
        }

        private int dec8(int value) {
            int result = (value - 1) & 0xFF;
            flagZ = result == 0;
            flagN = true;
            flagH = (value & 0x0F) == 0;
            return result;
        }

        private void addA(int value) {
            int result = a + value;
            flagZ = (result & 0xFF) == 0;
            flagN = false;
            flagH = ((a & 0x0F) + (value & 0x0F)) > 0x0F;
            flagC = result > 0xFF;
            a = result & 0xFF;
        }

        private void subA(int value) {
            int result = a - value;
            flagZ = (result & 0xFF) == 0;
            flagN = true;
            flagH = (a & 0x0F) < (value & 0x0F);
            flagC = a < value;
            a = result & 0xFF;
        }

        private void sbcA(int value) {
            int carry = flagC ? 1 : 0;
            int result = a - value - carry;
            flagZ = (result & 0xFF) == 0;
            flagN = true;
            flagH = (a & 0x0F) < ((value & 0x0F) + carry);
            flagC = a < value + carry;
            a = result & 0xFF;
        }

        private void andA(int value) {
            a &= value;
            a &= 0xFF;
            flagZ = a == 0;
            flagN = false;
            flagH = true;
            flagC = false;
        }

        private void xorA(int value) {
            a = (a ^ value) & 0xFF;
            flagZ = a == 0;
            flagN = false;
            flagH = false;
            flagC = false;
        }

        private void orA(int value) {
            a = (a | value) & 0xFF;
            flagZ = a == 0;
            flagN = false;
            flagH = false;
            flagC = false;
        }

        private void cpA(int value) {
            int result = a - value;
            flagZ = (result & 0xFF) == 0;
            flagN = true;
            flagH = (a & 0x0F) < (value & 0x0F);
            flagC = a < value;
        }

        private void addHL(int value) {
            int hl = getHL();
            int result = hl + value;
            flagN = false;
            flagH = ((hl & 0x0FFF) + (value & 0x0FFF)) > 0x0FFF;
            flagC = result > 0xFFFF;
            setHL(result);
        }

        private void cb(int opcodeAddress) {
            int cbOpcode = next8();
            int register = cbOpcode & 0x07;
            int value = getReg8(register);
            int result;
            if (cbOpcode < 0x40) {
                switch ((cbOpcode >>> 3) & 0x07) {
                    case 0 -> {
                        result = ((value << 1) | (value >>> 7)) & 0xFF;
                        flagC = (value & 0x80) != 0;
                    }
                    case 1 -> {
                        result = ((value >>> 1) | ((value & 0x01) << 7)) & 0xFF;
                        flagC = (value & 0x01) != 0;
                    }
                    case 2 -> {
                        int carry = flagC ? 1 : 0;
                        result = ((value << 1) | carry) & 0xFF;
                        flagC = (value & 0x80) != 0;
                    }
                    case 3 -> {
                        int carry = flagC ? 0x80 : 0;
                        result = ((value >>> 1) | carry) & 0xFF;
                        flagC = (value & 0x01) != 0;
                    }
                    case 4 -> {
                        result = (value << 1) & 0xFF;
                        flagC = (value & 0x80) != 0;
                    }
                    case 5 -> {
                        result = ((value >>> 1) | (value & 0x80)) & 0xFF;
                        flagC = (value & 0x01) != 0;
                    }
                    case 6 -> {
                        result = ((value << 4) | (value >>> 4)) & 0xFF;
                        flagC = false;
                    }
                    case 7 -> {
                        result = (value >>> 1) & 0xFF;
                        flagC = (value & 0x01) != 0;
                    }
                    default -> throw unsupported(0xCB00 | cbOpcode, opcodeAddress);
                }
                setReg8(register, result);
                flagZ = result == 0;
                flagN = false;
                flagH = false;
            } else if (cbOpcode < 0x80) {
                int bit = (cbOpcode >>> 3) & 0x07;
                flagZ = (value & (1 << bit)) == 0;
                flagN = false;
                flagH = true;
            } else if (cbOpcode < 0xC0) {
                int bit = (cbOpcode >>> 3) & 0x07;
                setReg8(register, value & ~(1 << bit));
            } else {
                int bit = (cbOpcode >>> 3) & 0x07;
                setReg8(register, value | (1 << bit));
            }
        }

        private int getReg8(int code) {
            return switch (code) {
                case 0 -> b;
                case 1 -> c;
                case 2 -> d;
                case 3 -> e;
                case 4 -> h;
                case 5 -> l;
                case 6 -> read8(getHL());
                case 7 -> a;
                default -> throw new IllegalArgumentException("register code " + code);
            };
        }

        private void setReg8(int code, int value) {
            int unsigned = value & 0xFF;
            switch (code) {
                case 0 -> b = unsigned;
                case 1 -> c = unsigned;
                case 2 -> d = unsigned;
                case 3 -> e = unsigned;
                case 4 -> h = unsigned;
                case 5 -> l = unsigned;
                case 6 -> write8(getHL(), unsigned);
                case 7 -> a = unsigned;
                default -> throw new IllegalArgumentException("register code " + code);
            }
        }

        private int getAF() {
            int flags = (flagZ ? 0x80 : 0)
                    | (flagN ? 0x40 : 0)
                    | (flagH ? 0x20 : 0)
                    | (flagC ? 0x10 : 0);
            return (a << 8) | flags;
        }

        private void setAF(int value) {
            a = (value >>> 8) & 0xFF;
            int flags = value & 0xF0;
            flagZ = (flags & 0x80) != 0;
            flagN = (flags & 0x40) != 0;
            flagH = (flags & 0x20) != 0;
            flagC = (flags & 0x10) != 0;
        }

        private int getBC() {
            return (b << 8) | c;
        }

        private void setBC(int value) {
            value = unsigned16(value);
            b = value >>> 8;
            c = value & 0xFF;
        }

        private int getDE() {
            return (d << 8) | e;
        }

        private void setDE(int value) {
            value = unsigned16(value);
            d = value >>> 8;
            e = value & 0xFF;
        }

        private int getHL() {
            return (h << 8) | l;
        }

        private void setHL(int value) {
            value = unsigned16(value);
            h = value >>> 8;
            l = value & 0xFF;
        }

        private IllegalStateException unsupported(int opcode, int opcodeAddress) {
            String rendered = opcode > 0xFF ? "CB " + hex8(opcode) : hex8(opcode);
            return new IllegalStateException("Unsupported SFX opcode " + rendered
                    + " at 1F:" + hex16(opcodeAddress));
        }
    }
}
