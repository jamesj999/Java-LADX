#!/usr/bin/env python3
"""Isolated injected Slime Eye handler trace; does not establish a D3 route."""
import argparse
import hashlib
import io
import json
from pathlib import Path
from pyboy import PyBoy

ROOT = Path(__file__).resolve().parents[2]
a = argparse.ArgumentParser()
a.add_argument('--rom', type=Path, default=ROOT / 'java/src/main/resources/rom/azle.gbc')
a.add_argument('--output', type=Path, default=ROOT / 'java/build/slime-eye')
a = a.parse_args()
a.output.mkdir(parents=True, exist_ok=True)
p = PyBoy(str(a.rom), window='null', sound_emulated=False, cgb=True,
          ram_file=io.BytesIO(bytes(0x8000)))
p.set_emulation_speed(0)
try:
    p.tick(3800); p.button('start', 8); p.tick(180)
    for button in ['a', 'a', 'start', 'a']:
        p.button(button, 8); p.tick(100)
    assert p.memory[0xdb95] == 0x0b and p.memory[0xfff6] == 0xa3
    # Match the engine comparison fixture random supplier; this hook changes only returned A.
    def zero_random(context): p.register_file.A = 0
    p.hook_register(0, 0x281D, zero_random, None)
    base = io.BytesIO(); p.save_state(base)
    columns = ['frame', 'status', 'state', 'x', 'y', 'z', 'speedZ', 'countdown',
               'variant', 'private4', 'slow', 'health']
    cases = {'body-drop': (1, 0, 0x7e, 0x50, 145), 'half-rise-drop': (2, 1, 0, 5, 145),
             'half-death': (0, 1, 0, 0, 226)}
    for name, (state, half, z, health, frames) in cases.items():
        base.seek(0); p.load_state(base)
        writes = {address: 0 for address in range(0xc200, 0xc490)}
        writes.update({0xc280: 1 if name == 'half-death' else 5, 0xc290: state,
                       0xc390: half, 0xc3a0: 0x5b, 0xc200: 0x50, 0xc210: 0x48,
                       0xc310: z, 0xc360: health, 0xc1bd: 1, 0xc11c: 0,
                       0xc19f: 0, 0xff98: 0x50, 0xff99: 0x60, 0xffa2: 0})
        # Source options/hitbox flags normally installed by entity initialization.
        writes[0xc430] = 0x90; writes[0xc350] = 0
        for address, value in writes.items(): p.memory[address] = value
        rows = []
        for frame in range(frames):
            # Hold Link at the same target; no input route is claimed.
            for address, value in {0xff98: 0x50, 0xff99: 0x60, 0xff9a: 0, 0xff9b: 0,
                                   0xffa2: 0, 0xc11c: 0, 0xc19f: 0}.items():
                p.memory[address] = value
            p.tick()
            rows.append([p.memory[0xffe7]] + [p.memory[address] for address in
                [0xc280, 0xc290, 0xc200, 0xc210, 0xc310, 0xc320, 0xc2e0,
                 0xc3b0, 0xc440, 0xc450, 0xc360]])
        (a.output / (name + '-emulator.tsv')).write_text('\t'.join(columns) + '\n' +
            ''.join('\t'.join(map(str, row)) + '\n' for row in rows))
        (a.output / (name + '-setup.json')).write_text(json.dumps(
            {hex(key): value for key, value in writes.items()}, indent=2) + '\n')
    # Visual probe: redirect the house door through the ROM's normal warp loader to D3 room 5A.
    # This sets up a room; it is deliberately not a normal progression route.
    base.seek(0); p.load_state(base)
    warp_setup = {}
    for warp in range(4):
        for index, value in enumerate([1, 2, 0x5A, 80, 96]):
            address = 0xD401 + warp * 5 + index
            p.memory[address] = value; warp_setup[hex(address)] = value
    for slot in range(16): p.memory[0xC280 + slot] = 0
    for address, value in {0xFF98: 80, 0xFF99: 112, 0xC11C: 0}.items():
        p.memory[address] = value; warp_setup[hex(address)] = value
    p.button('down', 40)
    for frame in range(15):
        p.tick(20); p.memory[0xC1BD] = 1
    assert p.memory[0xFFF6] == 0x5A and p.memory[0xFFF7] == 2
    warp_setup['boss_intro_suppression'] = 'wDidBossIntro C1BD = 1 after each 20-frame warp setup interval'
    for half in [False, True]:
        setup = {}
        for slot in range(16): p.memory[0xC280 + slot] = 0
        for address, value in {0xC1BD: 1, 0xC19F: 0, 0xC11C: 2}.items():
            p.memory[address] = value; setup[hex(address)] = value
        for slot, x in enumerate([54, 90] if half else [72]):
            for address, value in [(0xC280, 5), (0xC290, 0 if half else 3),
                    (0xC390, int(half)), (0xC3A0, 0x5B), (0xC200, x), (0xC210, 64),
                    (0xC310, 0), (0xC3B0, 0), (0xC430, 0x90), (0xC420, 0), (0xC2E0, 0x70)]:
                p.memory[address + slot] = value; setup[hex(address + slot)] = value
        p.tick(3)  # OAM -> DMA -> displayed frame pipeline.
        name = 'halves' if half else 'body'
        p.screen.image.save(str(a.output / (name + '-emulator.png')))
        (a.output / (name + '-visual-setup.json')).write_text(json.dumps(
            {'warp': warp_setup, 'presentation': setup, 'all_entity_statuses_cleared_before_probe': True}, indent=2) + '\n')
    (a.output / 'capture.json').write_text(json.dumps({'rom_sha256': hashlib.sha256(a.rom.read_bytes()).hexdigest(),
        'trace_setup': 'New-game house then injected entity arrays, fixed Link target per frame, GetRandomByte return A forced to zero at 00:281D.', 'visual_setup': 'House door warp structs redirected to map2 room5A; boss intro suppressed; injected fixed body/half poses; Link frozen; three display frames allowed.', 'route_verification': 'None; no normal D3 progression claim.'}, indent=2) + '\n')
finally:
    p.stop(save=False)
