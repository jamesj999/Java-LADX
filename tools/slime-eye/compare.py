#!/usr/bin/env python3
"""Compare the disclosed isolated Slime Eye probes; writes reports locally only."""
import argparse
import csv
import json
from pathlib import Path
import numpy as np
from PIL import Image

parser = argparse.ArgumentParser()
parser.add_argument('--output', type=Path, default=Path(__file__).resolve().parents[2] / 'java/build/slime-eye')
args = parser.parse_args()
report = {'scope': 'Injected handler states and injected fixed visual poses; no normal D3 route parity.', 'traces': {}, 'images': {}}
for name in ['body-drop', 'half-rise-drop', 'half-death']:
    def rows(suffix):
        with (args.output / (name + suffix + '.tsv')).open() as source:
            return list(csv.DictReader(source, delimiter='\t'))
    emulator, engine = rows('-emulator'), rows('-engine')
    assert len(emulator) == len(engine)
    mismatches = []
    for frame, (actual, expected) in enumerate(zip(emulator, engine)):
        # Clearing a ROM status preserves irrelevant backing bytes, whereas engine slots are values.
        if actual['status'] == expected['status'] == '0': continue
        delta = {key: [actual[key], expected[key]] for key in actual if actual[key] != expected[key]}
        if delta: mismatches.append({'frame': frame, 'delta': delta})
    report['traces'][name] = {'frames': len(engine), 'mismatches': mismatches}
for name in ['body', 'halves']:
    engine = np.array(Image.open(args.output / (name + '-fixture.png')).convert('RGB'))
    emulator = np.array(Image.open(args.output / (name + '-emulator.png')).convert('RGB'))
    # PyBoy maps RGB555 channels with <<3, engine expands 5 bits to 8; compare original 5-bit values.
    crop = (24, 24, 128, 72)
    x, y, x2, y2 = crop
    mismatch = np.any((engine[y:y2, x:x2] >> 3) != (emulator[y:y2, x:x2] >> 3), axis=2)
    report['images'][name] = {'crop_xyxy': crop, 'pixels': int(mismatch.size),
        'rgb555_mismatched_pixels': int(mismatch.sum()), 'rgb_mapping': 'each displayed channel >>3'}
(args.output / 'comparison.json').write_text(json.dumps(report, indent=2) + '\n')
print(json.dumps(report, indent=2))
assert all(not probe['mismatches'] for probe in report['traces'].values())
assert all(not probe['rgb555_mismatched_pixels'] for probe in report['images'].values())
