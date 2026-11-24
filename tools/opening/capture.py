#!/usr/bin/env python3
"""Run the shared opening button route against the original ROM, without RAM writes."""

import io
import argparse
import hashlib
import importlib.metadata
import json
from pathlib import Path
import sys
import zlib

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from common import ROOT, add_output_argument  # noqa: E402
from pyboy import PyBoy

args = argparse.ArgumentParser()
args.add_argument("--route", type=Path, default=ROOT / "tools/opening/house-route.tsv")
args.add_argument(
    "--body-trace-from",
    type=int,
    metavar="CHECKPOINT",
    help="Trace Link body tile uploads, OAM and frame gates from this one-based checkpoint",
)
args.add_argument(
    "--shield-trace-room",
    type=lambda value: int(value, 0),
    metavar="ROOM",
    help="Trace ordinary enemy shield bumps in this overworld room (e.g. 0xf1)",
)
args.add_argument(
    "--trace-entity-loads",
    action="store_true",
    help="Record occupied slots and each room definition allocation",
)
args.add_argument(
    "--trace-scroll-positions",
    action="store_true",
    help="Record raw coordinates and movement fractions at room scroll boundaries and steps",
)
add_output_argument(args, ROOT / "java/build/opening-route")
args = args.parse_args()
if args.body_trace_from is not None and args.body_trace_from < 1:
    raise SystemExit("--body-trace-from requires a positive checkpoint")
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
body_events = []
body_trace = []
body_trace_path = OUT / "emulator-body.tsv"
body_trace_path.unlink(missing_ok=True)
shield_trace_path = OUT / "emulator-shield.tsv"
shield_trace_path.unlink(missing_ok=True)
shield_row = None
shield_trace = []

scroll_trace_path = OUT / "emulator-scroll.tsv"
scroll_trace_path.unlink(missing_ok=True)
scroll_trace = []
load_trace_path = OUT / "emulator-entity-loads.tsv"
load_trace_path.unlink(missing_ok=True)
load_trace = []


def record_scroll_position(address):
    m = p.memory
    fields = (
        frame,
        segment + 1,
        f"{address:04x}",
        m[0xFFE7],
        m[0xDBA5],
        m[0xFFF9],
        m[0xFFF7],
        m[0xFFF6],
        m[0xC124],
        m[0xC125],
        m[0xFF98],
        m[0xFF99],
        m[0xFF9F],
        m[0xFFA0],
        m[0xC11A],
        m[0xC11B],
    )
    scroll_trace.append("\t".join(map(str, fields)) + "\n")


if args.trace_scroll_positions:
    scroll_trace.append(
        "frame\tcheckpoint\taddress_hex\tcpu_frame\tindoor\tside_view\tmap\troom\tphase\t"
        "direction_byte\tx\ty\tfinal_x\tfinal_y\tfraction_x\tfraction_y\n"
    )
    for address in (0x6CB6, 0x6D13, 0x6DE4, 0x790B, 0x7945):
        p.hook_register(2, address, record_scroll_position, address)


def record_entity_load(address):
    m = p.memory
    occupied = sum(1 << i for i in range(16) if m[0xC280 + i])
    fields = [frame, segment + 1, m[0xDBA5], m[0xFFF7], m[0xFFF6], m[0xC124], f"{occupied:04x}"]
    if address == 0x38B4:
        # E is the chosen slot; BC still points at the two-byte ROM definition.
        pointer = (p.register_file.B << 8) | p.register_file.C
        fields.extend((m[0xFFE4], p.register_file.E, m[pointer + 1]))
    else:
        fields.extend(("-", "-", "-"))
    load_trace.append("\t".join(map(str, fields)) + "\n")


if args.trace_entity_loads:
    load_trace.append(
        "frame\tcheckpoint\tindoor\tmap\troom\ttransition\toccupied_hex\torder\tslot\ttype\n"
    )
    for address in (0x37FE, 0x38B4):
        p.hook_register(0, address, record_entity_load, address)


def record_shield_event(address):
    global shield_row
    m = p.memory
    if m[0xDBA5] or m[0xFFF6] != args.shield_trace_room or m[0xC124]:
        return
    slot = p.register_file.C
    if address == 0x6F93:
        shield_row = [
            frame,
            segment + 1,
            m[0xFFE7],
            slot,
            m[0xC3A0 + slot],
            m[0xFF98],
            m[0xFF99],
            m[0xFFEE],
            m[0xFFEC],
            m[0xC200 + slot],
            m[0xC210 + slot],
            m[0xC13E],
        ]
    elif shield_row is not None and shield_row[3] == slot:
        if address == 0x6FCC:
            shield_row.append(p.register_file.A)
        elif len(shield_row) == 13:
            shield_row.extend(
                m[a] for a in (0xFF9A, 0xFF9B, 0xC3F0 + slot, 0xC400 + slot, 0xC410 + slot, 0xC13E)
            )
            shield_trace.append("\t".join(map(str, shield_row)) + "\n")
            shield_row = None


if args.shield_trace_room is not None:
    shield_trace.append(
        "frame\tcheckpoint\tcpu_frame\tslot\ttype\tlink_x\tlink_y\tcached_x\tcached_visual_y\t"
        "entity_x\tentity_y\told_link_ignore\tmagnitude\tlink_vx\tlink_vy\tenemy_vx\tenemy_vy\t"
        "enemy_ignore\tlink_ignore\n"
    )
    for address in (0x6F93, 0x6FCC, 0x73E6):
        p.hook_register(3, address, record_shield_event, address)


def record_body_event(address):
    memory = p.memory
    fields = (
        address,
        memory[0xFFE7],  # hFrameCounter at the CPU hook
        memory[0xFF9D],  # hLinkAnimationState
        memory[0xC124],  # wRoomTransitionState
        memory[0xDB9A],  # wWindowY
        memory[0xC14F],  # wInventoryAppearing
        memory[0xD601],  # wDrawCommand
        memory[0xD6F8],  # wSwitchableObjectAnimationStage
        memory[0xFFA5],  # hReplaceTiles
        memory[0xFFA4],  # hAnimatedTilesGroup
        memory[0xFFA6],  # hAnimatedTilesFrameCount
        memory[0xD6FE],  # wTilesetToLoad
        memory[0xD6FF],  # wBGMapToLoad
    )
    body_events.append(":".join(f"{value:02x}" for value in fields))


if args.body_trace_from is not None:
    for bank, address in (
        (0, 0x022F),
        (0, 0x024D),
        (0, 0x1B58),
        (0, 0x1C51),
        (0x20, 0x54F5),
        (0, 0x1D2E),
    ):
        p.hook_register(bank, address, record_body_event, address)
    body_trace.append(
        "frame\tcheckpoint\tscreen_counter\troom\tx\ty\tpose\tsword\ttransition\t"
        "bg_group\tbg_tick\tobj_vram_crc32\tleft_attr\tright_attr\tcpu_events_hex\n"
    )

frame = segment = 0
try:
    p.tick(3800)
    p.button("start", 8)
    p.tick(180)
    for button in ["a", "a", "start", "a"]:
        p.button(button, 8)
        p.tick(100)
    assert p.memory[0xDB95] == 0x0B and p.memory[0xFFF6] == 0xA3
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
            if args.body_trace_from is not None:
                body_events.clear()
            p.tick()
            if args.body_trace_from is not None and segment + 1 >= args.body_trace_from:
                memory = p.memory
                body_row = (
                    frame,
                    segment + 1,
                    *(
                        memory[address]
                        for address in (
                            0xFFE7,
                            0xFFF6,
                            0xFF98,
                            0xFF99,
                            0xFF9D,
                            0xC137,
                            0xC124,
                            0xFFA4,
                            0xFFA6,
                        )
                    ),
                    f"{zlib.crc32(bytes(memory[0, 0x8000:0x8040])):08x}",
                    memory[0xFE0B],
                    memory[0xFE0F],
                    ",".join(body_events),
                )
                body_trace.append("\t".join(map(str, body_row)) + "\n")
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
                if field in ("kingstate", "instrumentstate"):
                    entity_type = 0xE4 if field == "kingstate" else 0x39
                    slot = next(
                        (
                            i
                            for i in range(16)
                            if p.memory[0xC280 + i]
                            and p.memory[0xC3A0 + i] == entity_type
                            and (field != "instrumentstate" or p.memory[0xC2B0 + i] == 0)
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
    if args.body_trace_from is not None:
        body_trace_path.write_text("".join(body_trace))
    if args.trace_entity_loads:
        load_trace_path.write_text("".join(load_trace))
    if args.trace_scroll_positions:
        scroll_trace_path.write_text("".join(scroll_trace))
    if args.shield_trace_room is not None:
        shield_trace_path.write_text("".join(shield_trace))
    p.stop(save=False)
