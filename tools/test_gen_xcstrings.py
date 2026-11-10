"""Run from the repo root: python3 -m unittest tools/test_gen_xcstrings.py"""
import json, os, sys, tempfile, unittest
HERE = os.path.dirname(os.path.abspath(__file__))
sys.path.insert(0, HERE)
import gen_xcstrings as g


class ConvertFormat(unittest.TestCase):
    def test_android_to_apple_placeholders(self):
        self.assertEqual(g.convert_format("%1$s · %2$s"), "%1$@ · %2$@")
        self.assertEqual(g.convert_format("%d days"), "%lld days")
        self.assertEqual(g.convert_format("%1$d h %2$02d"), "%1$lld h %2$02lld")
        self.assertEqual(g.convert_format(" · rain %1$d%%"), " · rain %1$lld%%")
        self.assertEqual(g.convert_format("100% sure"), "100% sure")


class Catalog(unittest.TestCase):
    def setUp(self):
        self.src = tempfile.mkdtemp()
        with open(os.path.join(self.src, "A.swift"), "w") as f:
            f.write('Text(L("tab_today")); L("mac_quit"); let k = "wx_rain"; let url = "temperature_2m"\n')

    def test_catalog_contains_used_keys_with_translations(self):
        out = os.path.join(tempfile.mkdtemp(), "Localizable.xcstrings")
        g.generate([self.src], out)
        data = json.load(open(out))
        self.assertEqual(data["sourceLanguage"], "en")
        strings = data["strings"]
        self.assertEqual(set(strings), {"tab_today", "mac_quit", "wx_rain"})
        fr = g.load_lang("fr")
        self.assertEqual(strings["tab_today"]["localizations"]["fr"]["stringUnit"]["value"], fr[0]["tab_today"])
        self.assertIn("zh-Hans", strings["mac_quit"]["localizations"])

    def test_unknown_key_used_with_L_fails(self):
        with open(os.path.join(self.src, "B.swift"), "w") as f:
            f.write('L("nope_missing_key")\n')
        with self.assertRaises(SystemExit):
            g.generate([self.src], os.path.join(tempfile.mkdtemp(), "x.xcstrings"))

    def test_every_language_translates_every_mac_key(self):
        for lang in g.LANGS:
            _, mac = g.load_lang(lang)
            self.assertEqual(sorted(set(g.EN_MAC) - set(mac)), [], lang)


if __name__ == "__main__":
    unittest.main()
