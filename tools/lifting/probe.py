#!/usr/bin/env python3
"""Probe carry timing in a user-supplied DX ROM; write numeric traces only.

Requires PyBoy (tools/requirements.txt) and a locally built azle.sym. This uses
controlled RAM injection after boot, so it is not an opening-route replay.
"""
import io
import json
from pathlib import Path

from pyboy import PyBoy

ROOT = Path(__file__).resolve().parents[2]
symbols = {}
for line in ROOT.joinpath('LADX-Disassembly/azle.sym').read_text().splitlines():
    parts = line.split()
    if len(parts) == 2 and ':' in parts[0]:
        symbols[parts[1]] = int(parts[0].split(':')[1], 16)


def write(name, value):
    emulator.memory[symbols[name]] = value


emulator = PyBoy(str(ROOT / 'java/src/main/resources/rom/azle.gbc'),
                  window='null', sound_emulated=False, cgb=True,
                  ram_file=io.BytesIO(bytes(0x8000)))
emulator.set_emulation_speed(0)
traces = {}
try:
    emulator.tick(3800)
    emulator.button('start', 8)
    emulator.tick(180)
    for button in ['a', 'a', 'start', 'a']:
        emulator.button(button, 8)
        emulator.tick(100)

    for level in [1, 2]:
        for name, value in {
            'wGameplaySubtype': 7, 'wDialogState': 0, 'wDialogCooldown': 0,
            'wLinkMotionState': 0, 'wPowerBraceletLevel': level,
            'wActivePowerUp': 0, 'wTunicType': 0,
            'wIsCarryingLiftedObject': 0, 'wInventoryAppearing': 0,
        }.items():
            write(name, value)
        for slot in range(16):
            emulator.memory[symbols['wEntitiesStatusTable'] + slot] = 0
        for name, value in {
            'wEntitiesStatusTable': 7, 'wEntitiesTypeTable': 5,
            'wEntitiesLiftedTable': 0, 'wEntitiesTransitionCountdownTable': 2,
            'wEntitiesPosXTable': 80, 'wEntitiesPosYTable': 96,
            'wEntitiesPosZTable': 0, 'wC15D': 0,
            'hLinkPositionX': 80, 'hLinkPositionY': 96,
        }.items():
            write(name, value)
        trace = []
        for frame in range(1, 50):
            emulator.tick()
            trace.append([frame, emulator.memory[symbols['wEntitiesLiftedTable']],
                          emulator.memory[symbols['wEntitiesTransitionCountdownTable']],
                          emulator.memory[symbols['wIsCarryingLiftedObject']]])
            if trace[-1][-1] == 1:
                break
        assert trace[-1][0] == (20 if level == 1 else 12), trace
        traces[str(level)] = trace
finally:
    emulator.stop(save=False)

output = ROOT / 'java/build/bracelet-verification/lift-emulator.json'
output.parent.mkdir(parents=True, exist_ok=True)
output.write_text(json.dumps(traces, indent=2) + '\n')
print(output)
