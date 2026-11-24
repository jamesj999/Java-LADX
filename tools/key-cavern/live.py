#!/usr/bin/env python3
"""Run the controlled Key Cavern entrance route using a locally supplied ROM."""

import argparse
import hashlib
import io
import json
from pathlib import Path

from pyboy import PyBoy

ROOT = Path(__file__).resolve().parents[2]


def setup(p):
    p.tick(3800)
    p.button("start", 8)
    p.tick(180)
    for key in ("a", "a", "start", "a"):
        p.button(key, 8)
        p.tick(100)
    m = p.memory
    assert m[0xDB95] == 0x0B and m[0xFFF6] == 0xA3

    # Redirect the house exit through the normal room loader.
    for warp in range(4):
        for offset, value in enumerate((1, 2, 0x52, 72, 108)):
            m[0xD401 + warp * 5 + offset] = value
    for slot in range(16):
        m[0xC280 + slot] = 0
    m[0xFF98], m[0xFF99], m[0xC11C] = 80, 112, 0
    p.button("down", 40)
    p.tick(310)
    assert m[0xFFF6] == 0x52 and m[0xFFF7] == 2 and m[0xC124] == 0

    # Give Link the starting equipment and entry pose.
    for address, value in {
        0xFF98: 72,
        0xFF99: 108,
        0xFF9A: 0,
        0xFF9B: 0,
        0xFFA2: 0,
        0xFFA3: 0,
        0xFF9E: 2,
        0xDB43: 1,
        0xDB00: 3,
        0xDB01: 1,
        0xDB4E: 1,
        0xDB5A: 24,
        0xDB5B: 3,
    }.items():
        m[address] = value


def snapshot(m):
    return {
        "map": m[0xFFF7],
        "room": m[0xFFF6],
        "position": [m[0xFF98], m[0xFF99]],
        "health": m[0xDB5A],
        "carry": m[0xC15C],
        "room_event": m[0xC18E],
        "room_event_resolved": m[0xC18F],
        "scroll": m[0xC124],
        "chest_tile": m[0xD739],
        "small_keys": m[0xDBD0],
        "room_status": m[0xD900 + m[0xFFF6]],
        "entities": [
            dict(
                slot=slot,
                type=m[0xC3A0 + slot],
                status=m[0xC280 + slot],
                position=[m[0xC200 + slot], m[0xC210 + slot]],
            )
            for slot in range(16)
            if m[0xC280 + slot]
        ],
    }


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--rom", type=Path, default=ROOT / "java/src/main/resources/rom/azle.gbc")
    parser.add_argument("--route", type=Path, default=Path(__file__).with_name("entrance.tsv"))
    parser.add_argument(
        "--extension", type=Path, help="optional continuation after the entrance route"
    )
    parser.add_argument(
        "--trace-stalfos", action="store_true", help="record $54 background collision corrections"
    )
    parser.add_argument("--output", type=Path, required=True)
    args = parser.parse_args()
    p = PyBoy(
        str(args.rom),
        window="null",
        sound_emulated=False,
        cgb=True,
        ram_file=io.BytesIO(bytes(0x8000)),
    )
    p.set_emulation_speed(0)
    checkpoints = {}
    frame = 0
    steps = args.route.read_text().splitlines()
    if args.extension:
        steps.extend(args.extension.read_text().splitlines())
    stalfos_collisions = []
    pending_stalfos = None

    def trace_stalfos(phase):
        nonlocal pending_stalfos
        m = p.memory
        if m[0xFFF6] != 0x54 or m[0xFFF7] != 2:
            return
        slot = p.register_file.C
        position = [m[0xC200 + slot], m[0xC210 + slot]]
        if phase == "before":
            pending_stalfos = position
        elif pending_stalfos != position:
            stalfos_collisions.append(
                {
                    "frame": frame,
                    "slot": slot,
                    "state": m[0xC290 + slot],
                    "height": m[0xC310 + slot],
                    "speed": [m[0xC240 + slot], m[0xC250 + slot]],
                    "proposed": pending_stalfos,
                    "corrected": position,
                    "flags": m[0xC2A0 + slot],
                    "objects": [m[0xC503], m[0xC50D]],
                }
            )

    try:
        setup(p)
        if args.trace_stalfos:
            for address in (0x4B0F, 0x4B1E):
                p.hook_register(0x06, address, trace_stalfos, "before")
            for address in (0x4B12, 0x4B21):
                p.hook_register(0x06, address, trace_stalfos, "after")
        start = snapshot(p.memory)
        for line in steps:
            if not line.strip() or line.startswith("#"):
                continue
            buttons, duration, *label = line.split()
            pressed = [] if buttons == "none" else buttons.split("+")
            for button in pressed:
                p.button_press(button)
            if args.trace_stalfos:
                for _ in range(int(duration)):
                    frame += 1
                    p.tick()
            else:
                p.tick(int(duration))
                frame += int(duration)
            for button in pressed:
                p.button_release(button)
            if label:
                checkpoints[label[0]] = dict(frame=frame, **snapshot(p.memory))
    finally:
        p.stop(save=False)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    args.output.write_text(
        json.dumps(
            {
                "rom_sha256": hashlib.sha256(args.rom.read_bytes()).hexdigest(),
                "route_sha256": hashlib.sha256(args.route.read_bytes()).hexdigest(),
                "extension_sha256": hashlib.sha256(args.extension.read_bytes()).hexdigest()
                if args.extension
                else None,
                "setup": "D3 entrance via redirected house exit, then level-one equipment and entry pose",
                "start": start,
                "checkpoints": checkpoints,
                "stalfos_collisions": stalfos_collisions,
            },
            indent=2,
        )
        + "\n"
    )


if __name__ == "__main__":
    main()
