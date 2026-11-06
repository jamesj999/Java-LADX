#!/usr/bin/env python3
"""Report unmasked checkpoints from the shared Main/emulator opening route."""
import base64
import json
from pathlib import Path
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
OUT = ROOT / 'java/build/opening-route'
rows = []
for source in sorted((OUT / 'original').glob('*.png')):
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
        'receive the shield, and exit into room A2 using the same button durations. Java runs '
        'the desktop Main input, update and renderer; no scene or state injections follow bootstrap. '
        'The original is reached by normal file-menu input with fresh in-memory RAM.</p>',
        '<p>This is a diagnostic baseline, not a pixel-parity pass. Java starts at its new-game '
        'bootstrap; the emulator starts at the settled house after file-menu entry. Initial clocks '
        'are not synchronized. Animation/input/display phase differences remain, including a '
        'one-pixel walking endpoint difference. No masks or screenshot alignment are applied.</p>']
for i, label in [(0, 'Marin opening'), (9, 'Walk to Tarin'), (16, 'Shield received'), (21, 'House exit')]:
    html.append(f'<section><p>{label}: {rows[i]["pixels"]} differing pixels</p><article>')
    for folder, title in [('original', 'Original ROM'), ('ours', 'Java Main loop')]:
        encoded = base64.b64encode(OUT.joinpath(folder, f'{i:02}.png').read_bytes()).decode()
        html.append(f'<figure><figcaption>{title}</figcaption><img src="data:image/png;base64,{encoded}"></figure>')
    html.append('</article></section>')
html.append('<p>Reproduce: python tools/opening/capture.py; gradle -p java captureOpeningRoute; '
            'python tools/opening/compare.py. Python dependencies: tools/intro/requirements.txt.</p></html>')
OUT.joinpath('report.html').write_text('\n'.join(html))
print({k:v for k,v in result.items() if k != 'differences'})
raise SystemExit(result['exact'] != len(rows))
