#!/usr/bin/env python3
"""Run the shared opening button route against the original ROM, without RAM writes."""
import io
import argparse
import hashlib
import importlib.metadata
import json
from pathlib import Path
from pyboy import PyBoy

ROOT = Path(__file__).resolve().parents[2]
args = argparse.ArgumentParser()
args.add_argument('--route', type=Path, default=ROOT / 'tools/opening/house-route.tsv')
args.add_argument('--output', type=Path, default=ROOT / 'java/build/opening-route')
args = args.parse_args()
OUT = args.output
OUT.joinpath('original').mkdir(parents=True, exist_ok=True)
OUT.joinpath('emulator.json').unlink(missing_ok=True)
p = PyBoy(str(ROOT / 'java/src/main/resources/rom/azle.gbc'), window='null',
          sound_emulated=False, cgb=True, ram_file=io.BytesIO(bytes(0x8000)))
p.set_emulation_speed(0)
trace = []
checkpoints = []
combat_trace = []
try:
    p.tick(3800)
    p.button('start', 8)
    p.tick(180)
    for button in ['a', 'a', 'start', 'a']:
        p.button(button, 8)
        p.tick(100)
    assert p.memory[0xdb95] == 0x0b and p.memory[0xfff6] == 0xa3
    frame = segment = 0
    for line in args.route.read_text().splitlines():
        if not line or line.startswith('#'): continue
        fields = line.split()
        button, held, idle = fields[:3]
        if len(fields) in (4, 5, 6):
            button = fields[3]  # Explicit ROM input; an optional duration can replace the target.
        elif len(fields) != 3:
            raise ValueError('Expected buttons, target, idle, optional ROM buttons, held frames and idle frames: ' + line)
        # '-' keeps the shared target, allowing only the ROM idle to differ.
        if len(fields) >= 5 and fields[4] != '-':
            if not fields[4].isdigit() and '=' not in fields[4]:
                raise ValueError('ROM override must be a frame count or target condition: ' + line)
            held = fields[4]
        idle = int(fields[5] if len(fields) == 6 else idle)
        pulse_ticks = 0
        pulse_active = False
        def press():
            global pulse_active
            pulse_active = True
            for key in button.split('+'):
                if key != 'none': p.button_press(key.rstrip('*'))
        def release():
            global pulse_active
            pulse_active = False
            for key in button.split('+'):
                if key != 'none': p.button_release(key.rstrip('*'))
        def tick_frame():
            global frame, pulse_ticks
            if pulse_active:
                for key in button.split('+'):
                    if key.endswith('*'):
                        if pulse_ticks % 24 == 8: p.button_release(key[:-1])
                        elif pulse_ticks > 0 and pulse_ticks % 24 == 0: p.button_press(key[:-1])
                pulse_ticks += 1
            p.tick()
            if p.memory[0xdb5a] == 0:
                raise RuntimeError(f'Link died at frame {frame}, segment {segment}: {line}')
            trace.append([frame, p.memory[0xfff7], p.memory[0xfff6], p.memory[0xff98],
                          p.memory[0xff99], p.memory[0xc19f] != 0, p.memory[0xdb44], p.memory[0xc124],
                          p.memory[0xc125], p.memory[0xffe7], p.memory[0xff96], p.memory[0xff97],
                          p.memory[0xdb5a], p.memory[0xdb4e],
                          p.memory[0xffa1], p.memory[0xff9b], p.memory[0xc111],
                          p.memory[0xdb00], p.memory[0xdb01], p.memory[0xdb4b], p.memory[0xdb4c], p.memory[0xd851], p.memory[0xdb11], p.memory[0xdbd0], p.memory[0xdbcd], p.memory[0xdbcc], p.memory[0xdbcf], p.memory[0xff9d], p.memory[0xff9e], p.memory[0xc137], p.memory[0xffa2], p.memory[0xffa3], p.memory[0xff9a], p.memory[0xff9b], p.memory[0xdb5b], p.memory[0xdb65]])
            if p.memory[0xfff7] == 0 and p.memory[0xfff6] in (0x11,0x06):
                box = [int(p.memory[0xc5b0] != 0)] + list(p.memory[0xc140:0xc144])
                entities = [[p.memory[0xc3a0+i], p.memory[0xc200+i], p.memory[0xc210+i], p.memory[0xc310+i], p.memory[0xc360+i], p.memory[0xc280+i]] for i in range(16) if p.memory[0xc280+i] and p.memory[0xc3a0+i] in (0x81,0x82,0x59)]
                combat_trace.append(str(frame)+'\t'+';'.join(','.join(map(str,row)) for row in [box]+entities)+'\n')
            frame += 1
        if '=' in held:
            field, target = held.split('=')
            target = int(target, 0)
            address = {'room': 0xfff6, 'x': 0xff98, 'y': 0xff99, 'dialog': 0xc19f, 'sword': 0xdb4e, 'inventorytick': 0xde06, 'bgtick': 0xffa6, 'smallkeys': 0xdbd0, 'nightmarekey': 0xdbcf}[field]
            def reached():
                value = p.memory[address]
                if field == 'inventorytick':
                    value += (p.memory[0xde07] // 4) * 8
                if field == 'dialog': return (value != 0) == bool(target)
                if field not in ('x', 'y'): return value == target
                return value <= target if button.split('+')[0] in ('left', 'up') else value >= target
            press()
            for _ in range(2000):
                if reached(): break
                tick_frame()
            else: raise RuntimeError('Route blocked: ' + line + ' ' + str(trace[-1]))
            release()
            tick_frame()
            for _ in range(500):
                if p.memory[0xc124] == 0: break
                tick_frame()
            else: raise RuntimeError('Transition did not finish: ' + line)
            held = 0
        else:
            held = int(held)
        if held: press()
        for tick in range(held + idle):
            if tick == held and held: release()
            tick_frame()
        if held and idle == 0: release()
        p.screen.image.save(OUT / 'original' / f'{segment:02}.png')
        checkpoints.append([segment] + trace[-1])
        entities = [f'{p.memory[0xc3a0+i]:02X}:{p.memory[0xc280+i]}@{p.memory[0xc200+i]},{p.memory[0xc210+i]}'
                    for i in range(16) if p.memory[0xc280+i] != 0]
        print(segment + 1, trace[-1], 'entities', ' '.join(entities))
        segment += 1
    OUT.joinpath('emulator-checkpoints.tsv').write_text(''.join('\t'.join(map(str, r))+'\n' for r in checkpoints))
    OUT.joinpath('emulator.json').write_text(json.dumps({
        'emulator': 'PyBoy ' + importlib.metadata.version('pyboy'),
        'rom_sha256': hashlib.sha256(ROOT.joinpath('java/src/main/resources/rom/azle.gbc').read_bytes()).hexdigest(),
        'route_sha256': hashlib.sha256(args.route.read_bytes()).hexdigest(),
        'checkpoints': len(checkpoints),
        'rom_button_overrides': sum(len(line.split()) >= 4 for line in args.route.read_text().splitlines() if line and not line.startswith('#')),
        'memory_injection': False, 'save': 'fresh in-memory RAM, never saved',
    }, indent=2))
    OUT.joinpath('emulator-combat.tsv').write_text(''.join(combat_trace))
    OUT.joinpath('emulator-trace.tsv').write_text(''.join('\t'.join(map(str, r))+'\n' for r in trace))
finally:
    p.stop(save=False)
