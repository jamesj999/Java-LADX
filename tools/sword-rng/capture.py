#!/usr/bin/env python3
"""Bounded fixed-input original-ROM RNG observations; setup matches live Slime Eye tool."""
import argparse, hashlib, importlib.metadata, io, json
from pathlib import Path
from pyboy import PyBoy
ROOT=Path(__file__).resolve().parents[2]
parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('--rom',type=Path,default=ROOT/'java/src/main/resources/rom/azle.gbc')
parser.add_argument('--output',type=Path,default=Path('/tmp/ladx-sword-rng'))
parser.add_argument('--inputs',type=Path,default=ROOT/'tools/sword-rng/inputs.tsv')
args=parser.parse_args();args.output.mkdir(parents=True,exist_ok=True)
p=PyBoy(str(args.rom),window='null',sound_emulated=False,cgb=True,ram_file=io.BytesIO(bytes(0x8000)))
p.set_emulation_speed(0)
records=[];states=[];frame=-1;previous_seed=0xA2
try:
    p.tick(3800); p.button('start', 8); p.tick(180)
    for key in ['a', 'a', 'start', 'a']: p.button(key, 8); p.tick(100)
    assert p.memory[0xDB95] == 0x0B and p.memory[0xFFF6] == 0xA3
    setup = {}
    def write(address, value): p.memory[address] = value; setup[hex(address)] = value
    for i in range(4):
        for j, value in enumerate([1, 2, 0x5A, 80, 96]): write(0xD401 + i * 5 + j, value)
    for i in range(16): write(0xC280 + i, 0)
    write(0xFF98, 80); write(0xFF99, 112); write(0xC11C, 0)
    p.button('down', 40)
    for i in range(15): p.tick(20); write(0xC1BD, 1)
    assert p.memory[0xFFF6] == 0x5A and p.memory[0xFFF7] == 2
    # Initial setup only: initialized real room loaders, then reset its initial boss slot.
    for address in range(0xC200, 0xC490): write(address, 0)
    for address, value in {0xC280:4, 0xC3A0:0x5B, 0xC200:72, 0xC210:64,
            0xC430:0x90, 0xC360:80, 0xC1BD:0, 0xC165:32, 0xC157:7,
            0xFF98:72, 0xFF99:80, 0xFF9A:0, 0xFF9B:0, 0xFF9E:2,
            0xFFA2:0, 0xFFA3:0, 0xC11C:0, 0xC19F:0, 0xFFE7:1,
            0xDB00:8, 0xDB01:1, 0xDB4E:1, 0xDB44:0, 0xDB5A:48, 0xDB5B:6,
            0xC13D:0xA2, 0xC14A:0, 0xC5B0:0}.items(): write(address, value)
    def observe_rng(context):
        global previous_seed
        sp=p.register_file.SP;ret=p.memory[sp]|p.memory[sp+1]<<8
        value=p.register_file.A
        records.append(dict(frame=frame,counter=p.memory[0xFFE7],ly=p.memory[0xFF44],
                            previousSeed=previous_seed,value=value,caller=f"{0 if ret<0x4000 else p.memory[0xDBAF]:02X}:{ret:04X}"))
        previous_seed=value
    def observe_noise(context):
        records.append(dict(frame=frame,counter=p.memory[0xFFE7],ly=p.memory[0xFF44],noise=p.register_file.A))
    p.hook_register(0,0x281D,observe_rng,None)
    p.hook_register(0,0x1551,observe_noise,None)
    # Setup cutoff: only supplied input and emulator stepping below.
    held=set()
    for line in args.inputs.read_text().splitlines()[1:]:
        number,buttons=line.split('\t');frame=int(number);wanted=set(buttons.split('+'))if buttons else set()
        for key in held-wanted:p.button_release(key)
        for key in wanted-held:p.button_press(key)
        held=wanted;p.tick(1,render=False)
        states.append(dict(frame=frame,counter=p.memory[0xFFE7],buttons=buttons,dialog=p.memory[0xC19F],sword=p.memory[0xC137],linkX=p.memory[0xFF98],linkY=p.memory[0xFF99],bossState=p.memory[0xC290],slowCountdown=p.memory[0xC450],motionBlocked=p.memory[0xFFA1],swordTimer=p.memory[0xC138]))
    (args.output/'emulator-rng.jsonl').write_text(''.join(json.dumps(r)+'\n'for r in records))
    (args.output/'emulator-state.jsonl').write_text(''.join(json.dumps(r)+'\n'for r in states))
    (args.output/'setup.json').write_text(json.dumps(dict(rom_sha256=hashlib.sha256(args.rom.read_bytes()).hexdigest(),
        input_sha256=hashlib.sha256(args.inputs.read_bytes()).hexdigest(),setup_writes=setup,frames=len(states),emulator='PyBoy '+importlib.metadata.version('pyboy')),indent=2)+'\n')
    print(json.dumps(records,indent=2))
finally:p.stop(save=False)
