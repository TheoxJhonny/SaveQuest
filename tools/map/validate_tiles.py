import gzip, json, sys
from pathlib import Path
import mapbox_vector_tile
from pmtiles.reader import Reader, MemorySource, all_tiles
raw = Path(sys.argv[1]).read_bytes()
source = MemorySource(raw)
reader = Reader(source)
seen = set(); kinds = set(); classes = set(); count = 0
original = json.loads(Path(sys.argv[2]).read_bytes())['features']
for (z,x,y), tile in all_tiles(source):
    decoded = mapbox_vector_tile.decode(gzip.decompress(tile))['santiago']['features']
    count += 1
    for f in decoded:
        p = f['properties']; prior = original[f['id']-1]['properties']
        assert all(p.get(k) == prior.get(k) for k in ('kind','class','name'))
        kinds.add(p.get('kind')); classes.add(p.get('class'))
        if z == 15: seen.add(f['id'])
assert len(seen) == len(original), (len(seen),len(original))
print(json.dumps({'tiles_decoded':count,'source_features_at_z15':len(seen),'kinds':sorted(kinds),'classes':sorted(str(c) for c in classes),'header':reader.header()},default=str))
