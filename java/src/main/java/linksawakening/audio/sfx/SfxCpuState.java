package linksawakening.audio.sfx;

import static linksawakening.audio.sfx.RomSoundEffectEngine.*;

/** Reusable interpreter registers, stack, flags and arithmetic. */
class SfxCpuState {
    final RomSoundEffectEngine bus;

    SfxCpuState(RomSoundEffectEngine bus) {
        this.bus = bus;
    }

    void step() {
        throw new AssertionError("opcode decoder required");
    }

    final int[] stack = new int[512];
    int pc;
    int stackSize;
    int a;
    int b;
    int c;
    int d;
    int e;
    int h;
    int l;
    boolean flagZ;
    boolean flagN;
    boolean flagH;
    boolean flagC;
    boolean stopped;

    void runFrom(int entryAddress) {
        pc = entryAddress & 0xFFFF;
        stackSize = a = b = c = d = e = h = l = 0;
        flagZ = flagN = flagH = flagC = stopped = false;
        run();
    }

    void run() {
        for (int instructions = 0; !stopped && instructions < MAX_INSTRUCTIONS_PER_TICK; instructions++) {
            step();
        }
        if (!stopped) {
            throw new IllegalStateException("SFX handler did not return from 0x" + hex16(pc));
        }
    }

    int next8() {
        int value = bus.read8(pc);
        pc = (pc + 1) & 0xFFFF;
        return value;
    }

    int next16() {
        int low = next8();
        int high = next8();
        return low | (high << 8);
    }

    void jr(boolean condition) {
        int offset = signed8(next8());
        if (condition) {
            pc = (pc + offset) & 0xFFFF;
        }
    }

    void jp(boolean condition) {
        int address = next16();
        if (condition) {
            pc = address;
        }
    }

    void call(boolean condition) {
        int address = next16();
        if (condition) {
            push16(pc);
            pc = address;
        }
    }

    void ret(boolean condition) {
        if (!condition) {
            return;
        }
        if (stackSize == 0) {
            stopped = true;
        } else {
            pc = pop16();
        }
    }

    void push16(int value) {
        if (stackSize >= stack.length) {
            throw new IllegalStateException("SFX interpreter stack overflow");
        }
        stack[stackSize++] = value & 0xFFFF;
    }

    int pop16() {
        if (stackSize == 0) {
            throw new IllegalStateException("SFX interpreter stack underflow");
        }
        return stack[--stackSize];
    }

    int inc8(int value) {
        int result = (value + 1) & 0xFF;
        flagZ = result == 0;
        flagN = false;
        flagH = ((value & 0x0F) + 1) > 0x0F;
        return result;
    }

    int dec8(int value) {
        int result = (value - 1) & 0xFF;
        flagZ = result == 0;
        flagN = true;
        flagH = (value & 0x0F) == 0;
        return result;
    }

    void addA(int value) {
        int result = a + value;
        flagZ = (result & 0xFF) == 0;
        flagN = false;
        flagH = ((a & 0x0F) + (value & 0x0F)) > 0x0F;
        flagC = result > 0xFF;
        a = result & 0xFF;
    }

    void subA(int value) {
        int result = a - value;
        flagZ = (result & 0xFF) == 0;
        flagN = true;
        flagH = (a & 0x0F) < (value & 0x0F);
        flagC = a < value;
        a = result & 0xFF;
    }

    void sbcA(int value) {
        int carry = flagC ? 1 : 0;
        int result = a - value - carry;
        flagZ = (result & 0xFF) == 0;
        flagN = true;
        flagH = (a & 0x0F) < ((value & 0x0F) + carry);
        flagC = a < value + carry;
        a = result & 0xFF;
    }

    void andA(int value) {
        a &= value;
        a &= 0xFF;
        flagZ = a == 0;
        flagN = false;
        flagH = true;
        flagC = false;
    }

    void xorA(int value) {
        a = (a ^ value) & 0xFF;
        flagZ = a == 0;
        flagN = false;
        flagH = false;
        flagC = false;
    }

    void orA(int value) {
        a = (a | value) & 0xFF;
        flagZ = a == 0;
        flagN = false;
        flagH = false;
        flagC = false;
    }

    void cpA(int value) {
        int result = a - value;
        flagZ = (result & 0xFF) == 0;
        flagN = true;
        flagH = (a & 0x0F) < (value & 0x0F);
        flagC = a < value;
    }

    void addHL(int value) {
        int hl = getHL();
        int result = hl + value;
        flagN = false;
        flagH = ((hl & 0x0FFF) + (value & 0x0FFF)) > 0x0FFF;
        flagC = result > 0xFFFF;
        setHL(result);
    }

    void cb(int opcodeAddress) {
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

    int getReg8(int code) {
        return switch (code) {
            case 0 -> b;
            case 1 -> c;
            case 2 -> d;
            case 3 -> e;
            case 4 -> h;
            case 5 -> l;
            case 6 -> bus.read8(getHL());
            case 7 -> a;
            default -> throw new IllegalArgumentException("register code " + code);
        };
    }

    void setReg8(int code, int value) {
        int unsigned = value & 0xFF;
        switch (code) {
            case 0 -> b = unsigned;
            case 1 -> c = unsigned;
            case 2 -> d = unsigned;
            case 3 -> e = unsigned;
            case 4 -> h = unsigned;
            case 5 -> l = unsigned;
            case 6 -> bus.write8(getHL(), unsigned);
            case 7 -> a = unsigned;
            default -> throw new IllegalArgumentException("register code " + code);
        }
    }

    int getAF() {
        int flags = (flagZ ? 0x80 : 0) | (flagN ? 0x40 : 0) | (flagH ? 0x20 : 0) | (flagC ? 0x10 : 0);
        return (a << 8) | flags;
    }

    void setAF(int value) {
        a = (value >>> 8) & 0xFF;
        int flags = value & 0xF0;
        flagZ = (flags & 0x80) != 0;
        flagN = (flags & 0x40) != 0;
        flagH = (flags & 0x20) != 0;
        flagC = (flags & 0x10) != 0;
    }

    int getBC() {
        return (b << 8) | c;
    }

    void setBC(int value) {
        value = unsigned16(value);
        b = value >>> 8;
        c = value & 0xFF;
    }

    int getDE() {
        return (d << 8) | e;
    }

    void setDE(int value) {
        value = unsigned16(value);
        d = value >>> 8;
        e = value & 0xFF;
    }

    int getHL() {
        return (h << 8) | l;
    }

    void setHL(int value) {
        value = unsigned16(value);
        h = value >>> 8;
        l = value & 0xFF;
    }

    IllegalStateException unsupported(int opcode, int opcodeAddress) {
        String rendered = opcode > 0xFF ? "CB " + hex8(opcode) : hex8(opcode);
        return new IllegalStateException("Unsupported SFX opcode " + rendered + " at 1F:" + hex16(opcodeAddress));
    }
}
