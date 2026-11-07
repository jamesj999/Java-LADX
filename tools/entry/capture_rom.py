#!/usr/bin/env python3
"""Controlled local scroll-entry probe; writes RAM and captures user-ROM frames to /tmp.

This is a synthetic comparison, not a normal-input route or progression proof.
"""
import io,json
from pathlib import Path
from pyboy import PyBoy
root=Path(__file__).resolve().parents[2]
p=PyBoy(str(root/'java/src/main/resources/rom/azle.gbc'),window='null',sound_emulated=False,cgb=True,ram_file=io.BytesIO(bytes(0x8000)))
p.set_emulation_speed(0);p.tick(3800);p.button('start',8);p.tick(180)
for b in ['a','a','start','a']:p.button(b,8);p.tick(100)
for l in (root/'tools/opening/house-route.tsv').read_text().splitlines():
 if not l or l.startswith('#'):continue
 b,h,i=l.split();h=int(h);i=int(i)
 if h:p.button_press(b)
 p.tick(h)
 if h:p.button_release(b)
 p.tick(i)
print('outside',[p.memory[a] for a in [0xfff7,0xfff6,0xff98,0xff99,0xc124,0xdb95]])
state=io.BytesIO();p.save_state(state)
out=Path('/tmp/ladx-entry-controlled');out.mkdir(exist_ok=True)
allrows={}
ground_gates=[]
integrations=[]
def ground_gate(_):
 ground_gates.append([p.memory[a] for a in [0xffe7,0xc11f,0xff98,0xff99,0xff9a,0xff9b]])
p.hook_register(2,0x44a9,ground_gate,None)
def integrate(_):
 integrations.append([p.memory[a] for a in [0xffe7,0xc11f,0xff98,0xff99,0xff9a,0xff9b]])
p.hook_register(2,0x44b2,integrate,None)
for button,x,y in [('right',156,96),('left',3,96),('up',80,11),('down',80,132)]:
 state.seek(0);p.load_state(state)
 for b in ['right','left','up','down']:p.button_release(b)
 p.tick()
 p.memory[0xff98]=x;p.memory[0xff99]=y;p.memory[0xc11a]=160;p.memory[0xc11b]=80
 p.memory[0xff9a]=0;p.memory[0xff9b]=0
 p.button_press(button)
 rows=[];started=False
 for f in range(100):
  p.tick();phase=p.memory[0xc124]
  rows.append([f,*[p.memory[a] for a in [0xfff7,0xfff6,0xc124,0xc125,0xff98,0xff99,0xc11a,0xc11b,0xff9a,0xff9b,0xff96,0xff97,0xdbb1,0xdbb2,0xffcb,0xffb2,0xffe7,0xc11f,0xc133,0xffaf,0xff9f,0xffa0]]])
  if phase and not started:
   started=True;p.screen.image.save(out/f'rom-{button}-start.png')
  if started and not phase:
   p.screen.image.save(out/f'rom-{button}-complete.png');break
 allrows[button]=rows
 print(button,'start',rows[0],'complete',rows[-1])
 if button == 'right':
  for post in range(4):
   p.tick()
   print('right post',*[p.memory[a] for a in [0xffe7,0xff98,0xff99,0xff9a,0xff9b,0xc133,0xc11f]])
  print('right slow-ground gates',ground_gates[-5:])
  print('right normal-motion integrations',integrations[-4:])
(out/'rom-trace.json').write_text(json.dumps(allrows,indent=2));p.stop(save=False)
