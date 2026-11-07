#!/usr/bin/env python3
"""Controlled RAM-injected contact probes. Not normal-input playthroughs."""
import argparse
import hashlib
import io
import importlib.metadata
import json
from pathlib import Path
from pyboy import PyBoy

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--case', choices=['free', 'wall', 'octorok-body', 'octorok-sword'], default='free')
parser.add_argument('--output', type=Path, required=True)
args = parser.parse_args()
octorok = args.case.startswith('octorok-')
rom = ROOT / 'java/src/main/resources/rom/azle.gbc'
route = ROOT / 'tools/opening/bowwow-rescue-route.tsv'
p = PyBoy(str(rom), window='null', sound_emulated=False, cgb=True,
          ram_file=io.BytesIO(bytes(0x8000)))
p.set_emulation_speed(0)

def buttons(names, pressed):
    for name in names.split('+'):
        if name not in ('none', '-'):
            (p.button_press if pressed else p.button_release)(name)

try:
    p.tick(3800)
    p.button('start', 8)
    p.tick(180)
    for button in ['a', 'a', 'start', 'a']:
        p.button(button, 8)
        p.tick(100)
    # Only the opening approach; no combat input overrides.
    prefix = [line for line in route.read_text().splitlines()
              if line and not line.startswith('#')][:(40 if octorok else 45)]
    for line in prefix:
        button, held, idle = line.split()
        buttons(button, True)
        if '=' in held:
            field, value = held.split('=')
            target = int(value, 0)
            address = {'room': 0xfff6, 'x': 0xff98, 'y': 0xff99}[field]
            def reached():
                actual = p.memory[address]
                if field == 'room': return actual == target
                return actual <= target if button.split('+')[0] in ('left', 'up') else actual >= target
            for _ in range(2000):
                if reached(): break
                p.tick()
            else: raise RuntimeError('Approach blocked: ' + line)
            buttons(button, False)
            p.tick()
            for _ in range(500):
                if p.memory[0xc124] == 0: break
                p.tick()
            else: raise RuntimeError('Scroll blocked: ' + line)
        else:
            p.tick(int(held))
            buttons(button, False)
        p.tick(int(idle))
    assert (p.memory[0xfff7], p.memory[0xfff6]) == (0, 0xe0 if octorok else 0xf1)
    slots = [i for i in range(16) if p.memory[0xc280+i]
             and p.memory[0xc3a0+i] == (0x09 if octorok else 0x0e)]
    slot = slots[-1] if octorok else slots[0]
    for button in ['right', 'left', 'up', 'down', 'a', 'b']:
        p.button_release(button)
    for i in range(16):
        if i != slot: p.memory[0xc280+i] = 0
    entity_x, entity_y = (58, 82) if octorok else ((110, 40) if args.case == 'free' else (64, 65))
    link_x, link_y = (51, 88) if octorok else (entity_x - 1, entity_y - 1)
    writes = {0xff98: link_x, 0xff99: link_y, 0xff9a: 0, 0xff9b: 0,
              0xffa2: 0, 0xffa3: 0, 0xc11a: link_x, 0xc11b: link_y,
              0xc11c: 0, 0xc13e: 0, 0xc15b: 0, 0xc19f: 0, 0xc124: 0,
              0xc16b: 4, 0xdbc7: 0, 0xdb94: 0, 0xdb5a: 24, 0xd47c: 0,
              0xc140: 0, 0xc5b0: 0, 0xffe7: 135 if octorok else 35}
    for base, value in [(0xc200, entity_x), (0xc210, entity_y), (0xc240, 0 if octorok else 0xf8),
                        (0xc250, 0), (0xc260, 0), (0xc270, 0), (0xc280, 5),
                        (0xc290, 1 if octorok else 2), (0xc2e0, 100 if octorok else 109), (0xc310, 0), (0xc320, 0),
                        (0xc410, 0), (0xc420, 0)]:
        writes[base+slot] = value
    if octorok:
        writes[0xc360+slot] = 8
        writes[0xc2b0+slot] = 0
    for address, value in writes.items(): p.memory[address] = value
    collision = []
    recoil = []
    def observe_collision(_):
        collision.append(dict(frame=p.memory[0xffe7], cached_x=p.memory[0xffee],
                              cached_y=p.memory[0xffec], entity_x=p.memory[0xc200+slot],
                              entity_y=p.memory[0xc210+slot], link_x=p.memory[0xff98],
                              link_y=p.memory[0xff99]))
    def observe_recoil(_):
        recoil.append(dict(frame=p.memory[0xffe7], speed_x=p.memory[0xff9a],
                           speed_y=p.memory[0xff9b]))
    sword_writes = {0xc5b0: 1, 0xc140: 66, 0xc141: 5, 0xc142: 90, 0xc143: 5,
                    0xdb4e: 1, 0xc1ac: 0} if args.case == 'octorok-sword' else {}
    def inject_sword(_):
        # Install the measured sword rectangle after ordinary Link equipment
        # processing, at entry to the shared source combat routine.
        for address, value in sword_writes.items(): p.memory[address] = value
    if sword_writes: p.hook_register(3, 0x6e28, inject_sword, None)
    # Executable source labels CheckLinkCollisionWithEnemy and ret_003_7570.
    p.hook_register(3, 0x6c72, observe_collision, None)
    p.hook_register(3, 0x7570, observe_recoil, None)
    for _ in range(3):
        p.tick()
        if recoil: break
    if not recoil: raise RuntimeError('No source contact was observed')
    result = dict(controlled_ram_injection=True, case=args.case, slot=slot,
                  emulator='PyBoy ' + importlib.metadata.version('pyboy'),
                  rom_sha256=hashlib.sha256(rom.read_bytes()).hexdigest(),
                  approach_sha256=hashlib.sha256(('\n'.join(prefix)+'\n').encode()).hexdigest(),
                  writes={hex(k): v for k, v in writes.items()}, collision=collision, recoil=recoil,
                  sword_writes_at_combat={hex(k): v for k, v in sword_writes.items()},
                  enemy_health=p.memory[0xc360+slot],
                  subtract_health_buffer=p.memory[0xdb94], invincibility=p.memory[0xdbc7],
                  ignore_collision_countdown=p.memory[0xc13e])
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(json.dumps(result, indent=2)+'\n')
    print(args.output)
finally:
    p.stop(save=False)
