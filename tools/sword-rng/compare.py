#!/usr/bin/env python3
"""Numerical fixed-input call/gating evidence; no waveform/pixel parity assertion."""

import argparse
import csv
import hashlib
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
p = argparse.ArgumentParser(description=__doc__)
p.add_argument("--output", type=Path, default=Path("/tmp/ladx-sword-rng"))
a = p.parse_args()
d = a.output


def read(name):
    return [json.loads(line) for line in (d / name).read_text().splitlines()]


m = read("emulator-rng.jsonl")
after = read("engine-rng.jsonl")
ms = read("emulator-state.jsonl")
es = list(csv.DictReader((d / "engine-state.tsv").open(), delimiter="\t"))
summary = dict(
    frames=len(ms),
    input_sha256=hashlib.sha256((ROOT / "tools/sword-rng/inputs.tsv").read_bytes()).hexdigest(),
    rom_sha256=json.loads((d / "setup.json").read_text())["rom_sha256"],
    emulator=json.loads((d / "setup.json").read_text())["emulator"],
    engine_rng=after,
    emulator_rng=m,
    setup="Isolated D3 Slime Eye; initial frame counter1 andseedA2; identical320-frame button transcript; no state writes aftercutoff.",
    selected_states=[
        dict(frame=f, engine=es[f], emulator=ms[f])
        for f in [192, 193, 194, 200, 216, 240, 257, 264, 288, 312]
    ],
    limits=[
        "ROM hook observes actual call-time hFrameCounter andrLY; engine begins RNGframe before simulation and suppliesLY0.",
        "Post-tick frame counters can match while call-time inputs differ.",
        "Identical buttons do not imply identical dialog/slow-timer/position trajectories.",
        "00:1547 is sword random sound selection; no footstep RNG consumption is missing.",
    ],
)
before = d / "engine-rng-before.jsonl"
if before.exists():
    summary["engine_rng_before"] = [json.loads(line) for line in before.read_text().splitlines()]
(d / "comparison.json").write_text(json.dumps(summary, indent=2) + "\n")
print(json.dumps(summary, indent=2))
