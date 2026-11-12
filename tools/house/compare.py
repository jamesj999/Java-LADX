#!/usr/bin/env python3
"""Compare the settled opening house animation using PyBoy and production Java rendering.
Install tools/intro/requirements.txt; run --capture, gradle -p java captureHouse,
then run without --capture. No game memory or user saves are modified.
"""

import argparse
import base64
import hashlib
import io
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import ROOT, ROM, rgb555, pixel_mismatches  # noqa: E402
import numpy as np

OUT = ROOT / "java/build/house-comparison"
FRAMES = 400


def capture():
    from pyboy import PyBoy
    import importlib.metadata

    p = PyBoy(
        str(ROM), window="null", sound_emulated=False, cgb=True, ram_file=io.BytesIO(bytes(0x8000))
    )
    p.set_emulation_speed(0)
    try:
        p.tick(3800)
        p.button("start", 8)
        p.tick(180)
        for button in ["a", "a", "start", "a"]:
            p.button(button, 8)
            p.tick(100)
        assert p.memory[0xDB95] == 0x0B and p.memory[0xFFF6] == 0xA3
        folder = OUT / "original"
        folder.mkdir(parents=True, exist_ok=True)
        clocks = []
        for f in range(FRAMES):
            assert p.memory[0xC19F] == 0, "Capture must end before the wake dialog"
            p.screen.image.save(folder / f"{f:03}.png")
            clocks.append([p.memory[0xFFE7], p.memory[0xC450], p.memory[0xC3D0]])
            p.tick()
        (OUT / "clocks.tsv").write_text("".join("\t".join(map(str, row)) + "\n" for row in clocks))
        (OUT / "emulator.json").write_text(
            json.dumps(
                dict(
                    emulator="PyBoy " + importlib.metadata.version("pyboy"),
                    rom_sha256=hashlib.sha256(ROM.read_bytes()).hexdigest(),
                ),
                indent=2,
            )
        )
    finally:
        p.stop(save=False)
    print(f"Captured {FRAMES} house frames through normal new-file input.")


def compare():
    rows = []
    for f in range(FRAMES):
        a = rgb555(OUT / "original" / f"{f:03}.png")
        b = rgb555(OUT / "ours" / f"{f:03}.png")
        rows.append(int(pixel_mismatches(a, b).sum()))
    worst = int(np.argmax(rows))
    result = dict(
        frames=FRAMES, exact=sum(n == 0 for n in rows), max_pixels=max(rows), worst_frame=worst
    )
    (OUT / "results.json").write_text(json.dumps(result, indent=2))
    report = [
        '<!doctype html><meta charset="utf-8"><title>Opening house comparison</title><style>body{font:16px system-ui;margin:32px;background:#181a20;color:white}article{display:flex;gap:24px}img{width:480px;image-rendering:pixelated}</style><h1>Marin and Tarin’s house</h1>',
        f"<p>{result['exact']}/{FRAMES} full 160×144 screenshots match at RGB555 precision. Worst difference: {max(rows)}/23040 pixels.</p>",
        "<p>Original: "
        + json.loads((OUT / "emulator.json").read_text())["emulator"]
        + " CGB, built-in boot ROM. ROM SHA256: "
        + hashlib.sha256(ROM.read_bytes()).hexdigest()
        + ". Reached through button input; no memory injection. Java uses production RoomSession, MarinWakeUpMotion, GPU, GameFrameSceneBuilder and InventoryMenu. Initial animation clocks are synchronized once from read-only WRAM observations; subsequent frames advance normally, with a fixed two-frame OAM/display pipeline offset, no image alignment, masks or per-frame state overrides. This validates the settled opening room and all five bed poses, NPC idle animation and HUD. Fade-in, dialog, sound and desktop presentation are outside this comparison.</p>",
        "<p>Reproduce: install tools/intro/requirements.txt; python tools/house/compare.py --capture; gradle -p java captureHouse; python tools/house/compare.py.</p><article>",
    ]
    for folder in ["original", "ours"]:
        data = base64.b64encode((OUT / folder / f"{worst:03}.png").read_bytes()).decode()
        report.append(f'<div>{folder}<br><img src="data:image/png;base64,{data}"></div>')
    report.append("</article>")
    (OUT / "report.html").write_text("\n".join(report))
    print(result)
    return result["max_pixels"] != 0


if __name__ == "__main__":
    ap = argparse.ArgumentParser()
    ap.add_argument("--capture", action="store_true")
    args = ap.parse_args()
    OUT.mkdir(parents=True, exist_ok=True)
    if args.capture:
        capture()
    else:
        raise SystemExit(compare())
