#!/usr/bin/env python3
"""Run the shared opening button route against the original ROM, without RAM writes."""
import io
import hashlib
import importlib.metadata
import json
from pathlib import Path
from pyboy import PyBoy

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'java/build/opening-route'
OUT.joinpath('original').mkdir(parents=True, exist_ok=True)
p = PyBoy(str(ROOT / 'java/src/main/resources/rom/azle.gbc'), window='null',
          sound_emulated=False, cgb=True, ram_file=io.BytesIO(bytes(0x8000)))
p.set_emulation_speed(0)
trace = []
checkpoints = []
try:
    p.tick(3800)
    p.button('start', 8)
    p.tick(180)
    for button in ['a', 'a', 'start', 'a']:
        p.button(button, 8)
        p.tick(100)
    assert p.memory[0xdb95] == 0x0b and p.memory[0xfff6] == 0xa3
    frame = segment = 0
    for line in ROOT.joinpath('tools/opening/house-route.tsv').read_text().splitlines():
        if not line or line.startswith('#'): continue
        button, held, idle = line.split()
        held, idle = int(held), int(idle)
        if held: p.button_press(button)
        for tick in range(held + idle):
            if tick == held and held: p.button_release(button)
            p.tick()
            trace.append([frame, p.memory[0xfff7], p.memory[0xfff6], p.memory[0xff98],
                          p.memory[0xff99], p.memory[0xc19f] != 0, p.memory[0xdb44]])
            frame += 1
        p.screen.image.save(OUT / 'original' / f'{segment:02}.png')
        checkpoints.append([segment] + trace[-1])
        print(segment + 1, trace[-1])
        segment += 1
    OUT.joinpath('emulator-checkpoints.tsv').write_text(''.join('\t'.join(map(str, r))+'\n' for r in checkpoints))
    OUT.joinpath('emulator.json').write_text(json.dumps({
        'emulator': 'PyBoy ' + importlib.metadata.version('pyboy'),
        'rom_sha256': hashlib.sha256(ROOT.joinpath('java/src/main/resources/rom/azle.gbc').read_bytes()).hexdigest(),
        'route_sha256': hashlib.sha256(ROOT.joinpath('tools/opening/house-route.tsv').read_bytes()).hexdigest(),
        'memory_injection': False, 'save': 'fresh in-memory RAM, never saved',
    }, indent=2))
    OUT.joinpath('emulator-trace.tsv').write_text(''.join('\t'.join(map(str, r))+'\n' for r in trace))
finally:
    p.stop(save=False)
