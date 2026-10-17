"""Rebuild the bundled OSM extract; run manually, never from the Android app.
Data: OpenStreetMap contributors, ODbL 1.0. No public raster tiles downloaded.
"""
import json, pathlib, urllib.request, urllib.parse, datetime

BBOX = '-33.65,-70.85,-33.25,-70.45'
QUERY = f'''[out:json][timeout:180];(
way[highway~"^(motorway|trunk|primary|secondary|tertiary|residential|unclassified|living_street|pedestrian)(_link)?$"]({BBOX});
way[waterway~"^(river|canal)$"]({BBOX});
way[leisure=park]({BBOX});
);out geom;'''
out = pathlib.Path(__file__).parent
request = urllib.request.Request('https://overpass-api.de/api/interpreter',
    data=urllib.parse.urlencode({'data': QUERY}).encode(),
    headers={'User-Agent': 'SaveQuest-educational-offline-map/1.0'})
with urllib.request.urlopen(request, timeout=240) as response:
    data = json.load(response)
if data.get('remark'):
    raise RuntimeError(data['remark'])
features = []
for element in data['elements']:
    tags = element.get('tags', {})
    coords = [[round(p['lon'], 6), round(p['lat'], 6)] for p in element.get('geometry', [])]
    if len(coords) < 2:
        continue
    kind = 'road' if 'highway' in tags else 'water' if 'waterway' in tags else 'park'
    polygon = kind == 'park' and len(coords) >= 4 and coords[0] == coords[-1]
    if kind == 'park' and not polygon:
        continue
    features.append({'type': 'Feature', 'id': element['id'],
        'properties': {'kind': kind, 'name': tags.get('name', ''), 'class': tags.get('highway', '')},
        'geometry': {'type': 'Polygon' if polygon else 'LineString',
                     'coordinates': [coords] if polygon else coords}})
assert len(features) > 1000, 'Incomplete extract'
(out / 'santiago.geojson').write_text(json.dumps({'type':'FeatureCollection','features':features}, ensure_ascii=False, separators=(',', ':')), encoding='utf-8')
(out / 'source.json').write_text(json.dumps({'source':'https://www.openstreetmap.org/copyright',
    'license':'ODbL-1.0', 'attribution':'© OpenStreetMap contributors',
    'bbox':BBOX, 'downloadedAt':datetime.datetime.now(datetime.timezone.utc).isoformat(),
    'osmTimestamp':data['osm3s']['timestamp_osm_base'], 'features':len(features),
    'query':QUERY}, ensure_ascii=False, indent=2), encoding='utf-8')
print(f'Exported {len(features)} features')
