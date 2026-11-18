"""Synthetic coverage; no ROM or emulator initialization."""

import argparse
from pathlib import Path

import numpy as np
from PIL import Image

from common import ROOT, add_output_argument, pixel_mismatches, prepare_output, rgb555


def test_paths_are_relative_to_repository_not_working_directory(monkeypatch, tmp_path):
    monkeypatch.chdir(tmp_path)
    assert (ROOT / "tools/common.py").is_file()
    assert prepare_output(Path("nested/output")) == tmp_path / "nested/output"
    assert (tmp_path / "nested/output").is_dir()


def test_output_argument_preserves_defaults_and_relative_paths():
    parser = argparse.ArgumentParser()
    default = Path("default")
    add_output_argument(parser, default)
    assert parser.parse_args([]).output == default
    assert parser.parse_args(["--output", "relative"]).output == Path("relative")


def test_rgb555_discards_alpha_and_low_three_bits(tmp_path):
    path = tmp_path / "synthetic.png"
    pixels = np.array([[[7, 8, 255, 0], [0, 15, 248, 255]]], dtype=np.uint8)
    Image.fromarray(pixels).save(path)
    actual = rgb555(path)
    np.testing.assert_array_equal(actual, [[[0, 1, 31], [0, 1, 31]]])
    assert actual.dtype == np.uint8


def test_pixel_mask_counts_pixels_once_without_alignment():
    original = np.zeros((2, 3, 3), dtype=np.uint8)
    ours = original.copy()
    ours[0, 1] = [1, 2, 3]
    ours[1, 2, 0] = 1
    expected = [[False, True, False], [False, False, True]]
    np.testing.assert_array_equal(pixel_mismatches(original, ours), expected)
    assert not pixel_mismatches(original, original).any()
