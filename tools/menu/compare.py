#!/usr/bin/env python3
"""Capture file-menu states through ROM input, then compare Java's full animation cycle.
Uses tools/intro/requirements.txt. Run with --capture before gradle captureMenu,
then without --capture to compare. Fixture writes only save-summary WRAM; no
VRAM, palettes, OAM, code or menu state is injected.
"""
import argparse, base64, hashlib, json
from pathlib import Path
import numpy as np
from PIL import Image
ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT/'java/build/menu-comparison'
ROM = ROOT/'java/src/main/resources/rom/azle.gbc'
SCENARIOS = {
 'empty': (0, []), 'slot2': (0, ['down']), 'slot3': (0, ['down','down']),
 'new': (0,['a']), 'name': (0,['a','a','right','a','down','a']),
 'saved': (1,[]), 'erase_command': (1,['up']), 'copy_command': (1,['up','right']),
 'erase': (1,['up','a']), 'erase_confirm': (1,['up','a','a']),
 'erase_ok': (1,['up','a','a','right']),
 'copy': (1,['up','right','a']), 'copy_target': (1,['up','right','a','a']),
 'copy_target2': (1,['up','right','a','a','down']),
 'copy_confirm': (1,['up','right','a','a','down','a']),
 'copy_ok': (1,['up','right','a','a','down','a','right']),
 'all_saved': (7,[]), 'erase_second': (7,['up','a','down']),
 'erase_second_confirm': (7,['up','a','down','a']),
 'copy_overwrite': (7,['up','right','a','a','down','a']),
 'new_slot3': (0,['down','down','a']),
 'copy_return': (1,['up','right','a','a','up']),
}
def capture():
 from pyboy import PyBoy
 import io, importlib.metadata
 (OUT/"emulator.json").write_text(json.dumps({"emulator": "PyBoy " + importlib.metadata.version("pyboy"), "rom_sha256": hashlib.sha256(ROM.read_bytes()).hexdigest()}, indent=2))
 p=PyBoy(str(ROM),window='null',sound_emulated=False,cgb=True)
 p.set_emulation_speed(0)
 p.tick(3800)
 p.button('start',8)
 for _ in range(300):
  p.tick()
  if p.memory[0xdb95]==2 and p.memory[0xdb96]==0: break
 else: raise RuntimeError('Did not reach menu preparation')
 state=io.BytesIO(); p.save_state(state)
 manifest=[]
 for name,(saved,buttons) in SCENARIOS.items():
  state.seek(0);p.load_state(state)
  p.memory[0xdb80:0xdb8f]=([66,67,68,69,70]+([77,74,79,76,0,85,70,84,85,0] if saved == 7 else [0]*10)) if saved else [0]*15
  p.memory[0xdba7]=saved
  p.memory[0xdc00:0xdc06]=[0x23,0x01,0x99,0x09,0x07,0] # BCD death count 123
  p.memory[0xdc06:0xdc0c]=[24,24,24,10,14,3]
  p.tick(90)
  for b in buttons:
   p.button(b,8); p.tick(90)
  folder=OUT/'original'/name;folder.mkdir(parents=True,exist_ok=True)
  for f in range(32):
   p.tick();p.screen.image.save(folder/f'{f:02}.png')
  (folder/'tilemap.bin').write_bytes(bytes(p.memory[0x9800:0x9c00]))
  p.memory[0xff4f]=1
  (folder/'attrmap.bin').write_bytes(bytes(p.memory[0x9800:0x9c00]))
  p.memory[0xff4f]=0
  manifest.append(dict(name=name,saved=saved,buttons=buttons,gameplay=p.memory[0xdb95],subtype=p.memory[0xdb96]))
  print(manifest[-1])
 p.stop(save=False)
 (OUT/'scenarios.tsv').write_text(''.join(f'{r["name"]}\t{r["saved"]}\t{",".join(r["buttons"])}\n' for r in manifest))
 (OUT/'manifest.json').write_text(json.dumps(manifest,indent=2))
def compare():
 rows=[];report=['<!doctype html><meta charset="utf-8"><title>File menu comparison</title><style>body{font:16px system-ui;background:#181a20;color:white;margin:32px}article{display:flex;gap:20px}img{width:320px;image-rendering:pixelated}section{margin:30px 0}</style><h1>File menu screenshot comparison</h1>']
 for name in SCENARIOS:
  originals=[np.array(Image.open(OUT/'original'/name/f'{f:02}.png').convert('RGB'))>>3 for f in range(32)]
  ours=[np.array(Image.open(OUT/'ours'/name/f'{f:02}.png').convert('RGB'))>>3 for f in range(32)]
  # Align once for the entire cycle: no per-frame matching or pixel masks.
  a, b = np.array(originals), np.array(ours)
  scores = [int(np.any(a != np.roll(b, shift, axis=0), axis=3).sum()) for shift in range(32)]
  shift = int(np.argmin(scores))
  pairs = [(int(np.any(originals[f] != ours[(f-shift)%32], axis=2).sum()), f, (f-shift)%32)
           for f in range(32)]
  worst=max(pairs)
  rows.append(dict(name=name,max_pixels=worst[0],phase_shift=shift,exact=sum(p[0]==0 for p in pairs)))
  print(rows[-1])
  report.append(f'<section><h2>{name}</h2><p>{rows[-1]["exact"]}/32 frames match with one fixed cycle alignment; worst difference {worst[0]}/23040 pixels.</p><article>')
  for folder,f in [('original',worst[1]),('ours',worst[2])]:
   data=base64.b64encode((OUT/folder/name/f'{f:02}.png').read_bytes()).decode()
   report.append(f'<div>{folder}<br><img src="data:image/png;base64,{data}"></div>')
  diff=np.any(originals[worst[1]]!=ours[worst[2]],axis=2).astype('uint8')*255
  import io
  buf=io.BytesIO();Image.fromarray(diff).save(buf,format='PNG')
  report.append('<div>Difference<br><img src="data:image/png;base64,'+base64.b64encode(buf.getvalue()).decode()+'"></div></article></section>')
 report.insert(1,'<p>Original: PyBoy CGB, built-in boot ROM, ROM SHA256 '+hashlib.sha256(ROM.read_bytes()).hexdigest()+'. Java: production FileMenuController, GPU and GameFrameSceneBuilder framebuffer. Each settled state is sampled for 32 frames and compared against the Java animation cycle at RGB555 precision, without masks. A single cyclic phase offset aligns each 32-frame sequence, preserving animation cadence; transition timing, sound and desktop presentation are not tested. Synthetic saved slot: ABCDE, 10 hearts, 123 deaths; other slots empty. Additional full-save fixtures: LINK (14 hearts, 999 deaths) and TEST (3 hearts, 007 deaths). The entire ordered animation cycle is checked. Original menu states reached by button input; only save-summary WRAM is seeded before menu preparation. No user saves are changed.</p>')
 report.insert(1, f'<p><strong>{sum(r["exact"] for r in rows)}/{len(rows)*32} original screenshots match; {sum(r["max_pixels"] == 0 for r in rows)}/{len(rows)} complete ordered animation cycles match.</strong></p><p>Reproduce: install tools/intro/requirements.txt, run python tools/menu/compare.py --capture, gradle -p java captureMenu, then python tools/menu/compare.py.</p>')
 (OUT/'report.html').write_text('\n'.join(report))
 (OUT/'results.json').write_text(json.dumps(rows,indent=2))
 return any(r['max_pixels'] for r in rows)
if __name__=='__main__':
 ap=argparse.ArgumentParser();ap.add_argument('--capture',action='store_true');args=ap.parse_args()
 OUT.mkdir(parents=True,exist_ok=True)
 if args.capture:capture()
 else:raise SystemExit(compare())
