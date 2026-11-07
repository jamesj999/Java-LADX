"""Run with python3 -m unittest discover -s tools/rom."""
import unittest
from pathlib import Path
from gen_symbols import generate, ROOT

class GeneratorTest(unittest.TestCase):
    def test_checked_in_labels_and_addresses(self):
        import re
        source = (ROOT / 'java/src/main/java/linksawakening/rom/RomSymbols.java').read_text()
        entries = re.findall(r'RomAddress (\w+) = new RomAddress\(0x([0-9A-F]+), 0x([0-9A-F]+)\)', source)
        sym = '\n'.join(f'{bank}:{address} {label}' for label, bank, address in entries)
        self.assertEqual(source, generate(sym, (ROOT / 'tools/rom/symbols.txt').read_text()))

    def test_missing_and_duplicate_labels_fail(self):
        with self.assertRaises(KeyError): generate('', 'Missing')
        with self.assertRaises(ValueError): generate('01:4000 Label', 'Label\nLabel')

    def test_user_rom_symbols_are_readable_and_not_blank(self):
        import re
        rom = ROOT / 'java/src/main/resources/rom/azle.gbc'
        if not rom.exists(): self.skipTest('User-supplied ROM unavailable')
        data = rom.read_bytes()
        source = (ROOT / 'java/src/main/java/linksawakening/rom/RomSymbols.java').read_text()
        for bank, address in re.findall(r'new RomAddress\(0x([0-9A-F]+), 0x([0-9A-F]+)\)', source):
            b, a = int(bank, 16), int(address, 16)
            offset = a if b == 0 else b * 0x4000 + a - 0x4000
            with self.subTest(bank=bank, address=address):
                sample = data[offset:offset + 16]
                self.assertEqual(16, len(sample))
                self.assertNotEqual(bytes([255]) * 16, sample)
