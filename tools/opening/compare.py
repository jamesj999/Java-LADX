#!/usr/bin/env python3
"""Report unmasked checkpoints from the shared Main/emulator opening route."""
import argparse
import base64
import json
from pathlib import Path
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument('--output', type=Path, default=ROOT / 'java/build/opening-route')
OUT = parser.parse_args().output
rows = []
checkpoint_count = len(OUT.joinpath('emulator-checkpoints.tsv').read_text().splitlines())
for index in range(checkpoint_count):
    source = OUT / 'original' / f'{index:02}.png'
    original = np.array(Image.open(source).convert('RGB')) >> 3
    ours = np.array(Image.open(OUT / 'ours' / source.name).convert('RGB')) >> 3
    rows.append(dict(checkpoint=int(source.stem), pixels=int(np.any(original != ours, axis=2).sum())))
result = dict(checkpoints=len(rows), exact=sum(r['pixels'] == 0 for r in rows),
              worst_pixels=max(r['pixels'] for r in rows), differences=rows)
OUT.joinpath('results.json').write_text(json.dumps(result, indent=2))
html = ['''<!doctype html><html><meta charset="utf-8"><style>
article{display:flex;gap:16px;flex-wrap:wrap}figure{margin:0}img{width:240px;image-rendering:pixelated}section{margin:20px 0}p{line-height:1.5}
</style>''', f'<p><strong>Continuous opening route: {result["exact"]}/{len(rows)} checkpoints match.</strong> '
        f'Worst difference: {result["worst_pixels"]}/23040 pixels at RGB555 precision.</p>',
        '<p>Both games wake Link, finish Marin’s conversation, hop out of bed, walk to Tarin, '
        'receive the shield, and exit into room A2. Extended routes use coordinate and room '
        'goals, releasing held input before waiting for transitions to finish. Java runs '
        'the desktop Main input, update and renderer; no scene or state injections follow bootstrap. '
        'The original is reached by normal file-menu input with fresh in-memory RAM.</p>',
        '<p>This is a diagnostic baseline, not a pixel-parity pass. Java starts at its new-game '
        'bootstrap; the emulator starts at the settled house after file-menu entry. Initial clocks '
        'are not synchronized. Faster loading is intentional; loading duration is not a parity '
        'requirement. Sprite, collision, input and display differences remain to investigate. '
        'No masks or screenshot alignment are applied.</p>']
samples = [(0, 'Marin opening'), (16, 'Shield received'), (21, 'House exit')]
if checkpoint_count >= 69:
    samples = [(16, 'Shield received'), (53, 'Owl conversation'), (65, 'Sword pickup'), (68, 'Sword equipped')]
elif checkpoint_count > 22: samples.append((checkpoint_count - 1, 'Beach approach'))
if checkpoint_count >= 100:
    samples += [(81, 'Return stairway'), (85, 'Bush cutting'), (93, 'Village return'), (99, 'Forest owl')]
if checkpoint_count >= 111:
    samples = [(16, 'Shield received'), (99, 'Forest owl'), (108, 'Cave approach'), (110, 'Mushroom cave entrance')]
for i, label in samples:
    html.append(f'<section><p>{label}: {rows[i]["pixels"]} differing pixels</p><article>')
    for folder, title in [('original', 'Original ROM'), ('ours', 'Java Main loop')]:
        encoded = base64.b64encode(OUT.joinpath(folder, f'{i:02}.png').read_bytes()).decode()
        html.append(f'<figure><figcaption>{title}</figcaption><img src="data:image/png;base64,{encoded}"></figure>')
    html.append('</article></section>')
html.append('<p>Reproduce with tools/opening/capture.py, Gradle captureOpeningRoute and '
            'tools/opening/compare.py. Select the same route/output for both runtimes; '
            'see docs/reconstruction-roadmap.md for the exact beach commands. '
            'Python dependencies: tools/intro/requirements.txt.</p></html>')
OUT.joinpath('report.html').write_text('\n'.join(html))
print({k:v for k,v in result.items() if k != 'differences'})
raise SystemExit(result['exact'] != len(rows))
