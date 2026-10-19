"""Generate the offline POI catalog. Run on the development computer only."""

import argparse
import datetime
import json
import pathlib
import urllib.parse
import urllib.request

BBOX = "-33.65,-70.85,-33.25,-70.45"
ENDPOINT = "https://overpass-api.de/api/interpreter"
QUERY = f'''[out:json][timeout:180];(
nwr[shop~"^(supermarket|convenience|bakery|clothes|department_store|mall|hardware|books)$"]({BBOX});
nwr[amenity~"^(restaurant|cafe|fast_food|pharmacy)$"]({BBOX});
);out center tags;'''


def category(tags):
    if tags.get("shop") in {"supermarket", "convenience"}:
        return "groceries"
    if tags.get("amenity") == "pharmacy":
        return "pharmacy"
    if tags.get("amenity") in {"restaurant", "cafe", "fast_food"} or tags.get("shop") == "bakery":
        return "food"
    return "shopping"


def export(destination):
    request = urllib.request.Request(
        ENDPOINT,
        data=urllib.parse.urlencode({"data": QUERY}).encode(),
        headers={"User-Agent": "SaveQuest-educational-offline-map/1.0"},
    )
    with urllib.request.urlopen(request, timeout=240) as response:
        data = json.load(response)
    if data.get("remark"):
        raise RuntimeError(data["remark"])
    places = []
    for element in data["elements"]:
        tags = element.get("tags", {})
        name = (tags.get("name") or tags.get("brand") or "").strip()
        point = element if element["type"] == "node" else element.get("center", {})
        if not name or "lat" not in point or "lon" not in point:
            continue
        if not (-33.65 <= point["lat"] <= -33.25 and -70.85 <= point["lon"] <= -70.45):
            continue
        street = " ".join(filter(None, [tags.get("addr:street"), tags.get("addr:housenumber")]))
        address = ", ".join(filter(None, [street, tags.get("addr:suburb") or tags.get("addr:city")]))
        places.append({
            "id": f"osm:{element['type']}:{element['id']}",
            "name": name,
            "category": category(tags),
            "address": address,
            "latitude": round(point["lat"], 6),
            "longitude": round(point["lon"], 6),
        })
    places.sort(key=lambda place: place["id"])
    assert len(places) > 100, "Incomplete catalog; keep the previous assets"
    destination.mkdir(parents=True, exist_ok=True)
    (destination / "places.json").write_text(json.dumps(places, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    metadata = {
        "source": ENDPOINT,
        "license": "ODbL-1.0",
        "attribution": "© OpenStreetMap contributors",
        "bbox": BBOX,
        "downloadedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
        "osmTimestamp": data["osm3s"]["timestamp_osm_base"],
        "count": len(places),
        "query": QUERY,
        "transform": "Named shops only; way/relation bounding-box centers; coordinates rounded to six decimals. No proximity deduplication of distinct OSM objects.",
    }
    (destination / "places-source.json").write_text(json.dumps(metadata, ensure_ascii=False, indent=2), encoding="utf-8")
    print(f"Exported {len(places)} named places")


if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", type=pathlib.Path, required=True)
    export(parser.parse_args().output)
