"""Generate Santiago MVT/PMTiles. pip install -r requirements-tiles.txt"""
import gzip, hashlib, json, math, sys, time
from pathlib import Path
import mapbox_vector_tile
from shapely.geometry import shape, box
from shapely.ops import transform
from shapely import STRtree, make_valid
from pmtiles.writer import write
from pmtiles.tile import Compression, TileType, zxy_to_tileid

def project(x, y, z=None):
    return ((x + 180) / 360, (1 - math.asinh(math.tan(math.radians(y))) / math.pi) / 2)

def main(source, target):
    started = time.monotonic()
    raw = Path(source).read_bytes()
    features = json.loads(raw)['features']
    geometries = [transform(project, make_valid(shape(f['geometry']))) for f in features]
    tree = STRtree(geometries)
    bounds = (-70.85, -33.65, -70.45, -33.25)
    west, south = project(bounds[0], bounds[1]); east, north = project(bounds[2], bounds[3])
    tiles = []
    for zoom in range(10, 16):
        n = 2 ** zoom
        for x in range(int(west*n), int(east*n)+1):
            for y in range(int(north*n), int(south*n)+1):
                tiles.append((zxy_to_tileid(zoom,x,y), zoom,x,y))
    count = 0
    with write(target) as writer:
        for tile_id, zoom,x,y in sorted(tiles):
            n = 2 ** zoom
            # Eight pixels of overlap prevents seams for thick strokes at tile edges.
            pad = 8 / (256*n)
            clip = box(x/n-pad,y/n-pad,(x+1)/n+pad,(y+1)/n+pad)
            rows = []
            for index in sorted(tree.query(clip)):
                geom = geometries[index].intersection(clip)
                if geom.is_empty: continue
                # <= 1/8 screen pixel; finest zoom retains source detail.
                geom = geom.simplify(0 if zoom == 15 else 0.125/(256*n), preserve_topology=True)
                if geom.geom_type not in ('LineString','MultiLineString','Polygon','MultiPolygon'): continue
                rows.append({'geometry': geom, 'properties': features[index]['properties'], 'id': int(index)+1})
            if rows:
                data = mapbox_vector_tile.encode({'name':'santiago','features':rows}, default_options={'quantize_bounds':(x/n,y/n,(x+1)/n,(y+1)/n),'y_coord_down':True,'extents':16384})
                writer.write_tile(tile_id, gzip.compress(data, mtime=0))
                count += 1
            if count % 100 == 0: print('tiles',count,flush=True)
        writer.finalize({'tile_compression':Compression.GZIP,'tile_type':TileType.MVT,
            'min_lon_e7':int(bounds[0]*1e7),'min_lat_e7':int(bounds[1]*1e7),
            'max_lon_e7':int(bounds[2]*1e7),'max_lat_e7':int(bounds[3]*1e7),
            'center_zoom':12,'center_lon_e7':-706693000,'center_lat_e7':-334489000},
            {'name':'SaveQuest Santiago','attribution':'© OpenStreetMap contributors · ODbL',
             'source_sha256':hashlib.sha256(raw).hexdigest(),
             'vector_layers':[{'id':'santiago','fields':{'kind':'String','class':'String','name':'String'},'minzoom':10,'maxzoom':15}]})
    print(json.dumps({'source_bytes':len(raw),'features':len(features),'tiles':count,'pmtiles_bytes':Path(target).stat().st_size,'seconds':round(time.monotonic()-started,2)}),flush=True)

if __name__ == '__main__': main(*sys.argv[1:])
