#!/usr/bin/env python3
"""Report bounded live-fight checkpoints without claiming full-frame equivalence."""

import argparse
import csv
import hashlib
import json
from pathlib import Path
import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument("--output", type=Path, default=ROOT / "java/build/slime-eye/controls")
args = parser.parse_args()
report = {
    "scope": "Independent closed-loop normal-button runs after disclosed D3 setup; trajectories and pictures differ.",
    "runs": {},
    "images": {},
}
for name in ["engine", "emulator"]:
    with (args.output / (name + ".tsv")).open() as source:
        rows = list(csv.DictReader(source, delimiter="\t"))
    for row in rows:
        row["entities"] = [
            list(map(int, entry.split(","))) for entry in row["eyes"].split(";") if entry
        ]
    split = next(row for row in rows if len(row["entities"]) == 2)
    first_hit = next(row for row in rows if any(e[0] == 15 and e[6] < 5 for e in row["entities"]))
    hit_frame = int(first_hit["frame"])
    hit_eye = next(e for e in first_hit["entities"] if e[0] == 15)
    recoil_z = [
        next(e for e in row["entities"] if e[0] == 15)[5]
        for row in rows[hit_frame : hit_frame + 11]
    ]
    death = {}
    for slot in [14, 15]:
        start = next(
            int(row["frame"])
            for row in rows
            if any(e[0] == slot and e[1] == 1 for e in row["entities"])
        )
        clear = next(
            int(row["frame"])
            for row in rows[start:]
            if not any(e[0] == slot for e in row["entities"])
        )
        death[slot] = {"lethal_frame": start, "clear_frame": clear, "elapsed_frames": clear - start}
    report["runs"][name] = {
        "frames": len(rows),
        "split_frame": int(split["frame"]),
        "split_slots_and_hp": [[e[0], e[6]] for e in split["entities"]],
        "first_boots_half_hit_frame": hit_frame,
        "first_boots_half_hp": hit_eye[6],
        "running_after_first_half_hit": first_hit["running"].lower() == "true",
        "z_from_first_hit_through_next_10_frames": recoil_z,
        "death": death,
        "final_link_health": int(rows[-1]["health"]),
    }
    assert hit_eye[6] == 4 and first_hit["running"].lower() == "false"
    assert len(set(recoil_z)) == 1
    assert all(entry["elapsed_frames"] == 225 for entry in death.values())
for name in ["body", "split", "first-death", "reward"]:
    engine = np.array(Image.open(args.output / (name + ".png")).convert("RGB")) >> 3
    emulator = np.array(Image.open(args.output / (name + "-emulator.png")).convert("RGB")) >> 3
    report["images"][name] = {
        "rgb555_mismatched_pixels": int(np.any(engine != emulator, axis=2).sum()),
        "pixels": 160 * 144,
        "interpretation": "Semantic checkpoint comparison, not matched pose/frame parity.",
    }
report["engine_policy_sha256"] = hashlib.sha256(
    (ROOT / "java/src/test/java/linksawakening/world/entity/SlimeEyeControlsTest.java").read_bytes()
).hexdigest()
report["emulator_policy_sha256"] = hashlib.sha256(
    (ROOT / "tools/slime-eye/live.py").read_bytes()
).hexdigest()
report["remaining_gaps"] = [
    "ROM footstep RNG call 00:1547 precedes first boss RNG call; engine footstep path does not consume RNG.",
    "GameSession supplies scanline zero; observed original-ROM boss RNG calls use nonzero LY, including29 at first04:4B85 return.",
    "First body state3 is engine frame253 versus ROM256 under these setups/input response clocks.",
    "Live inputs, trajectories, Link damage, sprite phases, reward OAM presentation and whole frames differ; no normal D3 route claim.",
]
(args.output / "live-comparison.json").write_text(json.dumps(report, indent=2) + "\n")
print(json.dumps(report, indent=2))
