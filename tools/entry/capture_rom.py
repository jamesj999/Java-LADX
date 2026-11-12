#!/usr/bin/env python3
"""Controlled local scroll-entry probe; writes RAM and captures user-ROM frames to /tmp.

This is a synthetic comparison, not a normal-input route or progression proof.
"""

import io
import json
import argparse
from pathlib import Path
from pyboy import PyBoy

parser = argparse.ArgumentParser()
parser.add_argument(
    "--visual", action="store_true", help="Trace body VRAM uploads/OAM and save adjacent frames"
)
args = parser.parse_args()
root = Path(__file__).resolve().parents[2]
p = PyBoy(
    str(root / "java/src/main/resources/rom/azle.gbc"),
    window="null",
    sound_emulated=False,
    cgb=True,
    ram_file=io.BytesIO(bytes(0x8000)),
)
p.set_emulation_speed(0)
p.tick(3800)
p.button("start", 8)
p.tick(180)
for b in ["a", "a", "start", "a"]:
    p.button(b, 8)
    p.tick(100)
for low in (root / "tools/opening/house-route.tsv").read_text().splitlines():
    if not low or low.startswith("#"):
        continue
    b, h, i = low.split()
    h = int(h)
    i = int(i)
    if h:
        p.button_press(b)
    p.tick(h)
    if h:
        p.button_release(b)
    p.tick(i)
print("outside", [p.memory[a] for a in [0xFFF7, 0xFFF6, 0xFF98, 0xFF99, 0xC124, 0xDB95]])
state = io.BytesIO()
p.save_state(state)
out = Path("/tmp/ladx-entry-controlled")
out.mkdir(exist_ok=True)
allrows = {}
uploads = []


def upload(_):
    uploads.append([p.memory[a] for a in [0xFFE7, 0xC124, 0xFF9D]])


if args.visual:
    p.hook_register(0x20, 0x54F5, upload, None)
ground_gates = []
integrations = []


def ground_gate(_):
    ground_gates.append([p.memory[a] for a in [0xFFE7, 0xC11F, 0xFF98, 0xFF99, 0xFF9A, 0xFF9B]])


p.hook_register(2, 0x44A9, ground_gate, None)


def integrate(_):
    integrations.append([p.memory[a] for a in [0xFFE7, 0xC11F, 0xFF98, 0xFF99, 0xFF9A, 0xFF9B]])


p.hook_register(2, 0x44B2, integrate, None)
for button, x, y in [("right", 156, 96), ("left", 3, 96), ("up", 80, 11), ("down", 80, 132)]:
    state.seek(0)
    p.load_state(state)
    for b in ["right", "left", "up", "down"]:
        p.button_release(b)
    p.tick()
    p.memory[0xFF98] = x
    p.memory[0xFF99] = y
    p.memory[0xC11A] = 160
    p.memory[0xC11B] = 80
    p.memory[0xFF9A] = 0
    p.memory[0xFF9B] = 0
    p.button_press(button)
    rows = []
    started = False
    for f in range(100):
        p.tick()
        phase = p.memory[0xC124]
        rows.append(
            [
                f,
                *[
                    p.memory[a]
                    for a in [
                        0xFFF7,
                        0xFFF6,
                        0xC124,
                        0xC125,
                        0xFF98,
                        0xFF99,
                        0xC11A,
                        0xC11B,
                        0xFF9A,
                        0xFF9B,
                        0xFF96,
                        0xFF97,
                        0xDBB1,
                        0xDBB2,
                        0xFFCB,
                        0xFFB2,
                        0xFFE7,
                        0xC11F,
                        0xC133,
                        0xFFAF,
                        0xFF9F,
                        0xFFA0,
                    ]
                ],
            ]
        )
        if phase and not started:
            started = True
            p.screen.image.save(out / f"rom-{button}-start.png")
        if started and not phase:
            p.screen.image.save(out / f"rom-{button}-complete.png")
            break
    allrows[button] = rows
    print(button, "start", rows[0], "complete", rows[-1])
    if button == "right":
        if args.visual:
            rom = (root / "java/src/main/resources/rom/azle.gbc").read_bytes()

            # Exact source labels for this English DX v1.0 probe: table $20:5319,
            # LinkCharacter2Tiles $0C:5800 adjusted to GBC bank $2C by CopyLinkTilesPair.
            def matching_body_states():
                vram = bytes(p.memory[0, 0x8000:0x8040])
                matches = []
                for stateid in range(120):
                    at = 0x20 * 0x4000 + 0x1319 + stateid * 2
                    pair = rom[at : at + 2]
                    base = 0x2C * 0x4000 + 0x1800
                    candidate = (
                        rom[base + pair[0] * 16 : base + pair[0] * 16 + 32]
                        + rom[base + pair[1] * 16 : base + pair[1] * 16 + 32]
                    )
                    if candidate == vram:
                        matches.append(stateid)
                return matches

            print(
                "right completion body",
                "animation",
                p.memory[0xFF9D],
                "VRAM states",
                matching_body_states(),
                "OAM slots0..3",
                list(p.memory[0xFE00:0xFE10]),
                "uploads during scroll",
                uploads[-3:],
            )
        for post in range(4):
            p.tick()
            if args.visual:
                p.screen.image.save(out / f"rom-right-post{post + 1}.png")
                print(
                    "right visual post",
                    post + 1,
                    "animation",
                    p.memory[0xFF9D],
                    "VRAM states",
                    matching_body_states(),
                    "OAM slots0..3",
                    list(p.memory[0xFE00:0xFE10]),
                    "recent uploads",
                    uploads[-3:],
                )
            print(
                "right post",
                *[p.memory[a] for a in [0xFFE7, 0xFF98, 0xFF99, 0xFF9A, 0xFF9B, 0xC133, 0xC11F]],
            )
        print("right slow-ground gates", ground_gates[-5:])
        print("right normal-motion integrations", integrations[-4:])
(out / "rom-trace.json").write_text(json.dumps(allrows, indent=2))
p.stop(save=False)
