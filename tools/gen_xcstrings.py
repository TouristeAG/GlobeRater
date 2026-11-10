#!/usr/bin/env python3
"""Writes macos/Shared/Localizable.xcstrings (and InfoPlist.xcstrings) from tools/strings_en.py
and tools/i18n/<lang>.py, so the Mac app reuses the Android translations.
Only keys used by the Swift sources are included. Mac-only texts live in EN_MAC / T_MAC.
Run from the repo root:  python3 tools/gen_xcstrings.py
"""
import importlib.util, json, os, re, sys
HERE = os.path.dirname(os.path.abspath(__file__))
ROOT = os.path.join(HERE, '..')
sys.path.insert(0, HERE)
from strings_en import EN, EN_MAC

# language file -> Apple language code
LANGS = {'fr': 'fr', 'es': 'es', 'pt': 'pt', 'de': 'de', 'it': 'it', 'nl': 'nl', 'ru': 'ru', 'tr': 'tr',
         'ar': 'ar', 'hi': 'hi', 'zh': 'zh-Hans', 'ja': 'ja', 'ko': 'ko', 'id': 'id'}
SOURCES = [os.path.join(ROOT, 'macos', d) for d in ('GlobeRater', 'GlobeRaterWidgets', 'WidgetUI', 'GlobeKit/Sources')]
PLACEHOLDER = re.compile(r'%(\d+\$)?([-#+0,(]*\d*)([sd])')
LITERAL = re.compile(r'"([a-z][a-z0-9]*(?:_[a-z0-9]+)+)"')
CALL = re.compile(r'\bL\("([a-z0-9_]+)"')


def convert_format(text):
    """Android placeholders (%1$s, %d) → Apple ones (%1$@, %lld). %% stays as is."""
    parts = text.split('%%')
    conv = [PLACEHOLDER.sub(lambda m: '%' + (m.group(1) or '') + m.group(2) + ('@' if m.group(3) == 's' else 'lld'), p) for p in parts]
    return '%%'.join(conv)


def load_lang(lang):
    spec = importlib.util.spec_from_file_location(lang, os.path.join(HERE, 'i18n', f'{lang}.py'))
    mod = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(mod)
    return mod.T, getattr(mod, 'T_MAC', {})


def used_keys(dirs):
    """Snake-case string literals of the Swift sources that are known keys; L("…") keys must all be known."""
    known = set(EN) | set(EN_MAC)
    used, unknown = set(), set()
    for d in dirs:
        for base, _, files in os.walk(d):
            for name in files:
                if not name.endswith('.swift'):
                    continue
                src = open(os.path.join(base, name), encoding='utf-8').read()
                used |= {k for k in LITERAL.findall(src) if k in known}
                unknown |= {k for k in CALL.findall(src) if k not in known}
                unknown |= {k for k in LITERAL.findall(src) if k.startswith('mac_') and k not in known}
    if unknown:
        print('Unknown string keys used in Swift: ' + ', '.join(sorted(unknown)), file=sys.stderr)
        sys.exit(1)
    return used


def unit(value):
    return {'stringUnit': {'state': 'translated', 'value': convert_format(value)}}


def catalog(entries):
    return {'sourceLanguage': 'en', 'strings': entries, 'version': '1.0'}


def generate(dirs, out):
    langs = {lang: load_lang(lang) for lang in LANGS}
    for lang, (_, mac) in langs.items():
        missing = sorted(set(EN_MAC) - set(mac))
        if missing:
            print(f'{lang}: missing Mac translations: ' + ', '.join(missing), file=sys.stderr)
            sys.exit(1)
    strings = {}
    for key in sorted(used_keys(dirs)):
        en = EN_MAC.get(key, EN.get(key))
        locs = {'en': unit(en)}
        for lang, (t, mac) in langs.items():
            value = mac.get(key, t.get(key))
            if value:
                locs[LANGS[lang]] = unit(value)
        strings[key] = {'extractionState': 'manual', 'localizations': locs}
    write(out, catalog(strings))
    return strings


def generate_infoplist(out):
    """Location permission text shown by macOS."""
    locs = {'en': unit(EN_MAC['mac_location_usage'])}
    for lang, code in LANGS.items():
        locs[code] = unit(load_lang(lang)[1]['mac_location_usage'])
    write(out, catalog({'NSLocationUsageDescription': {'extractionState': 'manual', 'localizations': locs}}))


def write(path, data):
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2, sort_keys=True)
        f.write('\n')


if __name__ == '__main__':
    shared = os.path.join(ROOT, 'macos', 'Shared')
    n = len(generate(SOURCES, os.path.join(shared, 'Localizable.xcstrings')))
    generate_infoplist(os.path.join(ROOT, 'macos', 'GlobeRater', 'InfoPlist.xcstrings'))
    print(f'Localizable.xcstrings: {n} keys, {len(LANGS) + 1} languages')
