package linksawakening.audio.sfx;
/** Opcode decoding for the ROM-backed SFX interpreter. */
final class SfxCpu extends SfxCpuState {
    SfxCpu(RomSoundEffectEngine bus) {
        super(bus);
    }

    @Override
    void step() {
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

        if (opcode < 0x20) low(opcode, opcodeAddress);
        else if (opcode < 0x40) middle(opcode, opcodeAddress);
        else if (opcode < 0xE0) control(opcode, opcodeAddress);
        else high(opcode, opcodeAddress);
    }

    private void low(int opcode, int opcodeAddress) {
        switch (opcode) {
            case 0x00 -> {}
            case 0x01 -> setBC(next16());
            case 0x02 -> bus.write8(getBC(), a);
            case 0x03 -> setBC(getBC() + 1);
            case 0x04 -> b = inc8(b);
            case 0x05 -> b = dec8(b);
            case 0x06 -> b = next8();
            case 0x09 -> addHL(getBC());
            case 0x0A -> a = bus.read8(getBC());
            case 0x0B -> setBC(getBC() - 1);
            case 0x0C -> c = inc8(c);
            case 0x0D -> c = dec8(c);
            case 0x0E -> c = next8();
            case 0x11 -> setDE(next16());
            case 0x12 -> bus.write8(getDE(), a);
            case 0x13 -> setDE(getDE() + 1);
            case 0x14 -> d = inc8(d);
            case 0x15 -> d = dec8(d);
            case 0x16 -> d = next8();
            case 0x18 -> jr(true);
            case 0x19 -> addHL(getDE());
            case 0x1A -> a = bus.read8(getDE());
            case 0x1B -> setDE(getDE() - 1);
            case 0x1C -> e = inc8(e);
            case 0x1D -> e = dec8(e);
            case 0x1E -> e = next8();
            default -> throw unsupported(opcode, opcodeAddress);
        }
    }

    private void middle(int opcode, int opcodeAddress) {
        switch (opcode) {
            case 0x20 -> jr(!flagZ);
            case 0x21 -> setHL(next16());
            case 0x22 -> {
                bus.write8(getHL(), a);
                setHL(getHL() + 1);
            }
            case 0x23 -> setHL(getHL() + 1);
            case 0x24 -> h = inc8(h);
            case 0x25 -> h = dec8(h);
            case 0x26 -> h = next8();
            case 0x28 -> jr(flagZ);
            case 0x29 -> addHL(getHL());
            case 0x2A -> {
                a = bus.read8(getHL());
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
                bus.write8(getHL(), a);
                setHL(getHL() - 1);
            }
            case 0x33 -> {}
            case 0x34 -> bus.write8(getHL(), inc8(bus.read8(getHL())));
            case 0x35 -> bus.write8(getHL(), dec8(bus.read8(getHL())));
            case 0x36 -> bus.write8(getHL(), next8());
            case 0x37 -> {
                flagN = false;
                flagH = false;
                flagC = true;
            }
            case 0x38 -> jr(flagC);
            case 0x39 -> addHL(0);
            case 0x3A -> {
                a = bus.read8(getHL());
                setHL(getHL() - 1);
            }
            case 0x3B -> {}
            case 0x3C -> a = inc8(a);
            case 0x3D -> a = dec8(a);
            case 0x3E -> a = next8();
            case 0x3F -> {
                flagN = false;
                flagH = false;
                flagC = !flagC;
            }
            default -> throw unsupported(opcode, opcodeAddress);
        }
    }

    private void control(int opcode, int opcodeAddress) {
        switch (opcode) {
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
            default -> throw unsupported(opcode, opcodeAddress);
        }
    }

    private void high(int opcode, int opcodeAddress) {
        switch (opcode) {
            case 0xE0 -> bus.write8(0xFF00 | next8(), a);
            case 0xE1 -> setHL(pop16());
            case 0xE2 -> bus.write8(0xFF00 | c, a);
            case 0xE5 -> push16(getHL());
            case 0xE6 -> andA(next8());
            case 0xE9 -> pc = getHL();
            case 0xEA -> bus.write8(next16(), a);
            case 0xEE -> xorA(next8());
            case 0xF0 -> a = bus.read8(0xFF00 | next8());
            case 0xF1 -> setAF(pop16());
            case 0xF2 -> a = bus.read8(0xFF00 | c);
            case 0xF5 -> push16(getAF());
            case 0xF6 -> orA(next8());
            case 0xFA -> a = bus.read8(next16());
            case 0xFE -> cpA(next8());
            default -> throw unsupported(opcode, opcodeAddress);
        }
    }
}
