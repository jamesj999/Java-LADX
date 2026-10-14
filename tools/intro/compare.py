#!/usr/bin/env python3
"""Run the shipped ROM in PyBoy and compare production Java intro captures.

    gradle -p java captureIntro
    python tools/intro/compare.py --all

Requires the versions in requirements.txt. No ROM bytes or emulator frames are
used by the Java runtime. Output goes to java/build/intro-comparison by default.
"""
import argparse
import base64
import html
import json
import random
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
ROM = ROOT / 'java/src/main/resources/rom/azle.gbc'


def capture_original(output, count):
    from pyboy import PyBoy
    import hashlib
    import importlib.metadata
    folder = output / 'original'
    folder.mkdir(parents=True, exist_ok=True)
    records = []
    player = PyBoy(str(ROM), window='null', sound_emulated=False, cgb=True)
    player.set_emulation_speed(0)
    origin = None
    try:
        for frame in range(count):
            player.tick()
            mem = player.memory
            stage = mem[0xdb96]
            if origin is None and stage == 3:
                origin = frame
            oam = mem[0xfe00:0xfea0]
            rain = [[oam[i+1]-8, oam[i]-16, oam[i+2]] for i in range(0,160,4)
                    if oam[i+2] == 0x28 or 0x70 <= oam[i+2] <= 0x76]
            records.append(dict(frame=frame, stage=stage, rain=rain,
                                scrollX=mem[0xff96], scrollY=mem[0xff97]))
            player.screen.image.save(folder / f'{frame:04}.png')
    finally:
        player.stop(save=False)
    (folder/'frames.jsonl').write_text(''.join(json.dumps(r)+'\n' for r in records))
    (output/'emulator.json').write_text(json.dumps(dict(
        emulator='PyBoy ' + importlib.metadata.version('pyboy'), cgb=True, bootrom='PyBoy built-in', input='none',
        origin=origin, rom_sha256=hashlib.sha256(ROM.read_bytes()).hexdigest()), indent=2))


def rain_mask(records, rom):
    # OAM read after tick is the following display frame's DMA state.
    # Mask only nontransparent pixels of the randomly positioned rain sprites.
    # Ship, lightning, characters, background, fades and title stay unmasked.
    mask = np.zeros((144,160), dtype=bool)
    for x,y,tile in records:
        tile &= 0xfe
        offset = (0x30*0x4000 + 0x1400 + tile*16) if tile == 0x28 else (0x4000 + 0x2d4a + (tile-0x70)*16)
        for dy in range(16):
            lo, hi = rom[offset+dy*2:offset+dy*2+2]
            for dx in range(8):
                if 0 <= x+dx < 160 and 0 <= y+dy < 144 and ((lo|hi) & (0x80 >> dx)):
                    mask[y+dy,x+dx] = True
    return mask


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--output', type=Path, default=ROOT/'java/build/intro-comparison')
    parser.add_argument('--reuse-original', action='store_true')
    parser.add_argument('--all', action='store_true', help='Compare every frame as well as the sampled report')
    parser.add_argument('--frames', type=int, default=3800)
    parser.add_argument('--seed', type=int, default=20261015)
    args = parser.parse_args()
    out = args.output.resolve()
    out.mkdir(parents=True, exist_ok=True)
    if not args.reuse_original:
        capture_original(out, args.frames)
    original = [json.loads(s) for s in (out/'original/frames.jsonl').read_text().splitlines()]
    ours = [json.loads(s) for s in (out/'ours/frames.jsonl').read_text().splitlines()]
    info = json.loads((out/'emulator.json').read_text())
    origin = info['origin']
    rom = ROM.read_bytes()
    rng = random.Random(args.seed)
    bands = [('Sea', 8, 896), ('Link close-up', 901, 1060), ('Ship fade', 1062, 1148),
             ('Beach fade', 1155, 1409), ('Beach', 1410, 2724), ('Upward pan', 2725, 3396),
             ('Title reveal',3397,3429), ('Title DX',3430,3670)]
    samples = [(label, f) for label,lo,hi in bands for f in sorted(rng.sample(range(lo,hi),6))]
    # Keep the original random samples as fixed regression controls, now on one common clock.
    controls = [215,228,462,911,1032,1075,1077,1213,2225,2231,2286,2496,3494]
    samples += [('Original sample', f) for f in controls if f not in {f for _,f in samples}]
    def compare_frame(label, f):
        af = f + origin
        a = np.array(Image.open(out/'original'/f'{af:04}.png').convert('RGB')) >> 3
        b = np.array(Image.open(out/'ours'/f'{f:04}.png').convert('RGB')) >> 3
        mismatch = np.any(a != b, axis=2)
        # PyBoy's post-tick OAM is for the following displayed frame.
        mask = rain_mask(original[af-1]['rain'],rom) | rain_mask(ours[f]['rain'],rom)
        return dict(scene=label, original=af, ours=f, pixels=int(mismatch.sum()),
                    outside_rain=int((mismatch & ~mask).sum()), rain_pixels=int(mask.sum()))

    rows = [compare_frame(label, f) for label, f in samples]
    full = None
    if args.all:
        full = [compare_frame(ours[f]['stage'], f)
                for f in range(min(len(original)-origin, len(ours)))]
        (out/'all-frames.json').write_text(json.dumps(full, indent=2))
    (out/'samples-after.json').write_text(json.dumps(rows,indent=2))
    report = ['''<!doctype html><html lang="en"><meta charset="utf-8"><title>Intro emulator validation</title>
<style>body{font:16px/1.5 system-ui;background:#191c24;color:#edf0f7;max-width:1050px;margin:32px auto;padding:0 20px}section{padding:20px;margin:20px 0;background:#262c38}img{width:320px;height:288px;image-rendering:pixelated}figure{margin:0}article{display:flex;gap:20px;flex-wrap:wrap}code{font-size:13px}</style>
<h1>Intro emulator validation</h1>''']
    exact = sum(r['pixels']==0 for r in rows)
    controlled = sum(r['outside_rain']==0 for r in rows)
    report.append(f'<p>{exact}/{len(rows)} frames match every RGB555 pixel; {controlled}/{len(rows)} match outside the actual rain pixels. Seed: {args.seed}.</p>')
    if full is not None:
        report.append(f'<p>Exhaustive run: {sum(r["pixels"] == 0 for r in full)}/{len(full)} frames match completely; {sum(r["outside_rain"] == 0 for r in full)}/{len(full)} match outside rain.</p>')
    report.append(f'<p>ROM: <code>{info["rom_sha256"]}</code>. {html.escape(info["emulator"])} in CGB mode, built-in boot ROM, no input. Original sea origin: emulator frame {origin}. All pairs use this single origin; later phases are never realigned.</p>')
    report.append('<p>Reproduce: install <code>tools/intro/requirements.txt</code> in a Python virtual environment, run <code>gradle -p java captureIntro</code>, then <code>python tools/intro/compare.py --all</code>. The comparison exits unsuccessfully if any pixel outside rain differs.</p>')
    report.append('<p>Java uses IntroSequence → GameFrameSceneBuilder → production framebuffer. RGB channels are reduced to 5 bits to remove expansion differences. Rain placement depends on the Game Boy scanline counter during RNG calls; the reconstruction does not emulate instruction timing. Both full-frame differences and differences outside the exact nontransparent rain pixels are reported. No other pixels are excluded. Audio and desktop presentation are not tested here.</p>')
    for r in rows:
        report.append(f'<section><h2>{r["scene"]} · Java frame {r["ours"]}</h2><p>{r["pixels"]}/23040 differing pixels; {r["outside_rain"]} outside rain.</p><article>')
        for folder,frame,label in [('original',r['original'],'Original'),('ours',r['ours'],'Java')]:
            data=base64.b64encode((out/folder/f'{frame:04}.png').read_bytes()).decode()
            report.append(f'<figure><figcaption>{label} · frame {frame}</figcaption><img alt="{label} frame {frame}" src="data:image/png;base64,{data}"></figure>')
        report.append('</article></section>')
    report.append('</html>')
    (out/'report.html').write_text('\n'.join(report))
    print(f'{exact}/{len(rows)} exact; {controlled}/{len(rows)} exact outside rain')
    for row in rows:
        if row['outside_rain']: print(row)
    if full is not None:
        print(f'All frames: {sum(r["pixels"] == 0 for r in full)}/{len(full)} exact; {sum(r["outside_rain"] == 0 for r in full)}/{len(full)} exact outside rain')
    print(out/'report.html')
    if any(r['outside_rain'] for r in (full if full is not None else rows)):
        raise SystemExit(1)


if __name__ == '__main__':
    main()
