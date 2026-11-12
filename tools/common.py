"""ROM-free helpers shared by command-line capture and comparison scripts."""

from pathlib import Path

import numpy as np
from PIL import Image

ROOT = Path(__file__).resolve().parents[1]
ROM = ROOT / "java/src/main/resources/rom/azle.gbc"


def add_output_argument(parser, default):
    """Keep each CLI's existing default and Path-valued --output option."""
    parser.add_argument("--output", type=Path, default=default)


def prepare_output(path):
    """Resolve and create an output folder; callers choose when to invoke it."""
    output = path.resolve()
    output.mkdir(parents=True, exist_ok=True)
    return output


def rgb555(path):
    """Read RGB channels at the comparison scripts' existing five-bit precision."""
    with Image.open(path) as image:
        return np.array(image.convert("RGB")) >> 3


def pixel_mismatches(original, ours):
    """Return the unchanged per-pixel RGB difference mask, without alignment."""
    return np.any(original != ours, axis=2)
