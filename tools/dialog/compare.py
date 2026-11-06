#!/usr/bin/env python3
"""Verify settled opening dialogue against the shipped ROM in headless PyBoy.

Install tools/intro/requirements.txt in a virtual environment, then run:
    python tools/dialog/compare.py --capture
    gradle -p java captureDialog
    python tools/dialog/compare.py

For later checkpoints add --scenario marin-timing, marin-scroll, bed-exit, tarin-offer,
shield-pose, or tarin-shield
and pass -PdialogScenario=<scenario> to Gradle. Or run tools/verify_opening.py.

The emulator receives normal buttons and fresh, in-memory save RAM. No game
memory is injected. Java receives initial animation clocks and capture times,
never emulator pixels, tiles, palettes or OAM. Dialogue scenarios verify settled
pages; marin-timing covers opening/printing, marin-scroll adds a page advance,
and bed-exit covers the hop and landing. Closing is not yet verified.
"""
import argparse
import base64
import hashlib
import html
import io
import json
from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'java/build/dialog-comparison'
ROM = ROOT / 'java/src/main/resources/rom/azle.gbc'


def capture(scenario):
    from pyboy import PyBoy
    import importlib.metadata

    folder = OUT / 'original'
    folder.mkdir(parents=True, exist_ok=True)
    player = PyBoy(str(ROM), window='null', sound_emulated=False, cgb=True,
                   ram_file=io.BytesIO(bytes(0x8000)))
    player.set_emulation_speed(0)
    rows = []
    elapsed = 0
    try:
        player.tick(3800)
        player.button('start', 8)
        player.tick(180)
        # Create a fresh file named A, using only ordinary menu input.
        for button in ['a', 'a', 'start', 'a']:
            player.button(button, 8)
            player.tick(100)
        assert player.memory[0xdb95] == 0x0b and player.memory[0xfff6] == 0xa3
        def wait_for_dialog():
            for _ in range(1600):
                state = player.memory[0xc19f] & 0x7f
                if state == 12 or (state == 9 and player.memory[0xc1cc] == 1):
                    return state
                player.tick()
            raise RuntimeError('Dialogue did not reach an input wait')

        def finish_dialog():
            for _ in range(30):
                state = wait_for_dialog()
                player.button('a', 8)
                player.tick(9)
                if state == 12:
                    return
            raise RuntimeError('Dialogue did not end')

        if scenario not in ('marin', 'marin-timing', 'marin-scroll'):
            finish_dialog()
            player.tick(30)
            if scenario != 'bed-exit':
                player.button('right', 1)
                player.tick(62)
                for button, duration in [('down', 20), ('right', 35)]:
                    player.button(button, duration)
                    player.tick(duration + 1)
                assert (player.memory[0xff98], player.memory[0xff99]) == (105, 72)
                player.button('a', 8)
                player.tick(16)
                assert player.memory[0xc173] == 0x54
                if scenario in ('shield-pose', 'tarin-shield'):
                    finish_dialog()
                    if scenario == 'shield-pose':
                        player.tick(8)
                        assert player.memory[0xff9d] == 0x6c
                    else:
                        for _ in range(200):
                            if player.memory[0xc173] == 0x91:
                                break
                            player.tick()
                        else:
                            raise RuntimeError('Tarin did not grant the shield')
                        assert player.memory[0xdb44] == 1
        if scenario in ('marin-timing', 'marin-scroll'):
            for _ in range(1000):
                if player.memory[0xc19f] != 0: break
                player.tick()
            else: raise RuntimeError('Wake dialog did not open')
        initial = [player.memory[0xffe7], player.memory[0xc3d0]]
        if scenario in ('bed-exit', 'marin-timing', 'marin-scroll'):
            if scenario == 'bed-exit':
                assert player.memory[0xc19f] == 0
                player.button('right', 1)
            trace = []
            for frame in range(220 if scenario == 'marin-scroll' else 100 if scenario == 'marin-timing' else 64):
                if scenario == 'marin-scroll' and frame == 100:
                    player.button('a', 8)
                player.screen.image.save(folder / f'00-{frame:02}.png')
                rows.append([0, frame, frame, player.memory[0xffe7], 0])
                trace.append([frame] + [player.memory[a] for a in
                    (0xff98, 0xff99, 0xffa2, 0xffa3, 0xff9d, 0xc290)])
                player.tick()
            (OUT / 'motion.tsv').write_text(''.join('\t'.join(map(str, r)) + '\n' for r in trace))
        else:
            for page in range(20):
                for _ in range(1500):
                    state = player.memory[0xc19f] & 0x7f
                    if scenario == 'shield-pose' or state == 12 or (state == 9 and player.memory[0xc1cc] == 1):
                        break
                    player.tick()
                    elapsed += 1
                else:
                    raise RuntimeError('Opening dialogue did not reach its next input wait')
                # Let the final BG command and OAM DMA reach the displayed framebuffer.
                player.tick(4)
                elapsed += 4
                state = player.memory[0xc19f] & 0x7f
                if scenario != 'shield-pose':
                    assert state in (9, 12)
                for frame in range(32):
                    player.screen.image.save(folder / f'{page:02}-{frame:02}.png')
                    rows.append([page, frame, elapsed, player.memory[0xffe7],
                                 player.memory[0xc170]])
                    player.tick()
                    elapsed += 1
                if state == 12 or scenario == 'shield-pose':
                    if scenario == 'marin':
                        assert page == 5, 'Expected all six pages of Dialog001'
                    break
                player.button('a', 8)
                player.tick(16)
                elapsed += 16
            else:
                raise RuntimeError('Opening dialogue did not finish')
        (OUT / 'initial.tsv').write_text('\t'.join(map(str, initial)) + '\n')
        (OUT / 'frames.tsv').write_text(''.join('\t'.join(map(str, row)) + '\n' for row in rows))
        (OUT / 'emulator.json').write_text(json.dumps({
            'emulator': 'PyBoy ' + importlib.metadata.version('pyboy'),
            'rom_sha256': hashlib.sha256(ROM.read_bytes()).hexdigest(),
            'bootrom': 'PyBoy built-in', 'cgb': True,
            'save': 'fresh in-memory RAM; never saved',
        }, indent=2) + '\n')
        # Independent golden data; never generate these from the Java renderer.
        hashes = []
        for page, frame, *_ in rows:
            name = f'{page:02}-{frame:02}.png'
            pixels = np.array(Image.open(folder / name).convert('RGB')) >> 3
            hashes.append(name + ',' + hashlib.sha256(pixels.tobytes()).hexdigest())
        (OUT / 'emulator-rgb555-sha256.csv').write_text('\n'.join(hashes) + '\n')
    finally:
        player.stop(save=False)
    print(f'Captured {len(rows)} frames for {scenario}.')


def compare(scenario):
    rows = []
    for line in (OUT / 'frames.tsv').read_text().splitlines():
        page, frame, *_ = map(int, line.split())
        name = f'{page:02}-{frame:02}.png'
        original = np.array(Image.open(OUT / 'original' / name).convert('RGB')) >> 3
        ours = np.array(Image.open(OUT / 'ours' / name).convert('RGB')) >> 3
        rows.append({'page': page, 'frame': frame,
                     'pixels': int(np.any(original != ours, axis=2).sum())})
    result = {'frames': len(rows), 'exact': sum(r['pixels'] == 0 for r in rows),
              'max_pixels': max(r['pixels'] for r in rows), 'comparisons': rows}
    (OUT / 'results.json').write_text(json.dumps(result, indent=2) + '\n')
    info = json.loads((OUT / 'emulator.json').read_text())
    report = ['''<!doctype html><html lang="en"><meta charset="utf-8">
<title>Opening dialogue emulator comparison</title>
<style>body{font:14px/1.5 var(--font-sans,system-ui);color:var(--foreground);margin:0}
article{display:flex;gap:16px;flex-wrap:wrap}figure{margin:0}img{width:240px;image-rendering:pixelated}
section{margin:16px 0}code{overflow-wrap:anywhere}</style>''',
        f'<p><strong>{result["exact"]}/{len(rows)} full frames match at RGB555 precision.</strong> '
        f'Worst difference: {result["max_pixels"]}/23040 pixels. No masks or image alignment.</p>',
        f'<p>Scenario: {html.escape(scenario)}. '
        'Production Java room, bed, Link, NPC, HUD and dialogue rendering. '
        'One initial NPC clock synchronization and a fixed two-frame OAM display offset. '
        'Java scene/pose setup is checkpoint-based, not a continuous input replay. '
        'The marin-timing scenario covers box opening and first-page letter reveal; marin-scroll adds '
        'one player page advance, both scroll steps and the next two printed lines. Other dialogue '
        'scenarios cover settled pages. Closing, audio and desktop presentation remain '
        'outside these comparisons.</p>',
        f'<p>{html.escape(info["emulator"])} CGB; fresh in-memory save; normal button input. '
        f'ROM SHA256: <code>{info["rom_sha256"]}</code>.</p>']
    if scenario == 'marin-scroll':
        samples = [(0, frame, f'Scroll frame {frame}') for frame in (102, 103, 112, 144, 154, 190)]
    elif scenario == 'marin-timing':
        samples = [(0, frame, f'Opening frame {frame}') for frame in (0, 5, 7, 9, 12, 74)]
    elif scenario == 'bed-exit':
        samples = [(0, frame, f'Jump frame {frame}') for frame in (0, 4, 10, 16, 23, 63)]
    else:
        samples = []
        for page in sorted({row['page'] for row in rows}):
            worst = max((r for r in rows if r['page'] == page), key=lambda r: r['pixels'])
            samples.append((page, worst['frame'], f'Page {page + 1}'))
    for page, frame, label in samples:
        name = f'{page:02}-{frame:02}.png'
        report.append(f'<section><strong>{label}</strong><article>')
        for folder, source in [('original', 'Original ROM'), ('ours', 'Java reconstruction')]:
            data = base64.b64encode((OUT / folder / name).read_bytes()).decode()
            report.append(f'<figure><figcaption>{source}</figcaption>'
                          f'<img alt="{source}, {label}" src="data:image/png;base64,{data}"></figure>')
        report.append('</article></section>')
    report.append('<p>Reproduce: install tools/intro/requirements.txt; run '
                  f'python tools/dialog/compare.py --capture --scenario {scenario}; '
                  f'gradle -p java captureDialog -PdialogScenario={scenario}; '
                  f'python tools/dialog/compare.py --scenario {scenario}.</p></html>')
    (OUT / 'report.html').write_text('\n'.join(report))
    print({key: value for key, value in result.items() if key != 'comparisons'})
    return result['max_pixels'] != 0


if __name__ == '__main__':
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--capture', action='store_true')
    parser.add_argument('--scenario', choices=['marin', 'marin-scroll', 'marin-timing', 'bed-exit', 'tarin-offer', 'shield-pose', 'tarin-shield'], default='marin')
    args = parser.parse_args()
    if args.scenario != 'marin':
        OUT = OUT / args.scenario
    OUT.mkdir(parents=True, exist_ok=True)
    if args.capture:
        capture(args.scenario)
    else:
        raise SystemExit(compare(args.scenario))
