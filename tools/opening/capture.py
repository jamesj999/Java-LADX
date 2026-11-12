#!/usr/bin/env python3
"""Run the shared opening button route against the original ROM, without RAM writes."""

import io
import argparse
import hashlib
import importlib.metadata
import json
from pathlib import Path
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import ROOT, add_output_argument  # noqa: E402
from pyboy import PyBoy

args = argparse.ArgumentParser()
args.add_argument("--route", type=Path, default=ROOT / "tools/opening/house-route.tsv")
add_output_argument(args, ROOT / "java/build/opening-route")
args = args.parse_args()
OUT = args.output
OUT.joinpath("original").mkdir(parents=True, exist_ok=True)
OUT.joinpath("emulator.json").unlink(missing_ok=True)
p = PyBoy(
    str(ROOT / "java/src/main/resources/rom/azle.gbc"),
    window="null",
    sound_emulated=False,
    cgb=True,
    ram_file=io.BytesIO(bytes(0x8000)),
)
p.set_emulation_speed(0)
trace = []
checkpoints = []
combat_trace = []
instrument_trace = []
king_trace = []
try:
    p.tick(3800)
    p.button("start", 8)
    p.tick(180)
    for button in ["a", "a", "start", "a"]:
        p.button(button, 8)
        p.tick(100)
    assert p.memory[0xDB95] == 0x0B and p.memory[0xFFF6] == 0xA3
    frame = segment = 0
    for line in args.route.read_text().splitlines():
        if not line or line.startswith("#"):
            continue
        fields = line.split()
        button, held, idle = fields[:3]
        if len(fields) in (4, 5, 6):
            button = fields[3]  # Explicit ROM input; an optional duration can replace the target.
        elif len(fields) != 3:
            raise ValueError(
                "Expected buttons, target, idle, optional ROM buttons, held frames and idle frames: "
                + line
            )
        # '-' keeps the shared target, allowing only the ROM idle to differ.
        if len(fields) >= 5 and fields[4] != "-":
            if not fields[4].isdigit() and "=" not in fields[4]:
                raise ValueError("ROM override must be a frame count or target condition: " + line)
            held = fields[4]
        idle = int(fields[5] if len(fields) == 6 else idle)
        pulse_ticks = 0
        pulse_active = False

        def press():
            global pulse_active
            pulse_active = True
            for key in button.split("+"):
                if key != "none":
                    p.button_press(key.rstrip("*"))

        def release():
            global pulse_active
            pulse_active = False
            for key in button.split("+"):
                if key != "none":
                    p.button_release(key.rstrip("*"))

        def tick_frame():
            global frame, pulse_ticks
            if pulse_active:
                for key in button.split("+"):
                    if key.endswith("*"):
                        if pulse_ticks % 24 == 8:
                            p.button_release(key[:-1])
                        elif pulse_ticks > 0 and pulse_ticks % 24 == 0:
                            p.button_press(key[:-1])
                pulse_ticks += 1
            p.tick()
            if p.memory[0xDB5A] == 0:
                raise RuntimeError(f"Link died at frame {frame}, segment {segment}: {line}")
            trace.append(
                [
                    frame,
                    p.memory[0xFFF7],
                    p.memory[0xFFF6],
                    p.memory[0xFF98],
                    p.memory[0xFF99],
                    p.memory[0xC19F] != 0,
                    p.memory[0xDB44],
                    p.memory[0xC124],
                    p.memory[0xC125],
                    p.memory[0xFFE7],
                    p.memory[0xFF96],
                    p.memory[0xFF97],
                    p.memory[0xDB5A],
                    p.memory[0xDB4E],
                    p.memory[0xFFA1],
                    p.memory[0xFF9B],
                    p.memory[0xC111],
                    p.memory[0xDB00],
                    p.memory[0xDB01],
                    p.memory[0xDB4B],
                    p.memory[0xDB4C],
                    p.memory[0xD851],
                    p.memory[0xDB11],
                    p.memory[0xDBD0],
                    p.memory[0xDBCD],
                    p.memory[0xDBCC],
                    p.memory[0xDBCF],
                    p.memory[0xFF9D],
                    p.memory[0xFF9E],
                    p.memory[0xC137],
                    p.memory[0xFFA2],
                    p.memory[0xFFA3],
                    p.memory[0xFF9A],
                    p.memory[0xFF9B],
                    p.memory[0xDB5B],
                    p.memory[0xDB65],
                ]
            )
            if p.memory[0xFFF7] == 0 and p.memory[0xFFF6] in (0x11, 0x06):
                box = [int(p.memory[0xC5B0] != 0)] + list(p.memory[0xC140:0xC144])
                entities = [
                    [
                        p.memory[0xC3A0 + i],
                        p.memory[0xC200 + i],
                        p.memory[0xC210 + i],
                        p.memory[0xC310 + i],
                        p.memory[0xC360 + i],
                        p.memory[0xC280 + i],
                    ]
                    for i in range(16)
                    if p.memory[0xC280 + i] and p.memory[0xC3A0 + i] in (0x81, 0x82, 0x59)
                ]
                combat_trace.append(
                    str(frame)
                    + "\t"
                    + ";".join(",".join(map(str, row)) for row in [box] + entities)
                    + "\n"
                )
            if p.memory[0xFFF7] == 0 and p.memory[0xFFF6] == 2:
                entries = [
                    [i, p.memory[0xC290 + i], p.memory[0xC2E0 + i], p.memory[0xC450 + i]]
                    for i in range(16)
                    if p.memory[0xC280 + i]
                    and p.memory[0xC3A0 + i] == 0x39
                    and p.memory[0xC2B0 + i] != 1
                ]
                instrument_trace.append(
                    str(frame)
                    + "\t"
                    + str(p.memory[0xD369])
                    + ";"
                    + ";".join(",".join(map(str, row)) for row in entries)
                    + "\n"
                )
            if p.memory[0xFFF7] == 0x15 and p.memory[0xFFF6] == 0xE1:
                slot = next(
                    (i for i in range(16) if p.memory[0xC280 + i] and p.memory[0xC3A0 + i] == 0xE4),
                    None,
                )
                values = (
                    [-1]
                    if slot is None
                    else [
                        p.memory[base + slot]
                        for base in [0xC290, 0xC200, 0xC210, 0xC310, 0xC360, 0xC2E0, 0xC450]
                    ]
                )
                values.append(p.memory[0xD47C])
                king_trace.append(str(frame) + "\t" + ",".join(map(str, values)) + "\n")
            frame += 1

        if "=" in held:
            field, target = held.split("=")
            target = int(target, 0)
            address = {
                "room": 0xFFF6,
                "x": 0xFF98,
                "y": 0xFF99,
                "dialog": 0xC19F,
                "sword": 0xDB4E,
                "inventorytick": 0xDE06,
                "bgtick": 0xFFA6,
                "smallkeys": 0xDBD0,
                "nightmarekey": 0xDBCF,
                "instrumentstate": 0xC290,
                "kingstate": 0xC290,
                "bowwow": 0xDB56,
            }[field]

            def reached():
                value = p.memory[address]
                if field == "kingstate":
                    slot = next(
                        (
                            i
                            for i in range(16)
                            if p.memory[0xC280 + i] and p.memory[0xC3A0 + i] == 0xE4
                        ),
                        None,
                    )
                    value = -1 if slot is None else p.memory[0xC290 + slot]
                if field == "inventorytick":
                    value += (p.memory[0xDE07] // 4) * 8
                if field == "dialog":
                    return (value != 0) == bool(target)
                if field not in ("x", "y"):
                    return value == target
                return (
                    value <= target if button.split("+")[0] in ("left", "up") else value >= target
                )

            press()
            for _ in range(2000):
                if reached():
                    break
                tick_frame()
            else:
                raise RuntimeError("Route blocked: " + line + " " + str(trace[-1]))
            release()
            tick_frame()
            for _ in range(500):
                if p.memory[0xC124] == 0:
                    break
                tick_frame()
            else:
                raise RuntimeError("Transition did not finish: " + line)
            held = 0
        else:
            held = int(held)
        if held:
            press()
        for tick in range(held + idle):
            if tick == held and held:
                release()
            tick_frame()
        if held and idle == 0:
            release()
        p.screen.image.save(OUT / "original" / f"{segment:02}.png")
        checkpoints.append([segment] + trace[-1])
        entities = [
            f"{p.memory[0xC3A0 + i]:02X}:{p.memory[0xC280 + i]}@{p.memory[0xC200 + i]},{p.memory[0xC210 + i]}"
            for i in range(16)
            if p.memory[0xC280 + i] != 0
        ]
        print(segment + 1, trace[-1], "entities", " ".join(entities))
        segment += 1
    OUT.joinpath("emulator-checkpoints.tsv").write_text(
        "".join("\t".join(map(str, r)) + "\n" for r in checkpoints)
    )
    OUT.joinpath("emulator.json").write_text(
        json.dumps(
            {
                "emulator": "PyBoy " + importlib.metadata.version("pyboy"),
                "rom_sha256": hashlib.sha256(
                    ROOT.joinpath("java/src/main/resources/rom/azle.gbc").read_bytes()
                ).hexdigest(),
                "route_sha256": hashlib.sha256(args.route.read_bytes()).hexdigest(),
                "checkpoints": len(checkpoints),
                "bowwow_state": p.memory[0xDB56],
                "rom_button_overrides": sum(
                    len(line.split()) >= 4
                    for line in args.route.read_text().splitlines()
                    if line and not line.startswith("#")
                ),
                "memory_injection": False,
                "save": "fresh in-memory RAM, never saved",
            },
            indent=2,
        )
    )
    OUT.joinpath("emulator-instrument.tsv").write_text("".join(instrument_trace))
    OUT.joinpath("emulator-combat.tsv").write_text("".join(combat_trace))
    OUT.joinpath("emulator-trace.tsv").write_text(
        "".join("\t".join(map(str, r)) + "\n" for r in trace)
    )
finally:
    # Failed runs still carry the observations needed to find their first
    # divergence. A completion manifest is only written by the successful path.
    OUT.joinpath("emulator-checkpoints.tsv").write_text(
        "".join("\t".join(map(str, r)) + "\n" for r in checkpoints)
    )
    OUT.joinpath("emulator-trace.tsv").write_text(
        "".join("\t".join(map(str, r)) + "\n" for r in trace)
    )
    OUT.joinpath("emulator-instrument.tsv").write_text("".join(instrument_trace))
    OUT.joinpath("emulator-combat.tsv").write_text("".join(combat_trace))
    OUT.joinpath("emulator-king.tsv").write_text("".join(king_trace))
    p.stop(save=False)
