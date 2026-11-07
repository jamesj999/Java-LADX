#!/usr/bin/env python3
"""Measure a synthetic CH4 live-envelope write without fitting mixer gain."""
import argparse, hashlib, importlib.metadata, io, json, wave
from pathlib import Path
import numpy as np
import pyboy
from pyboy import PyBoy
ROOT=Path(__file__).resolve().parents[2]
p=argparse.ArgumentParser(description=__doc__)
p.add_argument('--output',type=Path,default=Path('/tmp/ladx-intro-audio'))
p.add_argument('--before-wav',type=Path,help='Optional local pre-fix WAV')
p.add_argument('--rom',type=Path,default=ROOT/'java/src/main/resources/rom/azle.gbc')
a=p.parse_args();a.output.mkdir(parents=True,exist_ok=True)
player=PyBoy(str(a.rom),window='null',sound_emulated=True,sound_sample_rate=48000,cgb=True,ram_file=io.BytesIO(bytes(0x8000)))
player.set_emulation_speed(0)
chunks=[]
try:
    player.tick(100,render=False,sound=True)
    # Park the CPU in caller-owned RAM: DI; JR -2. No game/driver changes follow.
    player.memory[0xc000:0xc003]=[0xf3,0x18,0xfe]
    player.register_file.PC=0xc000
    player.tick(1,render=False,sound=True)
    for register,value in [(0x26,0),(0x26,0x80),(0x24,0x77),(0x25,0x88),
                           (0x21,0xF0),(0x22,3),(0x23,0x80)]:
        player.memory[0xff00|register]=value
    for frame in range(24):
        if frame==8:player.memory[0xff21]=0x30
        if frame==16:player.memory[0xff23]=0x80
        if frame==20:player.memory[0xff21]=0
        player.tick(1,render=False,sound=True)
        chunks.append(player.sound.ndarray.copy().astype(np.int16)*256)
finally:player.stop(save=False)
with wave.open(str(a.output/'noise-emulator.wav'),'wb') as w:
    w.setnchannels(2);w.setsampwidth(2);w.setframerate(48000)
    w.writeframes(np.concatenate(chunks).astype('<i2').tobytes())
phases=[('initial_volume_15',1,8),('write_initial_3_without_trigger',9,16),
        ('trigger_initial_3',17,20),('dac_off',21,24)]
def measure(blocks):
    rows=[]
    for name,start,end in phases:
        values=np.concatenate(blocks[start:end]).astype(np.int32)
        rows.append(dict(phase=name,minimum=int(values.min()),maximum=int(values.max()),
                         peak_to_peak=int(values.max()-values.min())))
    base=rows[0]['peak_to_peak']
    for row in rows:row['relative_peak_to_peak']=row['peak_to_peak']/base
    return rows
result=dict(emulator_version='PyBoy '+importlib.metadata.version('pyboy'),
    rom_sha256=hashlib.sha256(a.rom.read_bytes()).hexdigest(),
    setup='CGB after 100 boot ticks, CPU parked at C000 DI/JR-2; NR52 off/on, NR50=77, NR51=88; only noise DAC enabled',
    commands=[dict(frame=0,NR42='F0',NR43='03',NR44='80'),dict(frame=8,NR42='30'),
              dict(frame=16,NR44='80'),dict(frame=20,NR42='00')],
    pyboy_sound_source_sha256=hashlib.sha256((Path(pyboy.__file__).parent/'core/sound.py').read_bytes()).hexdigest(),
    sample_rate=48000,emulator_samples=sum(len(c)for c in chunks),emulator=measure(chunks))
captures=[('engine_after',a.output/'noise-engine.wav')]
if a.before_wav:captures.insert(0,('engine_before',a.before_wav))
for name,file in captures:
    with wave.open(str(file),'rb')as w:
        data=np.frombuffer(w.readframes(w.getnframes()),dtype='<i2').reshape(-1,2)
    result[name]=measure([data[i*800:(i+1)*800]for i in range(24)])
result['limits']=['Compare per-source amplitude ratios, not raw PCM gain or DC: PyBoy emits unsigned digital channel sums; engine emits centered bipolar PCM.',
                  'Frame origins, LFSR phase and DIV-APU phase are not synchronized.',
                  'Both envelope periods are zero and directions decrease, avoiding CGB live-write zombie transitions.',
                  'This synthetic write-without-trigger scenario is not a claim that the captured intro executes it.']
(a.output/'noise-isolation.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(result,indent=2))
