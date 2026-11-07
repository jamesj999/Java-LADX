#!/usr/bin/env python3
"""Capture local intro PCM/event evidence; output contains ROM-derived audio, never publish it."""
import argparse, hashlib, importlib.metadata, io, json, wave
from pathlib import Path
import numpy as np
from pyboy import PyBoy

root = Path(__file__).resolve().parents[2]
p = argparse.ArgumentParser(description=__doc__)
p.add_argument('--output', type=Path, default=Path('/tmp/ladx-intro-audio'))
p.add_argument('--frames', type=int, default=3000)
p.add_argument('--rom', type=Path, default=root/'java/src/main/resources/rom/azle.gbc')
a = p.parse_args()
if a.frames <= 0: p.error('--frames must be positive')
a.output.mkdir(parents=True, exist_ok=True)
records=[]; chunks=[]; frame=0; origin=None
player=PyBoy(str(a.rom), window='null', sound_emulated=True, sound_sample_rate=48000,
             cgb=True, ram_file=io.BytesIO(bytes(0x8000)))
player.set_emulation_speed(0)
def event(noise):
    records.append(dict(frame=frame, noise=noise))
player.hook_register(1, 0x70a9, event, 12)
player.hook_register(1, 0x71d6, event, 15)
try:
    # Boot and initialization remain in the trace. Align at first sea stage (3).
    while origin is None or frame < origin+a.frames:
        if origin is None and frame >= 1000:
            raise RuntimeError("Intro sea stage not reached within 1000 boot ticks")
        player.tick(1, render=False, sound=True)
        stage=player.memory[0xdb96]
        if origin is None and stage == 3: origin=frame
        sound=player.sound.ndarray.copy()
        chunks.append(sound)
        records.append(dict(frame=frame, stage=stage, samples=len(sound),
                            registers=list(player.memory[0xff20:0xff27]),
                            activeNoise=player.memory[0xd378]))
        frame+=1
finally: player.stop(save=False)
(a.output/'emulator.jsonl').write_text(''.join(json.dumps(r)+'\n' for r in records))
# Save only the declared intro interval, excluding boot.
chunks = chunks[origin:origin+a.frames]
# PyBoy supplies signed int8 stereo; promote losslessly to PCM16.
with wave.open(str(a.output/'emulator.wav'),'wb') as w:
    w.setnchannels(2);w.setsampwidth(2);w.setframerate(48000)
    w.writeframes((np.concatenate(chunks).astype(np.int16)*256).astype('<i2').tobytes())
engine=[json.loads(l) for l in (a.output/'engine.jsonl').read_text().splitlines()]
events=lambda rs,offset:[dict(frame=r['frame']-offset,noise=r['noise']) for r in rs if 'noise'in r and r['frame']>=offset]
if sum('samples' in r for r in engine) != a.frames:
    raise RuntimeError('Engine trace frame count differs; rerun Java capture with matching --frames')
eng_samples=sum(r.get('samples',0)for r in engine)
em_samples=sum(r.get('samples',0)for r in records if r['frame']>=origin)
e=events(engine,0);m=events(records,origin)
summary=dict(rom_sha256=hashlib.sha256(a.rom.read_bytes()).hexdigest(),
 route_sha256=hashlib.sha256(f'no-input;first-sea-stage;{a.frames}-frames'.encode()).hexdigest(),
 emulator='PyBoy '+importlib.metadata.version('pyboy'),sample_rate=48000,frames=a.frames,
 emulator_origin=origin,engine_origin='first GameSession.update after initializeHeadlessIntro',
 emulator_origin_definition='first post-tick wGameplaySubtype == 3; includes that tick',
 engine_samples=eng_samples,emulator_samples=em_samples,sample_difference=em_samples-eng_samples,
 duration_difference_seconds=(em_samples-eng_samples)/48000,engine_events=e,emulator_events=m,
 paired_event_frame_deltas=[x['frame']-y['frame']for x,y in zip(e,m)],
 limits=['Emulator register values are post-tick snapshots, not all writes or CPU timestamps.',
 'Engine registers have update-level resolution; writes precede the same update PCM render.',
 'PCM phase/amplitude/filter matching is not established; no waveform equality claim.',
 'Both WAVs begin at their declared origins; a constant event delta can reflect origin convention.'])
# Compare frame envelopes after the observed one-frame origin offset; phase and mixer
# levels remain independent, so these metrics do not establish sample equality.
with wave.open(str(a.output/'engine.wav'), 'rb') as w:
    engine_pcm=np.frombuffer(w.readframes(w.getnframes()),dtype='<i2').reshape(-1,2)
emulator_pcm=np.concatenate(chunks).astype(np.int16)*256
engine_rms=[float(np.sqrt(np.mean(engine_pcm[i*800:(i+1)*800].astype(float)**2))) for i in range(a.frames)]
emulator_rms=[float(np.sqrt(np.mean(c.astype(float)**2)))*256 for c in chunks]
(a.output/'envelopes.json').write_text(json.dumps(dict(engine=engine_rms,emulator=emulator_rms)))
summary['engine_rms']=float(np.sqrt(np.mean(engine_pcm.astype(float)**2)))
summary['emulator_rms']=float(np.sqrt(np.mean(emulator_pcm.astype(float)**2)))
summary['frame_rms_correlation_shift_1']=float(np.corrcoef(engine_rms[:-1],emulator_rms[1:])[0,1])
# NR42/NR43 are readable in PyBoy; NR44 readback masks trigger bits.
# Keep only the final engine value for each frame and compare held values.
held={0x21:None,0x22:None}; by_frame={}
for r in engine:
    if r.get('register') in held: by_frame.setdefault(r['frame'],[]).append(r)
mframes={r['frame']-origin:r for r in records if 'registers' in r}
checks={str(reg):{'matched':0,'compared':0}for reg in held}
for f in range(a.frames-1):
    for r in by_frame.get(f,[]):held[r['register']]=r['value']
    for reg,value in held.items():
        if value is not None:
            checks[str(reg)]['compared']+=1
            checks[str(reg)]['matched']+=int(value==mframes[f+1]['registers'][reg-0x20])
summary['noise_register_post_frame_comparison_shift_1']=checks
(a.output/'comparison.json').write_text(json.dumps(summary,indent=2)+'\n')
print(json.dumps(summary,indent=2))
