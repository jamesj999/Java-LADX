#!/usr/bin/env python3
"""Run an isolated original-ROM Slime Eye encounter with normal buttons after setup."""

import argparse
import hashlib
import io
import json
from pathlib import Path
from pyboy import PyBoy

ROOT = Path(__file__).resolve().parents[2]
parser = argparse.ArgumentParser()
parser.add_argument("--rom", type=Path, default=ROOT / "java/src/main/resources/rom/azle.gbc")
parser.add_argument("--output", type=Path, default=ROOT / "java/build/slime-eye/controls")
args = parser.parse_args()
args.output.mkdir(parents=True, exist_ok=True)
p = PyBoy(
    str(args.rom), window="null", sound_emulated=False, cgb=True, ram_file=io.BytesIO(bytes(0x8000))
)
p.set_emulation_speed(0)
keys = ["right", "left", "up", "down"]
rng = []
try:
    p.tick(3800)
    p.button("start", 8)
    p.tick(180)
    for key in ["a", "a", "start", "a"]:
        p.button(key, 8)
        p.tick(100)
    assert p.memory[0xDB95] == 0x0B and p.memory[0xFFF6] == 0xA3
    setup = {}

    def write(address, value):
        p.memory[address] = value
        setup[hex(address)] = value

    for i in range(4):
        for j, value in enumerate([1, 2, 0x5A, 80, 96]):
            write(0xD401 + i * 5 + j, value)
    for i in range(16):
        write(0xC280 + i, 0)
    write(0xFF98, 80)
    write(0xFF99, 112)
    write(0xC11C, 0)
    p.button("down", 40)
    for i in range(15):
        p.tick(20)
        write(0xC1BD, 1)
    assert p.memory[0xFFF6] == 0x5A and p.memory[0xFFF7] == 2
    # Initial setup only: initialized real room loaders, then reset its initial boss slot.
    for address in range(0xC200, 0xC490):
        write(address, 0)
    for address, value in {
        0xC280: 4,
        0xC3A0: 0x5B,
        0xC200: 72,
        0xC210: 64,
        0xC430: 0x90,
        0xC360: 80,
        0xC1BD: 0,
        0xC165: 32,
        0xC157: 7,
        0xFF98: 72,
        0xFF99: 80,
        0xFF9A: 0,
        0xFF9B: 0,
        0xFF9E: 2,
        0xFFA2: 0,
        0xFFA3: 0,
        0xC11C: 0,
        0xC19F: 0,
        0xFFE7: 1,
        0xDB00: 8,
        0xDB01: 1,
        0xDB4E: 1,
        0xDB44: 0,
        0xDB5A: 48,
        0xDB5B: 6,
        0xC13D: 0xA2,
        0xC14A: 0,
        0xC5B0: 0,
    }.items():
        write(address, value)

    def observe_rng(context):
        sp = p.register_file.SP
        rng.append(
            [
                p.memory[0xFFE7],
                p.memory[0xFF44],
                p.register_file.A,
                p.memory[sp] | p.memory[sp + 1] << 8,
            ]
        )

    p.hook_register(0, 0x281D, observe_rng, None)
    # SETUP CUTOFF: no RAM/register/entity/Link/RNG writes from here onward.
    pressed = set()
    split = first_death = dead = reward = body = False
    trace = ["frame\tbuttons\tlinkX\tlinkY\tlinkZ\thealth\trunning\teyes\n"]
    checkpoints = []

    def distance(x, y):
        return abs(x - p.memory[0xFF98]) + abs(y - p.memory[0xFF99])

    def move(desired, x, y, facing=-1):
        dx, dy = x - p.memory[0xFF98], y - p.memory[0xFF99]
        if abs(dx) > 2 or abs(dy) > 2:
            desired.add(
                keys[(0 if dx >= 0 else 1) if abs(dx) >= abs(dy) else (3 if dy >= 0 else 2)]
            )
        elif facing >= 0 and p.memory[0xFF9E] != facing:
            desired.add(keys[facing])

    def approach(desired, x, y):
        poses = [[x - 16, y, 0], [x + 16, y, 1], [x, y + 16, 2], [x, y - 16, 3]]
        poses = [pose for pose in poses if 24 <= pose[0] <= 136 and 32 <= pose[1] <= 112]
        pose = (
            min(
                poses,
                key=lambda pose: distance(*pose[:2]) + (4 if pose[2] != p.memory[0xFF9E] else 0),
            )
            if poses
            else [80, 100, -1]
        )
        move(desired, *pose)

    def eyes():
        return [
            [
                i,
                p.memory[0xC280 + i],
                p.memory[0xC290 + i],
                p.memory[0xC200 + i],
                p.memory[0xC210 + i],
                p.memory[0xC310 + i],
                p.memory[0xC360 + i],
                p.memory[0xC2E0 + i],
            ]
            for i in range(16)
            if p.memory[0xC280 + i] and p.memory[0xC3A0 + i] == 0x5B
        ]

    def checkpoint(name, frame):
        p.screen.image.save(str(args.output / (name + "-emulator.png")))
        checkpoints.append(
            {
                "name": name,
                "frame": frame,
                "eyes": eyes(),
                "link": [p.memory[0xFF98], p.memory[0xFF99]],
                "health": p.memory[0xDB5A],
                "max_hearts": p.memory[0xDB5B],
            }
        )

    for frame in range(9000):
        es = eyes()
        active = [e for e in es if e[1] == 5]
        desired = set()
        if p.memory[0xC19F]:
            if frame % 24 < 8:
                desired.add("a")
        elif not split and len(es) == 1:
            eye = es[0]
            s = eye[0]
            if eye[2] >= 2:
                approach(desired, eye[3], eye[4])
                if p.memory[0xC2B0 + s] == 4:
                    desired.update(["a", "b"])
                elif frame % 24 < 8:
                    desired.add("a")
        elif active:
            if not split:
                split = True
                checkpoint("split", frame)
            if p.memory[0xC14A]:
                desired.add("a")
            else:
                target = min(
                    [e for e in active if e[5] < 0x70] or active, key=lambda e: distance(e[3], e[4])
                )
                if target[5] < 32:
                    approach(desired, target[3], target[4])
                else:
                    move(desired, 80, 100)
                if frame % 24 < 8:
                    desired.add("a")
        elif not es:
            dead = True
            hearts = [i for i in range(16) if p.memory[0xC280 + i] and p.memory[0xC3A0 + i] == 0x36]
            if hearts:
                move(desired, p.memory[0xC200 + hearts[0]], p.memory[0xC210 + hearts[0]])
            elif p.memory[0xDB5B] >= 7:
                reward = True
        for key in pressed - desired:
            p.button_release(key)
        for key in desired - pressed:
            p.button_press(key)
        pressed = desired
        p.tick()
        after = eyes()
        trace.append(
            "\t".join(
                map(
                    str,
                    [
                        frame,
                        "+".join(sorted(desired)),
                        p.memory[0xFF98],
                        p.memory[0xFF99],
                        p.memory[0xFFA2],
                        p.memory[0xDB5A],
                        bool(p.memory[0xC14A]),
                        ";".join(",".join(map(str, e)) for e in after),
                    ],
                )
            )
            + "\n"
        )
        if not body and len(after) == 1 and after[0][2] == 3:
            checkpoint("body", frame)
            body = True
        if split and not first_death and any(e[1] == 1 for e in after):
            checkpoint("first-death", frame)
            first_death = True
        if dead and not reward and frame % 8 == 0:
            checkpoint("defeated", frame)
        if reward:
            checkpoint("reward", frame)
            break
        if not p.memory[0xDB5A]:
            break
    (args.output / "emulator.tsv").write_text("".join(trace))
    (args.output / "emulator-rng.json").write_text(json.dumps(rng))
    (args.output / "live-emulator.json").write_text(
        json.dumps(
            {
                "rom_sha256": hashlib.sha256(args.rom.read_bytes()).hexdigest(),
                "setup_writes": setup,
                "policy": "same bounded cardinal-pose policy as SlimeEyeControlsTest; buttons only after setup cutoff; read-only RNG return hook",
                "checkpoints": checkpoints,
                "split": split,
                "dead": dead,
                "reward": reward,
            },
            indent=2,
        )
        + "\n"
    )
    print(
        json.dumps(
            {
                "frames": len(trace) - 1,
                "split": split,
                "dead": dead,
                "reward": reward,
                "health": p.memory[0xDB5A],
                "max_hearts": p.memory[0xDB5B],
            }
        )
    )
finally:
    p.stop(save=False)
