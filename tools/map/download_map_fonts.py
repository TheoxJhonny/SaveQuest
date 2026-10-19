"""Download pinned local glyphs for the names in a SaveQuest POI catalog."""

import argparse
import hashlib
import json
import pathlib
import urllib.request


def fetch(url):
    request = urllib.request.Request(
        url, headers={"User-Agent": "SaveQuest-educational-offline-map/1.0"}
    )
    with urllib.request.urlopen(request, timeout=60) as response:
        return response.read()


def export(catalog, destination, revision):
    places = json.loads(catalog.read_text(encoding="utf-8"))
    ranges = {0, 256, 512, 768}
    ranges.update(ord(char) // 256 * 256 for place in places for char in place["name"])
    base = f"https://raw.githubusercontent.com/maplibre/demotiles/{revision}/"
    font_folder = destination / "Noto Sans Regular"
    font_folder.mkdir(parents=True, exist_ok=True)
    hashes = {}
    for start in sorted(ranges):
        name = f"{start}-{start + 255}.pbf"
        content = fetch(base + "font/Noto%20Sans%20Regular/" + name)
        (font_folder / name).write_bytes(content)
        hashes[name] = hashlib.sha256(content).hexdigest()
    (destination / "MAPLIBRE-LICENSE.txt").write_bytes(fetch(base + "LICENSE"))
    (destination / "NOTO-OFL.txt").write_bytes(
        fetch("https://raw.githubusercontent.com/google/fonts/main/ofl/notosans/OFL.txt")
    )
    (destination / "source.json").write_text(
        json.dumps(
            {
                "repository": "https://github.com/maplibre/demotiles",
                "commit": revision,
                "font": "Noto Sans Regular",
                "sha256": hashes,
            },
            indent=2,
        ),
        encoding="utf-8",
    )
    print(f"Exported {len(hashes)} glyph ranges")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--catalog", type=pathlib.Path, required=True)
    parser.add_argument("--output", type=pathlib.Path, required=True)
    parser.add_argument("--revision", required=True, help="Commit from fonts/source.json")
    args = parser.parse_args()
    export(args.catalog, args.output, args.revision)
